package com.noir.common.lock.impl.factorys;

import com.noir.common.lock.DLockFactory;
import com.noir.common.lock.ReentrantDLock;
import com.noir.common.lock.impl.locks.RedisSetNXExpireLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * nx expire lock factory
 */
@Component
@ConditionalOnProperty(prefix = "locker", value = "type", havingValue = "redis-expire")
public class RedisSetNXExpireLockFactory implements DLockFactory {

    private final String nameSpace = "distributed:lock:";

    @Autowired
    private RedissonClient client;

    /**
     * 获取锁
     *
     * @param name 资源名称
     * @return ReentrantDLock
     */
    public ReentrantDLock getLock(String name) {
        return new RedisSetNXExpireLock(client, nameSpace, name);
    }

    /**
     * 获取锁
     *
     * @param name 资源名称
     * @param expire 过期时间
     * @param unit 时间单位
     * @return ReentrantDLock
     */
    public ReentrantDLock getLock(String name, long expire, TimeUnit unit) {
        return new RedisSetNXExpireLock(client, nameSpace, name, expire, unit);
    }
}

