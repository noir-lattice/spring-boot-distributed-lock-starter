package com.noir.common.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * DLock工厂
 *
 * 提供了DLock的工厂接口，为了更通用并可拓展，
 * 对 {@link ReentrantDLock} 进行了擦除，
 * 在进行具体自定义拓展时，建议继承ReentrantDLock
 * 以实现可重入支持。
 *
 * 具体工厂实现
 * @see com.noir.common.lock.impl.factorys.RedisSetNXExpireLockFactory
 * @see com.noir.common.lock.impl.factorys.RedisSetNXGetSetLockFactory
 * @see com.noir.common.lock.impl.factorys.RedLockFactory
 * @see com.noir.common.lock.impl.factorys.ZookeeperLockFactory
 */
public interface DLockFactory {

    /**
     * 获取锁
     *
     * @param name 锁名称
     * @return 锁对象
     */
    public Lock getLock(String name);

    /**
     * 获取锁
     *
     * @param name 锁名称
     * @param expire 过去时间
     * @param unit 时间单位
     * @return 锁对象
     */
    public Lock getLock(String name, long expire, TimeUnit unit);

}