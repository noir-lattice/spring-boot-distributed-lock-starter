package com.noir.common.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public interface DLockFactory {

    /**
     * 获取锁
     *
     * @param name
     * @return
     */
    public Lock getLock(String name);

    /**
     * 获取锁
     *
     * @param name
     * @param expire
     * @param unit
     * @return
     */
    public Lock getLock(String name, long expire, TimeUnit unit);

}