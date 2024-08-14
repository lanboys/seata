/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.datasource.exec;


import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.Callable;

import io.seata.common.exception.NotSupportYetException;
import io.seata.rm.datasource.AbstractConnectionProxy;
import io.seata.rm.datasource.ConnectionContext;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Abstract dml base executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 * @author sharajava
 */
public abstract class AbstractDMLBaseExecutor<T, S extends Statement> extends BaseTransactionalExecutor<T, S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDMLBaseExecutor.class);

    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public AbstractDMLBaseExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback,
                                   SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    /**
     * Instantiates a new Base transactional executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizers     the multi sql recognizer
     */
    public AbstractDMLBaseExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback,
                                   List<SQLRecognizer> sqlRecognizers) {
        super(statementProxy, statementCallback, sqlRecognizers);
    }

    @Override
    public T doExecute(Object... args) throws Throwable {
        AbstractConnectionProxy connectionProxy = statementProxy.getConnectionProxy();
        // 手动开启事务，必须要手动提交事务
        // 不手动开启事务，会自动开启事务，但是是否自动提交事务，取决于数据库的 autocommit 设置
        // MySQL 默认开启事务自动提交模式，即除了显式的开启事务（BEGIN 或 START TRANSACTION），否则每条 SQL 语句都会被当做一个单独的事务自动执行。
        // show variables like 'autocommit';

        // 如果不加事务注解 @Transactional，那么方法就没有拦截器，不会走 AbstractPlatformTransactionManager（会修改数据库默认设置），
        // 自动提交就是数据库默认设置，通常是 true
        LOGGER.info("是否自动提交：{}", connectionProxy.getAutoCommit());

        if (connectionProxy.getAutoCommit()) {
            // 1. 数据库设置为 true，并且没有加注解 @Transactional
            LOGGER.info("执行自动提交方法");
            return executeAutoCommitTrue(args);
        } else {
            // 1. 数据库设置就是 false，是否加注解走到这里都会是 false
            // 2. 数据库设置为 true，但是加了注解 @Transactional，会临时修改为 false
            return executeAutoCommitFalse(args);
        }
    }

    /**
     * Execute auto commit false t.
     *
     * @param args the args
     * @return the t
     * @throws Exception the exception
     */
    protected T executeAutoCommitFalse(Object[] args) throws Exception {
        if (!JdbcConstants.MYSQL.equalsIgnoreCase(getDbType()) && getTableMeta().getPrimaryKeyOnlyName().size() > 1)
        {
            throw new NotSupportYetException("multi pk only support mysql!");
        }
        TableRecords beforeImage = beforeImage();
        T result = statementCallback.execute(statementProxy.getTargetStatement(), args);
        TableRecords afterImage = afterImage(beforeImage);
        prepareUndoLog(beforeImage, afterImage);
        return result;
    }

    /**
     * Execute auto commit true t.
     *
     * @param args the args
     * @return the t
     * @throws Throwable the throwable
     */
    protected T executeAutoCommitTrue(Object[] args) throws Throwable {
        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();
        try {
            // 能不能不修改 为 false ? 直接让数据库自动提交，不行，这样的话 undo_log 就不再是同一个事务了
            // 1. 改 autoCommit 为 false
            connectionProxy.setAutoCommit(false);
            return new LockRetryPolicy(connectionProxy).execute(() -> {
                T result = executeAutoCommitFalse(args);
                // 2. 手动提交
                connectionProxy.commit();
                return result;
            });
        } catch (Exception e) {
            // when exception occur in finally,this exception will lost, so just print it here
            LOGGER.error("execute executeAutoCommitTrue error:{}", e.getMessage(), e);
            if (!LockRetryPolicy.isLockRetryPolicyBranchRollbackOnConflict()) {
                connectionProxy.getTargetConnection().rollback();
            }
            throw e;
        } finally {
            connectionProxy.getContext().reset();
            // 3. 恢复 autoCommit 为 true
            connectionProxy.setAutoCommit(true);
        }
    }

    /**
     * Before image table records.
     *
     * @return the table records
     * @throws SQLException the sql exception
     */
    protected abstract TableRecords beforeImage() throws SQLException;

    /**
     * After image table records.
     *
     * @param beforeImage the before image
     * @return the table records
     * @throws SQLException the sql exception
     */
    protected abstract TableRecords afterImage(TableRecords beforeImage) throws SQLException;

    private static class LockRetryPolicy extends ConnectionProxy.LockRetryPolicy {
        private final ConnectionProxy connection;

        LockRetryPolicy(final ConnectionProxy connection) {
            this.connection = connection;
        }

        @Override
        public <T> T execute(Callable<T> callable) throws Exception {
            if (LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT) {
                return doRetryOnLockConflict(callable);
            } else {
                return callable.call();
            }
        }

        @Override
        protected void onException(Exception e) throws Exception {
            ConnectionContext context = connection.getContext();
            //UndoItems can't use the Set collection class to prevent ABA
            context.getUndoItems().clear();
            context.getLockKeysBuffer().clear();
            connection.getTargetConnection().rollback();
        }

        public static boolean isLockRetryPolicyBranchRollbackOnConflict() {
            return LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT;
        }
    }
}
