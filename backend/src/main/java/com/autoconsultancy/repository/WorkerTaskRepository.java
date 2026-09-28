package com.autoconsultancy.repository;

import com.autoconsultancy.entity.WorkerTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public interface WorkerTaskRepository extends MongoRepository<WorkerTask, Long> {

    Page<WorkerTask> findByWorkerIdOrderByCreatedAtDesc(Long workerId, Pageable pageable);

    List<WorkerTask> findByWorkerIdAndDueDateOrderByPriorityDesc(Long workerId, LocalDate date);

    long countByWorkerIdAndStatus(Long workerId, WorkerTask.TaskStatus status);
    long countByWorkerIdAndTaskType(Long workerId, WorkerTask.TaskType type);
    long countByWorkerIdAndDueDateAndStatusNot(Long workerId, LocalDate date, WorkerTask.TaskStatus status);

    Page<WorkerTask> findAllByOrderByCreatedAtDesc(Pageable pageable);

    default Page<WorkerTask> findByWorkerIdFiltered(Long workerId, WorkerTask.TaskType type, WorkerTask.TaskStatus status, Pageable pageable) {
        List<WorkerTask> list = findAll().stream()
                .filter(t -> t.getWorker() != null && workerId.equals(t.getWorker().getId()))
                .collect(Collectors.toList());
        if (type != null) list = list.stream().filter(t -> t.getTaskType() == type).collect(Collectors.toList());
        if (status != null) list = list.stream().filter(t -> t.getStatus() == status).collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        List<WorkerTask> content = (start <= list.size()) ? list.subList(start, end) : Collections.emptyList();
        return new PageImpl<>(content, pageable, list.size());
    }

    default Page<WorkerTask> findAllFiltered(WorkerTask.TaskType type, WorkerTask.TaskStatus status, Long workerId, Pageable pageable) {
        List<WorkerTask> list = findAll();
        if (workerId != null) list = list.stream().filter(t -> t.getWorker() != null && workerId.equals(t.getWorker().getId())).collect(Collectors.toList());
        if (type != null) list = list.stream().filter(t -> t.getTaskType() == type).collect(Collectors.toList());
        if (status != null) list = list.stream().filter(t -> t.getStatus() == status).collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        List<WorkerTask> content = (start <= list.size()) ? list.subList(start, end) : Collections.emptyList();
        return new PageImpl<>(content, pageable, list.size());
    }
}
