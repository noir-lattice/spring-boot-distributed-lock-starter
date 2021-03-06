package com.noir.common.lock.properties.redis;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class RedisPoolProperties {

    private int maxIdle;

    private int minIdle;

    private int maxActive;

    private int maxWait;

    private int connTimeout;

    private int soTimeout;

    private int size;

    private int masterConnectionPoolSize;

    private int slaveConnectionPoolSize;
}
