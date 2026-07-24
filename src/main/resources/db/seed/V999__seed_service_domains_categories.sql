-- Service Domains and Categories Seed Data
-- This file loads the default service catalog for the Community Services Platform.

-- Domains
INSERT INTO service_domain (name, slug, icon, description, display_order, active, created_at, updated_at)
VALUES
('Home Services', 'home-services', '🏠', 'Residential maintenance and repair services', 1, true, NOW(), NOW()),
('Fitness & Wellness', 'fitness-wellness', '🏋', 'Health, fitness, and wellness services', 2, true, NOW(), NOW()),
('Healthcare', 'healthcare', '🩺', 'Medical and healthcare services', 3, true, NOW(), NOW()),
('Education', 'education', '📚', 'Tutoring and educational services', 4, true, NOW(), NOW()),
('Sports Coaching', 'sports-coaching', '🏸', 'Sports training and coaching services', 5, true, NOW(), NOW()),
('Pet Care', 'pet-care', '🐶', 'Pet care and veterinary services', 6, true, NOW(), NOW()),
('Events & Lifestyle', 'events-lifestyle', '🎉', 'Event planning and lifestyle services', 7, true, NOW(), NOW()),
('Automobile Services', 'automobile-services', '🚗', 'Vehicle maintenance and repair services', 8, true, NOW(), NOW()),
('Maintenance & AMC', 'maintenance-amc', '🛠', 'Annual maintenance and facility upkeep', 9, true, NOW(), NOW()),
('Child Care', 'child-care', '👶', 'Childcare and activity services', 10, true, NOW(), NOW()),
('Elder Care', 'elder-care', '👵', 'Senior citizen care services', 11, true, NOW(), NOW()),
('Community Facility Maintenance', 'community-facility', '🧹', 'Common area and facility maintenance', 12, true, NOW(), NOW()),
('Business & Professional Services', 'business-professional', '🏢', 'Professional and consulting services', 13, true, NOW(), NOW());

-- Categories: Home Services
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Electrician', 'electrician', '⚡', 1),
    ('Plumber', 'plumber', '🔧', 2),
    ('Carpenter', 'carpenter', '🪚', 3),
    ('Painter', 'painter', '🎨', 4),
    ('Mason', 'mason', '🧱', 5),
    ('Welder', 'welder', '🔥', 6),
    ('AC Technician', 'ac-technician', '❄️', 7),
    ('Appliance Repair', 'appliance-repair', '🔌', 8),
    ('Pest Control', 'pest-control', '🐛', 9),
    ('Deep Cleaning', 'deep-cleaning', '🧽', 10),
    ('House Cleaning', 'house-cleaning', '🏠', 11),
    ('Sofa Cleaning', 'sofa-cleaning', '🛋', 12),
    ('Water Tank Cleaning', 'water-tank-cleaning', '💧', 13),
    ('Kitchen Cleaning', 'kitchen-cleaning', '🍳', 14),
    ('Bathroom Cleaning', 'bathroom-cleaning', '🚿', 15),
    ('Interior Works', 'interior-works', '🏗', 16),
    ('Renovation', 'renovation', '🏘', 17),
    ('Waterproofing', 'waterproofing', '🌧', 18),
    ('Civil Works', 'civil-works', '🏛', 19)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'home-services';

-- Categories: Fitness & Wellness
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Personal Trainer', 'personal-trainer', '💪', 1),
    ('Yoga Trainer', 'yoga-trainer', '🧘', 2),
    ('Meditation Coach', 'meditation-coach', '🕉', 3),
    ('Zumba Trainer', 'zumba-trainer', '💃', 4),
    ('Dance Instructor', 'dance-instructor', '🩰', 5),
    ('Gym Trainer', 'gym-trainer', '🏋', 6),
    ('Pilates Coach', 'pilates-coach', '🤸', 7),
    ('Nutritionist', 'nutritionist', '🥗', 8),
    ('Dietician', 'dietician', '🍎', 9),
    ('Spa Therapist', 'spa-therapist', '💆', 10),
    ('Massage Therapist', 'massage-therapist', '🙌', 11),
    ('Beauty Services', 'beauty-services', '💅', 12),
    ('Salon at Home', 'salon-at-home', '💇', 13),
    ('Hair Stylist', 'hair-stylist', '✂️', 14)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'fitness-wellness';

