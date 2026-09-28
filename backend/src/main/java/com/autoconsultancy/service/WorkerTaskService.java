package com.autoconsultancy.service;

import com.autoconsultancy.dto.request.*;
import com.autoconsultancy.dto.response.WorkerTaskDetailResponse;
import com.autoconsultancy.dto.response.WorkerTaskResponse;
import com.autoconsultancy.entity.*;
import com.autoconsultancy.entity.WorkerTask.TaskStatus;
import com.autoconsultancy.entity.WorkerTask.TaskType;
import com.autoconsultancy.exception.ResourceNotFoundException;
import com.autoconsultancy.exception.UnauthorizedException;
import com.autoconsultancy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkerTaskService {

    private final WorkerTaskRepository     workerTaskRepo;
    private final ServiceJobRepository     serviceJobRepo;
    private final PaymentCollectionRepository paymentCollectionRepo;
    private final FieldVisitRepository     fieldVisitRepo;
    private final BikeRecoveryRepository   bikeRecoveryRepo;
    private final WorkerRepository         workerRepo;
    private final UserRepository           userRepo;
    private final CustomerRepository       customerRepo;
    private final BikeInventoryRepository  bikeInventoryRepo;
    private final ApplicationRepository    applicationRepo;
    private final FinanceDetailRepository  financeDetailRepo;
    private final NotificationService      notificationService;

    // ── Helper: resolve worker ─────────────────────────────────────────────
    private Worker resolveWorker(String email) {
        User u = userRepo.findByEmail(email).orElseThrow();
        return workerRepo.findById(u.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker profile not found"));
    }

    private void ensureOwner(WorkerTask task, String email) {
        Worker w = resolveWorker(email);
        if (!task.getWorker().getId().equals(w.getId()))
            throw new UnauthorizedException("Not authorized to access this task");
    }

    // ── Map helpers ────────────────────────────────────────────────────────
    private WorkerTaskResponse toResponse(WorkerTask t) {
        Customer c = t.getCustomer();
        BikeInventory b = t.getBikeInventory();
        Application a = t.getApplication();

        BigDecimal amountDue = null, amountCollected = null, outstanding = null;
        if (t.getPaymentCollection() != null) {
            amountDue       = t.getPaymentCollection().getAmountDue();
            amountCollected = t.getPaymentCollection().getAmountCollected();
        }
        if (t.getBikeRecovery() != null) outstanding = t.getBikeRecovery().getOutstandingAmount();

        return WorkerTaskResponse.builder()
                .id(t.getId())
                .taskType(t.getTaskType().name())
                .status(t.getStatus().name())
                .priority(t.getPriority().name())
                .title(t.getTitle())
                .description(t.getDescription())
                .adminNotes(t.getAdminNotes())
                .workerNotes(t.getWorkerNotes())
                .dueDate(t.getDueDate())
                .completedAt(t.getCompletedAt())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .workerId(t.getWorker().getId())
                .workerName(t.getWorker().getUser().getFirstName() + " " + t.getWorker().getUser().getLastName())
                .customerId(c != null ? c.getId() : null)
                .customerName(c != null ? c.getUser().getFirstName() + " " + c.getUser().getLastName() : null)
                .customerPhone(c != null ? c.getUser().getPhone() : null)
                .customerEmail(c != null ? c.getUser().getEmail() : null)
                .bikeInventoryId(b != null ? b.getId() : null)
                .bikeCode(b != null ? b.getBikeCode() : null)
                .bikeModel(b != null && b.getBikeModel() != null ? b.getBikeModel().getModelName() : null)
                .registrationNumber(b != null ? b.getRegistrationNumber() : null)
                .applicationId(a != null ? a.getId() : null)
                .applicationNumber(a != null ? a.getApplicationNumber() : null)
                .amountDue(amountDue)
                .amountCollected(amountCollected)
                .outstandingAmount(outstanding)
                .build();
    }

    private WorkerTaskDetailResponse toDetail(WorkerTask t) {
        WorkerTaskResponse base = toResponse(t);
        WorkerTaskDetailResponse.WorkerTaskDetailResponseBuilder b = WorkerTaskDetailResponse.builder().task(base);

        ServiceJob sj = t.getServiceJob();
        if (sj != null) {
            b.sjRegistrationNumber(sj.getRegistrationNumber())
             .sjTaskDescription(sj.getTaskDescription())
             .sjPartsUsed(sj.getPartsUsed())
             .sjLabourCharge(sj.getLabourCharge())
             .sjPartsCharge(sj.getPartsCharge())
             .sjTotalAmount(sj.getTotalAmount())
             .sjPaymentStatus(sj.getPaymentStatus() != null ? sj.getPaymentStatus().name() : null)
             .sjServiceDate(sj.getServiceDate())
             .sjCompletionDate(sj.getCompletionDate())
             .sjNotes(sj.getNotes());
        }

        PaymentCollection pc = t.getPaymentCollection();
        if (pc != null) {
            b.pcAmountDue(pc.getAmountDue())
             .pcAmountCollected(pc.getAmountCollected())
             .pcPaymentMethod(pc.getPaymentMethod() != null ? pc.getPaymentMethod().name() : null)
             .pcCollectionDate(pc.getCollectionDate())
             .pcReceiptNumber(pc.getReceiptNumber())
             .pcUpiReference(pc.getUpiReference())
             .pcNotes(pc.getNotes())
             .pcVerifiedByAdmin(pc.isVerifiedByAdmin());
        }

        FieldVisit fv = t.getFieldVisit();
        if (fv != null) {
            b.fvVisitDate(fv.getVisitDate())
             .fvCustomerContacted(fv.isCustomerContacted())
             .fvAmountCollected(fv.getAmountCollected())
             .fvPromiseToPayDate(fv.getPromiseToPayDate())
             .fvPromiseAmount(fv.getPromiseAmount())
             .fvCustomerResponse(fv.getCustomerResponse())
             .fvLocationNotes(fv.getLocationNotes())
             .fvOutcomeNotes(fv.getOutcomeNotes());
        }

        BikeRecovery br = t.getBikeRecovery();
        if (br != null) {
            b.brOutstandingAmount(br.getOutstandingAmount())
             .brReason(br.getReason())
             .brAuthorizationNumber(br.getAuthorizationNumber())
             .brRecoveryDate(br.getRecoveryDate())
             .brBikeCondition(br.getBikeCondition())
             .brCurrentMileage(br.getCurrentMileage())
             .brExistingDamage(br.getExistingDamage())
             .brAccessoriesReceived(br.isAccessoriesReceived())
             .brKeysReceived(br.isKeysReceived())
             .brDocumentsReceived(br.isDocumentsReceived())
             .brWorkerRecoveryNotes(br.getWorkerRecoveryNotes())
             .brCustomerAcknowledged(br.isCustomerAcknowledged());
        }
        return b.build();
    }

    // ══ WORKER OPERATIONS ══════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<WorkerTaskResponse> getMyTasks(String email, String type, String status, int page, int size) {
        Worker w = resolveWorker(email);
        Long wid = w.getId();
        Pageable pg = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Choose the most-specific DB query to avoid full collection scan
        Page<WorkerTask> tasks;
        if (type != null && status != null) {
            tasks = workerTaskRepo.findByWorkerIdAndTypeAndStatus(wid, type, status, pg);
        } else if (type != null) {
            tasks = workerTaskRepo.findByWorkerIdAndType(wid, type, pg);
        } else if (status != null) {
            tasks = workerTaskRepo.findByWorkerIdAndStatus(wid, status, pg);
        } else {
            tasks = workerTaskRepo.findByWorkerId(wid, pg);
        }
        return tasks.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public WorkerTaskDetailResponse getMyTaskDetail(Long id, String email) {
        WorkerTask t = workerTaskRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        ensureOwner(t, email);
        return toDetail(t);
    }

    @Transactional
    public WorkerTaskResponse updateStatus(Long id, TaskStatusUpdateRequest req, String email) {
        WorkerTask t = workerTaskRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        ensureOwner(t, email);
        t.setStatus(TaskStatus.valueOf(req.getStatus()));
        if (req.getWorkerNotes() != null) t.setWorkerNotes(req.getWorkerNotes());
        if (t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.BIKE_RECOVERED
                || t.getStatus() == TaskStatus.PAYMENT_COLLECTED)
            t.setCompletedAt(LocalDateTime.now());
        return toResponse(workerTaskRepo.save(t));
    }

    @Transactional
    public WorkerTaskDetailResponse updateServiceJob(Long taskId, ServiceJobUpdateRequest req, String email) {
        WorkerTask t = workerTaskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        ensureOwner(t, email);
        ServiceJob sj = serviceJobRepo.findByWorkerTaskId(taskId).orElse(ServiceJob.builder().workerTask(t).build());
        if (req.getRegistrationNumber() != null) sj.setRegistrationNumber(req.getRegistrationNumber());
        if (req.getTaskDescription() != null)    sj.setTaskDescription(req.getTaskDescription());
        if (req.getPartsUsed() != null)          sj.setPartsUsed(req.getPartsUsed());
        if (req.getLabourCharge() != null)       sj.setLabourCharge(req.getLabourCharge());
        if (req.getPartsCharge() != null)        sj.setPartsCharge(req.getPartsCharge());
        if (req.getTotalAmount() != null)        sj.setTotalAmount(req.getTotalAmount());
        if (req.getPaymentStatus() != null)      sj.setPaymentStatus(ServiceJob.PaymentStatus.valueOf(req.getPaymentStatus()));
        if (req.getServiceDate() != null)        sj.setServiceDate(req.getServiceDate());
        if (req.getCompletionDate() != null)     sj.setCompletionDate(req.getCompletionDate());
        if (req.getNotes() != null)              sj.setNotes(req.getNotes());
        serviceJobRepo.save(sj);
        t.setServiceJob(sj);
        return toDetail(workerTaskRepo.save(t));
    }

    @Transactional
    public WorkerTaskDetailResponse recordPayment(Long taskId, PaymentCollectionRequest req, String email) {
        WorkerTask t = workerTaskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        ensureOwner(t, email);
        PaymentCollection pc = paymentCollectionRepo.findByWorkerTaskId(taskId)
                .orElse(PaymentCollection.builder().workerTask(t).build());
        pc.setAmountCollected(req.getAmountCollected());
        if (req.getPaymentMethod() != null)
            pc.setPaymentMethod(PaymentCollection.PaymentMethod.valueOf(req.getPaymentMethod()));
        pc.setCollectionDate(req.getCollectionDate() != null ? req.getCollectionDate() : LocalDate.now());
        if (req.getReceiptNumber() != null) pc.setReceiptNumber(req.getReceiptNumber());
        if (req.getUpiReference() != null)  pc.setUpiReference(req.getUpiReference());
        if (req.getNotes() != null)         pc.setNotes(req.getNotes());
        paymentCollectionRepo.save(pc);
        t.setPaymentCollection(pc);
        t.setStatus(TaskStatus.PAYMENT_COLLECTED);
        t.setCompletedAt(LocalDateTime.now());
        // Update outstanding on linked finance detail
        if (pc.getFinanceDetail() != null && req.getAmountCollected() != null) {
            var fd = pc.getFinanceDetail();
            BigDecimal current = fd.getOutstandingLoanAmount() != null ? fd.getOutstandingLoanAmount() : BigDecimal.ZERO;
            fd.setOutstandingLoanAmount(current.subtract(req.getAmountCollected()).max(BigDecimal.ZERO));
            financeDetailRepo.save(fd);
        }
        return toDetail(workerTaskRepo.save(t));
    }

    @Transactional
    public WorkerTaskDetailResponse updateFieldVisit(Long taskId, FieldVisitUpdateRequest req, String email) {
        WorkerTask t = workerTaskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        ensureOwner(t, email);
        FieldVisit fv = fieldVisitRepo.findByWorkerTaskId(taskId)
                .orElse(FieldVisit.builder().workerTask(t).build());
        if (req.getVisitDate() != null)       fv.setVisitDate(req.getVisitDate());
        fv.setCustomerContacted(req.isCustomerContacted());
        if (req.getAmountCollected() != null) fv.setAmountCollected(req.getAmountCollected());
        if (req.getPromiseToPayDate() != null) fv.setPromiseToPayDate(req.getPromiseToPayDate());
        if (req.getPromiseAmount() != null)   fv.setPromiseAmount(req.getPromiseAmount());
        if (req.getCustomerResponse() != null) fv.setCustomerResponse(req.getCustomerResponse());
        if (req.getLocationNotes() != null)   fv.setLocationNotes(req.getLocationNotes());
        if (req.getOutcomeNotes() != null)    fv.setOutcomeNotes(req.getOutcomeNotes());
        fieldVisitRepo.save(fv);
        t.setFieldVisit(fv);
        if (req.getNewStatus() != null) t.setStatus(TaskStatus.valueOf(req.getNewStatus()));
        return toDetail(workerTaskRepo.save(t));
    }

    @Transactional
    public WorkerTaskDetailResponse updateRecovery(Long taskId, BikeRecoveryUpdateRequest req, String email) {
        WorkerTask t = workerTaskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        ensureOwner(t, email);
        BikeRecovery br = bikeRecoveryRepo.findByWorkerTaskId(taskId)
                .orElse(BikeRecovery.builder().workerTask(t).build());
        if (req.getRecoveryDate() != null)    br.setRecoveryDate(req.getRecoveryDate());
        if (req.getBikeCondition() != null)   br.setBikeCondition(req.getBikeCondition());
        if (req.getCurrentMileage() != null)  br.setCurrentMileage(req.getCurrentMileage());
        if (req.getExistingDamage() != null)  br.setExistingDamage(req.getExistingDamage());
        br.setAccessoriesReceived(req.isAccessoriesReceived());
        br.setKeysReceived(req.isKeysReceived());
        br.setDocumentsReceived(req.isDocumentsReceived());
        if (req.getWorkerRecoveryNotes() != null) br.setWorkerRecoveryNotes(req.getWorkerRecoveryNotes());
        br.setCustomerAcknowledged(req.isCustomerAcknowledged());
        bikeRecoveryRepo.save(br);
        t.setBikeRecovery(br);
        if (req.getNewStatus() != null) {
            t.setStatus(TaskStatus.valueOf(req.getNewStatus()));
            if (t.getStatus() == TaskStatus.BIKE_RECOVERED) t.setCompletedAt(LocalDateTime.now());
        }
        return toDetail(workerTaskRepo.save(t));
    }

    // ── Today's summary for dashboard ─────────────────────────────────────
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getTodaySummary(String email) {
        Worker w = resolveWorker(email);
        Long wid = w.getId();
        LocalDate today = LocalDate.now();
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("totalTasks",       workerTaskRepo.countByWorkerIdAndStatus(wid, TaskStatus.ASSIGNED)
                                  + workerTaskRepo.countByWorkerIdAndStatus(wid, TaskStatus.IN_PROGRESS));
        map.put("assigned",         workerTaskRepo.countByWorkerIdAndStatus(wid, TaskStatus.ASSIGNED));
        map.put("inProgress",       workerTaskRepo.countByWorkerIdAndStatus(wid, TaskStatus.IN_PROGRESS));
        map.put("completedToday",   workerTaskRepo.countByWorkerIdAndDueDateAndStatusNot(wid, today, TaskStatus.ASSIGNED));
        map.put("repairJobs",       workerTaskRepo.countByWorkerIdAndTaskType(wid, TaskType.REPAIR));
        map.put("collectionTasks",  workerTaskRepo.countByWorkerIdAndTaskType(wid, TaskType.COLLECTION));
        map.put("visitTasks",       workerTaskRepo.countByWorkerIdAndTaskType(wid, TaskType.VISIT));
        map.put("recoveryTasks",    workerTaskRepo.countByWorkerIdAndTaskType(wid, TaskType.RECOVERY));
        return map;
    }

    // ══ ADMIN OPERATIONS ═══════════════════════════════════════════════════

    @Transactional
    public WorkerTaskResponse adminCreateTask(WorkerTaskRequest req, String adminEmail) {
        User admin = userRepo.findByEmail(adminEmail).orElseThrow();
        Worker worker = workerRepo.findById(req.getWorkerId())
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + req.getWorkerId()));

        Customer customer = req.getCustomerId() != null
                ? customerRepo.findById(req.getCustomerId()).orElse(null) : null;
        BikeInventory bike = req.getBikeInventoryId() != null
                ? bikeInventoryRepo.findById(req.getBikeInventoryId()).orElse(null) : null;
        Application app = req.getApplicationId() != null
                ? applicationRepo.findById(req.getApplicationId()).orElse(null) : null;

        TaskType type = TaskType.valueOf(req.getTaskType());
        WorkerTask.Priority priority = req.getPriority() != null
                ? WorkerTask.Priority.valueOf(req.getPriority()) : WorkerTask.Priority.NORMAL;

        // Initial status depends on task type
        TaskStatus initStatus = (type == TaskType.RECOVERY) ? TaskStatus.RECOVERY_ASSIGNED : TaskStatus.ASSIGNED;

        WorkerTask task = WorkerTask.builder()
                .taskType(type)
                .status(initStatus)
                .priority(priority)
                .title(req.getTitle())
                .description(req.getDescription())
                .worker(worker)
                .customer(customer)
                .bikeInventory(bike)
                .application(app)
                .createdBy(admin)
                .dueDate(req.getDueDate())
                .adminNotes(req.getAdminNotes())
                .build();
        task = workerTaskRepo.save(task);

        // Create sub-detail record
        if (type == TaskType.REPAIR) {
            ServiceJob sj = ServiceJob.builder().workerTask(task).build();
            if (bike != null) sj.setRegistrationNumber(bike.getRegistrationNumber());
            serviceJobRepo.save(sj);
        } else if (type == TaskType.COLLECTION) {
            FinanceDetail fd = req.getFinanceDetailId() != null
                    ? financeDetailRepo.findById(req.getFinanceDetailId()).orElse(null) : null;
            PaymentCollection pc = PaymentCollection.builder()
                    .workerTask(task)
                    .financeDetail(fd)
                    .amountDue(req.getAmountDue())
                    .build();
            paymentCollectionRepo.save(pc);
        } else if (type == TaskType.VISIT) {
            FieldVisit fv = FieldVisit.builder().workerTask(task).build();
            fieldVisitRepo.save(fv);
        } else if (type == TaskType.RECOVERY) {
            BikeRecovery br = BikeRecovery.builder()
                    .workerTask(task)
                    .outstandingAmount(req.getOutstandingAmount())
                    .reason(req.getReason())
                    .authorizationNumber(req.getAuthorizationNumber())
                    .build();
            bikeRecoveryRepo.save(br);
        }

        // Notify worker
        if (worker.getUser() != null) {
            notificationService.createNotification(worker.getUser(),
                    "New Task Assigned: " + req.getTitle(),
                    "You have a new " + type.name().toLowerCase() + " task assigned to you.",
                    "INFO", null);
        }
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public Page<WorkerTaskResponse> adminGetAllTasks(String type, String status, Long workerId, int page, int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Choose the most-specific DB query to avoid full collection scan
        Page<WorkerTask> tasks;
        if (workerId != null && type != null && status != null) {
            tasks = workerTaskRepo.findAllByWorkerTypeStatusAdmin(workerId, type, status, pg);
        } else if (workerId != null && type != null) {
            tasks = workerTaskRepo.findAllByWorkerAndTypeAdmin(workerId, type, pg);
        } else if (workerId != null && status != null) {
            tasks = workerTaskRepo.findAllByWorkerAndStatusAdmin(workerId, status, pg);
        } else if (workerId != null) {
            tasks = workerTaskRepo.findAllByWorkerIdAdmin(workerId, pg);
        } else if (type != null && status != null) {
            tasks = workerTaskRepo.findAllByTypeAndStatusAdmin(type, status, pg);
        } else if (type != null) {
            tasks = workerTaskRepo.findAllByTypeAdmin(type, pg);
        } else if (status != null) {
            tasks = workerTaskRepo.findAllByStatusAdmin(status, pg);
        } else {
            tasks = workerTaskRepo.findAllByOrderByCreatedAtDesc(pg);
        }
        return tasks.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public WorkerTaskDetailResponse adminGetTaskDetail(Long id) {
        WorkerTask t = workerTaskRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        return toDetail(t);
    }

    @Transactional
    public WorkerTaskResponse adminUpdateTask(Long id, WorkerTaskRequest req) {
        WorkerTask t = workerTaskRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (req.getTitle() != null)      t.setTitle(req.getTitle());
        if (req.getDescription() != null) t.setDescription(req.getDescription());
        if (req.getPriority() != null)   t.setPriority(WorkerTask.Priority.valueOf(req.getPriority()));
        if (req.getDueDate() != null)    t.setDueDate(req.getDueDate());
        if (req.getAdminNotes() != null) t.setAdminNotes(req.getAdminNotes());
        if (req.getWorkerId() != null) {
            Worker w = workerRepo.findById(req.getWorkerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Worker not found"));
            t.setWorker(w);
        }
        return toResponse(workerTaskRepo.save(t));
    }

    @Transactional
    public void adminCancelTask(Long id) {
        WorkerTask t = workerTaskRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        t.setStatus(TaskStatus.CANCELLED);
        workerTaskRepo.save(t);
    }
}
