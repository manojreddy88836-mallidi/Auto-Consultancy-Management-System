package com.autoconsultancy.service;

import com.autoconsultancy.dto.request.CreateWorkerRequest;
import com.autoconsultancy.dto.response.WorkerDashboardStats;
import com.autoconsultancy.entity.User;
import com.autoconsultancy.entity.Worker;
import com.autoconsultancy.entity.WorkerTask;
import com.autoconsultancy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;
    private final WorkerTaskRepository workerTaskRepository;

    @Transactional(readOnly = true)
    public Worker getProfile(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return workerRepository.findById(user.getId()).orElseThrow();
    }

    @Transactional
    public Worker updateProfile(String email, CreateWorkerRequest request) {
        Worker worker = getProfile(email);
        User user = worker.getUser();
        user.setPhone(request.getPhone());
        userRepository.save(user);
        return workerRepository.save(worker); // L6 fix: persist worker document
    }

    @Transactional(readOnly = true)
    public WorkerDashboardStats getDashboardStats(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Long userId = user.getId();

        long totalAssigned       = applicationRepository.countActiveAssignmentsByWorkerUserId(userId);
        long pendingVerification = applicationRepository.countPendingVerificationByWorkerUserId(userId);
        long completed           = applicationRepository.countCompletedByWorkerUserId(userId);
        long documentsToReview   = documentRepository.countByStatus("UNDER_REVIEW");
        long financeToVerify     = applicationRepository.countPendingVerificationByWorkerUserId(userId);

        Long wid = userId;
        long totalTasks    = workerTaskRepository.countByWorkerIdAndStatus(wid, WorkerTask.TaskStatus.ASSIGNED)
                           + workerTaskRepository.countByWorkerIdAndStatus(wid, WorkerTask.TaskStatus.IN_PROGRESS);
        long tasksPending  = workerTaskRepository.countByWorkerIdAndStatus(wid, WorkerTask.TaskStatus.ASSIGNED);
        long tasksInProg   = workerTaskRepository.countByWorkerIdAndStatus(wid, WorkerTask.TaskStatus.IN_PROGRESS);
        long tasksDone     = workerTaskRepository.countByWorkerIdAndStatus(wid, WorkerTask.TaskStatus.COMPLETED)
                           + workerTaskRepository.countByWorkerIdAndStatus(wid, WorkerTask.TaskStatus.PAYMENT_COLLECTED)
                           + workerTaskRepository.countByWorkerIdAndStatus(wid, WorkerTask.TaskStatus.BIKE_RECOVERED);
        long repair        = workerTaskRepository.countByWorkerIdAndTaskType(wid, WorkerTask.TaskType.REPAIR);
        long collection    = workerTaskRepository.countByWorkerIdAndTaskType(wid, WorkerTask.TaskType.COLLECTION);
        long visit         = workerTaskRepository.countByWorkerIdAndTaskType(wid, WorkerTask.TaskType.VISIT);
        long recovery      = workerTaskRepository.countByWorkerIdAndTaskType(wid, WorkerTask.TaskType.RECOVERY);

        return WorkerDashboardStats.builder()
                .totalAssigned(totalAssigned)
                .pendingVerification(pendingVerification)
                .completedApplications(completed)
                .documentsToReview(documentsToReview)
                .financeToVerify(financeToVerify)
                .totalTasks(totalTasks)
                .tasksPending(tasksPending)
                .tasksInProgress(tasksInProg)
                .tasksCompleted(tasksDone)
                .repairJobs(repair)
                .collectionTasks(collection)
                .visitTasks(visit)
                .recoveryTasks(recovery)
                .build();
    }
}
