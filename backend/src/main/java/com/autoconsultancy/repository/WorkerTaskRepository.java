package com.autoconsultancy.repository;

import com.autoconsultancy.entity.WorkerTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface WorkerTaskRepository extends JpaRepository<WorkerTask, Long> {

    Page<WorkerTask> findByWorkerIdOrderByCreatedAtDesc(Long workerId, Pageable pageable);

    List<WorkerTask> findByWorkerIdAndDueDateOrderByPriorityDesc(Long workerId, LocalDate date);

    @Query("SELECT t FROM WorkerTask t WHERE t.worker.id = :wid AND (:type IS NULL OR t.taskType = :type) AND (:status IS NULL OR t.status = :status) ORDER BY t.createdAt DESC")
    Page<WorkerTask> findByWorkerIdFiltered(@Param("wid") Long workerId,
                                            @Param("type") WorkerTask.TaskType type,
                                            @Param("status") WorkerTask.TaskStatus status,
                                            Pageable pageable);

    long countByWorkerIdAndStatus(Long workerId, WorkerTask.TaskStatus status);
    long countByWorkerIdAndTaskType(Long workerId, WorkerTask.TaskType type);
    long countByWorkerIdAndDueDateAndStatusNot(Long workerId, LocalDate date, WorkerTask.TaskStatus status);

    Page<WorkerTask> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT t FROM WorkerTask t WHERE (:type IS NULL OR t.taskType = :type) AND (:status IS NULL OR t.status = :status) AND (:workerId IS NULL OR t.worker.id = :workerId) ORDER BY t.createdAt DESC")
    Page<WorkerTask> findAllFiltered(@Param("type") WorkerTask.TaskType type,
                                     @Param("status") WorkerTask.TaskStatus status,
                                     @Param("workerId") Long workerId,
                                     Pageable pageable);
}
