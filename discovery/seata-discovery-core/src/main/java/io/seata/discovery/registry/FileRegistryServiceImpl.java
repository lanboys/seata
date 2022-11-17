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
package io.seata.discovery.registry;


import io.seata.common.util.StringUtils;
import io.seata.config.ConfigChangeListener;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * The type File registry service.
 *
 * @author slievrly
 */
public class FileRegistryServiceImpl implements RegistryService<ConfigChangeListener> {
    private static volatile FileRegistryServiceImpl instance;
    private static final Configuration CONFIG = ConfigurationFactory.getInstance();
    private static final String POSTFIX_GROUPLIST = ".grouplist";
    private static final String ENDPOINT_SPLIT_CHAR = ";";
    private static final String IP_PORT_SPLIT_CHAR = ":";

    private FileRegistryServiceImpl() {
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    static FileRegistryServiceImpl getInstance() {
        if (instance == null) {
            synchronized (FileRegistryServiceImpl.class) {
                if (instance == null) {
                    instance = new FileRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(InetSocketAddress address) throws Exception {

    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {

    }

    @Override
    public void subscribe(String cluster, ConfigChangeListener listener) throws Exception {

    }

    @Override
    public void unsubscribe(String cluster, ConfigChangeListener listener) throws Exception {

    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        String clusterName = getServiceGroup(key);
        if (clusterName == null) {
            return null;
        }
        // https://seata.io/zh-cn/docs/user/configurations.html
        // registry.type=file或config.type=file 设计的初衷是让用户在不依赖第三方注册中心或配置中心的前提下，通过直连的方式，快速验证seata服务。

        // 如果配置文件这么配置 seata.config.type=nacos, seata.registry.type=file
        // 下面 CONFIG，取的是 NacosConfiguration 实例

        // 根据集群名字来获取 grouplist , 默认的是 service.default.grouplist
        // 如：service.springboot_http_cluster_a.grouplist

        // 模拟第三方注册中心，从注册中心获取具体的服务地址
        String endpointStr = CONFIG.getConfig(
            PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + clusterName + POSTFIX_GROUPLIST);
        if (StringUtils.isNullOrEmpty(endpointStr)) {
            throw new IllegalArgumentException(clusterName + POSTFIX_GROUPLIST + " is required");
        }
        String[] endpoints = endpointStr.split(ENDPOINT_SPLIT_CHAR);
        List<InetSocketAddress> inetSocketAddresses = new ArrayList<>();
        for (String endpoint : endpoints) {
            String[] ipAndPort = endpoint.split(IP_PORT_SPLIT_CHAR);
            if (ipAndPort.length != 2) {
                throw new IllegalArgumentException("endpoint format should like ip:port");
            }
            inetSocketAddresses.add(new InetSocketAddress(ipAndPort[0], Integer.parseInt(ipAndPort[1])));
        }
        return inetSocketAddresses;
    }

    @Override
    public void close() throws Exception {

    }
}
