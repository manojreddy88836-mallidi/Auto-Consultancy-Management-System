package com.autoconsultancy.service;

import com.autoconsultancy.entity.*;
import com.autoconsultancy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmiOverdueService {

    // ── Status constants ──────────────────────────────────────────────────────
    public static final String ON_TIME     = "ON_TIME";
    public static final String ONE_MONTH   = "ONE_MONTH";
    public static final String TWO_MONTHS  = "TWO_MONTHS";
    public static final String CRITICAL    = "CRITICAL";

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final FinanceDetailRepository   financeDetailRepo;
    private final EmiPaymentRepository      emiPaymentRepo;
    private final EmiOverdueAlertRepository alertRepo;
    private final NotificationService       notificationService;
    private final UserRepository            userRepository;

    // ─────────────────────────────────────────────────────────────────────────
    //  SCHEDULED: daily at 06:00 AM — scan all financed records
    // ─────────────────────────────────────────────────────────────────────────
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void runDailyOverdueCheck() {
        log.info("EMI Overdue daily check started");
        List<FinanceDetail> records = financeDetailRepo.findAllByUnderFinanceTrue();
        for (FinanceDetail fd : records) {
            try {
                refreshOverdueStatus(fd);
            } catch (Exception e) {
                log.error("Error refreshing overdue for finance_detail id={}: {}", fd.getId(), e.getMessage());
            }
        }
        log.info("EMI Overdue daily check complete — processed {} records", records.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PUBLIC: Refresh a single FinanceDetail's overdue status and save
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public void refreshOverdueStatus(FinanceDetail fd) {
        if (!fd.isUnderFinance()) return;

        // 1. Compute missed installments
        int missed = computeMissedEmis(fd);
        String newStatus = statusForMissed(missed);

        // 2. Compute outstanding from actual payments
        BigDecimal outstanding = computeOutstanding(fd);

        // 3. Compute lastEmiPaidDate from actual payment records
        LocalDate lastPaid = computeLastPaidDate(fd);

        // 4. Update fields on FinanceDetail
        fd.setMissedEmiMonths(missed);
        fd.setOverdueStatus(newStatus);
        fd.setOutstandingLoanAmount(outstanding);
        fd.setLastEmiPaidDate(lastPaid);

        // Keep paidEmis in sync with actual payment records
        int actualPaidCount = (int) emiPaymentRepo.countByFinanceDetailId(fd.getId());
        fd.setPaidEmis(actualPaidCount);
        int remaining = (fd.getNumberOfEmis() != null ? fd.getNumberOfEmis() : 0) - actualPaidCount;
        fd.setRemainingEmis(Math.max(0, remaining));

        financeDetailRepo.save(fd);

        // 5. Create notifications if overdue threshold crossed (deduped)
        if (TWO_MONTHS.equals(newStatus) || CRITICAL.equals(newStatus)) {
            sendOverdueNotificationIfNotSent(fd, newStatus, missed, outstanding);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  COMPUTE: how many due installments have NOT been paid
    // ─────────────────────────────────────────────────────────────────────────
    public int computeMissedEmis(FinanceDetail fd) {
        LocalDate firstDueDate = resolveFirstDueDate(fd);
        if (firstDueDate == null) return 0;

        int tenure = fd.getNumberOfEmis() != null ? fd.getNumberOfEmis() : 0;
        if (tenure == 0) return 0;

        LocalDate today = LocalDate.now();

        // Build set of paid installment numbers
        Set<Integer> paidInstallments = emiPaymentRepo
            .findByFinanceDetailIdOrderByInstallmentNumberAsc(fd.getId())
            .stream()
            .map(EmiPayment::getInstallmentNumber)
            .collect(Collectors.toSet());

        int missed = 0;
        for (int i = 1; i <= tenure; i++) {
            LocalDate dueDate = firstDueDate.plusMonths(i - 1);
            if (!dueDate.isAfter(today) && !paidInstallments.contains(i)) {
                missed++;
            }
        }
        return missed;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  COMPUTE: outstanding = total payable - (paid installments × EMI)
    // ─────────────────────────────────────────────────────────────────────────
    public BigDecimal computeOutstanding(FinanceDetail fd) {
        if (fd.getEmiAmount() == null || fd.getNumberOfEmis() == null) return BigDecimal.ZERO;
        long paid = emiPaymentRepo.countByFinanceDetailId(fd.getId());
        BigDecimal totalPaid = fd.getEmiAmount().multiply(BigDecimal.valueOf(paid));
        BigDecimal totalPayable = fd.getTotalPayable() != null ? fd.getTotalPayable()
            : fd.getEmiAmount().multiply(BigDecimal.valueOf(fd.getNumberOfEmis()));
        BigDecimal outstanding = totalPayable.subtract(totalPaid).setScale(2, RoundingMode.HALF_UP);
        return outstanding.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : outstanding;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  COMPUTE: last payment date from actual EmiPayment records
    // ─────────────────────────────────────────────────────────────────────────
    public LocalDate computeLastPaidDate(FinanceDetail fd) {
        List<EmiPayment> payments = emiPaymentRepo
            .findByFinanceDetailIdOrderByInstallmentNumberAsc(fd.getId());
        return payments.stream()
            .map(EmiPayment::getPaymentDate)
            .filter(d -> d != null)
            .max(LocalDate::compareTo)
            .orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPER: resolve the due date of installment #1
    // ─────────────────────────────────────────────────────────────────────────
    public LocalDate resolveFirstDueDate(FinanceDetail fd) {
        // Prefer loanStartDate: first EMI is 1 month after start
        if (fd.getLoanStartDate() != null) {
            return fd.getLoanStartDate().plusMonths(1);
        }
        // Fall back: nextEmiDueDate is the due date for installment (paidEmis+1)
        if (fd.getNextEmiDueDate() != null) {
            int paid = fd.getPaidEmis() != null ? fd.getPaidEmis() : 0;
            // actual paid from DB may differ; use EmiPayment count
            long actualPaid = emiPaymentRepo.countByFinanceDetailId(fd.getId());
            return fd.getNextEmiDueDate().minusMonths(actualPaid);
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPER: status string from missed count
    // ─────────────────────────────────────────────────────────────────────────
    public static String statusForMissed(int missed) {
        if (missed <= 0) return ON_TIME;
        if (missed == 1) return ONE_MONTH;
        if (missed == 2) return TWO_MONTHS;
        return CRITICAL;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NOTIFICATIONS: send to all admins, deduped per (financeId, status, month)
    // ─────────────────────────────────────────────────────────────────────────
    private void sendOverdueNotificationIfNotSent(FinanceDetail fd, String status, int missed, BigDecimal outstanding) {
        String alertMonth = YearMonth.now().format(MONTH_FMT);

        boolean alreadySent = alertRepo.existsByFinanceDetailIdAndOverdueStatusAndAlertMonth(
            fd.getId(), status, alertMonth);

        if (alreadySent) return;

        // Build notification content
        Application app = fd.getApplication();
        String customerName  = (app != null && app.getCustomer() != null && app.getCustomer().getUser() != null)
            ? (app.getCustomer().getUser().getFirstName() + " " + app.getCustomer().getUser().getLastName()).trim()
            : "Customer";
        String appNumber     = app != null ? app.getApplicationNumber() : "-";
        String outstandingStr = outstanding != null
            ? "Rs." + outstanding.setScale(2, RoundingMode.HALF_UP).toPlainString() : "-";

        String title, message, notifType;
        if (CRITICAL.equals(status)) {
            title   = "CRITICAL EMI PAYMENT ALERT";
            message = "Customer: " + customerName + "\n"
                + "Application: " + appNumber + "\n"
                + "Missed EMI: " + missed + " months\n"
                + "Outstanding: " + outstandingStr + "\n"
                + "Customer has not paid EMI for more than 2 months. Immediate attention required.";
            notifType = "EMI_CRITICAL";
        } else {
            title   = "EMI PAYMENT WARNING";
            message = "Customer: " + customerName + "\n"
                + "Application: " + appNumber + "\n"
                + "Missed EMI: " + missed + " months\n"
                + "Outstanding: " + outstandingStr + "\n"
                + "Please contact the customer.";
            notifType = "EMI_WARNING";
        }

        Long appId = app != null ? app.getId() : null;

        // Notify all admin users
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            notificationService.createNotification(admin, title, message, notifType, appId);
        }

        // Record that we sent this alert (dedup)
        EmiOverdueAlert alert = EmiOverdueAlert.builder()
            .financeDetail(fd)
            .overdueStatus(status)
            .alertMonth(alertMonth)
            .build();
        alertRepo.save(alert);

        log.info("EMI overdue notification sent: status={}, appNumber={}, missed={}", status, appNumber, missed);
    }
}
