package com.routeshare.booking.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TripShareProperties.class)
public class TripSharePropertiesConfig {}