-- Categories: Healthcare
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('General Physician', 'general-physician', '👨‍⚕️', 1),
    ('Pediatrician', 'pediatrician', '👶', 2),
    ('Physiotherapist', 'physiotherapist', '🦴', 3),
    ('Nurse', 'nurse', '👩‍⚕️', 4),
    ('Caretaker', 'caretaker', '🤝', 5),
    ('Ambulance', 'ambulance', '🚑', 6),
    ('Lab Test', 'lab-test', '🔬', 7),
    ('Blood Collection', 'blood-collection', '🩸', 8),
    ('Pharmacy Delivery', 'pharmacy-delivery', '💊', 9),
    ('Vaccination', 'vaccination', '💉', 10),
    ('Home Health Checkup', 'home-health-checkup', '🏥', 11),
    ('Medical Equipment Rental', 'medical-equipment-rental', '🩺', 12),
    ('Mental Health Counselling', 'mental-health-counselling', '🧠', 13)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'healthcare';

-- Categories: Education
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Home Tutor', 'home-tutor', '📖', 1),
    ('Coding Instructor', 'coding-instructor', '💻', 2),
    ('Music Teacher', 'music-teacher', '🎵', 3),
    ('Dance Teacher', 'dance-teacher', '💃', 4),
    ('Art Teacher', 'art-teacher', '🎨', 5),
    ('Language Trainer', 'language-trainer', '🗣', 6),
    ('Spoken English', 'spoken-english', '🇬🇧', 7),
    ('Robotics Trainer', 'robotics-trainer', '🤖', 8),
    ('Chess Coach', 'chess-coach', '♟', 9),
    ('Career Counselling', 'career-counselling', '🎯', 10),
    ('Tuition Classes', 'tuition-classes', '📝', 11)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'education';

-- Categories: Sports Coaching
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Cricket Coach', 'cricket-coach', '🏏', 1),
    ('Tennis Coach', 'tennis-coach', '🎾', 2),
    ('Swimming Coach', 'swimming-coach', '🏊', 3),
    ('Football Coach', 'football-coach', '⚽', 4),
    ('Basketball Coach', 'basketball-coach', '🏀', 5),
    ('Badminton Coach', 'badminton-coach', '🏸', 6),
    ('Skating Coach', 'skating-coach', '⛸', 7),
    ('Athletics Coach', 'athletics-coach', '🏃', 8),
    ('Table Tennis Coach', 'table-tennis-coach', '🏓', 9),
    ('Martial Arts Coach', 'martial-arts-coach', '🥋', 10),
    ('Gymnastics Coach', 'gymnastics-coach', '🤸', 11),
    ('Tournament Officials', 'tournament-officials', '📋', 12),
    ('Referees', 'referees', '🟨', 13),
    ('Umpires', 'umpires', '⚖️', 14)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'sports-coaching';

-- Categories: Pet Care
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Pet Grooming', 'pet-grooming', '🐩', 1),
    ('Veterinary Doctor', 'veterinary-doctor', '🩺', 2),
    ('Pet Walking', 'pet-walking', '🐕', 3),
    ('Pet Boarding', 'pet-boarding', '🏠', 4),
    ('Pet Sitting', 'pet-sitting', '🐱', 5),
    ('Dog Training', 'dog-training', '🦮', 6),
    ('Pet Vaccination', 'pet-vaccination', '💉', 7),
    ('Pet Taxi', 'pet-taxi', '🚕', 8),
    ('Pet Food Delivery', 'pet-food-delivery', '🦴', 9)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'pet-care';

-- Categories: Events & Lifestyle
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Catering', 'catering', '🍽', 1),
    ('Event Decoration', 'event-decoration', '🎊', 2),
    ('Photography', 'photography', '📸', 3),
    ('Videography', 'videography', '🎥', 4),
    ('DJ', 'dj', '🎧', 5),
    ('Event Planner', 'event-planner', '📋', 6),
    ('Birthday Organizer', 'birthday-organizer', '🎂', 7),
    ('Wedding Planner', 'wedding-planner', '💍', 8),
    ('Balloon Decoration', 'balloon-decoration', '🎈', 9),
    ('Sound System Rental', 'sound-system-rental', '🔊', 10),
    ('Stage Setup', 'stage-setup', '🎭', 11),
    ('Live Music', 'live-music', '🎸', 12),
    ('Community Event Management', 'community-event-management', '🏘', 13)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'events-lifestyle';

