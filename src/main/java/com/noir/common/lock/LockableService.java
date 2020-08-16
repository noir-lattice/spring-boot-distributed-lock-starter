package com.noir.common.lock;

import java.util.concurrent.Callable;

/**
 * 具有加锁逻辑的服务接口
 *
 * 提供了lambda表达式的上锁支持，标定锁名称
 * 与对应的Callable或者Runnable即可实现对
 * 块级逻辑的资源锁定，对于多个资源的锁定可以
 * 通过多级嵌套进行实现，同样的，因为默认实现
 * 的工程均实现了可重入接口 {@link ReentrantDLock}
 * 在未使用自定义的DLockFactory时均可重入
 *
 * 具体的实现
 * @see com.noir.common.lock.impl.LockableServiceImpl
 */
public interface LockableService {

    /**
     * 根据key进行加锁, 执行callable任务
     *
     * @param key      加锁的键值
     * @param callable 执行的操作
     * @param <T>      callable任务返回的结果类型
     * @return callable任务返回的结果
     * @throws Exception callable任务执行过程中产生的异常
     */
    <T> T lockAndExecute(String key, Callable<T> callable) throws Exception;

    /**
     * 根据key进行加锁，执行runnable任务
     *
     * @param key      加锁的键值
     * @param runnable 执行的操作
     * @throws Exception runnable任务执行过程中产生的异常
     */
    void lockAndExecute(String key, Runnable runnable) throws Exception;
}
