package com.routeshare.appreadiness.repository;

import com.routeshare.appreadiness.entity.WorkflowItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowItemRepository extends JpaRepository<WorkflowItemEntity, Long> {
  List<WorkflowItemEntity> findByItemTypeAndOwnerAppUserIdOrderByIdDesc(
      String itemType, Long ownerAppUserId);

  List<WorkflowItemEntity> findByItemTypeOrderByIdDesc(String itemType);

  List<WorkflowItemEntity> findTop50ByItemTypeOrderByIdDesc(String itemType);

  List<WorkflowItemEntity> findTop50ByOrderByIdDesc();
}
