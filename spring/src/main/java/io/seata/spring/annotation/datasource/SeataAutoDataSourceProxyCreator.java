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
package io.seata.spring.annotation.datasource;

import javax.sql.DataSource;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.beans.BeansException;

/**
 * @author xingfudeshi@gmail.com
 */
public class SeataAutoDataSourceProxyCreator extends AbstractAutoProxyCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataAutoDataSourceProxyCreator.class);
    private final String[] excludes;
    private final Advisor advisor = new DefaultIntroductionAdvisor(new SeataAutoDataSourceProxyAdvice());

    public SeataAutoDataSourceProxyCreator(boolean useJdkProxy, String[] excludes) {
        this.excludes = excludes;
        setProxyTargetClass(!useJdkProxy);
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource customTargetSource) throws BeansException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("自动代理了系统的 DataSource Auto proxy of [{}]", beanName);
        }
        return new Object[]{advisor};
    }

    @Override
    protected boolean shouldSkip(Class<?> beanClass, String beanName) {
        // 自动代理DataSource
        return SeataProxy.class.isAssignableFrom(beanClass) || // SeataProxy子类应该跳过
            !DataSource.class.isAssignableFrom(beanClass) || // 不是DataSource子类应该跳过
            Arrays.asList(excludes).contains(beanClass.getName()); // DataSource子类，且在excludes中 应该跳过
    }
}
