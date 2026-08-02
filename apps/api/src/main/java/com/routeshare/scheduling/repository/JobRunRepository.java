package com.routeshare.scheduling.repository;

import com.routeshare.scheduling.entity.JobRunEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRunRepository extends JpaRepository<JobRunEntity, Long> {

  Optional<JobRunEntity> findFirstByJobNameAndStatusOrderByStartedAtDesc(
      String jobName, String status);

  /**
   * Backs the health indicator: a job that has not succeeded within three ticks is either dead or
   * wedged, and either way riders are waiting on a clock that will never fire.
   */
  @Query(
      """
      SELECT COUNT(r) FROM JobRunEntity r
       WHERE r.jobName = :jobName
         AND r.status = 'SUCCEEDED'
         AND r.finishedAt >= :since
      """)
  long countSucceededSince(@Param("jobName") String jobName, @Param("since") Instant since);
}
