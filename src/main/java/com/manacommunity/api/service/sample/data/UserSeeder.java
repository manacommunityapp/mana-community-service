package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.repository.AppUserRepository;
import com.manacommunity.api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * UserSeeder — Seeds system administrators, key members, and block residents.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSeeder {

    private final AppUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final CommunitySeeder communitySeeder;

    private static final AtomicInteger phoneCounter = new AtomicInteger(1000000);

    @Transactional
    public void defaultSeed() {
        log.info("Seeding system and gated community users...");
        String hash = passwordEncoder.encode("password123");
        Community leCommunity = communitySeeder.getLeCommunity();

        // 1. Core administrative/test accounts
        AppUser superAdmin = createUser(
                "admin@manacommunity.com", "Super Admin", "SUPER_ADMIN", hash,
                null, "", "", "MALE", LocalDate.of(1990, 1, 1));
    }



    @Transactional
    public void seed() {
        log.info("Seeding system and gated community users...");
        String hash = passwordEncoder.encode("password123");
        Community leCommunity = communitySeeder.getLeCommunity();

        // 1. Core administrative/test accounts
        AppUser superAdmin = createUser(
                "admin@manacommunity.com", "Super Admin", "SUPER_ADMIN", hash,
                null, "", "", "MALE", LocalDate.of(1990, 1, 1));

        AppUser sandeep = createUser(
                "sandeep@gmail.com", "Sandeep Kamarapu", "MEMBER", hash,
                leCommunity, "B", "806", "MALE", LocalDate.of(1990, 1, 1));

        AppUser sunil = createUser(
                "sunil@gmail.com", "Sunil Kanthala", "ADMIN", hash,
                leCommunity, "C", "212", "MALE", LocalDate.of(1990, 1, 1));

        AppUser ramesh = createUser(
                "ramesh@gmail.com", "Ramesh Korlakunta", "SPORTS_ADMIN", hash,
                leCommunity, "B", "907", "MALE", LocalDate.of(1990, 1, 1));

        AppUser mady = createUser(
                "mady@gmail.com", "Mady", "MEMBER", hash,
                leCommunity, "D", "107", "MALE", LocalDate.of(1990, 1, 1));

        createUser(
                "user1@gmail.com", "user1", "MEMBER", hash,
                leCommunity, "D", "105", "MALE", LocalDate.of(1990, 1, 1));

        // 2. Block A Residents
        createUser("rahul.sharma@gmail.com", "Rahul Sharma", "MEMBER", hash, leCommunity, "A", "101", "MALE", LocalDate.of(1988, 5, 14));
        createUser("priya.patel@gmail.com", "Priya Patel", "MEMBER", hash, leCommunity, "A", "102", "FEMALE", LocalDate.of(1992, 8, 22));
        createUser("amit.kumar@gmail.com", "Amit Kumar", "MEMBER", hash, leCommunity, "A", "103", "MALE", LocalDate.of(1985, 11, 30));
        createUser("sneha.reddy@gmail.com", "Sneha Reddy", "MEMBER", hash, leCommunity, "A", "104", "FEMALE", LocalDate.of(1995, 2, 15));
        createUser("vikram.singh@gmail.com", "Vikram Singh", "MEMBER", hash, leCommunity, "A", "201", "MALE", LocalDate.of(1990, 7, 19));
        createUser("ananya.desai@gmail.com", "Ananya Desai", "MEMBER", hash, leCommunity, "A", "202", "FEMALE", LocalDate.of(1993, 4, 25));
        createUser("rohit.verma@gmail.com", "Rohit Verma", "MEMBER", hash, leCommunity, "A", "203", "MALE", LocalDate.of(1982, 9, 12));
        createUser("neha.gupta@gmail.com", "Neha Gupta", "MEMBER", hash, leCommunity, "A", "204", "FEMALE", LocalDate.of(1989, 12, 5));
        createUser("karan.malhotra@gmail.com", "Karan Malhotra", "MEMBER", hash, leCommunity, "A", "301", "MALE", LocalDate.of(1994, 3, 8));
        createUser("pooja.joshi@gmail.com", "Pooja Joshi", "MEMBER", hash, leCommunity, "A", "302", "FEMALE", LocalDate.of(1991, 6, 17));
        createUser("suresh.nair@gmail.com", "Suresh Nair", "MEMBER", hash, leCommunity, "A", "303", "MALE", LocalDate.of(1978, 1, 20));
        createUser("meera.iyer@gmail.com", "Meera Iyer", "MEMBER", hash, leCommunity, "A", "304", "FEMALE", LocalDate.of(1986, 10, 14));

        // 3. Block B Residents
        createUser("rajat.bhatia@gmail.com", "Rajat Bhatia", "MEMBER", hash, leCommunity, "B", "101", "MALE", LocalDate.of(1996, 8, 3));
        createUser("kavita.menon@gmail.com", "Kavita Menon", "MEMBER", hash, leCommunity, "B", "102", "FEMALE", LocalDate.of(1983, 5, 28));
        createUser("deepak.pillai@gmail.com", "Deepak Pillai", "MEMBER", hash, leCommunity, "B", "103", "MALE", LocalDate.of(1980, 11, 9));
        createUser("swati.jain@gmail.com", "Swati Jain", "MEMBER", hash, leCommunity, "B", "104", "FEMALE", LocalDate.of(1992, 9, 21));
        createUser("manish.tiwari@gmail.com", "Manish Tiwari", "MEMBER", hash, leCommunity, "B", "201", "MALE", LocalDate.of(1987, 4, 16));
        createUser("divya.rao@gmail.com", "Divya Rao", "MEMBER", hash, leCommunity, "B", "202", "FEMALE", LocalDate.of(1994, 1, 11));
        createUser("arjun.kapoor@gmail.com", "Arjun Kapoor", "MEMBER", hash, leCommunity, "B", "203", "MALE", LocalDate.of(1991, 7, 30));
        createUser("shweta.sinha@gmail.com", "Shweta Sinha", "MEMBER", hash, leCommunity, "B", "204", "FEMALE", LocalDate.of(1985, 12, 19));
        createUser("tarun.garg@gmail.com", "Tarun Garg", "MEMBER", hash, leCommunity, "B", "301", "MALE", LocalDate.of(1998, 2, 25));
        createUser("radhika.apte@gmail.com", "Radhika Apte", "MEMBER", hash, leCommunity, "B", "302", "FEMALE", LocalDate.of(1990, 6, 8));
        createUser("nitin.das@gmail.com", "Nitin Das", "MEMBER", hash, leCommunity, "B", "303", "MALE", LocalDate.of(1979, 10, 4));
        createUser("aarti.chawla@gmail.com", "Aarti Chawla", "MEMBER", hash, leCommunity, "B", "304", "FEMALE", LocalDate.of(1984, 8, 15));

        // 4. Block C Residents
        createUser("siddharth.bose@gmail.com", "Siddharth Bose", "MEMBER", hash, leCommunity, "C", "101", "MALE", LocalDate.of(1993, 3, 22));
        createUser("isha.khanna@gmail.com", "Isha Khanna", "MEMBER", hash, leCommunity, "C", "102", "FEMALE", LocalDate.of(1997, 11, 12));
        createUser("varun.mehta@gmail.com", "Varun Mehta", "MEMBER", hash, leCommunity, "C", "103", "MALE", LocalDate.of(1988, 1, 7));
        createUser("pallavi.sen@gmail.com", "Pallavi Sen", "MEMBER", hash, leCommunity, "C", "104", "FEMALE", LocalDate.of(1981, 9, 29));
        createUser("gourav.pandey@gmail.com", "Gourav Pandey", "MEMBER", hash, leCommunity, "C", "201", "MALE", LocalDate.of(1995, 5, 18));
        createUser("simran.kaur@gmail.com", "Simran Kaur", "MEMBER", hash, leCommunity, "C", "202", "FEMALE", LocalDate.of(1992, 12, 2));
        createUser("abhishek.mishra@gmail.com", "Abhishek Mishra", "MEMBER", hash, leCommunity, "C", "203", "MALE", LocalDate.of(1986, 7, 14));
        createUser("nidhi.agarwal@gmail.com", "Nidhi Agarwal", "MEMBER", hash, leCommunity, "C", "204", "FEMALE", LocalDate.of(1990, 10, 31));
        createUser("vishal.shetty@gmail.com", "Vishal Shetty", "MEMBER", hash, leCommunity, "C", "301", "MALE", LocalDate.of(1983, 2, 9));
        createUser("monica.goyal@gmail.com", "Monica Goyal", "MEMBER", hash, leCommunity, "C", "302", "FEMALE", LocalDate.of(1989, 4, 21));
        createUser("prashant.kadam@gmail.com", "Prashant Kadam", "MEMBER", hash, leCommunity, "C", "303", "MALE", LocalDate.of(1977, 8, 5));
        createUser("rashmi.dubey@gmail.com", "Rashmi Dubey", "MEMBER", hash, leCommunity, "C", "304", "FEMALE", LocalDate.of(1996, 6, 26));

        // 5. Block D Residents
        createUser("harsh.vardhan@gmail.com", "Harsh Vardhan", "MEMBER", hash, leCommunity, "D", "101", "MALE", LocalDate.of(1991, 11, 3));
        createUser("shreya.ghoshal@gmail.com", "Shreya Ghoshal", "MEMBER", hash, leCommunity, "D", "102", "FEMALE", LocalDate.of(1994, 3, 19));
        createUser("yash.chopra@gmail.com", "Yash Chopra", "MEMBER", hash, leCommunity, "D", "103", "MALE", LocalDate.of(1982, 1, 15));
        createUser("kriti.sanon@gmail.com", "Kriti Sanon", "MEMBER", hash, leCommunity, "D", "104", "FEMALE", LocalDate.of(1995, 9, 8));
        createUser("akash.ambani@gmail.com", "Akash Ambani", "MEMBER", hash, leCommunity, "D", "201", "MALE", LocalDate.of(1990, 12, 12));
        createUser("tanvi.shah@gmail.com", "Tanvi Shah", "MEMBER", hash, leCommunity, "D", "202", "FEMALE", LocalDate.of(1988, 5, 27));
        createUser("naveen.kumar@gmail.com", "Naveen Kumar", "MEMBER", hash, leCommunity, "D", "203", "MALE", LocalDate.of(1985, 2, 14));
        createUser("richa.chadha@gmail.com", "Richa Chadha", "MEMBER", hash, leCommunity, "D", "204", "FEMALE", LocalDate.of(1993, 10, 6));
        createUser("sanjay.dutt@gmail.com", "Sanjay Dutt", "MEMBER", hash, leCommunity, "D", "301", "MALE", LocalDate.of(1975, 7, 29));
        createUser("aditi.rao@gmail.com", "Aditi Rao", "MEMBER", hash, leCommunity, "D", "302", "FEMALE", LocalDate.of(1987, 4, 18));
        createUser("mahesh.babu@gmail.com", "Mahesh Babu", "MEMBER", hash, leCommunity, "D", "303", "MALE", LocalDate.of(1980, 8, 9));
        createUser("kiara.advani@gmail.com", "Kiara Advani", "MEMBER", hash, leCommunity, "D", "304", "FEMALE", LocalDate.of(1992, 11, 23));
        createUser("ajay.devgn@gmail.com", "Ajay Devgn", "MEMBER", hash, leCommunity, "D", "401", "MALE", LocalDate.of(1978, 3, 2));
        createUser("kajol.mukherjee@gmail.com", "Kajol Mukherjee", "MEMBER", hash, leCommunity, "D", "402", "FEMALE", LocalDate.of(1981, 6, 11));

        log.info("✓ Gated community user seeding completed: SuperAdmin, Sandeep, Sunil, Ramesh, Mady and 45 block residents.");
    }

    public AppUser getSuperAdmin() {
        return userRepo.findByEmail("admin@manacommunity.com")
                .orElseThrow(() -> new IllegalStateException("superAdmin has not been seeded yet."));
    }

    public AppUser getSandeep() {
        return userRepo.findByEmail("sandeep@gmail.com")
                .orElseThrow(() -> new IllegalStateException("sandeep has not been seeded yet."));
    }

    public AppUser getSunil() {
        return userRepo.findByEmail("sunil@gmail.com")
                .orElseThrow(() -> new IllegalStateException("sunil has not been seeded yet."));
    }

    public AppUser getRamesh() {
        return userRepo.findByEmail("ramesh@gmail.com")
                .orElseThrow(() -> new IllegalStateException("ramesh has not been seeded yet."));
    }

    public AppUser getMady() {
        return userRepo.findByEmail("mady@gmail.com")
                .orElseThrow(() -> new IllegalStateException("mady has not been seeded yet."));
    }

    public AppUser getUser1() {
        return userRepo.findByEmail("user1@gmail.com")
                .orElseThrow(() -> new IllegalStateException("user1 has not been seeded yet."));
    }

    public AppUser getUserByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User with email " + email + " has not been seeded yet."));
    }

    private AppUser createUser(String email, String name, String role, String hash,
                               Community community, String block, String flatNo,
                               String gender, LocalDate dob) {
        Role roleEntity;
        if (community != null) {
            roleEntity = roleRepo.findByNameIgnoreCaseAndCommunityId(role, community.getId())
                    .orElseGet(() -> roleRepo.save(Role.builder()
                            .name(role.toUpperCase())
                            .communityId(community.getId())
                            .build()));
        } else {
            roleEntity = roleRepo.findByNameIgnoreCaseAndCommunityIdIsNull(role)
                    .orElseGet(() -> roleRepo.save(Role.builder()
                            .name(role.toUpperCase())
                            .build()));
        }

        AppUser savedAppUser = userRepo.findByEmail(email).orElseGet(() -> {
            AppUser u = new AppUser();
            u.setEmail(email);
            u.setFullName(name);
            u.setPhone("999" + phoneCounter.incrementAndGet());
            u.setRole(role);
            u.setRoleEntity(roleEntity);
            u.setKycStatus("VERIFIED");
            u.setPasswordHash(hash);
            u.setDateOfBirth(dob);
            u.setGender(gender);
            u.setCommunity(community);
            u.setIsActive(Boolean.TRUE);
            u.setBlock(block);
            u.setFlatNo(flatNo);
            return userRepo.save(u);
        });

        return savedAppUser;
    }
}
