package com.routeshare.chat.service.impl;

import static org.mockito.Mockito.*;

import com.routeshare.booking.facade.BookingFacade;
import com.routeshare.chat.repository.*;
import com.routeshare.common.ratelimit.RateLimiter;
import com.routeshare.common.security.*;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.*;
import java.util.*;
import org.springframework.test.util.ReflectionTestUtils;

final class ChatTestFixture {
  final CurrentUserProvider current = mock(CurrentUserProvider.class);
  final IdentityFacade identity = mock(IdentityFacade.class);
  final BookingFacade bookings = mock(BookingFacade.class);
  final ChatThreadRepository threads = mock(ChatThreadRepository.class);
  final ChatMessageRepository messages = mock(ChatMessageRepository.class);
  final ChatAdminReadAuditRepository adminReads = mock(ChatAdminReadAuditRepository.class);
  final NotificationFacade notifications = mock(NotificationFacade.class);
  final RateLimiter rateLimiter = mock(RateLimiter.class);
  final Clock clock;
  final ChatServiceImpl service;

  ChatTestFixture(Instant now, long appUserId) {
    clock = Clock.fixed(now, ZoneOffset.UTC);
    service =
        new ChatServiceImpl(
            current,
            identity,
            bookings,
            threads,
            messages,
            adminReads,
            notifications,
            rateLimiter,
            new SimpleMeterRegistry(),
            clock);
    ReflectionTestUtils.setField(service, "messageRateLimit", 20);
    var token = new CurrentUser("sub", "user@test", null, "User", Set.of("PASSENGER"));
    when(current.requireCurrentUser()).thenReturn(token);
    when(identity.upsertFromToken(token))
        .thenReturn(
            new AppUser(appUserId, UUID.randomUUID(), "sub", "user@test", null, "User", "ACTIVE"));
  }

  static void setId(Object entity, long id) {
    ReflectionTestUtils.setField(entity, "id", id);
  }
}
