package com.routeshare.notification.push.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PushProperties.class)
public class PushPropertiesConfig {}
