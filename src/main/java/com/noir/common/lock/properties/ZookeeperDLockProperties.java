package com.noir.common.lock.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "locker.zookeeper")
public class ZookeeperDLockProperties {

    /**
     * zk 地址
     */
    private String address;

    /**
     * zk超时时间
     */
    private Integer timeout;


}
