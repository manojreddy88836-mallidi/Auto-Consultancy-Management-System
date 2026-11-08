package com.autoconsultancy;

import com.autoconsultancy.entity.*;
import com.autoconsultancy.repository.*;
import com.autoconsultancy.service.SequenceGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    private final PasswordEncoder passwordEncoder;
    private final SequenceGeneratorService sequenceGenerator;

    @org.springframework.beans.factory.annotation.Value("${admin.emails:venkatamanojreddymallidi@gmail.com,ganeshautoconsultancy358@gmail.com}")
    private String adminEmailsRaw;

    /** Returns the trimmed, comma-separated list of authorized admin emails. */
    private List<String> getAdminEmails() {
        return Arrays.stream(adminEmailsRaw.split(","))
                .map(String::trim)
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toList());
    }

    @org.springframework.beans.factory.annotation.Value("${SEED_ADMIN_PASSWORD:Admin@123}")
    private String adminPassword;
    @org.springframework.beans.factory.annotation.Value("${SEED_WORKER_PASSWORD:Worker@123}")
    private String workerPassword;

    @EventListener(ApplicationReadyEvent.class)
    public void seedData() {
        List<String> adminEmails = getAdminEmails();
        // Use the first admin email as the primary (for migration logic)
        String primaryAdminEmail = adminEmails.isEmpty()
                ? "venkatamanojreddymallidi@gmail.com"
                : adminEmails.get(0);

        // ── Admin email migration ─────────────────────────────────────────────
        // If the old admin email exists and the primary one doesn't, migrate it.
        final String OLD_ADMIN_EMAIL = "admin@autoconsultancy.com";
        if (!primaryAdminEmail.equalsIgnoreCase(OLD_ADMIN_EMAIL)) {
            userRepository.findByEmail(OLD_ADMIN_EMAIL).ifPresent(oldAdmin -> {
                if (!userRepository.findByEmail(primaryAdminEmail).isPresent()) {
                    oldAdmin.setEmail(primaryAdminEmail);
                    userRepository.save(oldAdmin);
                    System.out.println("[DataSeeder] Admin email migrated: " + OLD_ADMIN_EMAIL + " → " + primaryAdminEmail);
                }
            });
        }
        // ─────────────────────────────────────────────────────────────────────

        // ── Seed / sync ALL authorized admin emails ───────────────────────────
        for (String email : adminEmails) {
            // Sync admin password if it changed (avoids BCrypt re-hash on every startup)
            userRepository.findByEmail(email).ifPresent(existingAdmin -> {
                if (!passwordEncoder.matches(adminPassword, existingAdmin.getPassword())) {
                    existingAdmin.setPassword(passwordEncoder.encode(adminPassword));
                    userRepository.save(existingAdmin);
                    System.out.println("[DataSeeder] Admin password updated: " + email);
                } else {
                    System.out.println("[DataSeeder] Admin password unchanged — skipping re-hash: " + email);
                }
            });

            // Create admin account if it doesn't exist
            if (!userRepository.findByEmail(email).isPresent()) {
                long adminId = sequenceGenerator.generateSequence("users");
                User admin = User.builder()
                        .id(adminId)
                        .email(email)
                        .password(passwordEncoder.encode(adminPassword))
                        .firstName("Admin")
                        .lastName("User")
                        .role(Role.ADMIN)
                        .active(true)
                        .build();
                userRepository.save(admin);
                System.out.println("[DataSeeder] Admin seeded: " + email);

                // Seed workers only on first-ever admin creation (primary admin)
                if (email.equalsIgnoreCase(primaryAdminEmail)) {
                    seedWorker("worker1@autoconsultancy.com", workerPassword, "Worker", "One", "EMP1001", "Operations", "Senior Consultant");
                    seedWorker("worker2@autoconsultancy.com", workerPassword, "Worker", "Two", "EMP1002", "Finance", "Finance Analyst");
                    seedWorker("worker3@autoconsultancy.com", workerPassword, "Worker", "Three", "EMP1003", "Verification", "Document Specialist");
                }
            }
        }
        // ─────────────────────────────────────────────────────────────────────

        ensureDefaultUsersActive();

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
        // Keep all seeded accounts active
        List<String> defaultEmails = new java.util.ArrayList<>(getAdminEmails());
        defaultEmails.addAll(List.of(
                "admin@autoconsultancy.com",
                "worker1@autoconsultancy.com",
                "worker2@autoconsultancy.com",
                "worker3@autoconsultancy.com"
        ));
        for (String email : defaultEmails) {
            userRepository.findByEmail(email).ifPresent(u -> {
                if (!u.isActive()) {
                    u.setActive(true);
                    userRepository.save(u);
                }
            });
        }
    }

}
