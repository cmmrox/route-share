package com.routeshare.notification.repository;

import com.routeshare.notification.entity.NotificationPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository
    extends JpaRepository<NotificationPreferenceEntity, Long> {}
