package com.example.admin.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * MyBatis-Plus 拦截器：分页拦截器必须排第一，否则 count 拼错。
 * 默认（H2）用 H2 方言；mysql profile 用 MySQL 方言。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    @Profile("!mysql")
    public MybatisPlusInterceptor mybatisPlusInterceptorH2() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        return interceptor;
    }

    @Bean
    @Profile("mysql")
    public MybatisPlusInterceptor mybatisPlusInterceptorMysql() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
