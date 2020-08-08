package com.noir.common.lock;

import java.util.concurrent.Callable;

/**
 * 具有加锁逻辑的服务接口
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
