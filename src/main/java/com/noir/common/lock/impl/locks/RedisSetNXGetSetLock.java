package com.noir.common.lock.impl.locks;


import com.noir.common.lock.ReentrantDLock;
import com.noir.common.lock.excptions.LockExpiredException;
import lombok.SneakyThrows;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * setNX and getSet lock
 *
 * 依赖过期时间，过短会导致大量业务超过租期使业务回滚，
 * 过长影响资源可用性；大量getSet操作对redis的资源消耗高；
 */
public class RedisSetNXGetSetLock extends ReentrantDLock {
    private static final Long DEFAULT_TIMEOUT_SECONDS = 30L;

    private static final Logger log = LoggerFactory.getLogger(RedisSetNXGetSetLock.class);

    private static final int DEFAULT_SLEEP_MILLIS = 100;

    private final RedissonClient client;

    private final String namespace;

    private final String name;

    private Long lockValue;

    /**
     * 锁失效时间(毫秒)
     */
    private final long lockExpiresMilliseconds;

    public RedisSetNXGetSetLock(RedissonClient client, String namespace, String name) {
        //默认30分钟
        this(client, namespace, name, DEFAULT_TIMEOUT_SECONDS, TimeUnit.MINUTES);
    }

    public RedisSetNXGetSetLock(RedissonClient client, String namespace, String name, long expire, TimeUnit unit) {
        this.client = client;
        this.namespace = namespace;
        this.name = name;
        this.lockExpiresMilliseconds = unit.toMillis(expire);
    }

    /**
     * 获取拼接后的锁名
     *
     * @return str
     */
    private String getLockKey() {
        return namespace + ":" + name;
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

    /**
     * 尝试获取锁
     * @param l 时间长度
     * @param timeUnit 单位
     * @return 是否上锁成功
     * @throws InterruptedException interruptedException
     */
    @Override
    public boolean tryLock(long l, TimeUnit timeUnit) throws InterruptedException {
        String lockKey = getLockKey();

        if (isEntered(lockKey)) {
            return true;
        }

        long deadline = System.currentTimeMillis() + timeUnit.toMillis(l);

        while (deadline >= System.currentTimeMillis()) {
            long serverTime = System.currentTimeMillis();
            lockValue = serverTime + lockExpiresMilliseconds;

            if (client.getBucket(lockKey).trySet(lockValue)) {
                log.info(lockKey + " locked by setNX");
                enter(lockKey);
                return true;
            }
            Long currentValue = (Long) client.getBucket(lockKey).get();
            //判断锁是否失效
            if (currentValue != null && currentValue < serverTime) {
                Long oldValueStr = (Long) client.getBucket(lockKey).getAndSet(lockValue);
                if (oldValueStr != null && oldValueStr.equals(currentValue)) {
                    log.info("locked by getSet");
                    enter(lockKey);
                    return true;
                }
            }
            Thread.sleep(DEFAULT_SLEEP_MILLIS);
        }
        return false;
    }

    /**
     * 解锁删除key
     */
    @Override
    public void unlock() {
        String lockKey = getLockKey();
        exit(lockKey);
        boolean unlocked = client.getScript().eval(
                RScript.Mode.READ_WRITE,
                UNLOCK_LUA_SCRIPT,
                RScript.ReturnType.BOOLEAN,
                Collections.singletonList(lockKey),
                lockValue);
        if (!unlocked) {
            throw new LockExpiredException();
        }
        log.info(lockKey + " unlock");
    }

    @Override
    public Condition newCondition() {
        // pass
        return null;
    }

    /**
     * 保证原子性的解锁lua
     */
    private static final String UNLOCK_LUA_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then redis.call('del', KEYS[1]); return true; else return false end";

}