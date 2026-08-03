package com.routeshare.chat.service.impl;

import com.routeshare.booking.facade.BookingFacade;
import com.routeshare.chat.dto.*;
import com.routeshare.chat.entity.*;
import com.routeshare.chat.repository.*;
import com.routeshare.chat.service.ChatService;
import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.ratelimit.RateLimiter;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
  private static final int MAX_LIMIT = 100;

  private final CurrentUserProvider current;
  private final IdentityFacade identity;
  private final BookingFacade bookings;
  private final ChatThreadRepository threads;
  private final ChatMessageRepository messages;
  private final ChatAdminReadAuditRepository adminReads;
  private final NotificationFacade notifications;
  private final RateLimiter rateLimiter;
  private final MeterRegistry meters;
  private final Clock clock;

  @Value("${routeshare.chat.message-rate-limit-per-minute:20}")
  private int messageRateLimit;

  @PostConstruct
  void registerOpenThreadGauge() {
    meters.gauge(
        "routeshare_chat_threads_open", threads, repository -> repository.countByState("OPEN"));
  }

  @Override
  @Transactional
  public void openForConfirmedBooking(long bookingId) {
    if (threads.findByBookingId(bookingId).isPresent()) {
      return;
    }
    try {
      threads.saveAndFlush(ChatThreadEntity.open(bookingId, clock.instant()));
      meters.counter("routeshare_chat_threads_opened_total").increment();
    } catch (DataIntegrityViolationException race) {
      if (threads.findByBookingId(bookingId).isEmpty()) {
        throw race;
      }
    }
  }

  @Override
  @Transactional
  public void scheduleClose(long bookingId, Instant closesAt) {
    threads.findByBookingId(bookingId).ifPresent(thread -> thread.scheduleClose(closesAt));
  }

  @Override
  @Transactional
  public int closeDue() {
    Instant now = clock.instant();
    int closed = 0;
    for (var thread :
        threads.findTop200ByStateAndClosesAtLessThanEqualOrderByClosesAt("OPEN", now)) {
      if (thread.close(now)) {
        closed++;
      }
    }
    return closed;
  }

  @Override
  @Transactional
  public ChatThreadResponse thread(long bookingId) {
    var access = participantAccess(bookingId);
    return toThread(access.thread(), access.context(), access.appUserId());
  }

  @Override
  @Transactional
  public ChatMessagesResponse messages(long bookingId, long since, int limit) {
    var access = participantAccess(bookingId);
    return listMessages(access.thread().getId(), since, limit);
  }

  @Override
  @Transactional
  public ChatMessageResponse send(
      long bookingId, String idempotencyKey, ChatMessageRequest request) {
    if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 200) {
      throw new IllegalArgumentException("A valid Idempotency-Key header is required");
    }
    var access = participantAccess(bookingId);
    if (!"OPEN".equals(access.thread().getState())) {
      throw chatConflict("CHAT_CLOSED", "This booking chat is closed.", bookingId);
    }
    rateLimiter.check(
        "chat-message",
        access.appUserId() + ":" + access.thread().getId(),
        messageRateLimit,
        Duration.ofMinutes(1));
    var existing =
        messages.findByThreadIdAndSenderAppUserIdAndIdempotencyKey(
            access.thread().getId(), access.appUserId(), idempotencyKey);
    if (existing.isPresent()) {
      return toMessage(existing.get());
    }
    ChatMessageEntity saved;
    try {
      saved =
          messages.saveAndFlush(
              ChatMessageEntity.create(
                  access.thread().getId(),
                  access.appUserId(),
                  request.body().trim(),
                  idempotencyKey));
    } catch (DataIntegrityViolationException race) {
      saved =
          messages
              .findByThreadIdAndSenderAppUserIdAndIdempotencyKey(
                  access.thread().getId(), access.appUserId(), idempotencyKey)
              .orElseThrow(() -> race);
    }
    long recipient =
        access.appUserId() == access.context().passengerAppUserId()
            ? access.context().driverAppUserId()
            : access.context().passengerAppUserId();
    notifications.notifyUser(
        recipient,
        "CHAT_MESSAGE",
        "New trip message",
        "You have a new message about your booking.",
        Map.of(
            "bookingId", String.valueOf(bookingId),
            "chatMessageId", String.valueOf(saved.getId()),
            "actionPath", "/bookings/" + bookingId + "/chat"));
    meters.counter("routeshare_chat_messages_total").increment();
    return toMessage(saved);
  }

  @Override
  @Transactional
  public int markRead(long bookingId, ChatReadRequest request) {
    var access = participantAccess(bookingId);
    return messages.markRead(
        access.thread().getId(), access.appUserId(), request.upToMessageId(), clock.instant());
  }

  @Override
  @Transactional
  public ChatMessagesResponse adminMessages(long bookingId, String reason, long since, int limit) {
    CurrentUser token = current.requireCurrentUser();
    if (token.roles().stream().noneMatch(this::canReadAsSupport)) {
      throw new AccessDeniedException("Support chat access required");
    }
    if (reason == null || reason.trim().length() < 3 || reason.length() > 500) {
      throw new IllegalArgumentException("A reason between 3 and 500 characters is required");
    }
    long appUserId = identity.upsertFromToken(token).appUserId();
    var thread = requireThread(bookingId);
    adminReads.save(ChatAdminReadAuditEntity.record(thread.getId(), appUserId, reason.trim()));
    meters.counter("routeshare_chat_admin_reads_total").increment();
    return listMessages(thread.getId(), since, limit);
  }

  private ParticipantAccess participantAccess(long bookingId) {
    long appUserId = identity.upsertFromToken(current.requireCurrentUser()).appUserId();
    var context =
        bookings
            .findChatContext(bookingId)
            .orElseThrow(() -> new NoSuchElementException("Booking not found"));
    if (appUserId != context.passengerAppUserId() && appUserId != context.driverAppUserId()) {
      throw new AccessDeniedException("Chat belongs only to the booking participants");
    }
    return new ParticipantAccess(requireThread(bookingId), context, appUserId);
  }

  private ChatThreadEntity requireThread(long bookingId) {
    return threads
        .findByBookingId(bookingId)
        .orElseThrow(
            () ->
                chatConflict(
                    "CHAT_NOT_AVAILABLE",
                    "Chat becomes available after the booking is confirmed.",
                    bookingId));
  }

  private ChatMessagesResponse listMessages(long threadId, long since, int limit) {
    int capped = Math.min(Math.max(limit, 1), MAX_LIMIT);
    List<ChatMessageResponse> items =
        messages
            .findByThreadIdAndIdGreaterThanOrderById(
                threadId, Math.max(0, since), PageRequest.of(0, capped))
            .stream()
            .map(this::toMessage)
            .toList();
    long cursor = items.isEmpty() ? Math.max(0, since) : items.get(items.size() - 1).id();
    return new ChatMessagesResponse(items, cursor);
  }

  private ChatThreadResponse toThread(
      ChatThreadEntity thread, BookingFacade.ChatContext context, long viewerId) {
    boolean viewerIsPassenger = viewerId == context.passengerAppUserId();
    long counterpartyId =
        viewerIsPassenger ? context.driverAppUserId() : context.passengerAppUserId();
    String name =
        identity.findContact(counterpartyId).map(IdentityFacade.Contact::firstName).orElse(null);
    List<String> quickReplies =
        viewerIsPassenger
            ? List.of("I am at the pickup point", "I will be there shortly", "Please call me")
            : List.of("I am on my way", "I have arrived", "Please meet at the pickup point");
    return new ChatThreadResponse(
        thread.getId(),
        thread.getBookingId(),
        thread.getState(),
        thread.getOpenedAt(),
        thread.getClosesAt(),
        new ChatParticipantResponse(
            counterpartyId, name, viewerIsPassenger ? "DRIVER" : "PASSENGER"),
        quickReplies,
        true);
  }

  private ChatMessageResponse toMessage(ChatMessageEntity message) {
    return new ChatMessageResponse(
        message.getId(),
        message.getSenderAppUserId(),
        message.getBody(),
        message.getSentAt(),
        message.getReadByCounterpartyAt());
  }

  private GateConflictException chatConflict(String code, String message, long bookingId) {
    return new GateConflictException(code, message, "/bookings/" + bookingId);
  }

  private boolean canReadAsSupport(String role) {
    return role.equals("SUPPORT_AGENT")
        || role.equals("OPS_ADMIN")
        || role.equals("ADMIN")
        || role.equals("SUPER_ADMIN");
  }

  private record ParticipantAccess(
      ChatThreadEntity thread, BookingFacade.ChatContext context, long appUserId) {}
}
