package com.routeshare.driver.repository;

import com.routeshare.driver.entity.DriverPayoutProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverPayoutProfileRepository
    extends JpaRepository<DriverPayoutProfileEntity, Long> {}
