package com.autoconsultancy.dto.response;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class WorkerTaskDetailResponse {
    private WorkerTaskResponse task;

    // Service job detail
    private String sjRegistrationNumber;
    private String sjTaskDescription;
    private String sjPartsUsed;
    private BigDecimal sjLabourCharge;
    private BigDecimal sjPartsCharge;
    private BigDecimal sjTotalAmount;
    private String sjPaymentStatus;
    private LocalDate sjServiceDate;
    private LocalDate sjCompletionDate;
    private String sjNotes;

    // Payment collection detail
    private BigDecimal pcAmountDue;
    private BigDecimal pcAmountCollected;
    private String pcPaymentMethod;
    private LocalDate pcCollectionDate;
    private String pcReceiptNumber;
    private String pcUpiReference;
    private String pcNotes;
    private boolean pcVerifiedByAdmin;

    // Field visit detail
    private LocalDate fvVisitDate;
    private boolean fvCustomerContacted;
    private BigDecimal fvAmountCollected;
    private LocalDate fvPromiseToPayDate;
    private BigDecimal fvPromiseAmount;
    private String fvCustomerResponse;
    private String fvLocationNotes;
    private String fvOutcomeNotes;

    // Bike recovery detail
    private BigDecimal brOutstandingAmount;
    private String brReason;
    private String brAuthorizationNumber;
    private LocalDate brRecoveryDate;
    private String brBikeCondition;
    private Integer brCurrentMileage;
    private String brExistingDamage;
    private boolean brAccessoriesReceived;
    private boolean brKeysReceived;
    private boolean brDocumentsReceived;
    private String brWorkerRecoveryNotes;
    private boolean brCustomerAcknowledged;
}