-- Categories: Automobile Services
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Car Wash', 'car-wash', '🚗', 1),
    ('Bike Wash', 'bike-wash', '🏍', 2),
    ('Car Detailing', 'car-detailing', '✨', 3),
    ('Vehicle Repair', 'vehicle-repair', '🔧', 4),
    ('Tyre Repair', 'tyre-repair', '🛞', 5),
    ('Battery Replacement', 'battery-replacement', '🔋', 6),
    ('EV Charging Assistance', 'ev-charging', '⚡', 7),
    ('Towing', 'towing', '🚛', 8),
    ('Driver On Demand', 'driver-on-demand', '🚘', 9),
    ('Vehicle Inspection', 'vehicle-inspection', '🔍', 10)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'automobile-services';

-- Categories: Maintenance & AMC
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Lift AMC', 'lift-amc', '🛗', 1),
    ('Generator AMC', 'generator-amc', '⚡', 2),
    ('Solar AMC', 'solar-amc', '☀️', 3),
    ('CCTV AMC', 'cctv-amc', '📹', 4),
    ('Fire Safety AMC', 'fire-safety-amc', '🧯', 5),
    ('Water Purifier AMC', 'water-purifier-amc', '💧', 6),
    ('STP Maintenance', 'stp-maintenance', '🏭', 7),
    ('DG Maintenance', 'dg-maintenance', '🔌', 8),
    ('Swimming Pool Maintenance', 'swimming-pool-maintenance', '🏊', 9),
    ('Garden Maintenance', 'garden-maintenance', '🌿', 10),
    ('Electrical Maintenance', 'electrical-maintenance', '💡', 11),
    ('Plumbing Maintenance', 'plumbing-maintenance', '🚰', 12),
    ('Building Maintenance', 'building-maintenance', '🏢', 13)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'maintenance-amc';

-- Categories: Child Care
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Babysitting', 'babysitting', '👶', 1),
    ('Day Care', 'day-care', '🏫', 2),
    ('Child Pickup & Drop', 'child-pickup-drop', '🚐', 3),
    ('Activity Classes', 'activity-classes', '🎨', 4),
    ('Summer Camps', 'summer-camps', '⛺', 5),
    ('Homework Assistance', 'homework-assistance', '📝', 6),
    ('Child Counselling', 'child-counselling', '🧠', 7)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'child-care';

-- Categories: Elder Care
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Elder Care Assistant', 'elder-care-assistant', '👵', 1),
    ('Nursing Care', 'nursing-care', '👩‍⚕️', 2),
    ('Medical Checkups', 'medical-checkups', '🩺', 3),
    ('Physiotherapy', 'physiotherapy', '🦴', 4),
    ('Home Visit Doctor', 'home-visit-doctor', '🏥', 5),
    ('Medicine Delivery', 'medicine-delivery', '💊', 6),
    ('Companion Services', 'companion-services', '🤝', 7),
    ('Emergency Assistance', 'emergency-assistance', '🆘', 8)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'elder-care';

-- Categories: Community Facility Maintenance
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Housekeeping', 'housekeeping', '🧹', 1),
    ('Security Guards', 'security-guards', '💂', 2),
    ('Gardening', 'gardening', '🌱', 3),
    ('Waste Collection', 'waste-collection', '🗑', 4),
    ('STP Operations', 'stp-operations', '🏭', 5),
    ('Water Tank Cleaning', 'comm-water-tank-cleaning', '💧', 6),
    ('Swimming Pool Maintenance', 'comm-swimming-pool', '🏊', 7),
    ('Lift Maintenance', 'comm-lift-maintenance', '🛗', 8),
    ('Fire Safety Inspection', 'fire-safety-inspection', '🧯', 9),
    ('CCTV Monitoring', 'cctv-monitoring', '📹', 10),
    ('Generator Operations', 'generator-operations', '⚡', 11),
    ('Electrical Maintenance', 'comm-electrical', '💡', 12),
    ('Plumbing Maintenance', 'comm-plumbing', '🚰', 13),
    ('Common Area Cleaning', 'common-area-cleaning', '🧼', 14)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'community-facility';

-- Categories: Business & Professional Services
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Chartered Accountant', 'chartered-accountant', '📊', 1),
    ('Lawyer', 'lawyer', '⚖️', 2),
    ('Tax Consultant', 'tax-consultant', '🧾', 3),
    ('Insurance Advisor', 'insurance-advisor', '🛡', 4),
    ('Financial Planner', 'financial-planner', '💰', 5),
    ('Real Estate Consultant', 'real-estate-consultant', '🏘', 6),
    ('Architect', 'architect', '📐', 7),
    ('Interior Designer', 'interior-designer', '🎨', 8),
    ('Loan Consultant', 'loan-consultant', '🏦', 9),
    ('Immigration Consultant', 'immigration-consultant', '✈️', 10)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'business-professional';
