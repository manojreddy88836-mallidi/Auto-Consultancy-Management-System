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

        seedAllBrandsAndModels();
    }

    /**
     * Seeds all manufacturers and models.
     * Uses getOrCreate pattern — safe to run on existing data (no duplicates).
     */
    private void seedAllBrandsAndModels() {
        // ── ROYAL ENFIELD ────────────────────────────────────────────────────────
        Manufacturer royalEnfield = getOrCreateBrand("ROYAL ENFIELD", "India");
        seedModelIfAbsent(royalEnfield, "Classic 350", "Cruiser", "Petrol", 2010, 2024);
        seedModelIfAbsent(royalEnfield, "Bullet 350", "Cruiser", "Petrol", 2001, 2024);
        seedModelIfAbsent(royalEnfield, "Bullet 500", "Cruiser", "Petrol", 2001, 2020);
        seedModelIfAbsent(royalEnfield, "Meteor 350", "Cruiser", "Petrol", 2020, 2024);
        seedModelIfAbsent(royalEnfield, "Himalayan", "Adventure", "Petrol", 2016, 2024);
        seedModelIfAbsent(royalEnfield, "Himalayan 450", "Adventure", "Petrol", 2023, 2024);
        seedModelIfAbsent(royalEnfield, "Interceptor 650", "Cruiser", "Petrol", 2019, 2024);
        seedModelIfAbsent(royalEnfield, "Continental GT 650", "Cafe Racer", "Petrol", 2019, 2024);
        seedModelIfAbsent(royalEnfield, "Hunter 350", "Roadster", "Petrol", 2022, 2024);
        seedModelIfAbsent(royalEnfield, "Guerrilla 450", "Roadster", "Petrol", 2024, 2024);
        seedModelIfAbsent(royalEnfield, "Bear 650", "Scrambler", "Petrol", 2024, 2024);
        seedModelIfAbsent(royalEnfield, "Shotgun 650", "Bobber", "Petrol", 2024, 2024);
        seedModelIfAbsent(royalEnfield, "Super Meteor 650", "Cruiser", "Petrol", 2023, 2024);
        seedModelIfAbsent(royalEnfield, "Scram 411", "Scrambler", "Petrol", 2022, 2024);
        seedModelIfAbsent(royalEnfield, "Classic 500", "Cruiser", "Petrol", 2001, 2020);
        seedModelIfAbsent(royalEnfield, "Thunderbird 350", "Touring", "Petrol", 2002, 2019);
        seedModelIfAbsent(royalEnfield, "Thunderbird 500", "Touring", "Petrol", 2002, 2019);
        seedModelIfAbsent(royalEnfield, "Electra 350", "Cruiser", "Petrol", 2001, 2018);

        // ── HERO MOTOCORP ────────────────────────────────────────────────────────
        Manufacturer hero = getOrCreateBrand("HERO", "India");
        seedModelIfAbsent(hero, "Splendor Plus", "Commuter", "Petrol", 2001, 2024);
        seedModelIfAbsent(hero, "Splendor+ XTEC", "Commuter", "Petrol", 2021, 2024);
        seedModelIfAbsent(hero, "Splendor iSmart", "Commuter", "Petrol", 2015, 2024);
        seedModelIfAbsent(hero, "HF Deluxe", "Commuter", "Petrol", 2016, 2024);
        seedModelIfAbsent(hero, "HF 100", "Commuter", "Petrol", 2022, 2024);
        seedModelIfAbsent(hero, "Passion Pro", "Commuter", "Petrol", 2001, 2024);
        seedModelIfAbsent(hero, "Passion XPro", "Commuter", "Petrol", 2014, 2024);
        seedModelIfAbsent(hero, "Super Splendor", "Commuter", "Petrol", 2003, 2024);
        seedModelIfAbsent(hero, "Glamour", "Commuter", "Petrol", 2005, 2024);
        seedModelIfAbsent(hero, "Glamour XTEC", "Commuter", "Petrol", 2021, 2024);
        seedModelIfAbsent(hero, "Destini 125", "Scooter", "Petrol", 2018, 2024);
        seedModelIfAbsent(hero, "Destini 125 XTECH", "Scooter", "Petrol", 2022, 2024);
        seedModelIfAbsent(hero, "Maestro Edge 110", "Scooter", "Petrol", 2016, 2024);
        seedModelIfAbsent(hero, "Maestro Edge 125", "Scooter", "Petrol", 2018, 2024);
        seedModelIfAbsent(hero, "Pleasure+ 110", "Scooter", "Petrol", 2016, 2024);
        seedModelIfAbsent(hero, "Pleasure+ XTEC", "Scooter", "Petrol", 2021, 2024);
        seedModelIfAbsent(hero, "Xoom 110", "Scooter", "Petrol", 2022, 2024);
        seedModelIfAbsent(hero, "Xtreme 160R", "Sports", "Petrol", 2020, 2024);
        seedModelIfAbsent(hero, "Xtreme 160R 4V", "Sports", "Petrol", 2022, 2024);
        seedModelIfAbsent(hero, "Xtreme 200S", "Sports", "Petrol", 2019, 2024);
        seedModelIfAbsent(hero, "Xtreme 125R", "Sports", "Petrol", 2023, 2024);
        seedModelIfAbsent(hero, "Mavrick 440", "Roadster", "Petrol", 2023, 2024);
        seedModelIfAbsent(hero, "Karizma XMR", "Sports", "Petrol", 2023, 2024);
        seedModelIfAbsent(hero, "Xpulse 200", "Adventure", "Petrol", 2019, 2024);
        seedModelIfAbsent(hero, "Xpulse 200T", "Touring", "Petrol", 2019, 2024);
        seedModelIfAbsent(hero, "Xpulse 200 4V", "Adventure", "Petrol", 2021, 2024);
        seedModelIfAbsent(hero, "CD 100", "Commuter", "Petrol", 2001, 2010);
        seedModelIfAbsent(hero, "CD Deluxe", "Commuter", "Petrol", 2001, 2015);
        seedModelIfAbsent(hero, "CBZ Xtreme", "Sports", "Petrol", 2003, 2017);
        seedModelIfAbsent(hero, "Hunk", "Sports", "Petrol", 2007, 2016);
        seedModelIfAbsent(hero, "Ignitor", "Commuter", "Petrol", 2012, 2018);
        seedModelIfAbsent(hero, "Dash", "Scooter", "Petrol", 2014, 2016);
        seedModelIfAbsent(hero, "Optima Plus (EV)", "Scooter", "Electric", 2022, 2024);

        // ── HONDA ────────────────────────────────────────────────────────────────
        Manufacturer honda = getOrCreateBrand("HONDA", "Japan");
        seedModelIfAbsent(honda, "Activa 6G", "Scooter", "Petrol", 2019, 2024);
        seedModelIfAbsent(honda, "Activa 125", "Scooter", "Petrol", 2013, 2024);
        seedModelIfAbsent(honda, "Activa 125 Premium", "Scooter", "Petrol", 2022, 2024);
        seedModelIfAbsent(honda, "Activa EV", "Scooter", "Electric", 2024, 2024);
        seedModelIfAbsent(honda, "Shine 100", "Commuter", "Petrol", 2022, 2024);
        seedModelIfAbsent(honda, "Shine", "Commuter", "Petrol", 2006, 2024);
        seedModelIfAbsent(honda, "CB Shine SP", "Commuter", "Petrol", 2015, 2024);
        seedModelIfAbsent(honda, "Livo", "Commuter", "Petrol", 2015, 2024);
        seedModelIfAbsent(honda, "Dream Yuga", "Commuter", "Petrol", 2012, 2020);
        seedModelIfAbsent(honda, "Dream Neo", "Commuter", "Petrol", 2013, 2019);
        seedModelIfAbsent(honda, "SP 125", "Commuter", "Petrol", 2019, 2024);
        seedModelIfAbsent(honda, "SP 160", "Commuter", "Petrol", 2022, 2024);
        seedModelIfAbsent(honda, "Unicorn", "Commuter", "Petrol", 2004, 2024);
        seedModelIfAbsent(honda, "CB300R", "Roadster", "Petrol", 2019, 2024);
        seedModelIfAbsent(honda, "CB300F", "Sports", "Petrol", 2021, 2024);
        seedModelIfAbsent(honda, "CB350", "Roadster", "Petrol", 2020, 2024);
        seedModelIfAbsent(honda, "CB350RS", "Cafe Racer", "Petrol", 2021, 2024);
        seedModelIfAbsent(honda, "CB500F", "Roadster", "Petrol", 2019, 2024);
        seedModelIfAbsent(honda, "CB500X", "Adventure", "Petrol", 2019, 2024);
        seedModelIfAbsent(honda, "CB650R", "Roadster", "Petrol", 2019, 2024);
        seedModelIfAbsent(honda, "CBR650R", "Sports", "Petrol", 2019, 2024);
        seedModelIfAbsent(honda, "Hornet 2.0", "Roadster", "Petrol", 2020, 2024);
        seedModelIfAbsent(honda, "NX200", "Adventure", "Petrol", 2022, 2024);
        seedModelIfAbsent(honda, "NX500", "Adventure", "Petrol", 2023, 2024);
        seedModelIfAbsent(honda, "Africa Twin", "Adventure", "Petrol", 2016, 2024);
        seedModelIfAbsent(honda, "Gold Wing", "Touring", "Petrol", 2001, 2024);
        seedModelIfAbsent(honda, "CBR1000RR-R", "Superbike", "Petrol", 2020, 2024);
        seedModelIfAbsent(honda, "Dio 110", "Scooter", "Petrol", 2012, 2024);
        seedModelIfAbsent(honda, "Dio 125", "Scooter", "Petrol", 2021, 2024);
        seedModelIfAbsent(honda, "Grazia 125", "Scooter", "Petrol", 2017, 2023);
        seedModelIfAbsent(honda, "Aviator", "Scooter", "Petrol", 2004, 2020);
        seedModelIfAbsent(honda, "Cliq", "Scooter", "Petrol", 2016, 2020);
        seedModelIfAbsent(honda, "Navi", "Crossover", "Petrol", 2016, 2024);
        seedModelIfAbsent(honda, "X-Blade", "Sports", "Petrol", 2018, 2024);
        seedModelIfAbsent(honda, "CBR250R", "Sports", "Petrol", 2011, 2017);
        seedModelIfAbsent(honda, "CBR400R", "Sports", "Petrol", 2023, 2024);

        // ── TVS ──────────────────────────────────────────────────────────────────
        Manufacturer tvs = getOrCreateBrand("TVS", "India");
        seedModelIfAbsent(tvs, "Apache RTR 160", "Sports", "Petrol", 2007, 2024);
        seedModelIfAbsent(tvs, "Apache RTR 160 4V", "Sports", "Petrol", 2018, 2024);
        seedModelIfAbsent(tvs, "Apache RTR 180", "Sports", "Petrol", 2010, 2024);
        seedModelIfAbsent(tvs, "Apache RTR 200 4V", "Sports", "Petrol", 2016, 2024);
        seedModelIfAbsent(tvs, "Apache RR 310", "Superbike", "Petrol", 2017, 2024);
        seedModelIfAbsent(tvs, "Jupiter", "Scooter", "Petrol", 2013, 2024);
        seedModelIfAbsent(tvs, "Jupiter 125", "Scooter", "Petrol", 2021, 2024);
        seedModelIfAbsent(tvs, "Jupiter ZX", "Scooter", "Petrol", 2016, 2024);
        seedModelIfAbsent(tvs, "NTORQ 125", "Scooter", "Petrol", 2018, 2024);
        seedModelIfAbsent(tvs, "NTORQ 125 Race Edition", "Scooter", "Petrol", 2019, 2024);
        seedModelIfAbsent(tvs, "iQube Electric", "Scooter", "Electric", 2020, 2024);
        seedModelIfAbsent(tvs, "iQube ST", "Scooter", "Electric", 2022, 2024);
        seedModelIfAbsent(tvs, "iQube S", "Scooter", "Electric", 2022, 2024);
        seedModelIfAbsent(tvs, "Raider 125", "Commuter", "Petrol", 2021, 2024);
        seedModelIfAbsent(tvs, "Raider 125 Drum", "Commuter", "Petrol", 2021, 2024);
        seedModelIfAbsent(tvs, "Ronin", "Roadster", "Petrol", 2022, 2024);
        seedModelIfAbsent(tvs, "Sport", "Commuter", "Petrol", 2001, 2024);
        seedModelIfAbsent(tvs, "Star City+", "Commuter", "Petrol", 2016, 2024);
        seedModelIfAbsent(tvs, "XL100", "Moped", "Petrol", 2001, 2024);
        seedModelIfAbsent(tvs, "XL100 Heavy Duty", "Moped", "Petrol", 2016, 2024);
        seedModelIfAbsent(tvs, "Victor", "Commuter", "Petrol", 2001, 2018);
        seedModelIfAbsent(tvs, "Wego", "Scooter", "Petrol", 2011, 2019);
        seedModelIfAbsent(tvs, "Scooty Pep+", "Scooter", "Petrol", 2001, 2024);
        seedModelIfAbsent(tvs, "Scooty Zest 110", "Scooter", "Petrol", 2014, 2024);
        seedModelIfAbsent(tvs, "Fiero 125", "Sports", "Petrol", 2001, 2008);
        seedModelIfAbsent(tvs, "MAX4R", "Sports", "Petrol", 2001, 2007);

        // ── BAJAJ ────────────────────────────────────────────────────────────────
        Manufacturer bajaj = getOrCreateBrand("BAJAJ", "India");
        seedModelIfAbsent(bajaj, "Pulsar 125", "Sports", "Petrol", 2019, 2024);
        seedModelIfAbsent(bajaj, "Pulsar 150", "Sports", "Petrol", 2001, 2024);
        seedModelIfAbsent(bajaj, "Pulsar 150 Neon", "Sports", "Petrol", 2019, 2022);
        seedModelIfAbsent(bajaj, "Pulsar 180", "Sports", "Petrol", 2001, 2024);
        seedModelIfAbsent(bajaj, "Pulsar 220F", "Sports", "Petrol", 2007, 2024);
        seedModelIfAbsent(bajaj, "Pulsar NS 160", "Sports", "Petrol", 2017, 2024);
        seedModelIfAbsent(bajaj, "Pulsar NS 200", "Sports", "Petrol", 2012, 2024);
        seedModelIfAbsent(bajaj, "Pulsar RS 200", "Sports", "Petrol", 2015, 2024);
        seedModelIfAbsent(bajaj, "Pulsar N160", "Sports", "Petrol", 2022, 2024);
        seedModelIfAbsent(bajaj, "Pulsar N250", "Sports", "Petrol", 2021, 2024);
        seedModelIfAbsent(bajaj, "Pulsar F250", "Sports", "Petrol", 2021, 2024);
        seedModelIfAbsent(bajaj, "Pulsar P150", "Sports", "Petrol", 2022, 2024);
        seedModelIfAbsent(bajaj, "Avenger Street 160", "Cruiser", "Petrol", 2016, 2024);
        seedModelIfAbsent(bajaj, "Avenger Cruise 220", "Cruiser", "Petrol", 2010, 2024);
        seedModelIfAbsent(bajaj, "Avenger Street 220", "Cruiser", "Petrol", 2010, 2024);
        seedModelIfAbsent(bajaj, "Dominar 250", "Touring", "Petrol", 2019, 2024);
        seedModelIfAbsent(bajaj, "Dominar 400", "Touring", "Petrol", 2017, 2024);
        seedModelIfAbsent(bajaj, "CT 100", "Commuter", "Petrol", 2004, 2024);
        seedModelIfAbsent(bajaj, "CT 110", "Commuter", "Petrol", 2016, 2024);
        seedModelIfAbsent(bajaj, "CT 125X", "Commuter", "Petrol", 2022, 2024);
        seedModelIfAbsent(bajaj, "Platina 100", "Commuter", "Petrol", 2006, 2024);
        seedModelIfAbsent(bajaj, "Platina 110", "Commuter", "Petrol", 2017, 2024);
        seedModelIfAbsent(bajaj, "Platina 110 H Gear", "Commuter", "Petrol", 2019, 2024);
        seedModelIfAbsent(bajaj, "Chetak Electric", "Scooter", "Electric", 2020, 2024);
        seedModelIfAbsent(bajaj, "Chetak Premium", "Scooter", "Electric", 2022, 2024);
        seedModelIfAbsent(bajaj, "Discover 100", "Commuter", "Petrol", 2004, 2020);
        seedModelIfAbsent(bajaj, "Discover 125", "Commuter", "Petrol", 2004, 2024);
        seedModelIfAbsent(bajaj, "Discover 150", "Commuter", "Petrol", 2010, 2019);
        seedModelIfAbsent(bajaj, "Boxer 100", "Commuter", "Petrol", 2001, 2015);
        seedModelIfAbsent(bajaj, "Kristal", "Scooter", "Petrol", 2007, 2012);
        seedModelIfAbsent(bajaj, "Freedom 125 CNG", "Commuter", "CNG", 2024, 2024);

        // ── YAMAHA ───────────────────────────────────────────────────────────────
        Manufacturer yamaha = getOrCreateBrand("YAMAHA", "Japan");
        seedModelIfAbsent(yamaha, "FZ-S FI V4", "Roadster", "Petrol", 2022, 2024);
        seedModelIfAbsent(yamaha, "FZ-FI V3", "Roadster", "Petrol", 2019, 2024);
        seedModelIfAbsent(yamaha, "FZS-FI V4", "Roadster", "Petrol", 2022, 2024);
        seedModelIfAbsent(yamaha, "FZ-X", "Crossover", "Petrol", 2021, 2024);
        seedModelIfAbsent(yamaha, "FZ 25", "Roadster", "Petrol", 2017, 2024);
        seedModelIfAbsent(yamaha, "FZS 25", "Roadster", "Petrol", 2019, 2024);
        seedModelIfAbsent(yamaha, "MT-15 V2", "Roadster", "Petrol", 2022, 2024);
        seedModelIfAbsent(yamaha, "MT-03", "Roadster", "Petrol", 2016, 2024);
        seedModelIfAbsent(yamaha, "MT-07", "Roadster", "Petrol", 2016, 2024);
        seedModelIfAbsent(yamaha, "MT-09", "Roadster", "Petrol", 2016, 2024);
        seedModelIfAbsent(yamaha, "R15 V4", "Sports", "Petrol", 2021, 2024);
        seedModelIfAbsent(yamaha, "R15M", "Sports", "Petrol", 2021, 2024);
        seedModelIfAbsent(yamaha, "R15 V3", "Sports", "Petrol", 2017, 2021);
        seedModelIfAbsent(yamaha, "R15 S", "Sports", "Petrol", 2022, 2024);
        seedModelIfAbsent(yamaha, "R3", "Sports", "Petrol", 2015, 2024);
        seedModelIfAbsent(yamaha, "R7", "Sports", "Petrol", 2021, 2024);
        seedModelIfAbsent(yamaha, "YZF-R1", "Superbike", "Petrol", 2004, 2024);
        seedModelIfAbsent(yamaha, "Fascino 125", "Scooter", "Petrol", 2020, 2024);
        seedModelIfAbsent(yamaha, "Fascino 125 Hybrid", "Scooter", "Petrol", 2021, 2024);
        seedModelIfAbsent(yamaha, "Ray ZR 125", "Scooter", "Petrol", 2017, 2024);
        seedModelIfAbsent(yamaha, "Ray ZR Street Rally", "Scooter", "Petrol", 2019, 2024);
        seedModelIfAbsent(yamaha, "Aerox 155", "Scooter", "Petrol", 2021, 2024);
        seedModelIfAbsent(yamaha, "Alpha", "Scooter", "Petrol", 2014, 2020);
        seedModelIfAbsent(yamaha, "Cygnus Ray ZR", "Scooter", "Petrol", 2016, 2020);
        seedModelIfAbsent(yamaha, "SS 125", "Scooter", "Petrol", 2016, 2020);
        seedModelIfAbsent(yamaha, "Saluto 125", "Commuter", "Petrol", 2015, 2019);
        seedModelIfAbsent(yamaha, "SZ-RR V2", "Commuter", "Petrol", 2016, 2020);
        seedModelIfAbsent(yamaha, "Crux R", "Commuter", "Petrol", 2001, 2012);
        seedModelIfAbsent(yamaha, "Libero G5", "Commuter", "Petrol", 2001, 2008);
        seedModelIfAbsent(yamaha, "Fazer 25", "Touring", "Petrol", 2016, 2020);

        // ── SUZUKI ───────────────────────────────────────────────────────────────
        Manufacturer suzuki = getOrCreateBrand("SUZUKI", "Japan");
        seedModelIfAbsent(suzuki, "Gixxer 150", "Sports", "Petrol", 2014, 2024);
        seedModelIfAbsent(suzuki, "Gixxer SF 150", "Sports", "Petrol", 2015, 2024);
        seedModelIfAbsent(suzuki, "Gixxer 250", "Sports", "Petrol", 2019, 2024);
        seedModelIfAbsent(suzuki, "Gixxer SF 250", "Sports", "Petrol", 2019, 2024);
        seedModelIfAbsent(suzuki, "Access 125", "Scooter", "Petrol", 2007, 2024);
        seedModelIfAbsent(suzuki, "Access 125 Special Edition", "Scooter", "Petrol", 2019, 2024);
        seedModelIfAbsent(suzuki, "Burgman Street 125", "Scooter", "Petrol", 2018, 2024);
        seedModelIfAbsent(suzuki, "Burgman Street EX", "Scooter", "Petrol", 2022, 2024);
        seedModelIfAbsent(suzuki, "Avenis 125", "Scooter", "Petrol", 2021, 2024);
        seedModelIfAbsent(suzuki, "V-Strom SX 250", "Adventure", "Petrol", 2022, 2024);
        seedModelIfAbsent(suzuki, "V-Strom 650", "Adventure", "Petrol", 2016, 2024);
        seedModelIfAbsent(suzuki, "V-Strom 1050", "Adventure", "Petrol", 2020, 2024);
        seedModelIfAbsent(suzuki, "Hayabusa", "Superbike", "Petrol", 2001, 2024);
        seedModelIfAbsent(suzuki, "GSX-R1000", "Superbike", "Petrol", 2001, 2020);
        seedModelIfAbsent(suzuki, "GSX-S1000", "Roadster", "Petrol", 2015, 2024);
        seedModelIfAbsent(suzuki, "GSX-S750", "Roadster", "Petrol", 2016, 2022);
        seedModelIfAbsent(suzuki, "Intruder 150", "Cruiser", "Petrol", 2017, 2024);
        seedModelIfAbsent(suzuki, "Let's 110", "Scooter", "Petrol", 2016, 2020);
        seedModelIfAbsent(suzuki, "Swish 125", "Scooter", "Petrol", 2012, 2018);
        seedModelIfAbsent(suzuki, "Heat", "Commuter", "Petrol", 2007, 2014);
        seedModelIfAbsent(suzuki, "Slingshot Plus", "Commuter", "Petrol", 2007, 2016);

        // ── KTM ──────────────────────────────────────────────────────────────────
        Manufacturer ktm = getOrCreateBrand("KTM", "Austria");
        seedModelIfAbsent(ktm, "Duke 125", "Roadster", "Petrol", 2016, 2024);
        seedModelIfAbsent(ktm, "Duke 200", "Roadster", "Petrol", 2012, 2024);
        seedModelIfAbsent(ktm, "Duke 250", "Roadster", "Petrol", 2017, 2024);
        seedModelIfAbsent(ktm, "Duke 390", "Roadster", "Petrol", 2013, 2024);
        seedModelIfAbsent(ktm, "RC 125", "Sports", "Petrol", 2016, 2024);
        seedModelIfAbsent(ktm, "RC 200", "Sports", "Petrol", 2014, 2024);
        seedModelIfAbsent(ktm, "RC 390", "Sports", "Petrol", 2014, 2024);
        seedModelIfAbsent(ktm, "250 Adventure", "Adventure", "Petrol", 2020, 2024);
        seedModelIfAbsent(ktm, "390 Adventure", "Adventure", "Petrol", 2019, 2024);
        seedModelIfAbsent(ktm, "390 Adventure X", "Adventure", "Petrol", 2023, 2024);
        seedModelIfAbsent(ktm, "790 Adventure", "Adventure", "Petrol", 2019, 2024);
        seedModelIfAbsent(ktm, "890 Adventure", "Adventure", "Petrol", 2021, 2024);
        seedModelIfAbsent(ktm, "1290 Super Adventure", "Adventure", "Petrol", 2016, 2024);
        seedModelIfAbsent(ktm, "Super Duke R 1290", "Roadster", "Petrol", 2016, 2024);

        // ── KAWASAKI ─────────────────────────────────────────────────────────────
        Manufacturer kawasaki = getOrCreateBrand("KAWASAKI", "Japan");
        seedModelIfAbsent(kawasaki, "Z250", "Roadster", "Petrol", 2013, 2024);
        seedModelIfAbsent(kawasaki, "Z400", "Roadster", "Petrol", 2019, 2024);
        seedModelIfAbsent(kawasaki, "Z650", "Roadster", "Petrol", 2017, 2024);
        seedModelIfAbsent(kawasaki, "Z900", "Roadster", "Petrol", 2017, 2024);
        seedModelIfAbsent(kawasaki, "Z900RS", "Retro Roadster", "Petrol", 2018, 2024);
        seedModelIfAbsent(kawasaki, "Ninja 300", "Sports", "Petrol", 2013, 2019);
        seedModelIfAbsent(kawasaki, "Ninja 400", "Sports", "Petrol", 2018, 2024);
        seedModelIfAbsent(kawasaki, "Ninja 650", "Sports", "Petrol", 2012, 2024);
        seedModelIfAbsent(kawasaki, "Ninja 1000SX", "Sports Touring", "Petrol", 2020, 2024);
        seedModelIfAbsent(kawasaki, "Ninja ZX-6R", "Superbike", "Petrol", 2001, 2024);
        seedModelIfAbsent(kawasaki, "Ninja ZX-10R", "Superbike", "Petrol", 2004, 2024);
        seedModelIfAbsent(kawasaki, "Ninja H2", "Hyperbike", "Petrol", 2015, 2024);
        seedModelIfAbsent(kawasaki, "Versys 300", "Adventure", "Petrol", 2017, 2024);
        seedModelIfAbsent(kawasaki, "Versys 650", "Adventure", "Petrol", 2010, 2024);
        seedModelIfAbsent(kawasaki, "Versys 1000", "Adventure", "Petrol", 2012, 2024);
        seedModelIfAbsent(kawasaki, "W175", "Retro", "Petrol", 2018, 2024);
        seedModelIfAbsent(kawasaki, "Vulcan S", "Cruiser", "Petrol", 2015, 2024);
        seedModelIfAbsent(kawasaki, "Eliminator 450", "Cruiser", "Petrol", 2023, 2024);

        // ── JAWA ─────────────────────────────────────────────────────────────────
        Manufacturer jawa = getOrCreateBrand("JAWA", "India");
        seedModelIfAbsent(jawa, "Jawa 42", "Cruiser", "Petrol", 2018, 2024);
        seedModelIfAbsent(jawa, "Jawa 42 FJ", "Cruiser", "Petrol", 2022, 2024);
        seedModelIfAbsent(jawa, "Jawa Classic", "Cruiser", "Petrol", 2018, 2024);
        seedModelIfAbsent(jawa, "Jawa Perak", "Bobber", "Petrol", 2019, 2024);
        seedModelIfAbsent(jawa, "Jawa 350 OHC", "Roadster", "Petrol", 2023, 2024);
        seedModelIfAbsent(jawa, "Yezdi Roadster", "Roadster", "Petrol", 2022, 2024);
        seedModelIfAbsent(jawa, "Yezdi Adventure", "Adventure", "Petrol", 2022, 2024);
        seedModelIfAbsent(jawa, "Yezdi Scrambler", "Scrambler", "Petrol", 2022, 2024);

        // ── BENELLI ──────────────────────────────────────────────────────────────
        Manufacturer benelli = getOrCreateBrand("BENELLI", "Italy");
        seedModelIfAbsent(benelli, "Imperiale 400", "Cruiser", "Petrol", 2019, 2024);
        seedModelIfAbsent(benelli, "TRK 502", "Adventure", "Petrol", 2017, 2024);
        seedModelIfAbsent(benelli, "TRK 502X", "Adventure", "Petrol", 2019, 2024);
        seedModelIfAbsent(benelli, "Leoncino 500", "Scrambler", "Petrol", 2019, 2024);
        seedModelIfAbsent(benelli, "302R", "Sports", "Petrol", 2019, 2022);
        seedModelIfAbsent(benelli, "TNT 300", "Roadster", "Petrol", 2016, 2020);
        seedModelIfAbsent(benelli, "TNT 600", "Roadster", "Petrol", 2016, 2020);
        seedModelIfAbsent(benelli, "BN 302R", "Sports", "Petrol", 2016, 2022);
        seedModelIfAbsent(benelli, "Tornado 302R", "Sports", "Petrol", 2016, 2020);

        // ── TRIUMPH ──────────────────────────────────────────────────────────────
        Manufacturer triumph = getOrCreateBrand("TRIUMPH", "UK");
        seedModelIfAbsent(triumph, "Speed 400", "Roadster", "Petrol", 2023, 2024);
        seedModelIfAbsent(triumph, "Scrambler 400X", "Scrambler", "Petrol", 2023, 2024);
        seedModelIfAbsent(triumph, "Trident 660", "Roadster", "Petrol", 2021, 2024);
        seedModelIfAbsent(triumph, "Street Triple R", "Roadster", "Petrol", 2016, 2024);
        seedModelIfAbsent(triumph, "Street Triple RS", "Roadster", "Petrol", 2017, 2024);
        seedModelIfAbsent(triumph, "Tiger Sport 660", "Adventure", "Petrol", 2021, 2024);
        seedModelIfAbsent(triumph, "Tiger 850 Sport", "Adventure", "Petrol", 2021, 2024);
        seedModelIfAbsent(triumph, "Tiger 900 GT", "Adventure", "Petrol", 2020, 2024);
        seedModelIfAbsent(triumph, "Tiger 900 Rally", "Adventure", "Petrol", 2020, 2024);
        seedModelIfAbsent(triumph, "Tiger 1200", "Adventure", "Petrol", 2016, 2024);
        seedModelIfAbsent(triumph, "Bonneville T100", "Retro", "Petrol", 2016, 2024);
        seedModelIfAbsent(triumph, "Bonneville T120", "Retro", "Petrol", 2016, 2024);
        seedModelIfAbsent(triumph, "Thruxton RS", "Cafe Racer", "Petrol", 2016, 2024);
        seedModelIfAbsent(triumph, "Scrambler 1200 XC", "Scrambler", "Petrol", 2019, 2024);
        seedModelIfAbsent(triumph, "Scrambler 1200 XE", "Scrambler", "Petrol", 2019, 2024);
        seedModelIfAbsent(triumph, "Rocket 3", "Cruiser", "Petrol", 2016, 2024);
        seedModelIfAbsent(triumph, "Daytona 660", "Sports", "Petrol", 2023, 2024);
        seedModelIfAbsent(triumph, "Speed Twin 900", "Retro", "Petrol", 2023, 2024);

        // ── BMW MOTORRAD ─────────────────────────────────────────────────────────
        Manufacturer bmw = getOrCreateBrand("BMW MOTORRAD", "Germany");
        seedModelIfAbsent(bmw, "G 310 R", "Roadster", "Petrol", 2017, 2024);
        seedModelIfAbsent(bmw, "G 310 GS", "Adventure", "Petrol", 2017, 2024);
        seedModelIfAbsent(bmw, "F 900 R", "Roadster", "Petrol", 2020, 2024);
        seedModelIfAbsent(bmw, "F 900 XR", "Adventure", "Petrol", 2020, 2024);
        seedModelIfAbsent(bmw, "F 850 GS", "Adventure", "Petrol", 2018, 2024);
        seedModelIfAbsent(bmw, "R 1250 GS", "Adventure", "Petrol", 2019, 2024);
        seedModelIfAbsent(bmw, "R 1250 GS Adventure", "Adventure", "Petrol", 2019, 2024);
        seedModelIfAbsent(bmw, "S 1000 RR", "Superbike", "Petrol", 2010, 2024);
        seedModelIfAbsent(bmw, "S 1000 R", "Roadster", "Petrol", 2014, 2024);
        seedModelIfAbsent(bmw, "S 1000 XR", "Adventure", "Petrol", 2015, 2024);
        seedModelIfAbsent(bmw, "M 1000 RR", "Superbike", "Petrol", 2021, 2024);
        seedModelIfAbsent(bmw, "R 18", "Cruiser", "Petrol", 2020, 2024);
        seedModelIfAbsent(bmw, "CE 04", "Scooter", "Electric", 2022, 2024);

        // ── DUCATI ───────────────────────────────────────────────────────────────
        Manufacturer ducati = getOrCreateBrand("DUCATI", "Italy");
        seedModelIfAbsent(ducati, "Monster", "Roadster", "Petrol", 2001, 2024);
        seedModelIfAbsent(ducati, "Monster SP", "Roadster", "Petrol", 2023, 2024);
        seedModelIfAbsent(ducati, "Scrambler Icon", "Scrambler", "Petrol", 2015, 2024);
        seedModelIfAbsent(ducati, "Scrambler Desert Sled", "Scrambler", "Petrol", 2016, 2024);
        seedModelIfAbsent(ducati, "Scrambler Nightshift", "Scrambler", "Petrol", 2021, 2024);
        seedModelIfAbsent(ducati, "Panigale V2", "Superbike", "Petrol", 2020, 2024);
        seedModelIfAbsent(ducati, "Panigale V4", "Superbike", "Petrol", 2018, 2024);
        seedModelIfAbsent(ducati, "Panigale V4 R", "Superbike", "Petrol", 2019, 2024);
        seedModelIfAbsent(ducati, "Multistrada V2", "Adventure", "Petrol", 2022, 2024);
        seedModelIfAbsent(ducati, "Multistrada V4", "Adventure", "Petrol", 2021, 2024);
        seedModelIfAbsent(ducati, "Multistrada V4 S", "Adventure", "Petrol", 2021, 2024);
        seedModelIfAbsent(ducati, "Streetfighter V2", "Roadster", "Petrol", 2022, 2024);
        seedModelIfAbsent(ducati, "Streetfighter V4", "Roadster", "Petrol", 2020, 2024);
        seedModelIfAbsent(ducati, "Diavel V4", "Power Cruiser", "Petrol", 2023, 2024);
        seedModelIfAbsent(ducati, "Hypermotard 950", "Supermoto", "Petrol", 2019, 2024);
        seedModelIfAbsent(ducati, "DesertX", "Adventure", "Petrol", 2022, 2024);

        // ── HARLEY-DAVIDSON ──────────────────────────────────────────────────────
        Manufacturer harley = getOrCreateBrand("HARLEY-DAVIDSON", "USA");
        seedModelIfAbsent(harley, "X440", "Roadster", "Petrol", 2023, 2024);
        seedModelIfAbsent(harley, "Street 750", "Cruiser", "Petrol", 2015, 2022);
        seedModelIfAbsent(harley, "Iron 883", "Cruiser", "Petrol", 2016, 2024);
        seedModelIfAbsent(harley, "Iron 1200", "Cruiser", "Petrol", 2018, 2024);
        seedModelIfAbsent(harley, "Forty-Eight", "Cruiser", "Petrol", 2016, 2024);
        seedModelIfAbsent(harley, "Fat Boy", "Cruiser", "Petrol", 2001, 2024);
        seedModelIfAbsent(harley, "Fat Bob", "Cruiser", "Petrol", 2016, 2024);
        seedModelIfAbsent(harley, "Sportster S", "Cruiser", "Petrol", 2021, 2024);
        seedModelIfAbsent(harley, "Nightster", "Cruiser", "Petrol", 2022, 2024);
        seedModelIfAbsent(harley, "Road Glide", "Touring", "Petrol", 2001, 2024);
        seedModelIfAbsent(harley, "Road King", "Touring", "Petrol", 2001, 2024);
        seedModelIfAbsent(harley, "Street Glide", "Touring", "Petrol", 2001, 2024);
        seedModelIfAbsent(harley, "Electra Glide", "Touring", "Petrol", 2001, 2024);
        seedModelIfAbsent(harley, "Breakout", "Cruiser", "Petrol", 2016, 2024);
        seedModelIfAbsent(harley, "Low Rider", "Cruiser", "Petrol", 2016, 2024);
        seedModelIfAbsent(harley, "Softail Standard", "Cruiser", "Petrol", 2020, 2024);
        seedModelIfAbsent(harley, "LiveWire One", "Roadster", "Electric", 2021, 2024);

        // ── APRILIA ──────────────────────────────────────────────────────────────
        Manufacturer aprilia = getOrCreateBrand("APRILIA", "Italy");
        seedModelIfAbsent(aprilia, "RS 457", "Sports", "Petrol", 2023, 2024);
        seedModelIfAbsent(aprilia, "RS 660", "Sports", "Petrol", 2020, 2024);
        seedModelIfAbsent(aprilia, "Tuono 660", "Roadster", "Petrol", 2021, 2024);
        seedModelIfAbsent(aprilia, "RS4 125", "Sports", "Petrol", 2016, 2024);
        seedModelIfAbsent(aprilia, "Storm 125", "Scooter", "Petrol", 2019, 2024);
        seedModelIfAbsent(aprilia, "SXR 125", "Scooter", "Petrol", 2021, 2024);
        seedModelIfAbsent(aprilia, "SXR 160", "Scooter", "Petrol", 2021, 2024);
        seedModelIfAbsent(aprilia, "SR 160", "Scooter", "Petrol", 2020, 2024);
        seedModelIfAbsent(aprilia, "SR 125", "Scooter", "Petrol", 2019, 2024);
        seedModelIfAbsent(aprilia, "Tuono V4", "Roadster", "Petrol", 2016, 2024);
        seedModelIfAbsent(aprilia, "RSV4", "Superbike", "Petrol", 2016, 2024);

        // ── VESPA / PIAGGIO ──────────────────────────────────────────────────────
        Manufacturer vespa = getOrCreateBrand("VESPA", "Italy");
        seedModelIfAbsent(vespa, "VX 125", "Scooter", "Petrol", 2012, 2024);
        seedModelIfAbsent(vespa, "VXL 125", "Scooter", "Petrol", 2016, 2024);
        seedModelIfAbsent(vespa, "VXL 150", "Scooter", "Petrol", 2016, 2024);
        seedModelIfAbsent(vespa, "ZX 125", "Scooter", "Petrol", 2012, 2024);
        seedModelIfAbsent(vespa, "ZX 150", "Scooter", "Petrol", 2012, 2024);
        seedModelIfAbsent(vespa, "Notte 125", "Scooter", "Petrol", 2016, 2020);
        seedModelIfAbsent(vespa, "SXL 125", "Scooter", "Petrol", 2016, 2024);
        seedModelIfAbsent(vespa, "SXL 150", "Scooter", "Petrol", 2016, 2024);
        seedModelIfAbsent(vespa, "Elettrica", "Scooter", "Electric", 2019, 2024);
        seedModelIfAbsent(vespa, "Primavera 125", "Scooter", "Petrol", 2016, 2024);
        seedModelIfAbsent(vespa, "GTS 300", "Scooter", "Petrol", 2018, 2024);

        // ── ATHER ────────────────────────────────────────────────────────────────
        Manufacturer ather = getOrCreateBrand("ATHER", "India");
        seedModelIfAbsent(ather, "450X", "Scooter", "Electric", 2019, 2024);
        seedModelIfAbsent(ather, "450 Apex", "Scooter", "Electric", 2022, 2024);
        seedModelIfAbsent(ather, "450S", "Scooter", "Electric", 2023, 2024);
        seedModelIfAbsent(ather, "450 Plus", "Scooter", "Electric", 2021, 2024);
        seedModelIfAbsent(ather, "Rizta", "Scooter", "Electric", 2024, 2024);

        // ── OLA ELECTRIC ─────────────────────────────────────────────────────────
        Manufacturer ola = getOrCreateBrand("OLA ELECTRIC", "India");
        seedModelIfAbsent(ola, "S1", "Scooter", "Electric", 2021, 2024);
        seedModelIfAbsent(ola, "S1 Air", "Scooter", "Electric", 2022, 2024);
        seedModelIfAbsent(ola, "S1 Pro", "Scooter", "Electric", 2021, 2024);
        seedModelIfAbsent(ola, "S1 X", "Scooter", "Electric", 2023, 2024);
        seedModelIfAbsent(ola, "S1 X+", "Scooter", "Electric", 2023, 2024);
        seedModelIfAbsent(ola, "Roadster", "Roadster", "Electric", 2024, 2024);
        seedModelIfAbsent(ola, "Roadster Pro", "Roadster", "Electric", 2024, 2024);
        seedModelIfAbsent(ola, "Roadster X", "Roadster", "Electric", 2024, 2024);

        // ── REVOLT ───────────────────────────────────────────────────────────────
        Manufacturer revolt = getOrCreateBrand("REVOLT", "India");
        seedModelIfAbsent(revolt, "RV400", "Roadster", "Electric", 2019, 2024);
        seedModelIfAbsent(revolt, "RV400 BRZ", "Roadster", "Electric", 2022, 2024);
        seedModelIfAbsent(revolt, "RV1", "Roadster", "Electric", 2023, 2024);

        // ── TORK ─────────────────────────────────────────────────────────────────
        Manufacturer tork = getOrCreateBrand("TORK", "India");
        seedModelIfAbsent(tork, "KRATOS R", "Roadster", "Electric", 2022, 2024);
        seedModelIfAbsent(tork, "KRATOS", "Roadster", "Electric", 2022, 2024);

        // ── ULTRAVIOLETTE ────────────────────────────────────────────────────────
        Manufacturer uv = getOrCreateBrand("ULTRAVIOLETTE", "India");
        seedModelIfAbsent(uv, "F77 Mach 2", "Sports", "Electric", 2023, 2024);
        seedModelIfAbsent(uv, "F77", "Sports", "Electric", 2022, 2024);

        // ── PURE EV ──────────────────────────────────────────────────────────────
        Manufacturer pureEv = getOrCreateBrand("PURE EV", "India");
        seedModelIfAbsent(pureEv, "ETrance Neo", "Scooter", "Electric", 2020, 2024);
        seedModelIfAbsent(pureEv, "EPluto 7G", "Scooter", "Electric", 2020, 2024);
        seedModelIfAbsent(pureEv, "EcoRider", "Scooter", "Electric", 2020, 2024);
        seedModelIfAbsent(pureEv, "eTryst 350", "Roadster", "Electric", 2021, 2024);

        // ── BOUNCE ───────────────────────────────────────────────────────────────
        Manufacturer bounce = getOrCreateBrand("BOUNCE", "India");
        seedModelIfAbsent(bounce, "Infinity E1", "Scooter", "Electric", 2022, 2024);

        // ── AMPERE ───────────────────────────────────────────────────────────────
        Manufacturer ampere = getOrCreateBrand("AMPERE", "India");
        seedModelIfAbsent(ampere, "Magnus EX", "Scooter", "Electric", 2020, 2024);
        seedModelIfAbsent(ampere, "Nexus", "Scooter", "Electric", 2023, 2024);
        seedModelIfAbsent(ampere, "Primus", "Scooter", "Electric", 2022, 2024);
        seedModelIfAbsent(ampere, "Zeal EX", "Scooter", "Electric", 2020, 2024);

        // ── GREAVES ELECTRIC ─────────────────────────────────────────────────────
        Manufacturer greaves = getOrCreateBrand("GREAVES ELECTRIC", "India");
        seedModelIfAbsent(greaves, "Ampere Magnus", "Scooter", "Electric", 2019, 2024);

        // ── PIAGGIO ──────────────────────────────────────────────────────────────
        Manufacturer piaggio = getOrCreateBrand("PIAGGIO", "Italy");
        seedModelIfAbsent(piaggio, "Medley 150", "Scooter", "Petrol", 2016, 2020);
        seedModelIfAbsent(piaggio, "Liberty 125", "Scooter", "Petrol", 2016, 2020);

        // ── MAHINDRA ─────────────────────────────────────────────────────────────
        Manufacturer mahindra = getOrCreateBrand("MAHINDRA", "India");
        seedModelIfAbsent(mahindra, "Mojo 300", "Touring", "Petrol", 2015, 2022);
        seedModelIfAbsent(mahindra, "Gusto 110", "Scooter", "Petrol", 2014, 2019);
        seedModelIfAbsent(mahindra, "Duro DZ", "Scooter", "Petrol", 2010, 2017);
        seedModelIfAbsent(mahindra, "Rodeo RZ", "Scooter", "Petrol", 2009, 2017);
        seedModelIfAbsent(mahindra, "Stallio", "Commuter", "Petrol", 2010, 2015);

        // ── INDIAN MOTORCYCLE ────────────────────────────────────────────────────
        Manufacturer indian = getOrCreateBrand("INDIAN MOTORCYCLE", "USA");
        seedModelIfAbsent(indian, "Scout", "Cruiser", "Petrol", 2015, 2024);
        seedModelIfAbsent(indian, "Scout Bobber", "Bobber", "Petrol", 2018, 2024);
        seedModelIfAbsent(indian, "Chief", "Cruiser", "Petrol", 2016, 2024);
        seedModelIfAbsent(indian, "Chieftain", "Touring", "Petrol", 2016, 2024);
        seedModelIfAbsent(indian, "Roadmaster", "Touring", "Petrol", 2016, 2024);
        seedModelIfAbsent(indian, "FTR 1200", "Roadster", "Petrol", 2019, 2024);
        seedModelIfAbsent(indian, "Pursuit", "Touring", "Petrol", 2022, 2024);
        seedModelIfAbsent(indian, "Challenger", "Touring", "Petrol", 2020, 2024);
        seedModelIfAbsent(indian, "Sport Chief", "Cruiser", "Petrol", 2022, 2024);

        // ── MOTO GUZZI ───────────────────────────────────────────────────────────
        Manufacturer motoGuzzi = getOrCreateBrand("MOTO GUZZI", "Italy");
        seedModelIfAbsent(motoGuzzi, "V7", "Retro", "Petrol", 2016, 2024);
        seedModelIfAbsent(motoGuzzi, "V9 Bobber", "Bobber", "Petrol", 2016, 2024);
        seedModelIfAbsent(motoGuzzi, "V85 TT", "Adventure", "Petrol", 2019, 2024);
        seedModelIfAbsent(motoGuzzi, "V100 Mandello", "Sport Touring", "Petrol", 2022, 2024);

        // ── NORTON ───────────────────────────────────────────────────────────────
        Manufacturer norton = getOrCreateBrand("NORTON", "UK");
        seedModelIfAbsent(norton, "V4CR", "Cafe Racer", "Petrol", 2023, 2024);
        seedModelIfAbsent(norton, "Commando 961", "Retro", "Petrol", 2016, 2022);

        // ── HUSQVARNA ────────────────────────────────────────────────────────────
        Manufacturer husqvarna = getOrCreateBrand("HUSQVARNA", "Sweden");
        seedModelIfAbsent(husqvarna, "Svartpilen 250", "Scrambler", "Petrol", 2021, 2024);
        seedModelIfAbsent(husqvarna, "Vitpilen 250", "Cafe Racer", "Petrol", 2021, 2024);
        seedModelIfAbsent(husqvarna, "Svartpilen 401", "Scrambler", "Petrol", 2019, 2024);
        seedModelIfAbsent(husqvarna, "Vitpilen 401", "Cafe Racer", "Petrol", 2019, 2024);

        // ── CFMOTO ───────────────────────────────────────────────────────────────
        Manufacturer cfmoto = getOrCreateBrand("CFMOTO", "China");
        seedModelIfAbsent(cfmoto, "300NK", "Roadster", "Petrol", 2021, 2024);
        seedModelIfAbsent(cfmoto, "650NK", "Roadster", "Petrol", 2019, 2024);
        seedModelIfAbsent(cfmoto, "650MT", "Adventure", "Petrol", 2019, 2024);
        seedModelIfAbsent(cfmoto, "300SR", "Sports", "Petrol", 2021, 2024);
        seedModelIfAbsent(cfmoto, "400NK", "Roadster", "Petrol", 2022, 2024);
        seedModelIfAbsent(cfmoto, "800MT", "Adventure", "Petrol", 2022, 2024);

        // ── ZONTES ───────────────────────────────────────────────────────────────
        Manufacturer zontes = getOrCreateBrand("ZONTES", "China");
        seedModelIfAbsent(zontes, "350R", "Sports", "Petrol", 2022, 2024);
        seedModelIfAbsent(zontes, "350T", "Adventure", "Petrol", 2022, 2024);
        seedModelIfAbsent(zontes, "350X", "Crossover", "Petrol", 2022, 2024);

        // ── KEEWAY ───────────────────────────────────────────────────────────────
        Manufacturer keeway = getOrCreateBrand("KEEWAY", "China");
        seedModelIfAbsent(keeway, "SR125", "Scooter", "Petrol", 2021, 2024);
        seedModelIfAbsent(keeway, "V302C", "Cruiser", "Petrol", 2022, 2024);
        seedModelIfAbsent(keeway, "K-Light 202", "Cruiser", "Petrol", 2022, 2024);

        // ── ADDITIONAL ROYAL ENFIELD MODELS ──────────────────────────────────────
        seedModelIfAbsent(royalEnfield, "Classic 350 Reborn", "Cruiser", "Petrol", 2021, 2024);
        seedModelIfAbsent(royalEnfield, "Bullet 350 Standard", "Cruiser", "Petrol", 2001, 2024);
        seedModelIfAbsent(royalEnfield, "Classic 500 Pegasus", "Cruiser", "Petrol", 2018, 2020);
        seedModelIfAbsent(royalEnfield, "Himalayan 411", "Adventure", "Petrol", 2016, 2022);
        seedModelIfAbsent(royalEnfield, "Interceptor 650 Chrome", "Cruiser", "Petrol", 2020, 2024);
        seedModelIfAbsent(royalEnfield, "Continental GT 535", "Cafe Racer", "Petrol", 2013, 2017);
        seedModelIfAbsent(royalEnfield, "Thunderbird X 350", "Touring", "Petrol", 2018, 2019);
        seedModelIfAbsent(royalEnfield, "Thunderbird X 500", "Touring", "Petrol", 2018, 2019);

        // ── ADDITIONAL HERO MODELS ────────────────────────────────────────────────
        seedModelIfAbsent(hero, "Passion Pro BSVI", "Commuter", "Petrol", 2020, 2024);
        seedModelIfAbsent(hero, "Glamour 125 FI", "Commuter", "Petrol", 2020, 2024);
        seedModelIfAbsent(hero, "Super Splendor 125 BSVI", "Commuter", "Petrol", 2020, 2024);
        seedModelIfAbsent(hero, "HF Deluxe i3S", "Commuter", "Petrol", 2019, 2024);
        seedModelIfAbsent(hero, "Destini 125 Platinum", "Scooter", "Petrol", 2021, 2024);
        seedModelIfAbsent(hero, "Maestro Edge 125 Connected", "Scooter", "Petrol", 2021, 2024);
        seedModelIfAbsent(hero, "Xpulse 200 Rally Kit", "Adventure", "Petrol", 2022, 2024);
        seedModelIfAbsent(hero, "Xtreme 160R Stealth 2.0", "Sports", "Petrol", 2023, 2024);
        seedModelIfAbsent(hero, "Mavrick 440 S", "Roadster", "Petrol", 2024, 2024);
        seedModelIfAbsent(hero, "Karizma XMR 210", "Sports", "Petrol", 2024, 2024);
        seedModelIfAbsent(hero, "Surge S32 (EV)", "Scooter", "Electric", 2024, 2024);

        // ── ADDITIONAL HONDA MODELS ───────────────────────────────────────────────
        seedModelIfAbsent(honda, "CB150R", "Roadster", "Petrol", 2019, 2023);
        seedModelIfAbsent(honda, "CB200X", "Adventure", "Petrol", 2021, 2024);
        seedModelIfAbsent(honda, "CB750 Hornet", "Roadster", "Petrol", 2023, 2024);
        seedModelIfAbsent(honda, "CBR500R", "Sports", "Petrol", 2019, 2024);
        seedModelIfAbsent(honda, "CRF300L", "Adventure", "Petrol", 2021, 2024);
        seedModelIfAbsent(honda, "CRF1100L Africa Twin", "Adventure", "Petrol", 2020, 2024);
        seedModelIfAbsent(honda, "CB1000R", "Roadster", "Petrol", 2019, 2024);
        seedModelIfAbsent(honda, "CBR1000RR Fireblade", "Superbike", "Petrol", 2004, 2019);
        seedModelIfAbsent(honda, "Activa 3G", "Scooter", "Petrol", 2015, 2018);
        seedModelIfAbsent(honda, "Activa 4G", "Scooter", "Petrol", 2017, 2019);
        seedModelIfAbsent(honda, "Activa 5G", "Scooter", "Petrol", 2018, 2020);
        seedModelIfAbsent(honda, "Unicorn 160", "Commuter", "Petrol", 2015, 2024);
        seedModelIfAbsent(honda, "CB Trigger", "Commuter", "Petrol", 2012, 2018);
        seedModelIfAbsent(honda, "CB Dazzler", "Commuter", "Petrol", 2010, 2015);

        // ── ADDITIONAL TVS MODELS ─────────────────────────────────────────────────
        seedModelIfAbsent(tvs, "Apache RTR 160 Race Edition", "Sports", "Petrol", 2019, 2024);
        seedModelIfAbsent(tvs, "Apache RTR 200 ABS", "Sports", "Petrol", 2017, 2024);
        seedModelIfAbsent(tvs, "NTORQ 125 Super Squad Edition", "Scooter", "Petrol", 2021, 2024);
        seedModelIfAbsent(tvs, "iQube E-Scooter 2.2 kWh", "Scooter", "Electric", 2023, 2024);
        seedModelIfAbsent(tvs, "iQube E-Scooter 3.4 kWh", "Scooter", "Electric", 2023, 2024);
        seedModelIfAbsent(tvs, "Raider 125 Drum Alloy", "Commuter", "Petrol", 2022, 2024);
        seedModelIfAbsent(tvs, "Ronin 225", "Roadster", "Petrol", 2023, 2024);
        seedModelIfAbsent(tvs, "Sport ES", "Commuter", "Petrol", 2016, 2024);
        seedModelIfAbsent(tvs, "Scooty Pep+ BSVI", "Scooter", "Petrol", 2020, 2024);
        seedModelIfAbsent(tvs, "Jupiter ZX Disc", "Scooter", "Petrol", 2020, 2024);
        seedModelIfAbsent(tvs, "Apache RR 310 BTO", "Superbike", "Petrol", 2022, 2024);

        // ── ADDITIONAL BAJAJ MODELS ───────────────────────────────────────────────
        seedModelIfAbsent(bajaj, "Pulsar 150 Neon ABS", "Sports", "Petrol", 2020, 2024);
        seedModelIfAbsent(bajaj, "Pulsar NS 125", "Sports", "Petrol", 2021, 2024);
        seedModelIfAbsent(bajaj, "Pulsar 250F", "Sports", "Petrol", 2022, 2024);
        seedModelIfAbsent(bajaj, "CT 110X", "Commuter", "Petrol", 2020, 2024);
        seedModelIfAbsent(bajaj, "Discover 125 ST", "Commuter", "Petrol", 2012, 2018);
        seedModelIfAbsent(bajaj, "Discover 100M", "Commuter", "Petrol", 2013, 2018);
        seedModelIfAbsent(bajaj, "Chetak Electric Sport", "Scooter", "Electric", 2023, 2024);
        seedModelIfAbsent(bajaj, "Avenger 160 Street BS6", "Cruiser", "Petrol", 2020, 2024);
        seedModelIfAbsent(bajaj, "Dominar 400 Touring Edition", "Touring", "Petrol", 2019, 2024);
        seedModelIfAbsent(bajaj, "Platina 110 ES Disc", "Commuter", "Petrol", 2020, 2024);
        seedModelIfAbsent(bajaj, "Freedom 125 CNG Twin Tank", "Commuter", "CNG", 2024, 2024);

        // ── ADDITIONAL YAMAHA MODELS ──────────────────────────────────────────────
        seedModelIfAbsent(yamaha, "FZ-S FI Version 2", "Roadster", "Petrol", 2016, 2019);
        seedModelIfAbsent(yamaha, "FZ-S FI Version 3", "Roadster", "Petrol", 2019, 2021);
        seedModelIfAbsent(yamaha, "R15 Version 2", "Sports", "Petrol", 2012, 2017);
        seedModelIfAbsent(yamaha, "MT-15 Version 1", "Roadster", "Petrol", 2019, 2021);
        seedModelIfAbsent(yamaha, "Fascino 110", "Scooter", "Petrol", 2015, 2019);
        seedModelIfAbsent(yamaha, "Alpha 113", "Scooter", "Petrol", 2014, 2019);
        seedModelIfAbsent(yamaha, "Ray ZR 125 Hybrid", "Scooter", "Petrol", 2021, 2024);
        seedModelIfAbsent(yamaha, "R15M WorldGP 60th Anniversary", "Sports", "Petrol", 2022, 2023);
        seedModelIfAbsent(yamaha, "MT-09 SP", "Roadster", "Petrol", 2021, 2024);
        seedModelIfAbsent(yamaha, "TMAX 560", "Scooter", "Petrol", 2020, 2024);
        seedModelIfAbsent(yamaha, "NMAX 155", "Scooter", "Petrol", 2021, 2024);

        // ── ADDITIONAL SUZUKI MODELS ──────────────────────────────────────────────
        seedModelIfAbsent(suzuki, "Gixxer SF 150 MotoGP Edition", "Sports", "Petrol", 2019, 2022);
        seedModelIfAbsent(suzuki, "Access 125 Special Edition CBS", "Scooter", "Petrol", 2020, 2024);
        seedModelIfAbsent(suzuki, "Burgman Street 125 EX", "Scooter", "Petrol", 2021, 2024);
        seedModelIfAbsent(suzuki, "V-Strom 1050 XT", "Adventure", "Petrol", 2021, 2024);
        seedModelIfAbsent(suzuki, "GSX-S950", "Roadster", "Petrol", 2021, 2024);
        seedModelIfAbsent(suzuki, "Katana", "Roadster", "Petrol", 2019, 2024);
        seedModelIfAbsent(suzuki, "SV650", "Roadster", "Petrol", 2016, 2024);
        seedModelIfAbsent(suzuki, "GSX-R600", "Superbike", "Petrol", 2001, 2018);
        seedModelIfAbsent(suzuki, "GSX-R750", "Superbike", "Petrol", 2001, 2018);

        // ── ADDITIONAL KTM MODELS ─────────────────────────────────────────────────
        seedModelIfAbsent(ktm, "Duke 390 MY2023", "Roadster", "Petrol", 2023, 2024);
        seedModelIfAbsent(ktm, "RC 125 ABS", "Sports", "Petrol", 2021, 2024);
        seedModelIfAbsent(ktm, "390 Adventure S", "Adventure", "Petrol", 2024, 2024);
        seedModelIfAbsent(ktm, "1290 Super Duke GT", "Sport Touring", "Petrol", 2016, 2024);
        seedModelIfAbsent(ktm, "250 Duke SE", "Roadster", "Petrol", 2022, 2024);

        // ── ADDITIONAL KAWASAKI MODELS ────────────────────────────────────────────
        seedModelIfAbsent(kawasaki, "Z650RS", "Retro Roadster", "Petrol", 2022, 2024);
        seedModelIfAbsent(kawasaki, "Z900RS Cafe", "Cafe Racer", "Petrol", 2018, 2024);
        seedModelIfAbsent(kawasaki, "Ninja 250SL", "Sports", "Petrol", 2015, 2018);
        seedModelIfAbsent(kawasaki, "Ninja ZX-4RR", "Superbike", "Petrol", 2023, 2024);
        seedModelIfAbsent(kawasaki, "Ninja ZX-25R", "Superbike", "Petrol", 2020, 2024);
        seedModelIfAbsent(kawasaki, "Ninja H2 Carbon", "Hyperbike", "Petrol", 2016, 2024);
        seedModelIfAbsent(kawasaki, "W800", "Retro", "Petrol", 2019, 2024);
        seedModelIfAbsent(kawasaki, "Eliminator 500", "Cruiser", "Petrol", 2023, 2024);

        // ── ADDITIONAL TRIUMPH MODELS ─────────────────────────────────────────────
        seedModelIfAbsent(triumph, "Street Twin", "Retro", "Petrol", 2016, 2024);
        seedModelIfAbsent(triumph, "Street Scrambler", "Scrambler", "Petrol", 2016, 2024);
        seedModelIfAbsent(triumph, "Tiger 660 Sport", "Adventure", "Petrol", 2022, 2024);
        seedModelIfAbsent(triumph, "Bonneville Bobber", "Bobber", "Petrol", 2017, 2024);
        seedModelIfAbsent(triumph, "Bonneville Speedmaster", "Cruiser", "Petrol", 2018, 2024);
        seedModelIfAbsent(triumph, "Thruxton 1200", "Cafe Racer", "Petrol", 2016, 2024);
        seedModelIfAbsent(triumph, "Speed Triple 1200 RS", "Roadster", "Petrol", 2021, 2024);
        seedModelIfAbsent(triumph, "Daytona Moto2 765", "Sports", "Petrol", 2020, 2021);

        // ── ADDITIONAL BMW MOTORRAD MODELS ────────────────────────────────────────
        seedModelIfAbsent(bmw, "F 750 GS", "Adventure", "Petrol", 2018, 2024);
        seedModelIfAbsent(bmw, "R 1250 RT", "Sport Touring", "Petrol", 2019, 2024);
        seedModelIfAbsent(bmw, "R nineT", "Retro", "Petrol", 2014, 2024);
        seedModelIfAbsent(bmw, "R nineT Scrambler", "Scrambler", "Petrol", 2016, 2024);
        seedModelIfAbsent(bmw, "K 1600 GTL", "Touring", "Petrol", 2016, 2024);
        seedModelIfAbsent(bmw, "M 1000 R", "Roadster", "Petrol", 2023, 2024);

        // ── ADDITIONAL DUCATI MODELS ──────────────────────────────────────────────
        seedModelIfAbsent(ducati, "Scrambler Icon Dark", "Scrambler", "Petrol", 2020, 2024);
        seedModelIfAbsent(ducati, "Scrambler Full Throttle", "Scrambler", "Petrol", 2016, 2024);
        seedModelIfAbsent(ducati, "Monster 821", "Roadster", "Petrol", 2016, 2020);
        seedModelIfAbsent(ducati, "Monster 1200", "Roadster", "Petrol", 2016, 2020);
        seedModelIfAbsent(ducati, "Hyperstrada 821", "Adventure", "Petrol", 2016, 2018);
        seedModelIfAbsent(ducati, "XDiavel", "Power Cruiser", "Petrol", 2016, 2024);
        seedModelIfAbsent(ducati, "Supersport 950", "Sports", "Petrol", 2021, 2024);

        // ── ADDITIONAL HARLEY-DAVIDSON MODELS ────────────────────────────────────
        seedModelIfAbsent(harley, "Street Rod 750", "Cruiser", "Petrol", 2017, 2020);
        seedModelIfAbsent(harley, "Forty-Eight Special", "Cruiser", "Petrol", 2018, 2020);
        seedModelIfAbsent(harley, "Low Rider S", "Cruiser", "Petrol", 2020, 2024);
        seedModelIfAbsent(harley, "Pan America 1250", "Adventure", "Petrol", 2021, 2024);
        seedModelIfAbsent(harley, "Nightster Special", "Cruiser", "Petrol", 2023, 2024);
        seedModelIfAbsent(harley, "CVO Limited", "Touring", "Petrol", 2016, 2024);

        // ── ADDITIONAL APRILIA MODELS ─────────────────────────────────────────────
        seedModelIfAbsent(aprilia, "RS 660 Extrema", "Sports", "Petrol", 2022, 2024);
        seedModelIfAbsent(aprilia, "Tuono 660 Factory", "Roadster", "Petrol", 2022, 2024);
        seedModelIfAbsent(aprilia, "SR GT 125", "Scooter", "Petrol", 2022, 2024);
        seedModelIfAbsent(aprilia, "SR GT 200", "Scooter", "Petrol", 2022, 2024);
        seedModelIfAbsent(aprilia, "Dorsoduro 900", "Supermoto", "Petrol", 2016, 2020);
        seedModelIfAbsent(aprilia, "Caponord 1200", "Adventure", "Petrol", 2016, 2019);

        // ── ADDITIONAL EV MODELS ──────────────────────────────────────────────────
        Manufacturer simple = getOrCreateBrand("SIMPLE ENERGY", "India");
        seedModelIfAbsent(simple, "Simple One", "Scooter", "Electric", 2022, 2024);
        seedModelIfAbsent(simple, "Simple Dot", "Scooter", "Electric", 2024, 2024);

        Manufacturer bgauss = getOrCreateBrand("BGAUSS", "India");
        seedModelIfAbsent(bgauss, "B8", "Scooter", "Electric", 2020, 2024);
        seedModelIfAbsent(bgauss, "C12i", "Scooter", "Electric", 2022, 2024);
        seedModelIfAbsent(bgauss, "RUV350", "Scooter", "Electric", 2023, 2024);

        Manufacturer evtric = getOrCreateBrand("EVTRIC", "India");
        seedModelIfAbsent(evtric, "Ride", "Scooter", "Electric", 2021, 2024);
        seedModelIfAbsent(evtric, "Axis", "Scooter", "Electric", 2021, 2024);
        seedModelIfAbsent(evtric, "Rise", "Scooter", "Electric", 2022, 2024);

        Manufacturer okinawa = getOrCreateBrand("OKINAWA", "India");
        seedModelIfAbsent(okinawa, "Praise Pro", "Scooter", "Electric", 2019, 2024);
        seedModelIfAbsent(okinawa, "R30", "Scooter", "Electric", 2019, 2024);
        seedModelIfAbsent(okinawa, "iPraise+", "Scooter", "Electric", 2020, 2024);
        seedModelIfAbsent(okinawa, "Okhi 90", "Scooter", "Electric", 2021, 2024);
        seedModelIfAbsent(okinawa, "Ridge Plus", "Scooter", "Electric", 2020, 2024);

        Manufacturer hero2 = getOrCreateBrand("HERO ELECTRIC", "India");
        seedModelIfAbsent(hero2, "Optima CX", "Scooter", "Electric", 2019, 2024);
        seedModelIfAbsent(hero2, "Flash", "Scooter", "Electric", 2019, 2024);
        seedModelIfAbsent(hero2, "Nyx HX", "Scooter", "Electric", 2020, 2024);
        seedModelIfAbsent(hero2, "AE-29", "Scooter", "Electric", 2021, 2024);
        seedModelIfAbsent(hero2, "Eddy", "Scooter", "Electric", 2021, 2024);

        Manufacturer lectrix = getOrCreateBrand("LECTRIX", "India");
        seedModelIfAbsent(lectrix, "LXS G2.0", "Scooter", "Electric", 2021, 2024);
        seedModelIfAbsent(lectrix, "EV One", "Scooter", "Electric", 2022, 2024);

        Manufacturer kabira = getOrCreateBrand("KABIRA MOBILITY", "India");
        seedModelIfAbsent(kabira, "KM3000", "Roadster", "Electric", 2021, 2024);
        seedModelIfAbsent(kabira, "KM4000", "Roadster", "Electric", 2021, 2024);
        seedModelIfAbsent(kabira, "Intercity Neo", "Scooter", "Electric", 2022, 2024);

        Manufacturer matter = getOrCreateBrand("MATTER", "India");
        seedModelIfAbsent(matter, "AERA 5000", "Roadster", "Electric", 2023, 2024);
        seedModelIfAbsent(matter, "AERA 5000+", "Roadster", "Electric", 2023, 2024);

        // ── ADDITIONAL SCOOTER BRANDS ─────────────────────────────────────────────
        Manufacturer kymco = getOrCreateBrand("KYMCO", "Taiwan");
        seedModelIfAbsent(kymco, "Like 150i", "Scooter", "Petrol", 2016, 2022);
        seedModelIfAbsent(kymco, "Agility 125", "Scooter", "Petrol", 2016, 2022);
        seedModelIfAbsent(kymco, "Downtown 350i", "Scooter", "Petrol", 2016, 2022);

        Manufacturer sym = getOrCreateBrand("SYM", "Taiwan");
        seedModelIfAbsent(sym, "Joyride 200", "Scooter", "Petrol", 2016, 2021);
        seedModelIfAbsent(sym, "Wolf Classic 150", "Roadster", "Petrol", 2016, 2021);

        // ── ADDITIONAL SPORTY & ADVENTURE MODELS ─────────────────────────────────
        Manufacturer royalEnfield2 = getOrCreateBrand("ROYAL ENFIELD", "India");
        seedModelIfAbsent(royalEnfield2, "Guerrilla 450 Apex", "Roadster", "Petrol", 2024, 2024);
        seedModelIfAbsent(royalEnfield2, "Classic 350 Halcyon", "Cruiser", "Petrol", 2021, 2024);
        seedModelIfAbsent(royalEnfield2, "Meteor 350 Supernova", "Cruiser", "Petrol", 2021, 2024);

        Manufacturer tvsMore = getOrCreateBrand("TVS", "India");
        seedModelIfAbsent(tvsMore, "Apache RTR 165RP", "Sports", "Petrol", 2022, 2024);
        seedModelIfAbsent(tvsMore, "Creon (EV Concept)", "Scooter", "Electric", 2024, 2024);
        seedModelIfAbsent(tvsMore, "NTORQ 125 XT", "Scooter", "Petrol", 2022, 2024);

        Manufacturer heroMore = getOrCreateBrand("HERO", "India");
        seedModelIfAbsent(heroMore, "Xoom 110 E5", "Scooter", "Petrol", 2023, 2024);
        seedModelIfAbsent(heroMore, "Pleasure+ XTEC BS6", "Scooter", "Petrol", 2022, 2024);
        seedModelIfAbsent(heroMore, "Splendor+ Black and Accent", "Commuter", "Petrol", 2022, 2024);

        System.out.println("[DataSeeder] Bike model seeding complete.");
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

    private Manufacturer getOrCreateBrand(String name, String country) {
        return manufacturerRepository.findByName(name).orElseGet(() -> {
            long id = sequenceGenerator.generateSequence("manufacturers");
            Manufacturer m = Manufacturer.builder().id(id).name(name).country(country).active(true).build();
            return manufacturerRepository.save(m);
        });
    }

    private void seedModelIfAbsent(Manufacturer m, String modelName, String category, String fuelType, int startYear, int endYear) {
        boolean exists = bikeModelRepository.findByManufacturerIdAndModelName(m.getId(), modelName).isPresent();
        if (exists) return;

        long modelId = sequenceGenerator.generateSequence("bike_models");
        BikeModel model = BikeModel.builder().id(modelId).manufacturer(m).modelName(modelName)
                .category(category).fuelType(fuelType).active(true).build();
        model = bikeModelRepository.save(model);

        for (int y = startYear; y <= endYear; y++) {
            long yrId = sequenceGenerator.generateSequence("manufacturing_years");
            yearRepository.save(ManufacturingYear.builder().id(yrId).bikeModel(model).year(y).active(true).build());
        }

        long varId = sequenceGenerator.generateSequence("bike_variants");
        bikeVariantRepository.save(BikeVariant.builder().id(varId).bikeModel(model).variantName("Standard").active(true).build());
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
