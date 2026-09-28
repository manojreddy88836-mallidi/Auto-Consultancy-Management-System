package com.autoconsultancy;

import com.autoconsultancy.entity.*;
import com.autoconsultancy.repository.*;
import com.autoconsultancy.service.SequenceGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@SpringBootTest
public class DataRecoveryRunnerTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    @Autowired
    private BikeModelRepository bikeModelRepository;

    @Autowired
    private BikeVariantRepository bikeVariantRepository;

    @Autowired
    private ManufacturingYearRepository yearRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private BikeInventoryRepository bikeInventoryRepository;

    @Autowired
    private BikeInventoryImageRepository bikeInventoryImageRepository;

    @Autowired
    private BikeDetailRepository bikeDetailRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private FinanceDetailRepository financeDetailRepository;

    @Autowired
    private EmiPaymentRepository emiPaymentRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmiOverdueAlertRepository emiOverdueAlertRepository;

    @Autowired
    private BikeOfferRepository bikeOfferRepository;

    @Autowired
    private ServiceJobRepository serviceJobRepository;

    @Autowired
    private WorkerTaskRepository workerTaskRepository;

    @Autowired
    private ApplicationStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private SequenceGeneratorService sequenceGenerator;

    private Connection getMysqlConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/auto_consultancy?useSSL=false&allowPublicKeyRetrieval=true";
        return DriverManager.getConnection(url, "root", "root");
    }

    @Test
    public void executeFullDataRecovery() throws Exception {
        System.out.println("==================================================");
        System.out.println("STARTING DATA RECOVERY FROM MYSQL TO MONGODB");
        System.out.println("==================================================");

        // STEP 1: SAFETY BACKUP OF CURRENT MONGODB DATA
        exportMongoBackup();

        // STEP 2: RECOVER DATA TABLE BY TABLE
        try (Connection conn = getMysqlConnection()) {
            recoverManufacturers(conn);
            recoverBikeModels(conn);
            recoverBikeVariants(conn);
            recoverManufacturingYears(conn);
            recoverUsers(conn);
            recoverWorkers(conn);
            recoverCustomers(conn);
            recoverBikeInventory(conn);
            recoverBikeInventoryImages(conn);
            recoverBikeDetails(conn);
            recoverApplications(conn);
            recoverDocuments(conn);
            recoverNotifications(conn);
            recoverEmiOverdueAlerts(conn);
            recoverBikeOffers(conn);
            recoverWorkerTasks(conn);
            recoverServiceJobs(conn);
            recoverApplicationStatusHistory(conn);
        }

        // STEP 3: SYNCHRONIZE SEQUENCE COUNTERS
        syncSequenceCounters();

        // STEP 4: VERIFY POST-RECOVERY PARITY
        printFinalParityReport();
    }

    private void exportMongoBackup() {
        try {
            File backupDir = new File("C:/Users/LENOVO/.gemini/antigravity/brain/2c8448f2-0849-4be3-8b0e-e96216452b7f/scratch/mongodb_backup");
            backupDir.mkdirs();
            for (String coll : mongoTemplate.getCollectionNames()) {
                List<org.bson.Document> docs = new ArrayList<>();
                mongoTemplate.getCollection(coll).find().into(docs);
                File file = new File(backupDir, coll + ".json");
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("[\n");
                    for (int i = 0; i < docs.size(); i++) {
                        writer.write(docs.get(i).toJson());
                        if (i < docs.size() - 1) writer.write(",\n");
                    }
                    writer.write("\n]");
                }
            }
            System.out.println("[BACKUP] Exported pre-recovery MongoDB backup to scratch/mongodb_backup/");
        } catch (Exception e) {
            System.err.println("[BACKUP ERROR] " + e.getMessage());
        }
    }

    private void recoverManufacturers(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM manufacturers")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                String name = rs.getString("name");
                String logoUrl = rs.getMetaData().getColumnCount() >= 3 ? rs.getString("logo_url") : null;
                String country = rs.getMetaData().getColumnCount() >= 4 ? rs.getString("country") : null;
                boolean active = rs.getBoolean("active");

                Optional<Manufacturer> existing = manufacturerRepository.findById(id);
                if (existing.isEmpty()) {
                    Manufacturer byName = mongoTemplate.findOne(Query.query(Criteria.where("name").is(name)), Manufacturer.class);
                    if (byName != null) existing = Optional.of(byName);
                }

                if (existing.isEmpty()) {
                    Manufacturer m = Manufacturer.builder()
                            .id(id)
                            .name(name)
                            .logoUrl(logoUrl)
                            .country(country)
                            .active(active)
                            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                            .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
                            .build();
                    manufacturerRepository.save(m);
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] Manufacturers: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverBikeModels(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM bike_models")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                long mfgId = rs.getLong("manufacturer_id");
                String modelName = rs.getString("model_name");
                String category = rs.getString("category");
                String fuelType = rs.getString("fuel_type");
                boolean active = rs.getBoolean("active");

                if (!bikeModelRepository.existsById(id)) {
                    Manufacturer mfg = manufacturerRepository.findById(mfgId).orElse(null);
                    BikeModel model = BikeModel.builder()
                            .id(id)
                            .manufacturer(mfg)
                            .modelName(modelName)
                            .category(category)
                            .fuelType(fuelType)
                            .active(active)
                            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                            .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
                            .build();
                    bikeModelRepository.save(model);
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] Bike Models: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverBikeVariants(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM bike_variants")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                long modelId = rs.getLong("bike_model_id");
                String variantName = rs.getString("variant_name");
                int engineCC = rs.getInt("enginecc");
                boolean active = rs.getBoolean("active");

                if (!bikeVariantRepository.existsById(id)) {
                    BikeModel model = bikeModelRepository.findById(modelId).orElse(null);
                    BikeVariant variant = BikeVariant.builder()
                            .id(id)
                            .bikeModel(model)
                            .variantName(variantName)
                            .engineCC(engineCC)
                            .active(active)
                            .build();
                    bikeVariantRepository.save(variant);
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] Bike Variants: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverManufacturingYears(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM manufacturing_years")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                long modelId = rs.getLong("bike_model_id");
                int year = rs.getInt("year");
                boolean active = rs.getBoolean("active");

                if (!yearRepository.existsById(id)) {
                    BikeModel model = bikeModelRepository.findById(modelId).orElse(null);
                    ManufacturingYear my = ManufacturingYear.builder()
                            .id(id)
                            .bikeModel(model)
                            .year(year)
                            .active(active)
                            .build();
                    yearRepository.save(my);
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] Manufacturing Years: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverUsers(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                String email = rs.getString("email");
                String password = rs.getString("password");
                String roleStr = rs.getString("role");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String phone = rs.getString("phone");
                boolean active = rs.getBoolean("active");

                Optional<User> existing = userRepository.findById(id);
                if (existing.isEmpty()) {
                    existing = userRepository.findByEmail(email);
                }

                if (existing.isEmpty()) {
                    User u = User.builder()
                            .id(id)
                            .email(email)
                            .password(password)
                            .role(Role.valueOf(roleStr))
                            .firstName(firstName)
                            .lastName(lastName)
                            .phone(phone)
                            .active(active)
                            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                            .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
                            .build();
                    userRepository.save(u);
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] Users: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverWorkers(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM workers")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                long userId = id;
                String employeeId = rs.getString("employee_id");
                String dept = rs.getString("department");
                String desig = rs.getString("designation");
                LocalDate joiningDate = toLocalDate(rs.getDate("joining_date"));
                boolean active = rs.getBoolean("active");

                if (!workerRepository.existsById(id) && !workerRepository.existsById(userId)) {
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null) {
                        Worker w = Worker.builder()
                                .id(userId)
                                .user(user)
                                .employeeId(employeeId)
                                .department(dept)
                                .designation(desig)
                                .joiningDate(joiningDate)
                                .active(active)
                                .build();
                        workerRepository.save(w);
                        inserted++;
                    }
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] Workers: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverCustomers(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM customers")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                long userId = id;
                LocalDate dob = toLocalDate(rs.getDate("date_of_birth"));
                String address = rs.getString("address");
                String city = rs.getString("city");
                String state = rs.getString("state");
                String pincode = rs.getString("pincode");
                String idProof = rs.getString("identity_proof");
                String idNumber = rs.getString("identity_proof_number");
                boolean profileComplete = rs.getBoolean("profile_complete");

                if (!customerRepository.existsById(id) && !customerRepository.existsById(userId)) {
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null) {
                        Customer c = Customer.builder()
                                .id(userId)
                                .user(user)
                                .dateOfBirth(dob)
                                .address(address)
                                .city(city)
                                .state(state)
                                .pincode(pincode)
                                .identityProof(idProof)
                                .identityProofNumber(idNumber)
                                .profileComplete(profileComplete)
                                .build();
                        customerRepository.save(c);
                        inserted++;
                    }
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] Customers: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverBikeInventory(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM bike_inventory")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                if (!bikeInventoryRepository.existsById(id)) {
                    long modelId = rs.getLong("bike_model_id");
                    BikeModel model = bikeModelRepository.findById(modelId).orElse(null);
                    String saleStatusStr = rs.getString("sale_status");
                    BikeInventory.SaleStatus status = saleStatusStr != null ? BikeInventory.SaleStatus.valueOf(saleStatusStr) : BikeInventory.SaleStatus.NOT_FOR_SALE;

                    BikeInventory inv = BikeInventory.builder()
                            .id(id)
                            .bikeModel(model)
                            .bikeCode(rs.getString("bike_code"))
                            .price(rs.getBigDecimal("price"))
                            .color(rs.getString("color"))
                            .fuelType(rs.getString("fuel_type"))
                            .description(rs.getString("description"))
                            .active(rs.getBoolean("active"))
                            .saleStatus(status)
                            .build();
                    bikeInventoryRepository.save(inv);
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] Bike Inventory: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverApplications(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM applications")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                String appNo = rs.getString("application_number");
                long custId = rs.getLong("customer_id");
                String statusStr = rs.getString("status");
                String remarks = rs.getString("remarks");

                Optional<Application> existing = applicationRepository.findById(id);
                if (existing.isEmpty()) {
                    Application byNo = mongoTemplate.findOne(Query.query(Criteria.where("applicationNumber").is(appNo)), Application.class);
                    if (byNo != null) existing = Optional.of(byNo);
                }

                if (existing.isEmpty()) {
                    Customer cust = customerRepository.findById(custId).orElse(null);
                    Application app = Application.builder()
                            .id(id)
                            .applicationNumber(appNo)
                            .customer(cust)
                            .status(Application.ApplicationStatus.valueOf(statusStr))
                            .remarks(remarks)
                            .submittedAt(toLocalDateTime(rs.getTimestamp("submitted_at")))
                            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                            .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
                            .build();
                    applicationRepository.save(app);
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }

        // Recover related Finance Details
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM finance_details")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                long appId = rs.getLong("application_id");
                if (!financeDetailRepository.existsById(id)) {
                    Application app = applicationRepository.findById(appId).orElse(null);
                    FinanceDetail fd = FinanceDetail.builder()
                            .id(id)
                            .application(app)
                            .underFinance(rs.getBoolean("under_finance"))
                            .financeCompany(rs.getString("finance_company"))
                            .loanAccountNumber(rs.getString("loan_account_number"))
                            .loanAmount(rs.getBigDecimal("loan_amount"))
                            .annualInterestRate(rs.getBigDecimal("annual_interest_rate"))
                            .tenureMonths(rs.getInt("tenure_months"))
                            .numberOfEmis(rs.getInt("number_of_emis"))
                            .emiAmount(rs.getBigDecimal("emi_amount"))
                            .totalPayable(rs.getBigDecimal("total_payable"))
                            .totalInterest(rs.getBigDecimal("total_interest"))
                            .paidEmis(rs.getInt("paid_emis"))
                            .remainingEmis(rs.getInt("remaining_emis"))
                            .outstandingLoanAmount(rs.getBigDecimal("outstanding_loan_amount"))
                            .loanStartDate(toLocalDate(rs.getDate("loan_start_date")))
                            .nextEmiDueDate(toLocalDate(rs.getDate("next_emi_due_date")))
                            .emiPaymentStatus(rs.getString("emi_payment_status"))
                            .loanClosureStatus(rs.getString("loan_closure_status"))
                            .overdueStatus(rs.getString("overdue_status"))
                            .missedEmiMonths(rs.getInt("missed_emi_months"))
                            .lastEmiPaidDate(toLocalDate(rs.getDate("last_emi_paid_date")))
                            .build();
                    fd = financeDetailRepository.save(fd);
                    if (app != null) {
                        app.setFinanceDetail(fd);
                        applicationRepository.save(app);
                    }
                }
            }
        }

        // Recover related EMI Payments
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM emi_payments")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                long appId = rs.getLong("application_id");
                long fdId = rs.getLong("finance_detail_id");
                if (!emiPaymentRepository.existsById(id)) {
                    Application app = applicationRepository.findById(appId).orElse(null);
                    FinanceDetail fd = financeDetailRepository.findById(fdId).orElse(null);
                    EmiPayment pay = EmiPayment.builder()
                            .id(id)
                            .application(app)
                            .financeDetail(fd)
                            .installmentNumber(rs.getInt("installment_number"))
                            .paymentDate(toLocalDate(rs.getDate("payment_date")))
                            .amountPaid(rs.getBigDecimal("amount_paid"))
                            .paymentMode(rs.getString("payment_mode"))
                            .referenceNumber(rs.getString("reference_number"))
                            .notes(rs.getString("notes"))
                            .recordedAt(toLocalDateTime(rs.getTimestamp("recorded_at")))
                            .build();
                    emiPaymentRepository.save(pay);
                }
            }
        }

        System.out.println("[RECOVERY] Applications & Finance: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverDocuments(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM documents")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                if (!documentRepository.existsById(id)) {
                    long appId = rs.getLong("application_id");
                    long userId = rs.getLong("uploaded_by_user_id");
                    Application app = applicationRepository.findById(appId).orElse(null);
                    User user = userRepository.findById(userId).orElse(null);
                    Document doc = Document.builder()
                            .id(id)
                            .application(app)
                            .uploadedByUser(user)
                            .documentType(rs.getString("document_type"))
                            .fileName(rs.getString("file_name"))
                            .originalFileName(rs.getString("original_file_name"))
                            .filePath(rs.getString("file_path"))
                            .fileSize(rs.getLong("file_size"))
                            .mimeType(rs.getString("mime_type"))
                            .status(rs.getString("status"))
                            .remarks(rs.getString("remarks"))
                            .uploadedAt(toLocalDateTime(rs.getTimestamp("uploaded_at")))
                            .build();
                    documentRepository.save(doc);
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] Documents: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverNotifications(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM notifications")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                if (!notificationRepository.existsById(id)) {
                    long userId = rs.getLong("user_id");
                    User user = userRepository.findById(userId).orElse(null);
                    Notification n = Notification.builder()
                            .id(id)
                            .user(user)
                            .title(rs.getString("title"))
                            .message(rs.getString("message"))
                            .type(rs.getString("type"))
                            .read(rs.getBoolean("is_read"))
                            .relatedApplicationId(rs.getLong("related_application_id"))
                            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                            .build();
                    notificationRepository.save(n);
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] Notifications: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverEmiOverdueAlerts(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM emi_overdue_alerts")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                if (!emiOverdueAlertRepository.existsById(id)) {
                    long fdId = rs.getLong("finance_detail_id");
                    FinanceDetail fd = financeDetailRepository.findById(fdId).orElse(null);
                    EmiOverdueAlert alert = EmiOverdueAlert.builder()
                            .id(id)
                            .financeDetail(fd)
                            .overdueStatus(rs.getString("overdue_status"))
                            .alertMonth(rs.getString("alert_month"))
                            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                            .build();
                    emiOverdueAlertRepository.save(alert);
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] EMI Overdue Alerts: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverBikeOffers(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM bike_offers")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                if (!bikeOfferRepository.existsById(id)) {
                    long invId = rs.getLong("bike_inventory_id");
                    long custId = rs.getLong("customer_id");
                    BikeInventory inv = bikeInventoryRepository.findById(invId).orElse(null);
                    Customer cust = customerRepository.findById(custId).orElse(null);
                    String statusStr = rs.getString("status");
                    BikeOffer offer = BikeOffer.builder()
                            .id(id)
                            .bikeInventory(inv)
                            .customer(cust)
                            .listedPrice(rs.getBigDecimal("listed_price"))
                            .offeredPrice(rs.getBigDecimal("offered_price"))
                            .counterOfferPrice(rs.getBigDecimal("counter_offer_price"))
                            .agreedPrice(rs.getBigDecimal("agreed_price"))
                            .status(statusStr != null ? BikeOffer.OfferStatus.valueOf(statusStr) : BikeOffer.OfferStatus.PENDING)
                            .customerMessage(rs.getString("customer_message"))
                            .adminResponse(rs.getString("admin_response"))
                            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                            .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
                            .build();
                    bikeOfferRepository.save(offer);
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] Bike Offers: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverWorkerTasks(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM worker_tasks")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                if (!workerTaskRepository.existsById(id)) {
                    long workerId = rs.getLong("worker_id");
                    Worker worker = workerRepository.findById(workerId).orElse(null);
                    String taskTypeStr = rs.getString("task_type");
                    String statusStr = rs.getString("status");
                    String priorityStr = rs.getString("priority");

                    WorkerTask task = WorkerTask.builder()
                            .id(id)
                            .worker(worker)
                            .title(rs.getString("title"))
                            .description(rs.getString("description"))
                            .taskType(taskTypeStr != null ? WorkerTask.TaskType.valueOf(taskTypeStr) : WorkerTask.TaskType.REPAIR)
                            .status(statusStr != null ? WorkerTask.TaskStatus.valueOf(statusStr) : WorkerTask.TaskStatus.ASSIGNED)
                            .priority(priorityStr != null ? WorkerTask.Priority.valueOf(priorityStr) : WorkerTask.Priority.NORMAL)
                            .dueDate(toLocalDate(rs.getDate("due_date")))
                            .completedAt(toLocalDateTime(rs.getTimestamp("completed_at")))
                            .adminNotes(rs.getString("admin_notes"))
                            .workerNotes(rs.getString("worker_notes"))
                            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                            .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
                            .build();
                    workerTaskRepository.save(task);
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] Worker Tasks: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverServiceJobs(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM service_jobs")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                if (!serviceJobRepository.existsById(id)) {
                    long taskId = rs.getLong("worker_task_id");
                    WorkerTask wt = workerTaskRepository.findById(taskId).orElse(null);
                    String payStatus = rs.getString("payment_status");
                    ServiceJob job = ServiceJob.builder()
                            .id(id)
                            .workerTask(wt)
                            .registrationNumber(rs.getString("registration_number"))
                            .taskDescription(rs.getString("task_description"))
                            .partsUsed(rs.getString("parts_used"))
                            .labourCharge(rs.getBigDecimal("labour_charge"))
                            .partsCharge(rs.getBigDecimal("parts_charge"))
                            .totalAmount(rs.getBigDecimal("total_amount"))
                            .paymentStatus(payStatus != null ? ServiceJob.PaymentStatus.valueOf(payStatus) : ServiceJob.PaymentStatus.PENDING)
                            .serviceDate(toLocalDate(rs.getDate("service_date")))
                            .completionDate(toLocalDate(rs.getDate("completion_date")))
                            .notes(rs.getString("notes"))
                            .build();
                    serviceJobRepository.save(job);
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] Service Jobs: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverApplicationStatusHistory(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM application_status_history")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                if (!statusHistoryRepository.existsById(id)) {
                    long appId = rs.getLong("application_id");
                    long userId = rs.getLong("changed_by_user_id");
                    Application app = applicationRepository.findById(appId).orElse(null);
                    User user = userRepository.findById(userId).orElse(null);
                    ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                            .id(id)
                            .application(app)
                            .previousStatus(rs.getString("previous_status"))
                            .newStatus(rs.getString("new_status"))
                            .changedBy(user)
                            .remarks(rs.getString("remarks"))
                            .changedAt(toLocalDateTime(rs.getTimestamp("changed_at")))
                            .build();
                    statusHistoryRepository.save(history);
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] Status History: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverBikeInventoryImages(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM bike_inventory_images")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                if (!bikeInventoryImageRepository.existsById(id)) {
                    long invId = rs.getLong("bike_inventory_id");
                    BikeInventory inv = bikeInventoryRepository.findById(invId).orElse(null);
                    BikeInventoryImage img = BikeInventoryImage.builder()
                            .id(id)
                            .bikeInventory(inv)
                            .fileName(rs.getString("file_name"))
                            .filePath(rs.getString("file_path"))
                            .fileSize(rs.getLong("file_size"))
                            .mimeType(rs.getString("mime_type"))
                            .originalFileName(rs.getString("original_file_name"))
                            .primary(rs.getBoolean("is_primary"))
                            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                            .build();
                    bikeInventoryImageRepository.save(img);
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] Bike Inventory Images: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void recoverBikeDetails(Connection conn) throws SQLException {
        int inserted = 0, skipped = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM bike_details")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                if (!bikeDetailRepository.existsById(id)) {
                    long appId = rs.getLong("application_id");
                    long mfgId = rs.getLong("manufacturer_id");
                    long modelId = rs.getLong("bike_model_id");
                    long varId = rs.getLong("variant_id");
                    long invId = rs.getLong("bike_inventory_id");

                    Application app = applicationRepository.findById(appId).orElse(null);
                    Manufacturer mfg = manufacturerRepository.findById(mfgId).orElse(null);
                    BikeModel model = bikeModelRepository.findById(modelId).orElse(null);
                    BikeVariant variant = bikeVariantRepository.findById(varId).orElse(null);
                    BikeInventory inv = bikeInventoryRepository.findById(invId).orElse(null);

                    BikeDetail bd = BikeDetail.builder()
                            .id(id)
                            .application(app)
                            .manufacturer(mfg)
                            .bikeModel(model)
                            .variant(variant)
                            .bikeInventory(inv)
                            .manufacturingYear(rs.getInt("manufacturing_year"))
                            .registrationNumber(rs.getString("registration_number"))
                            .colour(rs.getString("colour"))
                            .purchaseDate(toLocalDate(rs.getDate("purchase_date")))
                            .build();
                    bd = bikeDetailRepository.save(bd);
                    if (app != null) {
                        app.setBikeDetail(bd);
                        applicationRepository.save(app);
                    }
                    inserted++;
                } else {
                    skipped++;
                }
            }
        }
        System.out.println("[RECOVERY] Bike Details: Inserted=" + inserted + ", Skipped=" + skipped);
    }

    private void syncSequenceCounters() {
        String[] collections = new String[]{
                "users", "customers", "workers", "manufacturers", "bike_models",
                "bike_variants", "manufacturing_years", "applications", "finance_details",
                "emi_payments", "documents", "notifications", "bike_inventory", "bike_inventory_images", "bike_details", "emi_overdue_alerts",
                "bike_offers", "service_jobs", "worker_tasks", "application_status_history"
        };
        for (String seqName : collections) {
            sequenceGenerator.resetSequenceToMax(seqName);
        }
        System.out.println("[RECOVERY] Synchronized all database_sequences to MAX(id).");
    }

    private void printFinalParityReport() throws Exception {
        System.out.println("==================================================");
        System.out.println("FINAL PARITY REPORT: MYSQL VS MONGODB");
        System.out.println("==================================================");

        List<String> mysqlTables = new ArrayList<>();
        Map<String, Long> mysqlCounts = new LinkedHashMap<>();
        Map<String, Long> mongoCounts = new LinkedHashMap<>();

        try (Connection conn = getMysqlConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables("auto_consultancy", null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    mysqlTables.add(rs.getString("TABLE_NAME"));
                }
            }
            for (String table : mysqlTables) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM `" + table + "`")) {
                    if (rs.next()) {
                        mysqlCounts.put(table, rs.getLong(1));
                    }
                }
            }
        }

        for (String coll : mongoTemplate.getCollectionNames()) {
            mongoCounts.put(coll, mongoTemplate.getCollection(coll).countDocuments());
        }

        System.out.printf("%-30s %-12s %-12s %-15s\n", "COLLECTION / TABLE", "MYSQL", "MONGODB", "STATUS");
        System.out.println("------------------------------------------------------------------");

        Set<String> allNames = new TreeSet<>();
        allNames.addAll(mysqlCounts.keySet());
        allNames.addAll(mongoCounts.keySet());

        for (String name : allNames) {
            Long myCount = mysqlCounts.getOrDefault(name, 0L);
            Long moCount = mongoCounts.getOrDefault(name, 0L);
            String status = (myCount.equals(moCount)) ? "MATCH" : (moCount >= myCount ? "OK (SUPERSET)" : "MISMATCH");
            System.out.printf("%-30s %-12d %-12d %-15s\n", name, myCount, moCount, status);
        }
        System.out.println("==================================================");
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private LocalDate toLocalDate(java.sql.Date date) {
        return date != null ? date.toLocalDate() : null;
    }
}
