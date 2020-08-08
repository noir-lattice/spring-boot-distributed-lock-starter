package com.noir.common.lock.impl;

import com.noir.common.lock.DLockFactory;
import com.noir.common.lock.LockableService;
import com.noir.common.lock.excptions.NotGetLocException;
import com.noir.common.lock.excptions.TryLockFailException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * 加锁逻辑的服务
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "locker", value = "enabled", havingValue = "true")
public class LockableServiceImpl implements LockableService {

    /**
     * 默认获取redis锁的等待时间(秒)
     */
    private static final long DEFAULT_ACQUIRE_LOCK_TIME_OUT = 30;

    /**
     * 默认的redis锁有效时间(分钟)
     */
    private static final long DEFAULT_LOCK_EXPIRED_TIME = 5;

    @Autowired
    private DLockFactory lockFactory;

    @Override
    public <T> T lockAndExecute(String key, Callable<T> callable) throws Exception {
        Lock lock = lockFactory.getLock(key, DEFAULT_LOCK_EXPIRED_TIME, TimeUnit.MINUTES);
        if (lock == null) {
            log.error("获取锁失败. key: {}", key);
            throw new NotGetLocException();
        }

        try {
            if (!lock.tryLock(DEFAULT_ACQUIRE_LOCK_TIME_OUT, TimeUnit.SECONDS)) {
                log.error("尝试加锁失败. key: {}", key);
                throw new TryLockFailException();
            }

            return callable.call();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void lockAndExecute(String key, Runnable runnable) throws Exception {
        Lock lock = lockFactory.getLock(key, DEFAULT_LOCK_EXPIRED_TIME, TimeUnit.MINUTES);
        if (lock == null) {
            log.error("获取锁失败. key: {}", key);
            throw new NotGetLocException();
        }

        try {
            if (!lock.tryLock(DEFAULT_ACQUIRE_LOCK_TIME_OUT, TimeUnit.SECONDS)) {
                log.error("尝试加锁失败. key: {}", key);
                throw new TryLockFailException();
            }

            runnable.run();
        } finally {
            lock.unlock();
        }
    }
}
