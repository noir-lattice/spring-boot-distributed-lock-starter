package com.noir.common.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

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
        locks.remove(lockName);
    }
}