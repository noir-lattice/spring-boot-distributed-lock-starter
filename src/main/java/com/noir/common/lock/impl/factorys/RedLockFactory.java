package com.noir.common.lock.impl.factorys;

import com.noir.common.lock.DLockFactory;
import com.noir.common.lock.ReentrantDLock;
import com.noir.common.lock.impl.locks.RedLockWrapper;
import com.noir.common.lock.impl.locks.RedisSetNXExpireLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * red lock factory
 *
 * {@link RedLockWrapper}的工厂，这里仅
 * 做匹配locker.type来提供IoC容器中的{@link RedLockFactory}
 */
@Component
@ConditionalOnProperty(prefix = "locker", value = "type", havingValue = "red-lock")
public class RedLockFactory implements DLockFactory {

    @Autowired
    private List<RedissonClient> clients;

    /**
     * 获取锁
     *
     * @param name 资源名称
     * @return ReentrantDLock
     */
    public Lock getLock(String name) {
        RLock[] rLocks = clients.stream().map(client -> client.getLock(getLockKey(name))).toArray(RLock[]::new);
        return new RedLockWrapper(name, rLocks);
    }

    /**
     * 获取锁
     *
     * @param name 资源名称
     * @param expire 过期时间
     * @param unit 时间单位
     * @return ReentrantDLock
     */
    public Lock getLock(String name, long expire, TimeUnit unit) {
        RLock[] rLocks = clients.stream().map(client -> client.getLock(getLockKey(name))).toArray(RLock[]::new);
        return new RedLockWrapper(name, rLocks);
    }


    /**
     * 获取拼接后的资源名称
     *
     * @return str
     */
    private String getLockKey(String name) {
        String nameSpace = "distributed:lock:";
        return nameSpace + ":" + name;
    }
}
