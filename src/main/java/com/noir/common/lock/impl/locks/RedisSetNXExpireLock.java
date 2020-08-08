package com.noir.common.lock.impl.locks;

import com.noir.common.lock.ReentrantDLock;
import lombok.SneakyThrows;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * setNX and getSet lock
 *
 * !!!: 依赖过期时间，过短会失去隔离性，过长影响资源可用性
 */
public class RedisSetNXExpireLock extends ReentrantDLock {
    private static final Long DEFAULT_TIMEOUT_SECONDS = 30L;

    private static final Logger log = LoggerFactory.getLogger(RedisSetNXExpireLock.class);

    private static final int DEFAULT_SLEEP_MILLIS = 100;

    private final RedissonClient client;

    private final String nameSpace;

    private final String name;

    private final String lockerName = UUID.randomUUID().toString();

    /**
     * 锁失效时间(毫秒)
     */
    private final long lockExpiresMilliseconds;

    public RedisSetNXExpireLock(RedissonClient client, String nameSpace, String name) {
        //默认30分钟
        this(client, nameSpace, name, DEFAULT_TIMEOUT_SECONDS, TimeUnit.MINUTES);
    }

    public RedisSetNXExpireLock(RedissonClient client, String nameSpace, String name, long expire, TimeUnit unit) {
        this.client = client;
        this.nameSpace = nameSpace;
        this.name = name;
        this.lockExpiresMilliseconds = unit.toMillis(expire);
    }

    /**
     * 获取拼接后的锁名
     *
     * @return str
     */
    private String getLockKey() {
        return nameSpace + ":" + name;
    }

    @Override
    public void lock() {
        while (!tryLock()) {
            // pass
            log.info("retry get dLock");
        }
    }

    @Override
    public void lockInterruptibly() {
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
            if (trySetLockRecode(lockKey)) {
                log.info(lockKey + " locked by setNX");
                enter(lockKey);
                return true;
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
        exit(getLockKey());
        cleanLockRecode(getLockKey());
    }

    @Override
    public Condition newCondition() {
        // pass
        return null;
    }

    private boolean trySetLockRecode(String key) {
        String status = client
                .getScript()
                .eval(
                        RScript.Mode.READ_WRITE,
                        genLockLuaScript(key, lockExpiresMilliseconds),
                        RScript.ReturnType.STATUS);
        return !Objects.isNull(status);
    }

    private void cleanLockRecode(String key) {
        client.getScript().eval(
                RScript.Mode.READ_WRITE,
                genUnlockLuaScript(key),
                RScript.ReturnType.VALUE);
    }

    private String genLockLuaScript(String key, Long second) {
        return "return redis.call('SET', '" +
                key +
                "', '" +
                lockerName +
                "', 'EX', " +
                second +
                ", 'NX'";
    }

    private String genUnlockLuaScript(String key) {
        return "if redis.call('get','" + key + "') == '" +
                lockerName + "' then  return redis.call('del','" + key + "') else return 0 end ";
    }
}