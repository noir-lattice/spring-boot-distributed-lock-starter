package com.noir.common.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

/**
 * 可重入DLock拓展
 *
 * 使用thread local实现锁状态标定，如拓展实现
 * 自定义的锁实现，请务必在解锁后 {@link ReentrantDLock#exit(String)}
 * 以保证状态的清除
 *
 * 提供的四种实现
 * @see com.noir.common.lock.impl.locks.RedLockWrapper
 * @see com.noir.common.lock.impl.locks.RedisSetNXGetSetLock
 * @see com.noir.common.lock.impl.locks.RedisSetNXExpireLock
 * @see com.noir.common.lock.impl.locks.ZookeeperLock
 */
public abstract class ReentrantDLock implements Lock {
    private ThreadLocal<List<String>> localLocks = new ThreadLocal<>();

    protected boolean isEntered(String lockName) {
        List<String> locks = localLocks.get();
        if (Objects.isNull(locks)) {
            return false;
        }
        return locks.contains(lockName);
    }

    protected void enter(String lockName) {
        List<String> locks = localLocks.get();
        if (Objects.isNull(locks)) {
            localLocks.set(new ArrayList<>());
            locks = localLocks.get();
        }
        locks.add(lockName);
    }

    protected void exit(String lockName) {
        List<String> locks = localLocks.get();
        if (Objects.nonNull(locks)) {
            locks.remove(lockName);
        }
    }
}