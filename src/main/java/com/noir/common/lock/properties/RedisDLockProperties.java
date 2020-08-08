package com.noir.common.lock.properties;

import com.noir.common.lock.properties.redis.RedisClusterProperties;
import com.noir.common.lock.properties.redis.RedisPoolProperties;
import com.noir.common.lock.properties.redis.RedisSentinelProperties;
import com.noir.common.lock.properties.redis.RedisSingleProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "locker.redis")
public class RedisDLockProperties {
    private int database;

    /**
     * 等待节点回复命令的时间。该时间从命令发送成功时开始计时
     */
    private int timeout;

    private String password;

    private String mode;

    /**
     * 池配置
     */
    private RedisPoolProperties pool;

    /**
     * 单机信息配置
     */
    private RedisSingleProperties single;

    /**
     * 集群 信息配置
     */
    private RedisClusterProperties cluster;

    /**
     * 哨兵配置
     */
    private RedisSentinelProperties sentinel;
}
