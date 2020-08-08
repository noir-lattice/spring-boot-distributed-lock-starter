package com.noir.common.lock.impl.locks;


import com.noir.common.lock.ReentrantDLock;
import lombok.SneakyThrows;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * redisson red lock wrapper
 *
 * 封装支持重入
 */
public class RedLockWrapper extends ReentrantDLock {
    private static final Long DEFAULT_TIMEOUT_SECONDS = 30L;

    private static final Logger log = LoggerFactory.getLogger(RedLockWrapper.class);

    private final String lockName;
    private final RedissonRedLock redissonRedLock;

    public RedLockWrapper(String lockName, RLock... locks) {
        this.lockName = lockName;
        this.redissonRedLock = new RedissonRedLock(locks);
    }

    @Override
    public void lock() {
        while (!tryLock()) {
            // pass
            log.info("retry get dLock");
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        // pass
    }

    @Override
    @SneakyThrows
    public boolean tryLock() {
        return tryLock(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public boolean tryLock(long l, TimeUnit timeUnit) throws InterruptedException {
        if (isEntered(lockName)) {
            return true;
        }

        boolean locked = redissonRedLock.tryLock(l, timeUnit);

        if (locked) {
            enter(lockName);
        }
        return locked;
    }

    @Override
    public void unlock() {
        exit(lockName);
        redissonRedLock.unlock();
    }

    @Override
    public Condition newCondition() {
        // pass
        return null;
    }

}
