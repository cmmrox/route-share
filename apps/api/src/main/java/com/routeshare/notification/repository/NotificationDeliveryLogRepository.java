package com.routeshare.notification.repository;

import com.routeshare.notification.entity.NotificationDeliveryLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryLogRepository
    extends JpaRepository<NotificationDeliveryLogEntity, Long> {}
