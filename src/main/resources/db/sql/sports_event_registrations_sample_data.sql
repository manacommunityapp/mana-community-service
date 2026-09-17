-- ============================================================================
-- sports_event_registrations_sample_data.sql
-- COMMENTED OUT: Sports Event Registrations sample data is now managed and seeded via Java
-- (com.manacommunity.api.service.sample.data.SportsEventSeeder & SportsEventRegistrationDataSeeder).
-- ============================================================================
/*
-- Seeding sports_event_registration table with tournament registrations
-- Mapped under the 'Annual Summer Cricket Cup' and 'Men''s Above 19' category
DO $$
DECLARE
    event_id_val INT;
    cat_mens_id INT;
    
    -- Variables for players
    user_sandeep INT;
    user_sunil INT;
    user_ramesh INT;
    user_user1 INT;
    user_rahul INT;
    user_amit INT;
    user_vikram INT;
    user_rohit INT;
    user_karan INT;
    user_suresh INT;
    user_rajat INT;
    user_deepak INT;
    user_manish INT;
    user_arjun INT;
    user_tarun INT;
    user_nitin INT;
    user_siddharth INT;
    user_varun INT;
    user_gourav INT;
    user_abhishek INT;
    user_vishal INT;
    user_prashant INT;
    user_harsh INT;
    user_yash INT;
    user_akash INT;
    user_naveen INT;
    user_sanjay INT;
    user_mahesh INT;
    user_ajay INT;
BEGIN
    SELECT id INTO event_id_val FROM sports_event WHERE name = 'Annual Summer Cricket Cup';
    SELECT id INTO cat_mens_id FROM sports_player_category WHERE name = 'Cricket Men (Above 18)';
    
    SELECT id INTO user_sandeep FROM app_user WHERE email = 'sandeep@gmail.com';
    SELECT id INTO user_sunil FROM app_user WHERE email = 'sunil@gmail.com';
    SELECT id INTO user_ramesh FROM app_user WHERE email = 'ramesh@gmail.com';
    SELECT id INTO user_user1 FROM app_user WHERE email = 'user1@gmail.com';
    SELECT id INTO user_rahul FROM app_user WHERE email = 'rahul.sharma@gmail.com';
    SELECT id INTO user_amit FROM app_user WHERE email = 'amit.kumar@gmail.com';
    SELECT id INTO user_vikram FROM app_user WHERE email = 'vikram.singh@gmail.com';
    SELECT id INTO user_rohit FROM app_user WHERE email = 'rohit.verma@gmail.com';
    SELECT id INTO user_karan FROM app_user WHERE email = 'karan.malhotra@gmail.com';
    SELECT id INTO user_suresh FROM app_user WHERE email = 'suresh.nair@gmail.com';
    SELECT id INTO user_rajat FROM app_user WHERE email = 'rajat.bhatia@gmail.com';
    SELECT id INTO user_deepak FROM app_user WHERE email = 'deepak.pillai@gmail.com';
    SELECT id INTO user_manish FROM app_user WHERE email = 'manish.tiwari@gmail.com';
    SELECT id INTO user_arjun FROM app_user WHERE email = 'arjun.kapoor@gmail.com';
    SELECT id INTO user_tarun FROM app_user WHERE email = 'tarun.garg@gmail.com';
    SELECT id INTO user_nitin FROM app_user WHERE email = 'nitin.das@gmail.com';
    SELECT id INTO user_siddharth FROM app_user WHERE email = 'siddharth.bose@gmail.com';
    SELECT id INTO user_varun FROM app_user WHERE email = 'varun.mehta@gmail.com';
    SELECT id INTO user_gourav FROM app_user WHERE email = 'gourav.pandey@gmail.com';
    SELECT id INTO user_abhishek FROM app_user WHERE email = 'abhishek.mishra@gmail.com';
    SELECT id INTO user_vishal FROM app_user WHERE email = 'vishal.shetty@gmail.com';
    SELECT id INTO user_prashant FROM app_user WHERE email = 'prashant.kadam@gmail.com';
    SELECT id INTO user_harsh FROM app_user WHERE email = 'harsh.vardhan@gmail.com';
    SELECT id INTO user_yash FROM app_user WHERE email = 'yash.chopra@gmail.com';
    SELECT id INTO user_akash FROM app_user WHERE email = 'akash.ambani@gmail.com';
    SELECT id INTO user_naveen FROM app_user WHERE email = 'naveen.kumar@gmail.com';
    SELECT id INTO user_sanjay FROM app_user WHERE email = 'sanjay.dutt@gmail.com';
    SELECT id INTO user_mahesh FROM app_user WHERE email = 'mahesh.babu@gmail.com';
    SELECT id INTO user_ajay FROM app_user WHERE email = 'ajay.devgn@gmail.com';

    IF event_id_val IS NOT NULL AND cat_mens_id IS NOT NULL THEN
        -- Core Player registrations (REGISTERED status)
        IF user_sandeep IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_sandeep AND player_name = 'Sandeep Kamarapu') THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_sandeep, cat_mens_id, 'SINGLES', 'REGISTERED', 'Sandeep Kamarapu', 36, 'B 806', 'All-rounder', CURRENT_TIMESTAMP);
        END IF;

        IF user_sunil IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_sunil AND player_name = 'Sunil Kanthala') THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_sunil, cat_mens_id, 'SINGLES', 'REGISTERED', 'Sunil Kanthala', 36, 'C 212', 'Batsman', CURRENT_TIMESTAMP);
        END IF;

        IF user_ramesh IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_ramesh AND player_name = 'Ramesh Korlakunta') THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_ramesh, cat_mens_id, 'SINGLES', 'REGISTERED', 'Ramesh Korlakunta', 36, 'B 907', 'Bowler', CURRENT_TIMESTAMP);
        END IF;

        IF user_user1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_user1 AND player_name = 'user1') THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_user1, cat_mens_id, 'SINGLES', 'REGISTERED', 'user1', 36, 'D 105', 'Wicket Keeper', CURRENT_TIMESTAMP);
        END IF;

        -- Block A (CONFIRMED status)
        IF user_rahul IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_rahul) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_rahul, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Rahul Sharma', 38, 'A 101', 'All-rounder', CURRENT_TIMESTAMP);
        END IF;

        IF user_amit IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_amit) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_amit, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Amit Kumar', 41, 'A 103', 'Bowler', CURRENT_TIMESTAMP);
        END IF;

        IF user_vikram IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_vikram) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_vikram, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Vikram Singh', 36, 'A 201', 'Batsman', CURRENT_TIMESTAMP);
        END IF;

        IF user_rohit IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_rohit) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_rohit, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Rohit Verma', 44, 'A 203', 'All-rounder', CURRENT_TIMESTAMP);
        END IF;

        IF user_karan IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_karan) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_karan, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Karan Malhotra', 32, 'A 301', 'Bowler', CURRENT_TIMESTAMP);
        END IF;

        IF user_suresh IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_suresh) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_suresh, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Suresh Nair', 48, 'A 303', 'Wicket Keeper', CURRENT_TIMESTAMP);
        END IF;

        -- Block B (CONFIRMED status)
        IF user_rajat IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_rajat) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_rajat, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Rajat Bhatia', 30, 'B 101', 'All-rounder', CURRENT_TIMESTAMP);
        END IF;

        IF user_deepak IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_deepak) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_deepak, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Deepak Pillai', 46, 'B 103', 'Batsman', CURRENT_TIMESTAMP);
        END IF;

        IF user_manish IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_manish) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_manish, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Manish Tiwari', 39, 'B 201', 'Bowler', CURRENT_TIMESTAMP);
        END IF;

        IF user_arjun IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_arjun) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_arjun, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Arjun Kapoor', 35, 'B 203', 'Batsman', CURRENT_TIMESTAMP);
        END IF;

        IF user_tarun IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_tarun) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_tarun, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Tarun Garg', 28, 'B 301', 'Wicket Keeper', CURRENT_TIMESTAMP);
        END IF;

        IF user_nitin IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_nitin) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_nitin, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Nitin Das', 47, 'B 303', 'All-rounder', CURRENT_TIMESTAMP);
        END IF;

        -- Block C (CONFIRMED status)
        IF user_siddharth IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_siddharth) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_siddharth, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Siddharth Bose', 33, 'C 101', 'Batsman', CURRENT_TIMESTAMP);
        END IF;

        IF user_varun IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_varun) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_varun, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Varun Mehta', 38, 'C 103', 'Bowler', CURRENT_TIMESTAMP);
        END IF;

        IF user_gourav IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_gourav) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_gourav, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Gourav Pandey', 31, 'C 201', 'All-rounder', CURRENT_TIMESTAMP);
        END IF;

        IF user_abhishek IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_abhishek) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_abhishek, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Abhishek Mishra', 40, 'C 203', 'Wicket Keeper', CURRENT_TIMESTAMP);
        END IF;

        IF user_vishal IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_vishal) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_vishal, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Vishal Shetty', 43, 'C 301', 'Batsman', CURRENT_TIMESTAMP);
        END IF;

        IF user_prashant IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_prashant) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_prashant, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Prashant Kadam', 49, 'C 303', 'Bowler', CURRENT_TIMESTAMP);
        END IF;

        -- Block D (CONFIRMED status)
        IF user_harsh IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_harsh) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_harsh, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Harsh Vardhan', 35, 'D 101', 'All-rounder', CURRENT_TIMESTAMP);
        END IF;

        IF user_yash IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_yash) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_yash, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Yash Chopra', 44, 'D 103', 'Batsman', CURRENT_TIMESTAMP);
        END IF;

        IF user_akash IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_akash) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_akash, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Akash Ambani', 36, 'D 201', 'Bowler', CURRENT_TIMESTAMP);
        END IF;

        IF user_naveen IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_naveen) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_naveen, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Naveen Kumar', 41, 'D 203', 'All-rounder', CURRENT_TIMESTAMP);
        END IF;

        IF user_sanjay IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_sanjay) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_sanjay, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Sanjay Dutt', 51, 'D 301', 'Wicket Keeper', CURRENT_TIMESTAMP);
        END IF;

        IF user_mahesh IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_mahesh) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_mahesh, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Mahesh Babu', 46, 'D 303', 'Batsman', CURRENT_TIMESTAMP);
        END IF;

        IF user_ajay IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sports_event_registration WHERE event_id = event_id_val AND user_id = user_ajay) THEN
            INSERT INTO sports_event_registration (event_id, user_id, category_id, match_type, status, player_name, age, flat_number, role, registered_at)
            VALUES (event_id_val, user_ajay, cat_mens_id, 'SINGLES', 'CONFIRMED', 'Ajay Devgn', 48, 'D 401', 'Bowler', CURRENT_TIMESTAMP);
        END IF;
    END IF;
END $$;
*/
