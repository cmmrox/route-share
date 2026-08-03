package com.routeshare.chat.repository;

import com.routeshare.chat.entity.ChatAdminReadAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatAdminReadAuditRepository
    extends JpaRepository<ChatAdminReadAuditEntity, Long> {}
