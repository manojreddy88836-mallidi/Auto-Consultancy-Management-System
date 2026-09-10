package com.autoconsultancy;

import com.autoconsultancy.entity.*;
import com.autoconsultancy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final BikeModelRepository bikeModelRepository;
    private final BikeVariantRepository bikeVariantRepository;
    private final ManufacturingYearRepository yearRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedData() {
        // Always re-activate the built-in admin/worker accounts on startup.
        // This ensures they can log in even if they were accidentally deactivated.
        ensureDefaultUsersActive();

        if (userRepository.count() > 0) {
            return;
        }

        // 1. Create admin
        User admin = User.builder()
                .email("admin@autoconsultancy.com")
                .password(passwordEncoder.encode("Admin@123"))
                .firstName("Super")
                .lastName("Admin")
                .role(Role.ADMIN)
                .active(true)
                .build();
        userRepository.save(admin);

        // 2. Create 3 workers
        seedWorker("worker1@autoconsultancy.com", "Worker@123", "Worker", "One", "EMP1001", "Operations", "Senior Consultant");
        seedWorker("worker2@autoconsultancy.com", "Worker@123", "Worker", "Two", "EMP1002", "Finance", "Finance Analyst");
        seedWorker("worker3@autoconsultancy.com", "Worker@123", "Worker", "Three", "EMP1003", "Verification", "Document Specialist");

        // 3. Seed 61 Brands (stored internally as Manufacturer entities, displayed as Brands)

        // ── Indian Brands ──
        Manufacturer royalEnfield = seedBrand("ROYAL ENFIELD", "India");
        seedModel(royalEnfield, "Classic 350", "Cruiser", "Petrol", 2010, 2024);
        seedModel(royalEnfield, "Bullet 350", "Cruiser", "Petrol", 2010, 2024);
        seedModel(royalEnfield, "Meteor 350", "Cruiser", "Petrol", 2020, 2024);
        seedModel(royalEnfield, "Himalayan", "Adventure", "Petrol", 2016, 2024);
        seedModel(royalEnfield, "Interceptor 650", "Cruiser", "Petrol", 2019, 2024);
        seedModel(royalEnfield, "Continental GT 650", "Cruiser", "Petrol", 2019, 2024);
        seedModel(royalEnfield, "Hunter 350", "Naked", "Petrol", 2022, 2024);
        seedModel(royalEnfield, "Super Meteor 650", "Cruiser", "Petrol", 2023, 2024);

        Manufacturer tvs = seedBrand("TVS", "India");
        seedModel(tvs, "Apache RTR 160", "Sports", "Petrol", 2010, 2024);
        seedModel(tvs, "Apache RTR 200", "Sports", "Petrol", 2015, 2024);
        seedModel(tvs, "Apache RR 310", "Sports", "Petrol", 2018, 2024);
        seedModel(tvs, "Jupiter", "Scooter", "Petrol", 2013, 2024);
        seedModel(tvs, "NTORQ 125", "Scooter", "Petrol", 2018, 2024);
        seedModel(tvs, "Ronin", "Naked", "Petrol", 2022, 2024);
        seedModel(tvs, "iQube", "Scooter", "Electric", 2020, 2024);

        Manufacturer bajaj = seedBrand("BAJAJ", "India");
        seedModel(bajaj, "Pulsar 150", "Sports", "Petrol", 2010, 2024);
        seedModel(bajaj, "Pulsar 200NS", "Sports", "Petrol", 2012, 2024);
        seedModel(bajaj, "Pulsar RS200", "Sports", "Petrol", 2015, 2024);
        seedModel(bajaj, "Pulsar N250", "Sports", "Petrol", 2021, 2024);
        seedModel(bajaj, "Dominar 400", "Touring", "Petrol", 2017, 2024);
        seedModel(bajaj, "CT 100", "Commuter", "Petrol", 2010, 2024);
        seedModel(bajaj, "Platina", "Commuter", "Petrol", 2010, 2024);
        seedModel(bajaj, "Avenger 220", "Cruiser", "Petrol", 2010, 2024);

        Manufacturer hero = seedBrand("HERO", "India");
        seedModel(hero, "Splendor Plus", "Commuter", "Petrol", 2012, 2024);
        seedModel(hero, "HF Deluxe", "Commuter", "Petrol", 2010, 2024);
        seedModel(hero, "Passion Pro", "Commuter", "Petrol", 2010, 2024);
        seedModel(hero, "Glamour", "Commuter", "Petrol", 2012, 2024);
        seedModel(hero, "Xtreme 160R", "Sports", "Petrol", 2020, 2024);
        seedModel(hero, "Xpulse 200", "Adventure", "Petrol", 2019, 2024);
        seedModel(hero, "Destini 125", "Scooter", "Petrol", 2018, 2024);

        // ── Japanese Brands ──
        Manufacturer honda = seedBrand("HONDA", "Japan");
        seedModel(honda, "Activa 6G", "Scooter", "Petrol", 2019, 2024);
        seedModel(honda, "Activa 125", "Scooter", "Petrol", 2018, 2024);
        seedModel(honda, "Shine", "Commuter", "Petrol", 2011, 2024);
        seedModel(honda, "CB350", "Cruiser", "Petrol", 2021, 2024);
        seedModel(honda, "CB300R", "Naked", "Petrol", 2019, 2024);

        Manufacturer yamaha = seedBrand("YAMAHA", "Japan");
        seedModel(yamaha, "FZ-S V3", "Naked", "Petrol", 2019, 2024);
        seedModel(yamaha, "FZ 25", "Naked", "Petrol", 2017, 2024);
        seedModel(yamaha, "MT-15", "Naked", "Petrol", 2019, 2024);
        seedModel(yamaha, "R15 V4", "Sports", "Petrol", 2021, 2024);
        seedModel(yamaha, "Fascino 125", "Scooter", "Petrol", 2020, 2024);
        seedModel(yamaha, "Aerox 155", "Scooter", "Petrol", 2021, 2024);

        Manufacturer suzuki = seedBrand("SUZUKI", "Japan");
        seedModel(suzuki, "Access 125", "Scooter", "Petrol", 2016, 2024);
        seedModel(suzuki, "Burgman Street", "Scooter", "Petrol", 2018, 2024);
        seedModel(suzuki, "Gixxer 250", "Sports", "Petrol", 2019, 2024);
        seedModel(suzuki, "Gixxer SF 250", "Sports", "Petrol", 2019, 2024);

        Manufacturer kawasaki = seedBrand("KAWASAKI", "Japan");
        seedModel(kawasaki, "Ninja 300", "Sports", "Petrol", 2013, 2024);
        seedModel(kawasaki, "Ninja 400", "Sports", "Petrol", 2018, 2024);
        seedModel(kawasaki, "Z650", "Naked", "Petrol", 2017, 2024);
        seedModel(kawasaki, "Versys 650", "Adventure", "Petrol", 2015, 2024);

        // ── Austrian Brand ──
        Manufacturer ktm = seedBrand("KTM", "Austria");
        seedModel(ktm, "Duke 125", "Naked", "Petrol", 2019, 2024);
        seedModel(ktm, "Duke 200", "Naked", "Petrol", 2012, 2024);
        seedModel(ktm, "Duke 390", "Naked", "Petrol", 2013, 2024);
        seedModel(ktm, "RC 390", "Sports", "Petrol", 2014, 2024);
        seedModel(ktm, "Adventure 390", "Adventure", "Petrol", 2019, 2024);

        // ── UK Brands ──
        Manufacturer triumph = seedBrand("TRIUMPH", "United Kingdom");
        seedModel(triumph, "Scrambler 400X", "Adventure", "Petrol", 2023, 2024);
        seedModel(triumph, "Speed 400", "Naked", "Petrol", 2023, 2024);
        seedModel(triumph, "Bonneville T100", "Cruiser", "Petrol", 2016, 2024);
        seedModel(triumph, "Tiger Sport 660", "Adventure", "Petrol", 2022, 2024);

        Manufacturer norton = seedBrand("NORTON", "United Kingdom");
        seedModel(norton, "V4SV", "Sports", "Petrol", 2023, 2024);
        seedModel(norton, "Commando 961", "Cruiser", "Petrol", 2019, 2024);

        Manufacturer bsa = seedBrand("BSA", "United Kingdom");
        seedModel(bsa, "Gold Star 650", "Cruiser", "Petrol", 2023, 2024);

        Manufacturer brixton = seedBrand("BRIXTON MOTORCYCLES", "United Kingdom");
        seedModel(brixton, "Cromwell 1200", "Cruiser", "Petrol", 2022, 2024);
        seedModel(brixton, "Sunray 125", "Naked", "Petrol", 2022, 2024);

        // ── US Brands ──
        Manufacturer harley = seedBrand("HARLEY-DAVIDSON", "United States");
        seedModel(harley, "X440", "Cruiser", "Petrol", 2023, 2024);
        seedModel(harley, "Iron 883", "Cruiser", "Petrol", 2015, 2024);
        seedModel(harley, "Sportster S", "Cruiser", "Petrol", 2022, 2024);
        seedModel(harley, "Fat Bob", "Cruiser", "Petrol", 2018, 2024);

        Manufacturer indian = seedBrand("INDIAN", "United States");
        seedModel(indian, "Scout 101", "Cruiser", "Petrol", 2024, 2024);
        seedModel(indian, "Chief", "Cruiser", "Petrol", 2022, 2024);
        seedModel(indian, "FTR 1200", "Naked", "Petrol", 2019, 2024);

        // ── German Brand ──
        Manufacturer bmw = seedBrand("BMW", "Germany");
        seedModel(bmw, "G 310 R", "Naked", "Petrol", 2018, 2024);
        seedModel(bmw, "G 310 GS", "Adventure", "Petrol", 2018, 2024);
        seedModel(bmw, "F 900 R", "Naked", "Petrol", 2020, 2024);

        // ── Italian Brands ──
        Manufacturer ducati = seedBrand("DUCATI", "Italy");
        seedModel(ducati, "Monster", "Naked", "Petrol", 2021, 2024);
        seedModel(ducati, "Panigale V2", "Sports", "Petrol", 2020, 2024);
        seedModel(ducati, "Scrambler", "Adventure", "Petrol", 2015, 2024);

        Manufacturer motoGuzzi = seedBrand("MOTO GUZZI", "Italy");
        seedModel(motoGuzzi, "V7", "Cruiser", "Petrol", 2021, 2024);
        seedModel(motoGuzzi, "V85 TT", "Adventure", "Petrol", 2019, 2024);

        Manufacturer motoMorini = seedBrand("MOTO MORINI", "Italy");
        seedModel(motoMorini, "X-Cape 650", "Adventure", "Petrol", 2022, 2024);
        seedModel(motoMorini, "Seiemmezzo 650", "Cruiser", "Petrol", 2022, 2024);

        Manufacturer aprilia = seedBrand("APRILIA", "Italy");
        seedModel(aprilia, "RS 457", "Sports", "Petrol", 2024, 2024);
        seedModel(aprilia, "SR 125", "Scooter", "Petrol", 2019, 2024);
        seedModel(aprilia, "Tuono 457", "Naked", "Petrol", 2024, 2024);

        Manufacturer vespa = seedBrand("VESPA", "Italy");
        seedModel(vespa, "VXL 125", "Scooter", "Petrol", 2017, 2024);
        seedModel(vespa, "SXL 150", "Scooter", "Petrol", 2017, 2024);
        seedModel(vespa, "ZX 125", "Scooter", "Petrol", 2019, 2024);

        // ── Swedish Brand ──
        Manufacturer husqvarna = seedBrand("HUSQVARNA", "Sweden");
        seedModel(husqvarna, "Vitpilen 401", "Naked", "Petrol", 2019, 2024);
        seedModel(husqvarna, "Svartpilen 401", "Adventure", "Petrol", 2019, 2024);
        seedModel(husqvarna, "Svartpilen 250", "Adventure", "Petrol", 2022, 2024);

        // ── Czech Brand ──
        Manufacturer jawa = seedBrand("JAWA", "Czech Republic");
        seedModel(jawa, "Jawa 42", "Naked", "Petrol", 2019, 2024);
        seedModel(jawa, "Jawa 42 Bobber", "Cruiser", "Petrol", 2022, 2024);
        seedModel(jawa, "Perak", "Cruiser", "Petrol", 2020, 2024);

        // ── Vietnamese Brand ──
        Manufacturer vinfast = seedBrand("VINFAST", "Vietnam");
        seedModel(vinfast, "Klara", "Scooter", "Electric", 2019, 2024);
        seedModel(vinfast, "Theon", "Scooter", "Electric", 2022, 2024);

        // ── Chinese Brands ──
        Manufacturer cfmoto = seedBrand("CFMOTO", "China");
        seedModel(cfmoto, "300NK", "Naked", "Petrol", 2019, 2024);
        seedModel(cfmoto, "650NK", "Naked", "Petrol", 2019, 2024);
        seedModel(cfmoto, "650MT", "Adventure", "Petrol", 2020, 2024);

        Manufacturer benelli = seedBrand("BENELLI", "China");
        seedModel(benelli, "Imperiale 400", "Cruiser", "Petrol", 2019, 2024);
        seedModel(benelli, "TRK 502", "Adventure", "Petrol", 2019, 2024);
        seedModel(benelli, "302R", "Sports", "Petrol", 2019, 2024);

        Manufacturer keeway = seedBrand("KEEWAY", "China");
        seedModel(keeway, "SR 125", "Naked", "Petrol", 2021, 2024);

        Manufacturer qjMotor = seedBrand("QJ MOTOR", "China");
        seedModel(qjMotor, "SRK 400", "Sports", "Petrol", 2022, 2024);

        Manufacturer zontes = seedBrand("ZONTES", "China");
        seedModel(zontes, "310R", "Naked", "Petrol", 2021, 2024);
        seedModel(zontes, "GK 125", "Naked", "Petrol", 2022, 2024);

        // ── Indian Electric Brands ──
        Manufacturer ather = seedBrand("ATHER", "India");
        seedModel(ather, "450X", "Scooter", "Electric", 2020, 2024);
        seedModel(ather, "450S", "Scooter", "Electric", 2023, 2024);
        seedModel(ather, "Rizta", "Scooter", "Electric", 2024, 2024);

        Manufacturer ola = seedBrand("OLA", "India");
        seedModel(ola, "S1", "Scooter", "Electric", 2021, 2024);
        seedModel(ola, "S1 Pro", "Scooter", "Electric", 2021, 2024);
        seedModel(ola, "S1 Air", "Scooter", "Electric", 2023, 2024);

        Manufacturer vida = seedBrand("VIDA", "India");
        seedModel(vida, "V1 Plus", "Scooter", "Electric", 2023, 2024);
        seedModel(vida, "V1 Pro", "Scooter", "Electric", 2023, 2024);

        Manufacturer ampere = seedBrand("AMPERE", "India");
        seedModel(ampere, "Magnus EX", "Scooter", "Electric", 2021, 2024);
        seedModel(ampere, "Primus", "Scooter", "Electric", 2023, 2024);

        Manufacturer okinawa = seedBrand("OKINAWA", "India");
        seedModel(okinawa, "Okhi 90", "Scooter", "Electric", 2022, 2024);
        seedModel(okinawa, "Ridge+", "Scooter", "Electric", 2020, 2024);

        Manufacturer pureEv = seedBrand("PURE EV", "India");
        seedModel(pureEv, "eTryst 350", "Scooter", "Electric", 2021, 2024);
        seedModel(pureEv, "Ecoline", "Scooter", "Electric", 2020, 2024);

        Manufacturer revolt = seedBrand("REVOLT", "India");
        seedModel(revolt, "RV400", "Commuter", "Electric", 2020, 2024);
        seedModel(revolt, "RV300", "Commuter", "Electric", 2020, 2024);

        Manufacturer ultraviolette = seedBrand("ULTRAVIOLETTE", "India");
        seedModel(ultraviolette, "F77", "Sports", "Electric", 2023, 2024);
        seedModel(ultraviolette, "F77 Mach 2", "Sports", "Electric", 2024, 2024);

        Manufacturer simpleEnergy = seedBrand("SIMPLE ENERGY", "India");
        seedModel(simpleEnergy, "Simple One", "Scooter", "Electric", 2023, 2024);

        Manufacturer raptee = seedBrand("RAPTEE HV", "India");
        seedModel(raptee, "Energy T30", "Commuter", "Electric", 2023, 2024);

        Manufacturer matter = seedBrand("MATTER", "India");
        seedModel(matter, "Aera 5000+", "Commuter", "Electric", 2023, 2024);

        Manufacturer river = seedBrand("RIVER", "India");
        seedModel(river, "Indie", "Scooter", "Electric", 2023, 2024);

        Manufacturer oben = seedBrand("OBEN", "India");
        seedModel(oben, "Rorr", "Commuter", "Electric", 2022, 2024);

        Manufacturer bgauss = seedBrand("BGAUSS", "India");
        seedModel(bgauss, "C12", "Scooter", "Electric", 2021, 2024);
        seedModel(bgauss, "B8", "Scooter", "Electric", 2022, 2024);

        Manufacturer evolet = seedBrand("EVOLET", "India");
        seedModel(evolet, "Derby Classic", "Scooter", "Electric", 2021, 2024);

        Manufacturer ferrato = seedBrand("FERRATO", "India");
        seedModel(ferrato, "Disruptor", "Commuter", "Electric", 2022, 2024);

        Manufacturer hopElectric = seedBrand("HOP ELECTRIC", "India");
        seedModel(hopElectric, "Leo", "Scooter", "Electric", 2021, 2024);
        seedModel(hopElectric, "Lyric", "Commuter", "Electric", 2022, 2024);

        Manufacturer ivoomi = seedBrand("IVOOMI", "India");
        seedModel(ivoomi, "S1", "Scooter", "Electric", 2022, 2024);
        seedModel(ivoomi, "Jeet X", "Scooter", "Electric", 2023, 2024);

        Manufacturer joyEbike = seedBrand("JOY E-BIKE", "India");
        seedModel(joyEbike, "Wolf", "Scooter", "Electric", 2021, 2024);
        seedModel(joyEbike, "Beast", "Scooter", "Electric", 2022, 2024);

        Manufacturer kinetic = seedBrand("KINETIC", "India");
        seedModel(kinetic, "Kinetic Luna", "Commuter", "Petrol", 2010, 2015);

        Manufacturer kineticGreen = seedBrand("KINETIC GREEN", "India");
        seedModel(kineticGreen, "Zulu", "Scooter", "Electric", 2022, 2024);

        Manufacturer lectrix = seedBrand("LECTRIX", "India");
        seedModel(lectrix, "LXS", "Scooter", "Electric", 2021, 2024);

        Manufacturer numeros = seedBrand("NUMEROS", "India");
        seedModel(numeros, "N7", "Scooter", "Electric", 2022, 2024);

        Manufacturer odsse = seedBrand("ODSSE", "India");
        seedModel(odsse, "Halo", "Scooter", "Electric", 2022, 2024);

        Manufacturer opg = seedBrand("OPG MOBILITY", "India");
        seedModel(opg, "Aelius 60", "Scooter", "Electric", 2022, 2024);

        Manufacturer quantumEnergy = seedBrand("QUANTUM ENERGY", "India");
        seedModel(quantumEnergy, "QE Maximus", "Scooter", "Electric", 2022, 2024);

        Manufacturer avore = seedBrand("AVORE", "India");
        seedModel(avore, "Avore E", "Scooter", "Electric", 2023, 2024);

        Manufacturer bounce = seedBrand("BOUNCE", "India");
        seedModel(bounce, "Infinity E1", "Scooter", "Electric", 2022, 2024);

        Manufacturer gemopai = seedBrand("GEMOPAI", "India");
        seedModel(gemopai, "Astrid Lite", "Scooter", "Electric", 2021, 2024);

        Manufacturer yo = seedBrand("YO", "India");
        seedModel(yo, "Electron", "Scooter", "Electric", 2022, 2024);

        Manufacturer yezdi = seedBrand("YEZDI", "India");
        seedModel(yezdi, "Adventure", "Adventure", "Petrol", 2022, 2024);
        seedModel(yezdi, "Scrambler", "Adventure", "Petrol", 2022, 2024);
        seedModel(yezdi, "Roadster", "Cruiser", "Petrol", 2022, 2024);

        Manufacturer lambretta = seedBrand("LAMBRETTA", "Italy");
        seedModel(lambretta, "G-Special 125", "Scooter", "Petrol", 2022, 2024);

        Manufacturer vlf = seedBrand("VLF", "India");
        seedModel(vlf, "Borile B500", "Cruiser", "Petrol", 2022, 2024);

    }

    private void seedWorker(String email, String password, String firstName, String lastName, String empId, String dept, String desig) {
        User u = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .role(Role.WORKER)
                .active(true)
                .build();
        u = userRepository.save(u);

        Worker w = Worker.builder()
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
        Manufacturer m = Manufacturer.builder().name(name).country(country).active(true).build();
        return manufacturerRepository.save(m);
    }

    private BikeModel seedModel(Manufacturer m, String modelName, String category, String fuelType, int startYear, int endYear) {
        BikeModel model = BikeModel.builder().manufacturer(m).modelName(modelName).category(category).fuelType(fuelType).active(true).build();
        model = bikeModelRepository.save(model);

        for (int y = startYear; y <= endYear; y++) {
            yearRepository.save(ManufacturingYear.builder().bikeModel(model).year(y).active(true).build());
        }

        BikeVariant variant = BikeVariant.builder().bikeModel(model).variantName("Standard").active(true).build();
        bikeVariantRepository.save(variant);

        return model;
    }

    /**
     * Ensures the built-in admin and worker accounts are always active on startup.
     * Called unconditionally so a restart recovers from accidental deactivation.
     */
    private void ensureDefaultUsersActive() {
        java.util.List<String> defaultEmails = java.util.List.of(
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
}
