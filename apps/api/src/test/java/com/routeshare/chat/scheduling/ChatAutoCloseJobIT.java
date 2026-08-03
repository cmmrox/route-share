package com.routeshare.chat.scheduling;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.routeshare.chat.service.ChatService;
import org.junit.jupiter.api.Test;

class ChatAutoCloseJobIT {
  @Test
  void schedulerDelegatesToTheDueThreadClosure() {
    ChatService chat = mock(ChatService.class);
    when(chat.closeDue()).thenReturn(3);
    var job = new ChatAutoCloseJob(chat);

    assertThat(job.name()).isEqualTo("chat-auto-close");
    assertThat(job.run()).isEqualTo(3);
    verify(chat).closeDue();
  }
}
