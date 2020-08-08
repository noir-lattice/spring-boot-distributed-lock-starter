package com.noir.common.lock.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "locker")
public class DLockProperties {

    /**
     * 是否开启
     */
    private boolean enable;

    /**
     * 锁类型
     *
     * redis-expire redis过期时间与上锁id标记
     * redis-get-set redis getSet 时间戳
     * red-lock redisson红锁
     * zookeeper zk公平锁
     */
    private String type;

}
