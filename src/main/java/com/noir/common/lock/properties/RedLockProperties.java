package com.noir.common.lock.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "locker.red-lock")
public class RedLockProperties {
    List<RedisDLockProperties> clients;
}
