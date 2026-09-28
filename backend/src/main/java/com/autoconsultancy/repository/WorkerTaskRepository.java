package com.autoconsultancy.repository;

import com.autoconsultancy.entity.WorkerTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkerTaskRepository extends MongoRepository<WorkerTask, Long> {

    Page<WorkerTask> findByWorkerIdOrderByCreatedAtDesc(Long workerId, Pageable pageable);

    List<WorkerTask> findByWorkerIdAndDueDateOrderByPriorityDesc(Long workerId, LocalDate date);

    long countByWorkerIdAndStatus(Long workerId, WorkerTask.TaskStatus status);
    long countByWorkerIdAndTaskType(Long workerId, WorkerTask.TaskType type);
    long countByWorkerIdAndDueDateAndStatusNot(Long workerId, LocalDate date, WorkerTask.TaskStatus status);

    Page<WorkerTask> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // ── DB-level filtered queries (replace the findAll()-based default methods) ──

    /** All tasks for a specific worker, filtered by type and status when provided. */
    @Query("{'worker.id': ?0}")
    Page<WorkerTask> findByWorkerId(Long workerId, Pageable pageable);

    @Query("{'worker.id': ?0, 'taskType': ?1}")
    Page<WorkerTask> findByWorkerIdAndType(Long workerId, String taskType, Pageable pageable);

    @Query("{'worker.id': ?0, 'status': ?1}")
    Page<WorkerTask> findByWorkerIdAndStatus(Long workerId, String status, Pageable pageable);

    @Query("{'worker.id': ?0, 'taskType': ?1, 'status': ?2}")
    Page<WorkerTask> findByWorkerIdAndTypeAndStatus(Long workerId, String taskType, String status, Pageable pageable);

    /** Admin — all tasks, optionally filtered by workerId, type, status. */
    @Query("{'worker.id': ?0}")
    Page<WorkerTask> findAllByWorkerIdAdmin(Long workerId, Pageable pageable);

    @Query("{'taskType': ?0}")
    Page<WorkerTask> findAllByTypeAdmin(String taskType, Pageable pageable);

    @Query("{'status': ?0}")
    Page<WorkerTask> findAllByStatusAdmin(String status, Pageable pageable);

    @Query("{'taskType': ?0, 'status': ?1}")
    Page<WorkerTask> findAllByTypeAndStatusAdmin(String taskType, String status, Pageable pageable);

    @Query("{'worker.id': ?0, 'taskType': ?1}")
    Page<WorkerTask> findAllByWorkerAndTypeAdmin(Long workerId, String taskType, Pageable pageable);

    @Query("{'worker.id': ?0, 'status': ?1}")
    Page<WorkerTask> findAllByWorkerAndStatusAdmin(Long workerId, String status, Pageable pageable);

    @Query("{'worker.id': ?0, 'taskType': ?1, 'status': ?2}")
    Page<WorkerTask> findAllByWorkerTypeStatusAdmin(Long workerId, String taskType, String status, Pageable pageable);
}
