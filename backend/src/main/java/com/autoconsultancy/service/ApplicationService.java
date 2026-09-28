package com.autoconsultancy.service;

import com.autoconsultancy.dto.request.ApplicationRequest;
import com.autoconsultancy.dto.request.BikeDetailRequest;
import com.autoconsultancy.dto.request.FinanceDetailRequest;
import com.autoconsultancy.dto.request.StatusUpdateRequest;
import com.autoconsultancy.dto.request.WorkerAssignmentRequest;
import com.autoconsultancy.dto.response.*;
import com.autoconsultancy.entity.*;
import com.autoconsultancy.exception.BadRequestException;
import com.autoconsultancy.exception.ResourceNotFoundException;
import com.autoconsultancy.exception.UnauthorizedException;
import com.autoconsultancy.repository.*;
import com.autoconsultancy.entity.BikeInventory;
import com.autoconsultancy.util.EmiCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final BikeModelRepository bikeModelRepository;
    private final BikeVariantRepository bikeVariantRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final WorkerRepository workerRepository;
    private final WorkerAssignmentRepository workerAssignmentRepository;
    private final NotificationService notificationService;
    private final BikeDetailRepository bikeDetailRepository;
    private final FinanceDetailRepository financeDetailRepository;
    private final BikeInventoryRepository bikeInventoryRepository;
    private final BikeInventoryImageRepository bikeInventoryImageRepository;
    private final EmiOverdueService emiOverdueService;
    private final MongoTemplate mongoTemplate;

    private String generateApplicationNumber() {
        // Use MAX(id)+1 to ensure uniqueness even after deletions.
        // count()+1 would cause duplicate key if any rows were deleted.
        Long maxId = applicationRepository.findMaxId();
        long next  = (maxId == null ? 0L : maxId) + 1;
        return String.format("AUTO-%d-%05d", LocalDateTime.now().getYear(), next);
    }

    private String buildBikeImageUrl(BikeModel bike) {
        if (bike == null || bike.getImages() == null || bike.getImages().isEmpty()) return null;
        return bike.getImages().stream()
                .filter(BikeImage::isPrimary)
                .findFirst()
                .or(() -> bike.getImages().stream().findFirst())
                .map(img -> "/api/bike-models/images/" + img.getId() + "/file")
                .orElse(null);
    }

    private ApplicationResponse mapToSummary(Application app) {
        BikeModel bikeModel = app.getBikeDetail() != null ? app.getBikeDetail().getBikeModel() : null;
        BikeInventory inventory = app.getBikeDetail() != null ? app.getBikeDetail().getBikeInventory() : null;

        // Prefer BikeInventory image over BikeModel image
        String imageUrl = inventory != null ? buildInventoryImageUrl(inventory) : buildBikeImageUrl(bikeModel);
        String saleStatus = inventory != null ? inventory.getSaleStatus().name()
                : (bikeModel != null && bikeModel.getSaleStatus() != null ? bikeModel.getSaleStatus().name() : null);

        return ApplicationResponse.builder()
                .id(app.getId())
                .applicationNumber(app.getApplicationNumber())
                .customerName(app.getCustomer() != null && app.getCustomer().getUser() != null ? app.getCustomer().getUser().getFirstName() + " " + app.getCustomer().getUser().getLastName() : null)
                .customerEmail(app.getCustomer() != null && app.getCustomer().getUser() != null ? app.getCustomer().getUser().getEmail() : null)
                .customerPhone(app.getCustomer() != null && app.getCustomer().getUser() != null ? app.getCustomer().getUser().getPhone() : null)
                .manufacturerName(app.getBikeDetail() != null && app.getBikeDetail().getManufacturer() != null ? app.getBikeDetail().getManufacturer().getName() : null)
                .modelName(app.getBikeDetail() != null && app.getBikeDetail().getBikeModel() != null ? app.getBikeDetail().getBikeModel().getModelName() : null)
                .variantName(app.getBikeDetail() != null && app.getBikeDetail().getVariant() != null ? app.getBikeDetail().getVariant().getVariantName() : null)
                .manufacturingYear(app.getBikeDetail() != null ? app.getBikeDetail().getManufacturingYear() : null)
                .registrationNumber(app.getBikeDetail() != null ? app.getBikeDetail().getRegistrationNumber() : null)
                .status(app.getStatus().name())
                .underFinance(app.getFinanceDetail() != null ? app.getFinanceDetail().isUnderFinance() : null)
                .financeCompany(app.getFinanceDetail() != null ? app.getFinanceDetail().getFinanceCompany() : null)
                // Finance computed fields — all from FinanceDetail
                .loanAmount(app.getFinanceDetail() != null ? app.getFinanceDetail().getLoanAmount() : null)
                .annualInterestRate(app.getFinanceDetail() != null ? app.getFinanceDetail().getAnnualInterestRate() : null)
                .tenureYears(app.getFinanceDetail() != null ? app.getFinanceDetail().getTenureYears() : null)
                .tenureMonths(app.getFinanceDetail() != null ? app.getFinanceDetail().getTenureMonths() : null)
                .emiAmount(app.getFinanceDetail() != null ? app.getFinanceDetail().getEmiAmount() : null)
                .totalPayable(app.getFinanceDetail() != null ? app.getFinanceDetail().getTotalPayable() : null)
                .totalInterest(app.getFinanceDetail() != null ? app.getFinanceDetail().getTotalInterest() : null)
                .numberOfEmis(app.getFinanceDetail() != null ? app.getFinanceDetail().getNumberOfEmis() : null)
                .paidEmis(app.getFinanceDetail() != null ? app.getFinanceDetail().getPaidEmis() : null)
                .remainingEmis(app.getFinanceDetail() != null ? app.getFinanceDetail().getRemainingEmis() : null)
                .loanClosureStatus(app.getFinanceDetail() != null ? app.getFinanceDetail().getLoanClosureStatus() : null)
                .financeStatus(app.getFinanceDetail() != null ? app.getFinanceDetail().getFinanceStatus() : null)
                .outstandingLoanAmount(app.getFinanceDetail() != null ? app.getFinanceDetail().getOutstandingLoanAmount() : null)
                // EMI overdue tracking fields
                .financeDetailId(app.getFinanceDetail() != null ? app.getFinanceDetail().getId() : null)
                .missedEmiMonths(app.getFinanceDetail() != null ? app.getFinanceDetail().getMissedEmiMonths() : null)
                .overdueStatus(app.getFinanceDetail() != null ? app.getFinanceDetail().getOverdueStatus() : null)
                .lastEmiPaidDate(app.getFinanceDetail() != null ? app.getFinanceDetail().getLastEmiPaidDate() : null)
                .loanStartDate(app.getFinanceDetail() != null ? app.getFinanceDetail().getLoanStartDate() : null)
                .nextEmiDueDate(app.getFinanceDetail() != null ? app.getFinanceDetail().getNextEmiDueDate() : null)
                .assignedWorkerName(app.getWorkerAssignment() != null && app.getWorkerAssignment().getWorker() != null && app.getWorkerAssignment().getWorker().getUser() != null ? app.getWorkerAssignment().getWorker().getUser().getFirstName() : null)
                .createdAt(app.getCreatedAt())
                .submittedAt(app.getSubmittedAt())
                .updatedAt(app.getUpdatedAt())
                // Bike sale info
                .bikeModelId(bikeModel != null ? bikeModel.getId() : null)
                .bikeSaleStatus(saleStatus)
                .bikeImageUrl(imageUrl)
                .bikeCategory(bikeModel != null ? bikeModel.getCategory() : (inventory != null && inventory.getBikeModel() != null ? inventory.getBikeModel().getCategory() : null))
                .bikeFuelType(bikeModel != null ? bikeModel.getFuelType() : (inventory != null ? inventory.getFuelType() : null))
                // Bike Inventory reference
                .bikeInventoryId(inventory != null ? inventory.getId() : null)
                .bikeCode(inventory != null ? inventory.getBikeCode() : null)
                // Price comes from BikeInventory (BikeModel has no price field)
                .bikePrice(inventory != null ? inventory.getPrice() : null)
                .build();
    }

    private String buildInventoryImageUrl(BikeInventory inv) {
        return bikeInventoryImageRepository
                .findByBikeInventoryIdAndPrimaryTrue(inv.getId())
                .or(() -> bikeInventoryImageRepository
                        .findByBikeInventoryIdOrderByPrimaryDescCreatedAtAsc(inv.getId()).stream().findFirst())
                .map(img -> "/api/bikes/" + inv.getId() + "/images/" + img.getId() + "/file")
                .orElse(null);
    }

    @Transactional
    public ApplicationResponse create(String email, ApplicationRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Customer customer = customerRepository.findById(user.getId()).orElseThrow();

        Application app = Application.builder()
                .applicationNumber(generateApplicationNumber())
                .customer(customer)
                .status(Application.ApplicationStatus.DRAFT)
                .build();

        // ── PRIMARY PATH: bikeInventoryId (exact physical bike from Browse Bikes) ─
        if (request != null && request.getBikeInventoryId() != null) {
            BikeInventory inventory = bikeInventoryRepository.findById(request.getBikeInventoryId())
                    .filter(BikeInventory::isActive)
                    .orElseThrow(() -> new ResourceNotFoundException("Bike not found"));

            // BACKEND VALIDATION: must be AVAILABLE and have at least one image
            if (inventory.getSaleStatus() != BikeInventory.SaleStatus.AVAILABLE) {
                throw new BadRequestException("This bike is not available for sale.");
            }
            long imgCount = bikeInventoryImageRepository.countByBikeInventoryId(inventory.getId());
            if (imgCount == 0) {
                throw new BadRequestException("Bike cannot be applied for: no images uploaded.");
            }

            // Pre-fill BikeDetail with inventory + model data
            BikeModel bikeModel = inventory.getBikeModel();
            BikeDetail bd = new BikeDetail();
            bd.setApplication(app);
            bd.setManufacturer(bikeModel.getManufacturer());
            bd.setBikeModel(bikeModel);
            bd.setBikeInventory(inventory);
            app.setBikeDetail(bd);

        } else if (request != null && request.getBikeModelId() != null) {
            // LEGACY PATH: bikeModelId only (backward compat — uses old BikeModel sale status)
            BikeModel bikeModel = bikeModelRepository.findById(request.getBikeModelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bike model not found"));

            Boolean avail = bikeModel.getAvailableForSale();
            BikeModel.SaleStatus status = bikeModel.getSaleStatus();
            if (avail == null || !avail || status == null || status != BikeModel.SaleStatus.AVAILABLE) {
                throw new BadRequestException("This bike is currently not available for sale.");
            }

            BikeDetail bd = new BikeDetail();
            bd.setApplication(app);
            bd.setManufacturer(bikeModel.getManufacturer());
            bd.setBikeModel(bikeModel);
            app.setBikeDetail(bd);
        }

        return mapToSummary(applicationRepository.save(app));
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return applicationRepository.findByCustomerId(user.getId()).stream()
                .map(this::mapToSummary).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApplicationDetailResponse getApplicationDetail(Long id, String email) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        User user = userRepository.findByEmail(email).orElseThrow();

        // Access control
        if (user.getRole() == Role.CUSTOMER && !app.getCustomer().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized access to application");
        }
        if (user.getRole() == Role.WORKER &&
                (app.getWorkerAssignment() == null || !app.getWorkerAssignment().getWorker().getUser().getId().equals(user.getId()))) {
            throw new UnauthorizedException("Not assigned to this application");
        }

        // Map bike detail
        BikeDetailResponse bikeDetailResponse = null;
        if (app.getBikeDetail() != null) {
            BikeDetail bd = app.getBikeDetail();
            bikeDetailResponse = BikeDetailResponse.builder()
                    .id(bd.getId())
                    .manufacturerId(bd.getManufacturer() != null ? bd.getManufacturer().getId() : null)
                    .manufacturerName(bd.getManufacturer() != null ? bd.getManufacturer().getName() : null)
                    .bikeModelId(bd.getBikeModel() != null ? bd.getBikeModel().getId() : null)
                    .modelName(bd.getBikeModel() != null ? bd.getBikeModel().getModelName() : null)
                    .variantId(bd.getVariant() != null ? bd.getVariant().getId() : null)
                    .variantName(bd.getVariant() != null ? bd.getVariant().getVariantName() : null)
                    .manufacturingYear(bd.getManufacturingYear())
                    .registrationNumber(bd.getRegistrationNumber())
                    .colour(bd.getColour())
                    .purchaseDate(bd.getPurchaseDate())
                    .build();
        }

        // Map finance detail
        FinanceDetailResponse financeDetailResponse = null;
        if (app.getFinanceDetail() != null) {
            FinanceDetail fd = app.getFinanceDetail();
            financeDetailResponse = FinanceDetailResponse.builder()
                    .id(fd.getId())
                    .underFinance(fd.isUnderFinance())
                    .financeCompany(fd.getFinanceCompany())
                    .loanAccountNumber(fd.getLoanAccountNumber())
                    .loanStartDate(fd.getLoanStartDate())
                    .loanAmount(fd.getLoanAmount())
                    .annualInterestRate(fd.getAnnualInterestRate())
                    .tenureYears(fd.getTenureYears())
                    .tenureMonths(fd.getTenureMonths())
                    .emiAmount(fd.getEmiAmount())
                    .numberOfEmis(fd.getNumberOfEmis())
                    .paidEmis(fd.getPaidEmis())
                    .remainingEmis(fd.getRemainingEmis())
                    .totalPayable(fd.getTotalPayable())
                    .totalInterest(fd.getTotalInterest())
                    .outstandingLoanAmount(fd.getOutstandingLoanAmount())
                    .nextEmiDueDate(fd.getNextEmiDueDate())
                    .emiPaymentStatus(fd.getEmiPaymentStatus())
                    .loanClosureStatus(fd.getLoanClosureStatus())
                    .nocAvailable(fd.isNocAvailable())
                    .bikePaid(fd.isBikePaid())
                    .purchasePaymentMethod(fd.getPurchasePaymentMethod())
                    .financeStatus(fd.getFinanceStatus())
                    .build();
        }

        // Map documents
        List<DocumentResponse> docs = app.getDocuments() == null ? Collections.emptyList() :
                app.getDocuments().stream().map(doc -> DocumentResponse.builder()
                        .id(doc.getId())
                        .applicationId(app.getId())
                        .documentType(doc.getDocumentType())
                        .originalFileName(doc.getOriginalFileName())
                        .fileName(doc.getFileName())
                        .fileSize(doc.getFileSize())
                        .mimeType(doc.getMimeType())
                        .status(doc.getStatus())
                        .remarks(doc.getRemarks())
                        .uploadedAt(doc.getUploadedAt())
                        .reviewedAt(doc.getReviewedAt())
                        .uploadedByName(doc.getUploadedByUser() != null ?
                                doc.getUploadedByUser().getFirstName() + " " + doc.getUploadedByUser().getLastName() : null)
                        .reviewedByName(doc.getReviewedBy() != null ?
                                doc.getReviewedBy().getFirstName() + " " + doc.getReviewedBy().getLastName() : null)
                        .build()).collect(Collectors.toList());

        // Map status history
        List<StatusHistoryResponse> history = app.getStatusHistory() == null ? Collections.emptyList() :
                app.getStatusHistory().stream().map(h -> StatusHistoryResponse.builder()
                        .id(h.getId())
                        .previousStatus(h.getPreviousStatus())
                        .newStatus(h.getNewStatus())
                        .remarks(h.getRemarks())
                        .changedAt(h.getChangedAt())
                        .changedByName(h.getChangedBy() != null ?
                                h.getChangedBy().getFirstName() + " " + h.getChangedBy().getLastName() : null)
                        .build()).collect(Collectors.toList());

        CustomerSummaryDto customerDto = null;
        if (app.getCustomer() != null && app.getCustomer().getUser() != null) {
            customerDto = CustomerSummaryDto.builder()
                    .id(app.getCustomer().getId())
                    .fullName(app.getCustomer().getUser().getFirstName() + " " + app.getCustomer().getUser().getLastName())
                    .email(app.getCustomer().getUser().getEmail())
                    .phone(app.getCustomer().getUser().getPhone())
                    .city(app.getCustomer().getCity())
                    .state(app.getCustomer().getState())
                    .build();
        }

        // Map worker assignment
        WorkerAssignmentResponse workerAssignmentResponse = null;
        if (app.getWorkerAssignment() != null) {
            WorkerAssignment wa = app.getWorkerAssignment();
            workerAssignmentResponse = WorkerAssignmentResponse.builder()
                    .id(wa.getId())
                    .workerName(wa.getWorker().getUser().getFirstName() + " " + wa.getWorker().getUser().getLastName())
                    .workerEmail(wa.getWorker().getUser().getEmail())
                    .assignedAt(wa.getAssignedAt())
                    .notes(wa.getNotes())
                    .build();
        }

        BikeModel appBikeModel = app.getBikeDetail() != null ? app.getBikeDetail().getBikeModel() : null;
        BikeInventory appInventory = app.getBikeDetail() != null ? app.getBikeDetail().getBikeInventory() : null;

        String detailSaleStatus = appInventory != null ? appInventory.getSaleStatus().name()
                : (appBikeModel != null && appBikeModel.getSaleStatus() != null ? appBikeModel.getSaleStatus().name() : null);
        String detailImageUrl = appInventory != null ? buildInventoryImageUrl(appInventory) : buildBikeImageUrl(appBikeModel);

        return ApplicationDetailResponse.builder()
                .id(app.getId())
                .applicationNumber(app.getApplicationNumber())
                .status(app.getStatus().name())
                .remarks(app.getRemarks())
                .customer(customerDto)
                .bikeDetail(bikeDetailResponse)
                .financeDetail(financeDetailResponse)
                .documents(docs)
                .statusHistory(history)
                .workerAssignment(workerAssignmentResponse)
                // Flat customer fields
                .customerName(customerDto != null ? customerDto.getFullName() : null)
                .customerEmail(customerDto != null ? customerDto.getEmail() : null)
                .customerPhone(customerDto != null ? customerDto.getPhone() : null)
                // Bike sale info
                .bikeModelId(appBikeModel != null ? appBikeModel.getId() : null)
                .bikeSaleStatus(detailSaleStatus)
                .bikeImageUrl(detailImageUrl)
                .bikeCategory(appBikeModel != null ? appBikeModel.getCategory() : null)
                .bikeFuelType(appBikeModel != null ? appBikeModel.getFuelType() : (appInventory != null ? appInventory.getFuelType() : null))
                // Price: prefer BikeInventory price
                .bikePrice(appInventory != null ? appInventory.getPrice() : null)
                // Flat bike name fields (from inventory → model)
                .manufacturerName(appBikeModel != null && appBikeModel.getManufacturer() != null ? appBikeModel.getManufacturer().getName() : null)
                .modelName(appBikeModel != null ? appBikeModel.getModelName() : null)
                .variantName(bikeDetailResponse != null ? bikeDetailResponse.getVariantName() : null)
                .manufacturingYear(bikeDetailResponse != null ? bikeDetailResponse.getManufacturingYear() : null)
                .registrationNumber(bikeDetailResponse != null ? bikeDetailResponse.getRegistrationNumber() : (appInventory != null ? appInventory.getRegistrationNumber() : null))
                // Bike Inventory reference
                .bikeInventoryId(appInventory != null ? appInventory.getId() : null)
                .bikeCode(appInventory != null ? appInventory.getBikeCode() : null)
                .createdAt(app.getCreatedAt())
                .submittedAt(app.getSubmittedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }


    @Transactional
    public ApplicationResponse submit(Long id, String email) {
        Application app = applicationRepository.findById(id).orElseThrow();
        User user = userRepository.findByEmail(email).orElseThrow();
        if (!app.getCustomer().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized");
        }
        app.setStatus(Application.ApplicationStatus.SUBMITTED);
        app.setSubmittedAt(LocalDateTime.now());
        
        notificationService.createNotification(user, "Application Submitted", "Your application " + app.getApplicationNumber() + " has been submitted.", "SUCCESS", app.getId());
        
        return mapToSummary(applicationRepository.save(app));
    }

    @Transactional
    public ApplicationResponse updateBikeDetails(Long id, BikeDetailRequest request, String email) {
        Application app = applicationRepository.findById(id).orElseThrow();
        User user = userRepository.findByEmail(email).orElseThrow();
        if (user.getRole() == Role.CUSTOMER && !app.getCustomer().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized");
        }

        BikeDetail bd = app.getBikeDetail();
        if (bd == null) {
            bd = new BikeDetail();
            bd.setApplication(app);
            app.setBikeDetail(bd);
        }
        
        if (request.getManufacturerId() != null) bd.setManufacturer(manufacturerRepository.findById(request.getManufacturerId()).orElse(null));
        if (request.getBikeModelId() != null) bd.setBikeModel(bikeModelRepository.findById(request.getBikeModelId()).orElse(null));
        if (request.getVariantId() != null) bd.setVariant(bikeVariantRepository.findById(request.getVariantId()).orElse(null));
        
        bd.setManufacturingYear(request.getManufacturingYear());
        bd.setRegistrationNumber(request.getRegistrationNumber());
        bd.setColour(request.getColour());
        bd.setPurchaseDate(request.getPurchaseDate());

        bikeDetailRepository.save(bd);
        return mapToSummary(applicationRepository.save(app));
    }

    @Transactional
    public ApplicationResponse updateFinanceDetails(Long id, FinanceDetailRequest request, String email) {
        Application app = applicationRepository.findById(id).orElseThrow();
        User user = userRepository.findByEmail(email).orElseThrow();
        if (user.getRole() == Role.CUSTOMER && !app.getCustomer().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized");
        }

        FinanceDetail fd = app.getFinanceDetail();
        if (fd == null) {
            fd = new FinanceDetail();
            fd.setApplication(app);
            app.setFinanceDetail(fd);
        }

        fd.setUnderFinance(request.getUnderFinance() != null ? request.getUnderFinance() : false);

        if (fd.isUnderFinance()) {
            // ── Mandatory loan fields ──────────────────────────────────────────
            BigDecimal principal       = request.getLoanAmount();
            BigDecimal annualRate      = request.getAnnualInterestRate();

            // Prefer tenureMonths (new); fall back to tenureYears*12 (legacy)
            Integer tenureMonths = request.getTenureMonths();
            if ((tenureMonths == null || tenureMonths < 1) && request.getTenureYears() != null && request.getTenureYears() > 0) {
                tenureMonths = request.getTenureYears() * 12;
            }

            if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0)
                throw new BadRequestException("Loan amount must be greater than 0.");
            if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) < 0)
                throw new BadRequestException("Annual interest rate cannot be negative.");
            if (tenureMonths == null || tenureMonths < 1)
                throw new BadRequestException("Tenure must be at least 1 month.");

            // ── Server-side EMI calculation (authoritative — Full/Flat Interest) ───
            int        totalMonths   = tenureMonths;
            BigDecimal totalPayable  = EmiCalculator.totalPayable(principal, annualRate, totalMonths);
            BigDecimal totalInterest = EmiCalculator.totalInterest(totalPayable, principal);
            BigDecimal emi           = EmiCalculator.monthlyEmi(principal, annualRate, totalMonths);
            int        paidEmis      = request.getPaidEmis() != null ? request.getPaidEmis() : 0;
            int        remainingEmis = EmiCalculator.remainingEmis(totalMonths, paidEmis);

            // ── Store all calculated values ────────────────────────────────────
            fd.setLoanAmount(principal);
            fd.setAnnualInterestRate(annualRate);
            fd.setTenureMonths(tenureMonths);           // primary months field
            fd.setTenureYears(null);                   // clear legacy field (months is authoritative)
            fd.setEmiAmount(emi);
            fd.setNumberOfEmis(totalMonths);
            fd.setPaidEmis(paidEmis);
            fd.setRemainingEmis(remainingEmis);
            fd.setTotalPayable(totalPayable);
            fd.setTotalInterest(totalInterest);

            // ── Optional loan tracking fields ──────────────────────────────────
            fd.setFinanceCompany(request.getFinanceCompany());
            fd.setLoanAccountNumber(request.getLoanAccountNumber());
            fd.setLoanStartDate(request.getLoanStartDate());
            fd.setOutstandingLoanAmount(request.getOutstandingLoanAmount());
            fd.setNextEmiDueDate(request.getNextEmiDueDate());
            fd.setEmiPaymentStatus(request.getEmiPaymentStatus());
            fd.setLoanClosureStatus(request.getLoanClosureStatus());
            fd.setNocAvailable(request.getNocAvailable() != null ? request.getNocAvailable() : false);
        } else {
            fd.setBikePaid(request.getBikePaid() != null ? request.getBikePaid() : false);
            fd.setPurchasePaymentMethod(request.getPurchasePaymentMethod());
        }

        if (request.getFinanceStatus() != null) {
            fd.setFinanceStatus(request.getFinanceStatus());
        }

        financeDetailRepository.save(fd);

        // Initialize / refresh overdue status immediately after finance detail is saved
        if (fd.isUnderFinance()) {
            emiOverdueService.refreshOverdueStatus(fd);
        }

        return mapToSummary(applicationRepository.save(app));
    }

    @Transactional
    public ApplicationResponse updateStatus(Long id, StatusUpdateRequest request, String email) {
        Application app = applicationRepository.findById(id).orElseThrow();
        User user = userRepository.findByEmail(email).orElseThrow();
        
        Application.ApplicationStatus newStatus = Application.ApplicationStatus.valueOf(request.getStatus());
        String oldStatus = app.getStatus().name();
        app.setStatus(newStatus);
        app.setRemarks(request.getRemarks());
        
        ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                .application(app)
                .previousStatus(oldStatus)
                .newStatus(newStatus.name())
                .changedBy(user)
                .remarks(request.getRemarks())
                .build();
        statusHistoryRepository.save(history);

        // ── Auto-update BikeInventory sale status based on application transitions ──
        BikeInventory inventory = app.getBikeDetail() != null ? app.getBikeDetail().getBikeInventory() : null;
        if (inventory != null) {
            switch (newStatus) {
                case APPROVED  -> inventory.setSaleStatus(BikeInventory.SaleStatus.RESERVED);
                case COMPLETED -> inventory.setSaleStatus(BikeInventory.SaleStatus.SOLD);
                case REJECTED  -> {
                    if (inventory.getSaleStatus() == BikeInventory.SaleStatus.RESERVED) {
                        inventory.setSaleStatus(BikeInventory.SaleStatus.AVAILABLE);
                    }
                }
                default -> { /* no status change */ }
            }
            bikeInventoryRepository.save(inventory);
        }

        // ── Also update legacy BikeModel sale status (backward compat) ─────────
        BikeModel bikeModel = app.getBikeDetail() != null ? app.getBikeDetail().getBikeModel() : null;
        if (bikeModel != null && inventory == null) {
            // Only update BikeModel status if this application is NOT linked to BikeInventory
            switch (newStatus) {
                case APPROVED  -> bikeModel.setSaleStatus(BikeModel.SaleStatus.RESERVED);
                case COMPLETED -> bikeModel.setSaleStatus(BikeModel.SaleStatus.SOLD);
                case REJECTED  -> {
                    if (bikeModel.getSaleStatus() == BikeModel.SaleStatus.RESERVED) {
                        bikeModel.setSaleStatus(BikeModel.SaleStatus.AVAILABLE);
                        bikeModel.setAvailableForSale(true);
                    }
                }
                default -> {}
            }
            bikeModelRepository.save(bikeModel);
        }
        
        notificationService.createNotification(app.getCustomer().getUser(), "Status Updated", "Your application status changed to " + newStatus.name(), "INFO", app.getId());
        
        return mapToSummary(applicationRepository.save(app));
    }

    @Transactional
    public ApplicationResponse assignWorker(Long id, WorkerAssignmentRequest request, String email) {
        Application app = applicationRepository.findById(id).orElseThrow();
        User admin = userRepository.findByEmail(email).orElseThrow();
        Worker worker = workerRepository.findById(request.getWorkerId()).orElseThrow();
        
        WorkerAssignment assignment = app.getWorkerAssignment();
        if (assignment == null) {
            assignment = new WorkerAssignment();
            assignment.setApplication(app);
        } else {
            assignment.setActive(false);
            workerAssignmentRepository.save(assignment);
            
            assignment = new WorkerAssignment();
            assignment.setApplication(app);
        }
        
        assignment.setWorker(worker);
        assignment.setAssignedBy(admin);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setNotes(request.getNotes());
        assignment.setActive(true);
        app.setWorkerAssignment(assignment);
        
        app.setStatus(Application.ApplicationStatus.WORKER_ASSIGNED);
        
        notificationService.createNotification(worker.getUser(), "New Assignment", "You have been assigned to application " + app.getApplicationNumber(), "INFO", app.getId());
        
        return mapToSummary(applicationRepository.save(app));
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getAll(Pageable pageable, String status, String search) {
        // Sanitize search input
        String term = null;
        if (search != null && !search.isBlank()) {
            String safe = search.length() > 200 ? search.substring(0, 200) : search;
            term = safe.replaceAll("[\\x00-\\x1F\\x7F]", "").trim();
            if (term.isEmpty()) term = null;
        }

        Application.ApplicationStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try { statusEnum = Application.ApplicationStatus.valueOf(status); }
            catch (IllegalArgumentException ignored) {}
        }

        // If no search term — use Spring Data repository (faster, index-friendly)
        if (term == null) {
            Page<Application> page = (statusEnum != null)
                    ? applicationRepository.findByStatus(statusEnum, pageable)
                    : applicationRepository.findAllBy(pageable);
            return page.map(this::mapToSummary);
        }

        // With search — use raw BSON query to bypass Spring Data's @DBRef path validator
        // Criteria.where() validates paths against entity mapping; for @DBRef-chained paths like
        // "customer.user.firstName" the mapper fails. We use BasicQuery with raw BSON instead.
        String escapedTerm = term.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                                 .replace(".", "\\.").replace("*", "\\*").replace("+", "\\+")
                                 .replace("?", "\\?").replace("[", "\\[").replace("]", "\\]")
                                 .replace("{", "\\{").replace("}", "\\}").replace("^", "\\^");

        org.bson.Document regexDoc = new org.bson.Document("$regex", escapedTerm)
                .append("$options", "i");

        java.util.List<org.bson.Document> orConditions = java.util.Arrays.asList(
                new org.bson.Document("applicationNumber", regexDoc),
                new org.bson.Document("bikeDetail.registrationNumber", regexDoc)
                // Note: customer.user.firstName/email etc. are @DBRef references stored as IDs.
                // MongoDB cannot query across @DBRef fields without $lookup aggregation.
                // For customer name/email search, use the search by applicationNumber instead.
        );

        org.bson.Document filterDoc = new org.bson.Document("$or", orConditions);
        if (statusEnum != null) {
            filterDoc.append("status", statusEnum.name());
        }

        // Use collection name (not entity class) to bypass Spring Data's @DBRef path validation.
        // Also avoid using pageable.sort — Spring Data validates sort field paths through MappingContext.
        // Instead, apply sort, skip, limit directly on the BasicQuery via raw BSON.
        final String COLLECTION = "applications";

        org.springframework.data.mongodb.core.query.BasicQuery countQuery =
                new org.springframework.data.mongodb.core.query.BasicQuery(filterDoc.toJson());
        long total = mongoTemplate.count(countQuery, COLLECTION);

        org.springframework.data.mongodb.core.query.BasicQuery dataQuery =
                new org.springframework.data.mongodb.core.query.BasicQuery(filterDoc.toJson());
        // Apply sort via raw BSON (bypass entity mapping path validation)
        dataQuery.setSortObject(new org.bson.Document("createdAt", -1));
        dataQuery.skip((long) pageable.getPageNumber() * pageable.getPageSize());
        dataQuery.limit(pageable.getPageSize());

        List<Application> results = mongoTemplate.find(dataQuery, Application.class, COLLECTION);

        List<ApplicationResponse> content = results.stream().map(this::mapToSummary).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, total);
    }
    
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAssignedApplications(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return applicationRepository.findByWorkerUserId(user.getId()).stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    /**
     * Safely delete an application and all its owned child records.
     * Master data (customers, bike models, manufacturers, inventory) is NEVER deleted.
     * If a BikeInventory item was SOLD/RESERVED for this application, it is reset
     * to AVAILABLE so it can be re-applied for.
     */
    @Transactional
    public void deleteApplication(Long id) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));

        // 1. Reset BikeInventory status if this application had reserved/sold it
        BikeDetail bd = app.getBikeDetail();
        if (bd != null && bd.getBikeInventory() != null) {
            BikeInventory inv = bd.getBikeInventory();
            if (inv.getSaleStatus() == BikeInventory.SaleStatus.SOLD
                    || inv.getSaleStatus() == BikeInventory.SaleStatus.RESERVED) {
                inv.setSaleStatus(BikeInventory.SaleStatus.AVAILABLE);
                bikeInventoryRepository.save(inv);
            }
        }

        // 2. Delete status history rows
        statusHistoryRepository.deleteAllByApplicationId(id);

        // 3. Delete worker assignment
        if (app.getWorkerAssignment() != null) {
            workerAssignmentRepository.delete(app.getWorkerAssignment());
            app.setWorkerAssignment(null);
        }

        // 4. Detach & delete documents
        if (app.getDocuments() != null) {
            app.getDocuments().clear();
        }

        // 5. Delete finance detail
        if (app.getFinanceDetail() != null) {
            financeDetailRepository.delete(app.getFinanceDetail());
            app.setFinanceDetail(null);
        }

        // 6. Delete bike detail
        if (app.getBikeDetail() != null) {
            bikeDetailRepository.delete(app.getBikeDetail());
            app.setBikeDetail(null);
        }

        // 7. Delete the application itself
        applicationRepository.delete(app);
    }
}
