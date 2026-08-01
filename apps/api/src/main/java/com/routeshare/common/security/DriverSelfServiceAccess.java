package com.routeshare.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Driver-facing surfaces that survive a deactivation: earnings and payout details, and support.
 *
 * <p>Board D34 is explicit that a deactivated driver keeps their money and their route to an
 * appeal. Putting these endpoints behind {@link DriverAccess} would strand exactly the person the
 * screen is written for — they would be told to contact support by a screen whose support call
 * returns 403. Suspension still blocks them; only the driver deactivation is tolerated here.
 *
 * @see DriverGuard
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@driverGuard.canManageDriverAccount(authentication)")
public @interface DriverSelfServiceAccess {}
