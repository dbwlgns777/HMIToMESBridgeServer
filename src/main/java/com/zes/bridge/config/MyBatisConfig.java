package com.zes.bridge.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.zes.bridge.mapper")
public class MyBatisConfig {
}
