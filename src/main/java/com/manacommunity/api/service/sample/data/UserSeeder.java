package com.manacommunity.api.service.sample.data;

import static com.manacommunity.api.constants.PermissionConstants.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.model.RolePermission;
import com.manacommunity.api.user.repository.AppUserRepository;
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
        Community leCommunity = communitySeeder.getGeneralCommunity();

        // 1. Core administrative/test accounts
        AppUser superAdmin = createUser(
                "manacommunity@gmail.com", "Super Admin", ROLE_SUPER_ADMIN, hash,
                leCommunity, "", "", "MALE", LocalDate.of(1990, 1, 1));

        // 1. Core administrative/test accounts
        AppUser superAdmin2 = createUser(
                "admin@manacommunity.com", "Super Admin", ROLE_SUPER_ADMIN, hash,
                leCommunity, "", "", "MALE", LocalDate.of(1990, 1, 1));

    }



    @Transactional
    public void seed() {
        log.info("Seeding system and gated community users...");
        String hash = passwordEncoder.encode("password123");
        Community leCommunity = communitySeeder.getLeCommunity();

        AppUser sandeep = createUser(
                "sandeep60.kamarapu@gmail.com", "Sandeep Kamarapu", ROLE_USER, hash,
                leCommunity, "B", "806", "MALE", LocalDate.of(1990, 1, 1));

        AppUser sunil = createUser(
                "sunil@gmail.com", "Sunil Kanthala", ROLE_ADMIN, hash,
                leCommunity, "C", "212", "MALE", LocalDate.of(1990, 1, 1));

        AppUser chethan = createUser(
                "chethan@gmail.com", "Chethan Reddy", ROLE_SPORTS_ADMIN, hash,
                leCommunity, "B", "504", "MALE", LocalDate.of(1990, 1, 1));

        AppUser ramesh = createUser(
                "ramesh@gmail.com", "Ramesh Korlakunta", ROLE_SPORTS_ADMIN, hash,
                leCommunity, "B", "907", "MALE", LocalDate.of(1990, 1, 1));

        AppUser mady = createUser(
                "mady@gmail.com", "Mady", ROLE_USER, hash,
                leCommunity, "D", "107", "MALE", LocalDate.of(1990, 1, 1));

        createUser(
                "kusivarshitha23@gmail.com", "varshitha kusi", ROLE_EVENTS_ADMIN, hash,
                leCommunity, "B", "807", "MALE", LocalDate.of(2007, 6, 14));

        createUser(
                "mokshitagali@gmail.com", "sai mokshitha gali", ROLE_USER, hash,
                leCommunity, "B", "810", "MALE", LocalDate.of(2007, 01, 8));


        createUser("Bhupal@gmail.com", "Bhupal", ROLE_USER, hash, leCommunity, "B", "209", "MALE", LocalDate.of(1986, 11, 14));


        // 2. Block A Residents
        //createUser("rahul.sharma@gmail.com", "Rahul Sharma", ROLE_MEMBER, hash, leCommunity, "A", "101", "MALE", LocalDate.of(1988, 5, 14));
        //createUser("priya.patel@gmail.com", "Priya Patel", ROLE_MEMBER, hash, leCommunity, "A", "102", "FEMALE", LocalDate.of(1992, 8, 22));
        //createUser("amit.kumar@gmail.com", "Amit Kumar", ROLE_MEMBER, hash, leCommunity, "A", "103", "MALE", LocalDate.of(1985, 11, 30));
        //createUser("sneha.reddy@gmail.com", "Sneha Reddy", ROLE_MEMBER, hash, leCommunity, "A", "104", "FEMALE", LocalDate.of(1995, 2, 15));
        //createUser("vikram.singh@gmail.com", "Vikram Singh", ROLE_MEMBER, hash, leCommunity, "A", "201", "MALE", LocalDate.of(1990, 7, 19));
        //createUser("ananya.desai@gmail.com", "Ananya Desai", ROLE_MEMBER, hash, leCommunity, "A", "202", "FEMALE", LocalDate.of(1993, 4, 25));
        //createUser("rohit.verma@gmail.com", "Rohit Verma", ROLE_MEMBER, hash, leCommunity, "A", "203", "MALE", LocalDate.of(1982, 9, 12));
        //createUser("neha.gupta@gmail.com", "Neha Gupta", ROLE_MEMBER, hash, leCommunity, "A", "204", "FEMALE", LocalDate.of(1989, 12, 5));
        //createUser("karan.malhotra@gmail.com", "Karan Malhotra", ROLE_MEMBER, hash, leCommunity, "A", "301", "MALE", LocalDate.of(1994, 3, 8));
        //createUser("pooja.joshi@gmail.com", "Pooja Joshi", ROLE_MEMBER, hash, leCommunity, "A", "302", "FEMALE", LocalDate.of(1991, 6, 17));
        //createUser("suresh.nair@gmail.com", "Suresh Nair", ROLE_MEMBER, hash, leCommunity, "A", "303", "MALE", LocalDate.of(1978, 1, 20));
        //createUser("meera.iyer@gmail.com", "Meera Iyer", ROLE_MEMBER, hash, leCommunity, "A", "304", "FEMALE", LocalDate.of(1986, 10, 14));


        // 3. Block B Residents
        //createUser("rajat.bhatia@gmail.com", "Rajat Bhatia", ROLE_MEMBER, hash, leCommunity, "B", "101", "MALE", LocalDate.of(1996, 8, 3));
        //createUser("kavita.menon@gmail.com", "Kavita Menon", ROLE_MEMBER, hash, leCommunity, "B", "102", "FEMALE", LocalDate.of(1983, 5, 28));
        //createUser("deepak.pillai@gmail.com", "Deepak Pillai", ROLE_MEMBER, hash, leCommunity, "B", "103", "MALE", LocalDate.of(1980, 11, 9));
        //createUser("swati.jain@gmail.com", "Swati Jain", ROLE_MEMBER, hash, leCommunity, "B", "104", "FEMALE", LocalDate.of(1992, 9, 21));
        //createUser("manish.tiwari@gmail.com", "Manish Tiwari", ROLE_MEMBER, hash, leCommunity, "B", "201", "MALE", LocalDate.of(1987, 4, 16));
        //createUser("divya.rao@gmail.com", "Divya Rao", ROLE_MEMBER, hash, leCommunity, "B", "202", "FEMALE", LocalDate.of(1994, 1, 11));
        //createUser("arjun.kapoor@gmail.com", "Arjun Kapoor", ROLE_MEMBER, hash, leCommunity, "B", "203", "MALE", LocalDate.of(1991, 7, 30));
        //createUser("shweta.sinha@gmail.com", "Shweta Sinha", ROLE_MEMBER, hash, leCommunity, "B", "204", "FEMALE", LocalDate.of(1985, 12, 19));
        //createUser("tarun.garg@gmail.com", "Tarun Garg", ROLE_MEMBER, hash, leCommunity, "B", "301", "MALE", LocalDate.of(1998, 2, 25));
        //createUser("radhika.apte@gmail.com", "Radhika Apte", ROLE_MEMBER, hash, leCommunity, "B", "302", "FEMALE", LocalDate.of(1990, 6, 8));
        //createUser("nitin.das@gmail.com", "Nitin Das", ROLE_MEMBER, hash, leCommunity, "B", "303", "MALE", LocalDate.of(1979, 10, 4));
        //createUser("aarti.chawla@gmail.com", "Aarti Chawla", ROLE_MEMBER, hash, leCommunity, "B", "304", "FEMALE", LocalDate.of(1984, 8, 15));

        // 4. Block C Residents
        //createUser("siddharth.bose@gmail.com", "Siddharth Bose", ROLE_MEMBER, hash, leCommunity, "C", "101", "MALE", LocalDate.of(1993, 3, 22));
        //createUser("isha.khanna@gmail.com", "Isha Khanna", ROLE_MEMBER, hash, leCommunity, "C", "102", "FEMALE", LocalDate.of(1997, 11, 12));
        //createUser("varun.mehta@gmail.com", "Varun Mehta", ROLE_MEMBER, hash, leCommunity, "C", "103", "MALE", LocalDate.of(1988, 1, 7));
        //createUser("pallavi.sen@gmail.com", "Pallavi Sen", ROLE_MEMBER, hash, leCommunity, "C", "104", "FEMALE", LocalDate.of(1981, 9, 29));
        //createUser("gourav.pandey@gmail.com", "Gourav Pandey", ROLE_MEMBER, hash, leCommunity, "C", "201", "MALE", LocalDate.of(1995, 5, 18));
        //createUser("simran.kaur@gmail.com", "Simran Kaur", ROLE_MEMBER, hash, leCommunity, "C", "202", "FEMALE", LocalDate.of(1992, 12, 2));
        //createUser("abhishek.mishra@gmail.com", "Abhishek Mishra", ROLE_MEMBER, hash, leCommunity, "C", "203", "MALE", LocalDate.of(1986, 7, 14));
        //createUser("nidhi.agarwal@gmail.com", "Nidhi Agarwal", ROLE_MEMBER, hash, leCommunity, "C", "204", "FEMALE", LocalDate.of(1990, 10, 31));
        //createUser("vishal.shetty@gmail.com", "Vishal Shetty", ROLE_MEMBER, hash, leCommunity, "C", "301", "MALE", LocalDate.of(1983, 2, 9));
        //createUser("monica.goyal@gmail.com", "Monica Goyal", ROLE_MEMBER, hash, leCommunity, "C", "302", "FEMALE", LocalDate.of(1989, 4, 21));
        //createUser("prashant.kadam@gmail.com", "Prashant Kadam", ROLE_MEMBER, hash, leCommunity, "C", "303", "MALE", LocalDate.of(1977, 8, 5));
        //createUser("rashmi.dubey@gmail.com", "Rashmi Dubey", ROLE_MEMBER, hash, leCommunity, "C", "304", "FEMALE", LocalDate.of(1996, 6, 26));

        // 5. Block D Residents
        //createUser("harsh.vardhan@gmail.com", "Harsh Vardhan", ROLE_MEMBER, hash, leCommunity, "D", "101", "MALE", LocalDate.of(1991, 11, 3));
        //createUser("shreya.ghoshal@gmail.com", "Shreya Ghoshal", ROLE_MEMBER, hash, leCommunity, "D", "102", "FEMALE", LocalDate.of(1994, 3, 19));
        //createUser("yash.chopra@gmail.com", "Yash Chopra", ROLE_MEMBER, hash, leCommunity, "D", "103", "MALE", LocalDate.of(1982, 1, 15));
        //createUser("kriti.sanon@gmail.com", "Kriti Sanon", ROLE_MEMBER, hash, leCommunity, "D", "104", "FEMALE", LocalDate.of(1995, 9, 8));
        //createUser("akash.ambani@gmail.com", "Akash Ambani", ROLE_MEMBER, hash, leCommunity, "D", "201", "MALE", LocalDate.of(1990, 12, 12));
        //createUser("tanvi.shah@gmail.com", "Tanvi Shah", ROLE_MEMBER, hash, leCommunity, "D", "202", "FEMALE", LocalDate.of(1988, 5, 27));
        //createUser("naveen.kumar@gmail.com", "Naveen Kumar", ROLE_MEMBER, hash, leCommunity, "D", "203", "MALE", LocalDate.of(1985, 2, 14));
        //createUser("richa.chadha@gmail.com", "Richa Chadha", ROLE_MEMBER, hash, leCommunity, "D", "204", "FEMALE", LocalDate.of(1993, 10, 6));
       // createUser("sanjay.dutt@gmail.com", "Sanjay Dutt", ROLE_MEMBER, hash, leCommunity, "D", "301", "MALE", LocalDate.of(1975, 7, 29));
        //createUser("aditi.rao@gmail.com", "Aditi Rao", ROLE_MEMBER, hash, leCommunity, "D", "302", "FEMALE", LocalDate.of(1987, 4, 18));
        //createUser("mahesh.babu@gmail.com", "Mahesh Babu", ROLE_MEMBER, hash, leCommunity, "D", "303", "MALE", LocalDate.of(1980, 8, 9));
        //createUser("kiara.advani@gmail.com", "Kiara Advani", ROLE_MEMBER, hash, leCommunity, "D", "304", "FEMALE", LocalDate.of(1992, 11, 23));
        //createUser("ajay.devgn@gmail.com", "Ajay Devgn", ROLE_MEMBER, hash, leCommunity, "D", "401", "MALE", LocalDate.of(1978, 3, 2));
        //createUser("kajol.mukherjee@gmail.com", "Kajol Mukherjee", ROLE_MEMBER, hash, leCommunity, "D", "402", "FEMALE", LocalDate.of(1981, 6, 11));

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

    public AppUser getVarshitha() {
        return userRepo.findByEmail("varshitha@gmail.com")
                .orElseThrow(() -> new IllegalStateException("varshitha has not been seeded yet."));
    }

    public AppUser getUserByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User with email " + email + " has not been seeded yet."));
    }

    @Transactional
    public AppUser getOrCreateUser(String email, String name, String role, Community community) {
        return userRepo.findByEmail(email).orElseGet(() -> {
            String hash = passwordEncoder.encode("password123");
            return createUser(email, name, role, hash, community, "A", "100", "MALE", LocalDate.of(1990, 1, 1));
        });
    }

    private AppUser createUser(String email, String name, String role, String hash,
                               Community community, String block, String flatNo,
                               String gender, LocalDate dob) {
        String reqRoleUpper = role.trim().toUpperCase();

        Role userRoleEntity;
        Role primaryRoleEntity;

        if (community != null) {
            userRoleEntity = roleRepo.findByNameIgnoreCaseAndCommunityId(ROLE_USER, community.getId())
                    .orElseGet(() -> createCommunityRoleWithPermissions(ROLE_USER, community.getId()));
            if (reqRoleUpper.equals(ROLE_USER)) {
                primaryRoleEntity = userRoleEntity;
            } else {
                primaryRoleEntity = roleRepo.findByNameIgnoreCaseAndCommunityId(reqRoleUpper, community.getId())
                        .orElseGet(() -> createCommunityRoleWithPermissions(reqRoleUpper, community.getId()));
            }
        } else {
            userRoleEntity = roleRepo.findByNameIgnoreCaseAndCommunityIdIsNull(ROLE_USER)
                    .orElseGet(() -> roleRepo.save(Role.builder()
                            .name(ROLE_USER)
                            .build()));
            if (reqRoleUpper.equals(ROLE_USER)) {
                primaryRoleEntity = userRoleEntity;
            } else {
                primaryRoleEntity = roleRepo.findByNameIgnoreCaseAndCommunityIdIsNull(reqRoleUpper)
                        .orElseGet(() -> roleRepo.save(Role.builder()
                                .name(reqRoleUpper)
                                .build()));
            }
        }

        java.util.Set<Role> rolesSet = new java.util.LinkedHashSet<>();
        rolesSet.add(userRoleEntity);
        if (primaryRoleEntity != null) {
            rolesSet.add(primaryRoleEntity);
        }

        AppUser savedAppUser = userRepo.findByEmail(email).orElseGet(() -> {
            AppUser u = new AppUser();
            u.setEmail(email);
            u.setFullName(name);
            String phone;
            do {
                phone = "999" + phoneCounter.incrementAndGet();
            } while (userRepo.existsByPhone(phone));
            u.setPhone(phone);
            u.setRoleEntity(primaryRoleEntity);
            u.setUserRoles(rolesSet);
            u.syncRoleString();
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

    private Role createCommunityRoleWithPermissions(String roleName, Long communityId) {
        Role role = Role.builder()
                .name(roleName)
                .communityId(communityId)
                .permissions(new java.util.HashSet<>())
                .build();
        for (String p : getPermissionsForRole(roleName)) {
            role.getPermissions().add(RolePermission.builder()
                    .role(roleName)
                    .permissionKey(p)
                    .roleEntity(role)
                    .build());
        }
        return roleRepo.save(role);
    }
}
