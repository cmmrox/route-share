package com.routeshare.storage.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Always registers {@link ObjectStorageProperties} so document services can read presign TTLs even
 * when storage is disabled (in which case {@link
 * com.routeshare.storage.service.impl.DisabledObjectStorageAdapter} is active).
 */
@Configuration
@EnableConfigurationProperties(ObjectStorageProperties.class)
public class StoragePropertiesConfig {}
