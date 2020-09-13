# spring-boot-distributed-lock-starter
多种分布式锁的实现与spring-boot-starter封装  
共提供四种类型分布式锁：  
 *  redis-expire: setNx与设置key的过期时间并通过随机uuid标记上锁人
 *  redis-get-set: setNx与getSet并设置当前时间戳来保证上锁与解锁
 *  red-lock: redisson红锁的再封装 
 *  zookeeper: 顺序临时节点的公平锁实现
 
 以上锁实现均可重入并提供了注解的aop支持。

## required
 * JDK 8+
 * Maven or Gradle
 * Spring boot
 
## feature
  * 实现的四种锁均支持可重入
  * 单节点redis锁的实现较健壮，保证资源隔离
  * zk锁的顺序支持
  * 注解式的锁与编程性锁
  * 提供自定义锁拓展点（自定义实现DLockFactory）

## todo
  * 单节点redis锁的续约
  * 对于打到同一个服务的资源锁是否可以实现支持锁的降级策略
  * 对于redis的sleep自旋的优化

## install
Maven：
```xml
    <dependency>
        <groupId>com.github.lattice-boot.common</groupId>
        <artifactId>spring-boot-distributed-lock-starter</artifactId>
        <version>0.1.6-RELEASE</version>
    </dependency>
```   
Gradle:
```
    compile group: 'com.github.lattice-boot.common', name: 'spring-boot-distributed-lock-starter', version: '0.1.6-RELEASE'
```

## config
以下均已yaml配置文件为实例  

redis-expire/redis-get-set
```yaml
locker:
  type: redis-expire # or redis-get-set
  redis:
    mode: single # single\cluster\sentinel @see com.noir.common.lock.properties.RedisDLockProperties
    single:
      address: xxxxxx
      conn-timeout: 300000
    pool:
      size: 10
      minIdle: 1
```

redlock
```yaml
locker:
  type: red-lock
  clients:
    - redis:
          mode: single # single\cluster\sentinel @see com.noir.common.lock.properties.RedisDLockProperties
          single:
            address: xxxxxx
            conn-timeout: 300000
          pool:
            size: 10
            minIdle: 1
    - redis:
              mode: single # single\cluster\sentinel @see com.noir.common.lock.properties.RedisDLockProperties
              single:
                address: xxxxxx
                conn-timeout: 300000
              pool:
                size: 10
                minIdle: 1
    - redis:
              mode: single # single\cluster\sentinel @see com.noir.common.lock.properties.RedisDLockProperties
              single:
                address: xxxxxx
                conn-timeout: 300000
              pool:
                size: 10
                minIdle: 1
    - redis:
              mode: single # single\cluster\sentinel @see com.noir.common.lock.properties.RedisDLockProperties
              single:
                address: xxxxxx
                conn-timeout: 300000
              pool:
                size: 10
                minIdle: 1
```

zookeeper
```yaml
locker:
  type: zookeeper
  zookeeper:
    address: xxx # Multiple support like: xx.xx.xx.xx,yy.yy.yy.yy,zz.zz.zz.zz
    timeout: 30000
```

## use
可以简单的通过注解上锁(支持元注解并使用alias annotation)
```java
@Dlock("lock_#{#accountId}")
public void changeAccount(Long accountId) {
    ...
}
```

通过LockFactory
```java
@Autowired
private DLockFactory lockFactory;

public void changeAccount(Long accountId) {
    Lock lock = lockFactory.getLock("lock_" + accountId.toString(), 30, TimeUnit.SECONDS);
    
    try {
        lock.lock(); // or tryLock
        ...
    }
    finally {
        lock.unlock(); // 此处可能因为超过租期抛出LockExpiredException
    }
}
```
需要注意的是，使用redis单点锁的两种方式都可能因为超过租期抛出LockExpiredException，通常我们应该使业务回滚，如果使用编程事务记得抓一下unlock

通过LockableService
```java
@Autowired
private LockableService lockableService;

public void changeAccount(Long accountId) {
    lockableService.lockAndExecute("lock_" + accountId.toString(), () -> {
        ...
        return xxx; // or not return
    });
}
```

以上方式上的锁均可重入。
