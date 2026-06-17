package com.routeshare.payment.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({CybersourceProperties.class, CommissionProperties.class})
public class PaymentGatewayConfig {}
