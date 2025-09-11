package com.spacemit.musebackend.config;

import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.spacemit.musebackend.repository")
public class RepositoryConfig {
    // 这个配置确保只有JPA Repository被扫描，排除Redis Repository
}




