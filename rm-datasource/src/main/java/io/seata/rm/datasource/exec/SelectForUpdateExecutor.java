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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.rm.datasource.StatementProxy;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLSelectRecognizer;
import io.seata.rm.datasource.sql.struct.TableRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Select for update executor.
 *
 * @param <S> the type parameter
 * @author sharajava
 */
public class SelectForUpdateExecutor<T, S extends Statement> extends BaseTransactionalExecutor<T, S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SelectForUpdateExecutor.class);

    /**
     * Instantiates a new Select for update executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public SelectForUpdateExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback,
                                   SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    public T doExecute(Object... args) throws Throwable {
        Connection conn = statementProxy.getConnection();
        DatabaseMetaData dbmd = conn.getMetaData();
        T rs;
        Savepoint sp = null;
        LockRetryController lockRetryController = new LockRetryController();
        boolean originalAutoCommit = conn.getAutoCommit();
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String selectPKSQL = buildSelectSQL(paramAppenderList);
        try {
            if (originalAutoCommit) {
                /*
                 * In order to hold the local db lock during global lock checking
                 * set auto commit value to false first if original auto commit was true
                 */
                conn.setAutoCommit(false);
            } else if (dbmd.supportsSavepoints()) {
                /*
                 * In order to release the local db lock when global lock conflict
                 * create a save point if original auto commit was false, then use the save point here to release db
                 * lock during global lock checking if necessary
                 */
                sp = conn.setSavepoint();
            } else {
                throw new SQLException("not support savepoint. please check your db version");
            }

            while (true) {
                try {
                    // #870
                    // execute return Boolean
                    // executeQuery return ResultSet
                    rs = statementCallback.execute(statementProxy.getTargetStatement(), args);

                    // Try to get global lock of those rows selected
                    TableRecords selectPKRows = buildTableRecords(getTableMeta(), selectPKSQL, paramAppenderList);
                    String lockKeys = buildLockKey(selectPKRows);
                    if (StringUtils.isNullOrEmpty(lockKeys)) {
                        break;
                    }

                    if (RootContext.inGlobalTransaction()) {
                        // https://seata.apache.org/zh-cn/docs/v1.3/dev/mode/at-mode

                        // SELECT...FOR UPDATE 申请全局锁，如果别人持有全局锁，则需要等别人释放锁后才能继续，否则阻塞等待，有点类似串行化，性能有问题
                        // 不加锁的话，属于读未提交，就是读全局未提交的意思，比如，全局事务还没提交，其中一个本地事务已经提交，
                        // 别人一个简单的查询，就能查询到这个提交的本地事务，如果加锁了，就会阻塞等待

                        // 并发 写 - 读      隔离
                        // 并发 写 - 写      隔离

                        // 例子：流程状态和发货通知单，发货通知单状态改了，流程状态还没改，别人就查询发货通知单，会发现流程状态和发货通知单状态不一致
                        //do as usual
                        LOGGER.info("SELECT...FOR UPDATE 语句，申请全局锁，保证全局读已提交");
                        statementProxy.getConnectionProxy().checkLock(lockKeys);
                    } else if (RootContext.requireGlobalLock()) {
                        //check lock key before commit just like DML to avoid reentrant lock problem(no xid thus can
                        // not reentrant)
                        statementProxy.getConnectionProxy().appendLockKey(lockKeys);
                    } else {
                        throw new RuntimeException("Unknown situation!");
                    }
                    break;
                } catch (LockConflictException lce) {
                    if (sp != null) {
                        conn.rollback(sp);
                    } else {
                        // 在MySQL中，当事务提交或回滚时，SELECT ... FOR UPDATE 语句所获取的锁会被释放
                        LOGGER.info("回滚事务，SELECT...FOR UPDATE 语句所获取的锁会被释放");
                        conn.rollback();
                    }
                    lockRetryController.sleep(lce);
                }
            }
        } finally {
            if (sp != null) {
                try {
                    conn.releaseSavepoint(sp);
                } catch (SQLException e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("{} does not support release save point, but this is not a error.", getDbType());
                    }
                }
            }
            if (originalAutoCommit) {
                conn.setAutoCommit(true);
            }
        }
        return rs;
    }

    private String buildSelectSQL(ArrayList<List<Object>> paramAppenderList) {
        SQLSelectRecognizer recognizer = (SQLSelectRecognizer)sqlRecognizer;
        StringBuilder selectSQLAppender = new StringBuilder("SELECT ");
        selectSQLAppender.append(getColumnNamesInSQL(getTableMeta().getEscapePkNameList(getDbType())));
        selectSQLAppender.append(" FROM ").append(getFromTableInSQL());
        String whereCondition = buildWhereCondition(recognizer, paramAppenderList);
        if (StringUtils.isNotBlank(whereCondition)) {
            selectSQLAppender.append(" WHERE ").append(whereCondition);
        }
        selectSQLAppender.append(" FOR UPDATE");
        return selectSQLAppender.toString();
    }
}
