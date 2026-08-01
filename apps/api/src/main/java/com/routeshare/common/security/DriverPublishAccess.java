package com.routeshare.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Endpoints that put a route in front of riders. Stricter than {@link DriverAccess}: approved
 * documents, an approved vehicle and (from slice 02) a rate band.
 *
 * @see DriverGuard
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@driverGuard.canPublish(authentication)")
public @interface DriverPublishAccess {}
