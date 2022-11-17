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
package io.seata.spring.boot.autoconfigure.provider;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import io.seata.common.holder.ObjectHolder;
import io.seata.config.Configuration;
import io.seata.config.ExtConfigurationProvider;
import org.apache.commons.lang.StringUtils;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.context.ApplicationContext;


import static io.seata.common.util.StringFormatUtils.DOT;
import static io.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_MAP;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SEATA_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVICE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SPECIAL_KEY_GROUPLIST;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SPECIAL_KEY_VGROUP_MAPPING;

/**
 * @author xingfudeshi@gmail.com
 */
public class SpringBootConfigurationProvider implements ExtConfigurationProvider {
    private static final String INTERCEPT_METHOD_PREFIX = "get";

    @Override
    public Configuration provide(Configuration originalConfiguration) {
        return (Configuration) Enhancer.create(originalConfiguration.getClass(), new MethodInterceptor() {
            @Override
            public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy)
                throws Throwable {
                // spring-boot-starter  配置文件的扩展、增强, 作用是直接覆盖 registry.conf file.conf 中的配置，
                // 所以真正起作用的配置是在 application.properties 或者 application.yaml 文件中，不配置则用配置文件中的默认值
                // 示例 https://github.com/seata/seata/blob/master/script/client/spring/application.properties
                if (method.getName().startsWith(INTERCEPT_METHOD_PREFIX) && args.length > 0) {
                    Object result = null;
                    String rawDataId = (String) args[0];
                    if (args.length == 1) {
                        result = get(convertDataId(rawDataId));
                    } else if (args.length == 2) {
                        result = get(convertDataId(rawDataId), args[1]);
                    } else if (args.length == 3) {
                        result = get(convertDataId(rawDataId), args[1], (Long) args[2]);
                    }
                    if (result != null) {
                        //If the return type is String,need to convert the object to string
                        if (method.getReturnType().equals(String.class)) {
                            return String.valueOf(result);
                        }
                        return result;
                    }
                }

                return method.invoke(originalConfiguration, args);
            }
        });
    }

    private Object get(String dataId, Object defaultValue, long timeoutMills) throws IllegalAccessException {
        return get(dataId, defaultValue);

    }

    private Object get(String dataId, Object defaultValue) throws IllegalAccessException {
        Object result = get(dataId);
        if (result == null) {
            return defaultValue;
        }
        return result;
    }

    private Object get(String dataId) throws IllegalAccessException {
        String propertySuffix = getPropertySuffix(dataId);
        // 根据前缀获取对应的配置文件
        Class propertyClass = getPropertyClass(getPropertyPrefix(dataId));
        if (propertyClass != null) {
            Object propertyObject = ObjectHolder.INSTANCE.getObject(ApplicationContext.class).getBean(propertyClass);
            // 根据后缀获取对应配置
            Optional<Field> fieldOptional = Stream.of(propertyObject.getClass().getDeclaredFields()).filter(
                f -> f.getName().equalsIgnoreCase(propertySuffix)).findAny();
            if (fieldOptional.isPresent()) {
                Field field = fieldOptional.get();
                field.setAccessible(true);
                Object valueObject = field.get(propertyObject);
                if (valueObject instanceof Map) {
                    String key = StringUtils.substringAfterLast(dataId, String.valueOf(DOT));
                    valueObject = ((Map) valueObject).get(key);
                }
                return valueObject;
            }
        }
        return null;
    }

    /**
     * convert data id
     *
     * https://seata.io/zh-cn/docs/user/configurations.html
     * 由于spring-boot本身配置文件语法的要求,这个地方需要将file.conf中的default.grouplist写成grouplist.default,效果是一样的.
     *
     * default 是可变的 所以放后面
     *
     * @param rawDataId
     * @return dataId
     */
    private String convertDataId(String rawDataId) {
        if (rawDataId.endsWith(SPECIAL_KEY_GROUPLIST)) {// grouplist
            String suffix = StringUtils.removeEnd(rawDataId, DOT + SPECIAL_KEY_GROUPLIST);
            //change the format of default.grouplist to grouplist.default
            //修改前后顺序格式
            return SERVICE_PREFIX + DOT + SPECIAL_KEY_GROUPLIST + DOT + suffix;
        }
        return SEATA_PREFIX + DOT + rawDataId;// 拼接 seata.
    }

    /**
     * Get property prefix
     *
     * @param dataId
     * @return propertyPrefix
     */
    private String getPropertyPrefix(String dataId) {
        if (dataId.contains(SPECIAL_KEY_VGROUP_MAPPING)) {// vgroupMapping
            return SERVICE_PREFIX;// seata.service
        }
        if (dataId.contains(SPECIAL_KEY_GROUPLIST)) {// grouplist
            return SERVICE_PREFIX;
        }
        // 截取前缀，排除最后一个 . 后面的字符
        return StringUtils.substringBeforeLast(dataId, String.valueOf(DOT));
    }

    /**
     * Get property suffix
     *
     * @param dataId
     * @return propertySuffix
     */
    private String getPropertySuffix(String dataId) {
        if (dataId.contains(SPECIAL_KEY_VGROUP_MAPPING)) {
            return SPECIAL_KEY_VGROUP_MAPPING;
        }
        if (dataId.contains(SPECIAL_KEY_GROUPLIST)) {
            return SPECIAL_KEY_GROUPLIST;
        }
        // 截取后缀，保留最后一个 . 后面的字符
        return StringUtils.substringAfterLast(dataId, String.valueOf(DOT));
    }

    /**
     * Get property class
     *
     * @param propertyPrefix
     * @return propertyClass
     */
    private Class getPropertyClass(String propertyPrefix) {
        return PROPERTY_MAP.entrySet().stream()
            .filter(e -> propertyPrefix.equals(e.getKey()))
            .findAny()
            .map(Map.Entry::getValue)
            .orElse(null);
    }
}
