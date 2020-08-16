package com.noir.common;

import com.noir.common.lock.properties.DLockProperties;
import com.noir.common.lock.properties.RedLockProperties;
import com.noir.common.lock.properties.RedisDLockProperties;
import com.noir.common.lock.properties.ZookeeperDLockProperties;
import lombok.extern.slf4j.Slf4j;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * starter配置启动类
 *
 * 扫描了 {@link com.noir.common.lock} 下的
 * 所有类进行初始化，包含aop，对应配置的工程类，
 * 对应中间件配置以及封装使用的service
 *
 * 仅在 {@code locker.enabled=true} 时有效
 *
 * 具体实现参考接口及拓展类
 * @see com.noir.common.lock.DLockFactory
 * @see com.noir.common.lock.LockableService
 * @see com.noir.common.lock.ReentrantDLock
 */
@Slf4j
@Configuration
@ComponentScan("com.noir.common.lock")
@ConditionalOnProperty(prefix = "locker", value = "enabled", havingValue = "true")
@EnableConfigurationProperties({DLockProperties.class, RedisDLockProperties.class, RedLockProperties.class, ZookeeperDLockProperties.class})
public class LockStarterAutoConfiguration {
    @Autowired(required = false)
    private RedisDLockProperties redisDLockProperties;

    @Autowired(required = false)
    private RedLockProperties redLockProperties;

    @Autowired(required = false)
    private ZookeeperDLockProperties zookeeperDLockProperties;

    @Bean
    @ConditionalOnProperty(prefix = "locker", value = "type", havingValue = "redis-expire")
    public RedissonClient redissonEXClient() {
        return createRedissonClient(redisDLockProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "locker", value = "type", havingValue = "redis-get-set")
    public RedissonClient redissonGetSetClient() {
        return createRedissonClient(redisDLockProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "locker", value = "type", havingValue = "red-lock")
    public List<RedissonClient> redissonRedLockClients() {
        return redLockProperties.getClients().stream().map(this::createRedissonClient).collect(Collectors.toList());
    }

    @Bean
    @ConditionalOnProperty(prefix = "locker", value = "type", havingValue = "zookeeper")
    public ZooKeeper zooKeeper() {
        return createZooKeeper(zookeeperDLockProperties);
    }

    private ZooKeeper createZooKeeper(ZookeeperDLockProperties zookeeperDLockProperties) {
        ZooKeeper zooKeeper = null;
        try {
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            zooKeeper = new ZooKeeper(zookeeperDLockProperties.getAddress(), zookeeperDLockProperties.getTimeout(), event -> {
                if(Watcher.Event.KeeperState.SyncConnected==event.getState()){
                    //如果收到了服务端的响应事件,连接成功
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await();
            log.info("【初始化ZooKeeper连接状态....】={}", zooKeeper.getState());
        } catch (Exception e){
            e.printStackTrace();
            log.error("【初始化ZooKeeper连接异常....】= {}", e.getMessage());
        }
        return zooKeeper;
    }

    private RedissonClient createRedissonClient(RedisDLockProperties redisDLockProperties) {
        switch (redisDLockProperties.getMode()) {
            case "single":
                return redissonSingle(redisDLockProperties);
            case "cluster":
                return redissonCluster(redisDLockProperties);
            case "sentinel":
                return redissonSentinel(redisDLockProperties);
        }
        return null;
    }

    /**
     * 单机模式 redisson 客户端
     */
    private RedissonClient redissonSingle(RedisDLockProperties redisDLockProperties) {
        Config config = new Config();
        String node = redisDLockProperties.getSingle().getAddress();
        node = node.startsWith("redis://") ? node : "redis://" + node;
        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress(node)
                .setTimeout(redisDLockProperties.getSingle().getConnTimeout())
                .setConnectionPoolSize(redisDLockProperties.getPool().getSize())
                .setConnectionMinimumIdleSize(redisDLockProperties.getPool().getMinIdle());
        if (!StringUtils.isEmpty(redisDLockProperties.getPassword())) {
            serverConfig.setPassword(redisDLockProperties.getPassword());
        }
        return Redisson.create(config);
    }


    /**
     * 集群模式的 redisson 客户端
     */
    private RedissonClient redissonCluster(RedisDLockProperties redisDLockProperties) {
        System.out.println("cluster redisProperties:" + redisDLockProperties.getCluster());

        Config config = new Config();
        String[] nodes = extractRedisAddressByString(redisDLockProperties.getCluster().getNodes());

        ClusterServersConfig serverConfig = config.useClusterServers()
                .addNodeAddress(nodes)
                .setScanInterval(
                        redisDLockProperties.getCluster().getScanInterval())
                .setIdleConnectionTimeout(
                        redisDLockProperties.getPool().getSoTimeout())
                .setConnectTimeout(
                        redisDLockProperties.getPool().getConnTimeout())
                .setFailedAttempts(
                        redisDLockProperties.getCluster().getFailedAttempts())
                .setRetryAttempts(
                        redisDLockProperties.getCluster().getRetryAttempts())
                .setRetryInterval(
                        redisDLockProperties.getCluster().getRetryInterval())
                .setMasterConnectionPoolSize(redisDLockProperties.getCluster()
                        .getMasterConnectionPoolSize())
                .setSlaveConnectionPoolSize(redisDLockProperties.getCluster()
                        .getSlaveConnectionPoolSize())
                .setTimeout(redisDLockProperties.getTimeout());
        if (!StringUtils.isEmpty(redisDLockProperties.getPassword())) {
            serverConfig.setPassword(redisDLockProperties.getPassword());
        }
        return Redisson.create(config);
    }

    /**
     * 哨兵模式 redisson 客户端
     */
    private RedissonClient redissonSentinel(RedisDLockProperties redisDLockProperties) {
        System.out.println("sentinel redisProperties:" + redisDLockProperties.getSentinel());
        Config config = new Config();

        String[] nodes = extractRedisAddressByString(redisDLockProperties.getSentinel().getNodes());

        SentinelServersConfig serverConfig = config.useSentinelServers()
                .addSentinelAddress(nodes)
                .setMasterName(redisDLockProperties.getSentinel().getMaster())
                .setReadMode(ReadMode.SLAVE)
                .setFailedAttempts(redisDLockProperties.getSentinel().getFailMax())
                .setTimeout(redisDLockProperties.getTimeout())
                .setMasterConnectionPoolSize(redisDLockProperties.getPool().getMasterConnectionPoolSize())
                .setSlaveConnectionPoolSize(redisDLockProperties.getPool().getSlaveConnectionPoolSize());

        if (!StringUtils.isEmpty(redisDLockProperties.getPassword())) {
            serverConfig.setPassword(redisDLockProperties.getPassword());
        }

        return Redisson.create(config);
    }

    private String[] extractRedisAddressByString(String nodeStr) {
        String[] nodes = nodeStr.split(",");
        return Arrays.stream(nodes)
                        .map(str -> str.startsWith("redis://") ? str : "redis://" + str)
                        .toArray(String[]::new);
    }
}
