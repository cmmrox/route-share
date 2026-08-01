package com.routeshare.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Driver-only endpoint. Replaces {@code @PreAuthorize("hasRole('DRIVER')")} everywhere: the role
 * alone stopped meaning "may drive" the moment one account could be both rider and driver.
 *
 * @see DriverGuard
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@driverGuard.canDrive(authentication)")
public @interface DriverAccess {}
