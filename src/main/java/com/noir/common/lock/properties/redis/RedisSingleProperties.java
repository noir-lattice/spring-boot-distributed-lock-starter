package com.noir.common.lock.properties.redis;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class RedisSingleProperties {
    private  String address;

    private int connTimeout;
}
