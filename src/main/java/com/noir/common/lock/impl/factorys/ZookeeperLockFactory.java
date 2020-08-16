package com.noir.common.lock.impl.factorys;

import com.noir.common.lock.DLockFactory;
import com.noir.common.lock.impl.locks.RedLockWrapper;
import com.noir.common.lock.impl.locks.ZookeeperLock;
import lombok.SneakyThrows;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * zookeeper lock factory
 *
 * {@link ZookeeperLock}的工厂，这里仅
 * 做匹配locker.type来提供IoC容器中的{@link RedLockFactory}
 */
@Component
@ConditionalOnProperty(prefix = "locker", value = "type", havingValue = "zookeeper")
public class ZookeeperLockFactory implements DLockFactory {

    @Autowired
    ZooKeeper zk;

    @SneakyThrows
    @Override
    public Lock getLock(String name) {
        return new ZookeeperLock(zk, name);
    }

    @SneakyThrows
    @Override
    public Lock getLock(String name, long expire, TimeUnit unit) {
        return new ZookeeperLock(zk, name, expire, unit);
    }
}

