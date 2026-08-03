package com.routeshare.chat.scheduling;

import com.routeshare.chat.service.ChatService;
import com.routeshare.scheduling.domain.ScheduledJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatAutoCloseJob implements ScheduledJob {
  private final ChatService chat;

  @Override
  public String name() {
    return "chat-auto-close";
  }

  @Override
  public int run() {
    return chat.closeDue();
  }
}
