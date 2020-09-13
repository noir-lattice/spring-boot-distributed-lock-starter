package com.noir.common.lock.impl.locks;

import com.noir.common.lock.ReentrantDLock;
import com.noir.common.lock.excptions.LockExpiredException;
import lombok.SneakyThrows;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * setNX and getSet lock
 *
 * 依赖过期时间，过短会使大量长调用链业务回滚，过长影响资源可用性
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
                        LOCK_LUA_SCRIPT,
                        RScript.ReturnType.STATUS,
                        Collections.singletonList(key),
                        lockerName,
                        lockExpiresMilliseconds);
        return !Objects.isNull(status);
    }

    private void cleanLockRecode(String key) throws LockExpiredException {
        boolean unlocked = client.getScript().eval(
                RScript.Mode.READ_WRITE,
                UNLOCK_LUA_SCRIPT,
                RScript.ReturnType.BOOLEAN,
                Collections.singletonList(key),
                lockerName);
        if (!unlocked) {
            throw new LockExpiredException();
        }
    }

    /**
     * 设置超时时间使用lua脚本保证操作原子性
     */
    private static final String LOCK_LUA_SCRIPT = "return redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2], 'NX')";
    /**
     * 解锁时对当前锁持有者进行check，仅在
     * 持有方为自己时释放锁，并返回释放状态
     * 以供上层做业务回滚
     */
    private static final String UNLOCK_LUA_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then redis.call('del', KEYS[1]); return true; else return false end";
}