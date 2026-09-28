package com.autoconsultancy;

import com.autoconsultancy.entity.*;
import com.autoconsultancy.repository.*;
import com.autoconsultancy.service.SequenceGeneratorService;
import com.autoconsultancy.util.EmiCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final WorkerRepository workerRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final BikeModelRepository bikeModelRepository;
    private final BikeVariantRepository bikeVariantRepository;
    private final ManufacturingYearRepository yearRepository;
    private final ApplicationRepository applicationRepository;
    private final FinanceDetailRepository financeDetailRepository;
    private final EmiPaymentRepository emiPaymentRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final SequenceGeneratorService sequenceGenerator;

    @EventListener(ApplicationReadyEvent.class)
    public void seedData() {
        if (!userRepository.findByEmail("admin@autoconsultancy.com").isPresent()) {
            long adminId = sequenceGenerator.generateSequence("users");
            User admin = User.builder()
                    .id(adminId)
                    .email("admin@autoconsultancy.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .firstName("Super")
                    .lastName("Admin")
                    .role(Role.ADMIN)
                    .active(true)
                    .build();
            userRepository.save(admin);

            seedWorker("worker1@autoconsultancy.com", "Worker@123", "Worker", "One", "EMP1001", "Operations", "Senior Consultant");
            seedWorker("worker2@autoconsultancy.com", "Worker@123", "Worker", "Two", "EMP1002", "Finance", "Finance Analyst");
            seedWorker("worker3@autoconsultancy.com", "Worker@123", "Worker", "Three", "EMP1003", "Verification", "Document Specialist");
        }

        ensureDefaultUsersActive();

        if (applicationRepository.count() == 0) {
            seedTestCustomerScenarios();
        }

        if (manufacturerRepository.count() > 0) {
            return;
        }

        // 3. Seed Brands
        Manufacturer royalEnfield = seedBrand("ROYAL ENFIELD", "India");
        seedModel(royalEnfield, "Classic 350", "Cruiser", "Petrol", 2010, 2024);
        seedModel(royalEnfield, "Bullet 350", "Cruiser", "Petrol", 2010, 2024);
        seedModel(royalEnfield, "Meteor 350", "Cruiser", "Petrol", 2020, 2024);
        seedModel(royalEnfield, "Himalayan", "Adventure", "Petrol", 2016, 2024);
        seedModel(royalEnfield, "Interceptor 650", "Cruiser", "Petrol", 2019, 2024);

        Manufacturer tvs = seedBrand("TVS", "India");
        seedModel(tvs, "Apache RTR 160", "Sports", "Petrol", 2010, 2024);
        seedModel(tvs, "Jupiter", "Scooter", "Petrol", 2013, 2024);

        Manufacturer bajaj = seedBrand("BAJAJ", "India");
        seedModel(bajaj, "Pulsar 150", "Sports", "Petrol", 2010, 2024);
        seedModel(bajaj, "Dominar 400", "Touring", "Petrol", 2017, 2024);

        Manufacturer hero = seedBrand("HERO", "India");
        seedModel(hero, "Splendor Plus", "Commuter", "Petrol", 2012, 2024);

        Manufacturer honda = seedBrand("HONDA", "Japan");
        seedModel(honda, "Activa 6G", "Scooter", "Petrol", 2019, 2024);
        seedModel(honda, "Shine", "Commuter", "Petrol", 2011, 2024);
    }

    private void seedWorker(String email, String password, String firstName, String lastName, String empId, String dept, String desig) {
        long userId = sequenceGenerator.generateSequence("users");
        User u = User.builder()
                .id(userId)
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .role(Role.WORKER)
                .active(true)
                .build();
        u = userRepository.save(u);

        Worker w = Worker.builder()
                .id(userId)
                .user(u)
                .employeeId(empId)
                .department(dept)
                .designation(desig)
                .joiningDate(LocalDate.now())
                .active(true)
                .build();
        workerRepository.save(w);
    }

    private Manufacturer seedBrand(String name, String country) {
        long id = sequenceGenerator.generateSequence("manufacturers");
        Manufacturer m = Manufacturer.builder().id(id).name(name).country(country).active(true).build();
        return manufacturerRepository.save(m);
    }

    private BikeModel seedModel(Manufacturer m, String modelName, String category, String fuelType, int startYear, int endYear) {
        long modelId = sequenceGenerator.generateSequence("bike_models");
        BikeModel model = BikeModel.builder().id(modelId).manufacturer(m).modelName(modelName).category(category).fuelType(fuelType).active(true).build();
        model = bikeModelRepository.save(model);

        for (int y = startYear; y <= endYear; y++) {
            long yrId = sequenceGenerator.generateSequence("manufacturing_years");
            yearRepository.save(ManufacturingYear.builder().id(yrId).bikeModel(model).year(y).active(true).build());
        }

        long varId = sequenceGenerator.generateSequence("bike_variants");
        BikeVariant variant = BikeVariant.builder().id(varId).bikeModel(model).variantName("Standard").active(true).build();
        bikeVariantRepository.save(variant);

        return model;
    }

    private void ensureDefaultUsersActive() {
        List<String> defaultEmails = List.of(
                "admin@autoconsultancy.com",
                "worker1@autoconsultancy.com",
                "worker2@autoconsultancy.com",
                "worker3@autoconsultancy.com"
        );
        for (String email : defaultEmails) {
            userRepository.findByEmail(email).ifPresent(u -> {
                if (!u.isActive()) {
                    u.setActive(true);
                    userRepository.save(u);
                }
            });
        }
    }

    private void seedTestCustomerScenarios() {
        // Customer 1: Rahul Sharma (ON_TIME)
        Customer c1 = seedTestCustomer("rahul.sharma.test@autoconsult.dev", "Test@1234", "Rahul", "Sharma", "9876543210");
        createFinancedApp(c1, "AUTO-TEST-00001", 60000.0, 2.0, 12, 8, "ON_TIME", 0);

        // Customer 2: Priya Nair (ONE_MONTH)
        Customer c2 = seedTestCustomer("priya.nair.test@autoconsult.dev", "Test@1234", "Priya", "Nair", "9876543211");
        createFinancedApp(c2, "AUTO-TEST-00002", 75000.0, 3.0, 24, 6, "ONE_MONTH", 1);

        // Customer 3: Amit Verma (TWO_MONTHS)
        Customer c3 = seedTestCustomer("amit.verma.test@autoconsult.dev", "Test@1234", "Amit", "Verma", "9876543212");
        createFinancedApp(c3, "AUTO-TEST-00003", 120000.0, 5.0, 36, 4, "TWO_MONTHS", 2);

        // Customer 4: Sunita Rao (CRITICAL)
        Customer c4 = seedTestCustomer("sunita.rao.test@autoconsult.dev", "Test@1234", "Sunita", "Rao", "9876543213");
        createFinancedApp(c4, "AUTO-TEST-00004", 100000.0, 12.0, 12, 1, "CRITICAL", 3);

        // Customer 5: Deepak Mehta (CRITICAL)
        Customer c5 = seedTestCustomer("deepak.mehta.test@autoconsult.dev", "Test@1234", "Deepak", "Mehta", "9876543214");
        createFinancedApp(c5, "AUTO-TEST-00005", 250000.0, 8.0, 48, 2, "CRITICAL", 3);

        // Customer 6: Test Customer
        Customer c6 = seedTestCustomer("testcustomer@auto.com", "Test@1234", "Test", "Customer", "9876543215");
        createFinancedApp(c6, "AUTO-2026-00020", 370000.0, 2.0, 12, 1, "ONE_MONTH", 1);
    }

    private Customer seedTestCustomer(String email, String password, String firstName, String lastName, String phone) {
        long userId = sequenceGenerator.generateSequence("users");
        User u = User.builder()
                .id(userId)
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .role(Role.CUSTOMER)
                .active(true)
                .build();
        u = userRepository.save(u);

        Customer c = Customer.builder()
                .id(userId)
                .user(u)
                .profileComplete(true)
                .build();
        return customerRepository.save(c);
    }

    private void createFinancedApp(Customer cust, String appNo, double principalDouble, double rateDouble, int tenure, int paidCount, String overdueStatus, int missedMonths) {
        long appId = sequenceGenerator.generateSequence("applications");
        Application app = Application.builder()
                .id(appId)
                .applicationNumber(appNo)
                .customer(cust)
                .status(Application.ApplicationStatus.APPROVED)
                .submittedAt(LocalDateTime.now())
                .build();
        app = applicationRepository.save(app);

        BigDecimal principal = BigDecimal.valueOf(principalDouble);
        BigDecimal rate = BigDecimal.valueOf(rateDouble);

        BigDecimal totalPayable = EmiCalculator.totalPayable(principal, rate, tenure);
        BigDecimal totalInterest = EmiCalculator.totalInterest(totalPayable, principal);
        BigDecimal emi = EmiCalculator.monthlyEmi(principal, rate, tenure);
        BigDecimal paidAmt = emi.multiply(BigDecimal.valueOf(paidCount)).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal outstanding = totalPayable.subtract(paidAmt).setScale(2, java.math.RoundingMode.HALF_UP);

        long fdId = sequenceGenerator.generateSequence("finance_details");
        FinanceDetail fd = FinanceDetail.builder()
                .id(fdId)
                .application(app)
                .underFinance(true)
                .loanAmount(principal)
                .annualInterestRate(rate)
                .tenureMonths(tenure)
                .numberOfEmis(tenure)
                .emiAmount(emi)
                .totalPayable(totalPayable)
                .totalInterest(totalInterest)
                .paidEmis(paidCount)
                .remainingEmis(tenure - paidCount)
                .outstandingLoanAmount(outstanding)
                .loanStartDate(LocalDate.now().minusMonths(paidCount + missedMonths))
                .nextEmiDueDate(LocalDate.now().plusDays(10))
                .financeCompany("Auto Finance Pvt Ltd")
                .loanAccountNumber("LOAN-" + appNo)
                .emiPaymentStatus(missedMonths > 0 ? "OVERDUE_" + missedMonths : "CURRENT")
                .loanClosureStatus("ACTIVE")
                .overdueStatus(overdueStatus)
                .missedEmiMonths(missedMonths)
                .lastEmiPaidDate(LocalDate.now().minusMonths(missedMonths))
                .build();
        fd = financeDetailRepository.save(fd);

        app.setFinanceDetail(fd);
        applicationRepository.save(app);

        // Seed paid EMI records
        for (int i = 1; i <= paidCount; i++) {
            long payId = sequenceGenerator.generateSequence("emi_payments");
            EmiPayment pay = EmiPayment.builder()
                    .id(payId)
                    .financeDetail(fd)
                    .application(app)
                    .installmentNumber(i)
                    .paymentDate(LocalDate.now().minusMonths(paidCount - i + 1))
                    .amountPaid(emi)
                    .paymentMode("UPI")
                    .referenceNumber("PAY-REF-" + appNo + "-" + i)
                    .recordedAt(LocalDateTime.now())
                    .build();
            emiPaymentRepository.save(pay);
        }
    }
}
