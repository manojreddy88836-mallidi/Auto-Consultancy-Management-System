package com.autoconsultancy.controller;

import com.autoconsultancy.dto.request.EmiPaymentRequest;
import com.autoconsultancy.dto.response.ApiResponse;
import com.autoconsultancy.dto.response.EmiPaymentResponse;
import com.autoconsultancy.entity.*;
import com.autoconsultancy.exception.BadRequestException;
import com.autoconsultancy.exception.ResourceNotFoundException;
import com.autoconsultancy.repository.*;
import com.autoconsultancy.service.EmiOverdueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/emi-payments")
@RequiredArgsConstructor
@Transactional
public class EmiPaymentController {

    private final EmiPaymentRepository     emiPaymentRepo;
    private final FinanceDetailRepository  financeDetailRepo;
    private final UserRepository           userRepo;
    private final EmiOverdueService        emiOverdueService;

    // â”€â”€ GET: all payments for a finance record â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @GetMapping("/finance/{financeDetailId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<EmiPaymentResponse>>> getPayments(
            @PathVariable Long financeDetailId) {

        List<EmiPayment> payments = emiPaymentRepo
            .findByFinanceDetailIdOrderByInstallmentNumberAsc(financeDetailId);
        return ResponseEntity.ok(ApiResponse.success(
            payments.stream().map(this::toResponse).collect(Collectors.toList()),
            "Fetched EMI payments"));
    }

    // â”€â”€ POST: record a new payment â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<EmiPaymentResponse>> recordPayment(
            @RequestBody EmiPaymentRequest req,
            Authentication auth) {

        FinanceDetail fd = financeDetailRepo.findById(req.getFinanceDetailId())
            .orElseThrow(() -> new ResourceNotFoundException("Finance detail not found"));

        if (!fd.isUnderFinance()) {
            throw new BadRequestException("This application is not under finance.");
        }

        // Prevent duplicate installment payment
        if (req.getInstallmentNumber() != null &&
            emiPaymentRepo.existsByFinanceDetailIdAndInstallmentNumber(
                fd.getId(), req.getInstallmentNumber())) {
            throw new BadRequestException(
                "Installment #" + req.getInstallmentNumber() + " is already marked as paid.");
        }

        User currentUser = userRepo.findByEmail(auth.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        EmiPayment payment = EmiPayment.builder()
            .financeDetail(fd)
            .application(fd.getApplication())
            .installmentNumber(req.getInstallmentNumber())
            .paymentDate(req.getPaymentDate())
            .amountPaid(req.getAmountPaid())
            .paymentMode(req.getPaymentMode())
            .referenceNumber(req.getReferenceNumber())
            .notes(req.getNotes())
            .recordedBy(currentUser)
            .build();

        emiPaymentRepo.save(payment);

        // Refresh overdue status after payment
        emiOverdueService.refreshOverdueStatus(fd);

        return ResponseEntity.ok(ApiResponse.success(toResponse(payment), "Payment recorded successfully"));
    }

    // â”€â”€ PUT: edit an existing payment â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<EmiPaymentResponse>> updatePayment(
            @PathVariable Long id,
            @RequestBody EmiPaymentRequest req) {

        EmiPayment payment = emiPaymentRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("EMI payment not found"));

        if (req.getPaymentDate() != null)     payment.setPaymentDate(req.getPaymentDate());
        if (req.getAmountPaid() != null)      payment.setAmountPaid(req.getAmountPaid());
        if (req.getPaymentMode() != null)     payment.setPaymentMode(req.getPaymentMode());
        if (req.getReferenceNumber() != null) payment.setReferenceNumber(req.getReferenceNumber());
        if (req.getNotes() != null)           payment.setNotes(req.getNotes());

        emiPaymentRepo.save(payment);

        // Refresh overdue status
        emiOverdueService.refreshOverdueStatus(payment.getFinanceDetail());

        return ResponseEntity.ok(ApiResponse.success(toResponse(payment), "Payment updated"));
    }

    // â”€â”€ DELETE: remove a payment â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable Long id) {

        EmiPayment payment = emiPaymentRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("EMI payment not found"));
        FinanceDetail fd = payment.getFinanceDetail();

        emiPaymentRepo.delete(payment);

        // Refresh overdue status after deletion
        emiOverdueService.refreshOverdueStatus(fd);

        return ResponseEntity.ok(ApiResponse.success(null, "Payment deleted and overdue status refreshed"));
    }

    // â”€â”€ POST: manually trigger overdue refresh for all records â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @PostMapping("/refresh-overdue")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<String>> refreshAllOverdue() {
        emiOverdueService.runDailyOverdueCheck();
        return ResponseEntity.ok(ApiResponse.success("OK", "Overdue check triggered for all financed records"));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private EmiPaymentResponse toResponse(EmiPayment p) {
        return EmiPaymentResponse.builder()
            .id(p.getId())
            .financeDetailId(p.getFinanceDetail() != null ? p.getFinanceDetail().getId() : null)
            .applicationId(p.getApplication() != null ? p.getApplication().getId() : null)
            .applicationNumber(p.getApplication() != null ? p.getApplication().getApplicationNumber() : null)
            .installmentNumber(p.getInstallmentNumber())
            .paymentDate(p.getPaymentDate())
            .amountPaid(p.getAmountPaid())
            .paymentMode(p.getPaymentMode())
            .referenceNumber(p.getReferenceNumber())
            .notes(p.getNotes())
            .recordedByName(p.getRecordedBy() != null
                ? (p.getRecordedBy().getFirstName() + " " + p.getRecordedBy().getLastName()).trim()
                : null)
            .recordedAt(p.getRecordedAt())
            .build();
    }
}
