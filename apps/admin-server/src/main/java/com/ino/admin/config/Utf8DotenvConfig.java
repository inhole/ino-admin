package com.ino.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@PropertySource(
        value = "file:.env",
        ignoreResourceNotFound = true,
        encoding = "UTF-8",
        factory = Utf8BomAwarePropertySourceFactory.class)
class Utf8DotenvConfig {}
