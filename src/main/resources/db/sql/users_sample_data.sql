-- Seeding app_user and sync legacy_user table with baseline test users
-- Binds to 'Lakshmi''s Emperia' community (invite_code: LE-MY-HYD)
DO $$
DECLARE
    le_comm_id INT;
    sa_role_id INT;
    admin_role_id INT;
    sa_role_admin_id INT;
    member_role_id INT;
    ph_counter INT := 1000000;
    hash_val VARCHAR := '$2a$10$8.UnVuG9HHgffUDAlk8GPuae3RVC9CblhcR1t0CoFKo73VSLhcJZW'; -- bcrypt encoded 'password123'
BEGIN
    SELECT id INTO le_comm_id FROM community WHERE invite_code = 'LE-MY-HYD';
    SELECT id INTO sa_role_id FROM roles WHERE name = 'SUPER_ADMIN';
    SELECT id INTO admin_role_id FROM roles WHERE name = 'ADMIN';
    SELECT id INTO sa_role_admin_id FROM roles WHERE name = 'SPORTS_ADMIN';
    SELECT id INTO member_role_id FROM roles WHERE name = 'MEMBER';
    
    -- Helper Function to Seed a User cleanly inside PGSQL Block
    -- 1. Super Admin
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'admin@manacommunity.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, is_active, block, flat_no)
        VALUES ('admin@manacommunity.com', 'Super Admin', '9991000451', 'SUPER_ADMIN', sa_role_id, 'VERIFIED', hash_val, '1990-01-01', 'MALE', TRUE, '', '');
    END IF;

    -- 2. Sandeep
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'sandeep@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('sandeep@gmail.com', 'Sandeep Kamarapu', '9991000002', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1990-01-01', 'MALE', le_comm_id, TRUE, 'B', '806');
    END IF;

    -- 3. Sunil
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'sunil@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('sunil@gmail.com', 'Sunil Kanthala', '9991000003', 'ADMIN', admin_role_id, 'VERIFIED', hash_val, '1990-01-01', 'MALE', le_comm_id, TRUE, 'C', '212');
    END IF;

    -- 4. Ramesh
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'ramesh@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('ramesh@gmail.com', 'Ramesh Korlakunta', '9991000004', 'SPORTS_ADMIN', sa_role_admin_id, 'VERIFIED', hash_val, '1990-01-01', 'MALE', le_comm_id, TRUE, 'B', '907');
    END IF;

    -- 5. Mady
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'mady@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('mady@gmail.com', 'Mady', '9991000005', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1990-01-01', 'MALE', le_comm_id, TRUE, 'D', '107');
    END IF;

    -- 6. User1
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'user1@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('user1@gmail.com', 'user1', '9991000006', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1990-01-01', 'MALE', le_comm_id, TRUE, 'D', '105');
    END IF;

    -- 7. Block Residents
    -- Block A
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'rahul.sharma@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('rahul.sharma@gmail.com', 'Rahul Sharma', '9991000007', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1988-05-14', 'MALE', le_comm_id, TRUE, 'A', '101');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'priya.patel@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('priya.patel@gmail.com', 'Priya Patel', '9991000008', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1992-08-22', 'FEMALE', le_comm_id, TRUE, 'A', '102');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'amit.kumar@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('amit.kumar@gmail.com', 'Amit Kumar', '9991000009', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1985-11-30', 'MALE', le_comm_id, TRUE, 'A', '103');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'vikram.singh@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('vikram.singh@gmail.com', 'Vikram Singh', '9991000010', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1990-07-19', 'MALE', le_comm_id, TRUE, 'A', '201');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'rohit.verma@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('rohit.verma@gmail.com', 'Rohit Verma', '9991000011', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1982-09-12', 'MALE', le_comm_id, TRUE, 'A', '203');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'suresh.nair@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('suresh.nair@gmail.com', 'Suresh Nair', '9991000012', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1978-01-20', 'MALE', le_comm_id, TRUE, 'A', '303');
    END IF;

    -- Block B
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'rajat.bhatia@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('rajat.bhatia@gmail.com', 'Rajat Bhatia', '9991000013', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1996-08-03', 'MALE', le_comm_id, TRUE, 'B', '101');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'deepak.pillai@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('deepak.pillai@gmail.com', 'Deepak Pillai', '9991000014', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1980-11-09', 'MALE', le_comm_id, TRUE, 'B', '103');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'manish.tiwari@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('manish.tiwari@gmail.com', 'Manish Tiwari', '9991000015', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1987-04-16', 'MALE', le_comm_id, TRUE, 'B', '201');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'arjun.kapoor@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('arjun.kapoor@gmail.com', 'Arjun Kapoor', '9991000016', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1991-07-30', 'MALE', le_comm_id, TRUE, 'B', '203');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'tarun.garg@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('tarun.garg@gmail.com', 'Tarun Garg', '9991000017', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1998-02-25', 'MALE', le_comm_id, TRUE, 'B', '301');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'nitin.das@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('nitin.das@gmail.com', 'Nitin Das', '9991000018', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1979-10-04', 'MALE', le_comm_id, TRUE, 'B', '303');
    END IF;

    -- Block C
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'siddharth.bose@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('siddharth.bose@gmail.com', 'Siddharth Bose', '9991000019', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1993-03-22', 'MALE', le_comm_id, TRUE, 'C', '101');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'varun.mehta@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('varun.mehta@gmail.com', 'Varun Mehta', '9991000020', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1988-01-07', 'MALE', le_comm_id, TRUE, 'C', '103');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'gourav.pandey@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('gourav.pandey@gmail.com', 'Gourav Pandey', '9991000021', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1995-05-18', 'MALE', le_comm_id, TRUE, 'C', '201');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'abhishek.mishra@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('abhishek.mishra@gmail.com', 'Abhishek Mishra', '9991000022', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1986-07-14', 'MALE', le_comm_id, TRUE, 'C', '203');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'vishal.shetty@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('vishal.shetty@gmail.com', 'Vishal Shetty', '9991000023', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1983-02-09', 'MALE', le_comm_id, TRUE, 'C', '301');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'prashant.kadam@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('prashant.kadam@gmail.com', 'Prashant Kadam', '9991000024', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1977-08-05', 'MALE', le_comm_id, TRUE, 'C', '303');
    END IF;

    -- Block D
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'harsh.vardhan@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('harsh.vardhan@gmail.com', 'Harsh Vardhan', '9991000025', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1991-11-03', 'MALE', le_comm_id, TRUE, 'D', '101');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'yash.chopra@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('yash.chopra@gmail.com', 'Yash Chopra', '9991000026', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1982-01-15', 'MALE', le_comm_id, TRUE, 'D', '103');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'akash.ambani@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('akash.ambani@gmail.com', 'Akash Ambani', '9991000027', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1990-12-12', 'MALE', le_comm_id, TRUE, 'D', '201');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'naveen.kumar@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('naveen.kumar@gmail.com', 'Naveen Kumar', '9991000028', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1985-02-14', 'MALE', le_comm_id, TRUE, 'D', '203');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'sanjay.dutt@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('sanjay.dutt@gmail.com', 'Sanjay Dutt', '9991000029', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1975-07-29', 'MALE', le_comm_id, TRUE, 'D', '301');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'mahesh.babu@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('mahesh.babu@gmail.com', 'Mahesh Babu', '9991000030', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1980-08-09', 'MALE', le_comm_id, TRUE, 'D', '303');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'ajay.devgn@gmail.com') THEN
        INSERT INTO app_user (email, full_name, phone, role, role_id, kyc_status, password_hash, date_of_birth, gender, community_id, is_active, block, flat_no)
        VALUES ('ajay.devgn@gmail.com', 'Ajay Devgn', '9991000031', 'MEMBER', member_role_id, 'VERIFIED', hash_val, '1978-03-02', 'MALE', le_comm_id, TRUE, 'D', '401');
    END IF;

    -- Sync to legacy/Cognito user table
    INSERT INTO users (id, cognito_sub, full_name, email, phone_number, kyc_status, password_hash, role_id)
    SELECT CAST(u.id AS VARCHAR), 'mock-cognito-' || u.id, u.full_name, u.email, u.phone, u.kyc_status, u.password_hash, u.role_id
    FROM app_user u
    ON CONFLICT (email) DO NOTHING;

END $$;
