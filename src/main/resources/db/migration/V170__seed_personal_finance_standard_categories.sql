-- V161: Seed Standard Money Manager Personal Finance Categories
-- Global system defaults (user_id IS NULL) accessible to all residents

INSERT INTO personal_finance_categories (id, user_id, name, icon, color, type, parent_id, created_at)
VALUES
  -- ── Expense Categories ──
  ('cat-food', NULL, 'Food & Dining', 'restaurant-outline', '#F97316', 'EXPENSE', NULL, NOW()),
  ('sub-food-dining', NULL, 'Dining Out', 'restaurant-outline', '#F97316', 'EXPENSE', 'cat-food', NOW()),
  ('sub-food-delivery', NULL, 'Food Delivery', 'bicycle-outline', '#F97316', 'EXPENSE', 'cat-food', NOW()),
  ('sub-food-cafe', NULL, 'Cafes & Snacks', 'cafe-outline', '#F97316', 'EXPENSE', 'cat-food', NOW()),

  ('cat-groceries', NULL, 'Groceries', 'cart-outline', '#10B981', 'EXPENSE', NULL, NOW()),
  ('sub-groc-supermarket', NULL, 'Supermarket', 'basket-outline', '#10B981', 'EXPENSE', 'cat-groceries', NOW()),
  ('sub-groc-veg', NULL, 'Fruits & Vegetables', 'leaf-outline', '#10B981', 'EXPENSE', 'cat-groceries', NOW()),
  ('sub-groc-dairy', NULL, 'Dairy & Bakery', 'nutrition-outline', '#10B981', 'EXPENSE', 'cat-groceries', NOW()),

  ('cat-travel', NULL, 'Travel & Commute', 'airplane-outline', '#06B6D4', 'EXPENSE', NULL, NOW()),
  ('sub-travel-flights', NULL, 'Flights & Trains', 'airplane-outline', '#06B6D4', 'EXPENSE', 'cat-travel', NOW()),
  ('sub-travel-cab', NULL, 'Cabs & Auto', 'car-outline', '#06B6D4', 'EXPENSE', 'cat-travel', NOW()),
  ('sub-travel-hotel', NULL, 'Hotels & Stay', 'bed-outline', '#06B6D4', 'EXPENSE', 'cat-travel', NOW()),

  ('cat-fuel', NULL, 'Fuel & Vehicle', 'speedometer-outline', '#EAB308', 'EXPENSE', NULL, NOW()),
  ('sub-fuel-petrol', NULL, 'Petrol / Diesel', 'speedometer-outline', '#EAB308', 'EXPENSE', 'cat-fuel', NOW()),
  ('sub-fuel-ev', NULL, 'EV Charging', 'flash-outline', '#EAB308', 'EXPENSE', 'cat-fuel', NOW()),
  ('sub-fuel-service', NULL, 'Vehicle Service', 'construct-outline', '#EAB308', 'EXPENSE', 'cat-fuel', NOW()),

  ('cat-shopping', NULL, 'Shopping', 'bag-handle-outline', '#8B5CF6', 'EXPENSE', NULL, NOW()),
  ('sub-shop-clothing', NULL, 'Clothing & Footwear', 'shirt-outline', '#8B5CF6', 'EXPENSE', 'cat-shopping', NOW()),
  ('sub-shop-elec', NULL, 'Electronics & Gadgets', 'phone-portrait-outline', '#8B5CF6', 'EXPENSE', 'cat-shopping', NOW()),
  ('sub-shop-home', NULL, 'Home & Furniture', 'home-outline', '#8B5CF6', 'EXPENSE', 'cat-shopping', NOW()),

  ('cat-education', NULL, 'Education', 'school-outline', '#6366F1', 'EXPENSE', NULL, NOW()),
  ('sub-edu-fees', NULL, 'School / Tuition Fees', 'school-outline', '#6366F1', 'EXPENSE', 'cat-education', NOW()),
  ('sub-edu-courses', NULL, 'Online Courses & Certs', 'laptop-outline', '#6366F1', 'EXPENSE', 'cat-education', NOW()),
  ('sub-edu-books', NULL, 'Books & Stationery', 'book-outline', '#6366F1', 'EXPENSE', 'cat-education', NOW()),

  ('cat-healthcare', NULL, 'Healthcare', 'medkit-outline', '#EF4444', 'EXPENSE', NULL, NOW()),
  ('sub-health-doc', NULL, 'Doctor & Consultation', 'fitness-outline', '#EF4444', 'EXPENSE', 'cat-healthcare', NOW()),
  ('sub-health-meds', NULL, 'Medicines & Pharmacy', 'medkit-outline', '#EF4444', 'EXPENSE', 'cat-healthcare', NOW()),
  ('sub-health-lab', NULL, 'Lab Tests & Diagnostics', 'flask-outline', '#EF4444', 'EXPENSE', 'cat-healthcare', NOW()),

  ('cat-entertainment', NULL, 'Entertainment', 'film-outline', '#EC4899', 'EXPENSE', NULL, NOW()),
  ('sub-ent-movies', NULL, 'Movies & Events', 'film-outline', '#EC4899', 'EXPENSE', 'cat-entertainment', NOW()),
  ('sub-ent-games', NULL, 'Gaming & Outings', 'game-controller-outline', '#EC4899', 'EXPENSE', 'cat-entertainment', NOW()),

  ('cat-utilities', NULL, 'Utilities & Bills', 'receipt-outline', '#F59E0B', 'EXPENSE', NULL, NOW()),
  ('sub-util-elec', NULL, 'Electricity', 'flash-outline', '#F59E0B', 'EXPENSE', 'cat-utilities', NOW()),
  ('sub-util-wifi', NULL, 'Internet / Broadband', 'wifi-outline', '#F59E0B', 'EXPENSE', 'cat-utilities', NOW()),
  ('sub-util-water', NULL, 'Water & Gas', 'water-outline', '#F59E0B', 'EXPENSE', 'cat-utilities', NOW()),
  ('sub-util-mobile', NULL, 'Mobile Recharge', 'call-outline', '#F59E0B', 'EXPENSE', 'cat-utilities', NOW()),

  ('cat-rent', NULL, 'Rent & Housing', 'key-outline', '#0D9488', 'EXPENSE', NULL, NOW()),
  ('sub-rent-house', NULL, 'House Rent', 'home-outline', '#0D9488', 'EXPENSE', 'cat-rent', NOW()),
  ('sub-rent-maint', NULL, 'Society Maintenance', 'business-outline', '#0D9488', 'EXPENSE', 'cat-rent', NOW()),

  ('cat-insurance', NULL, 'Insurance', 'shield-checkmark-outline', '#14B8A6', 'EXPENSE', NULL, NOW()),
  ('sub-ins-health', NULL, 'Health Insurance', 'shield-outline', '#14B8A6', 'EXPENSE', 'cat-insurance', NOW()),
  ('sub-ins-life', NULL, 'Life Insurance / Term', 'heart-outline', '#14B8A6', 'EXPENSE', 'cat-insurance', NOW()),
  ('sub-ins-vehicle', NULL, 'Vehicle Insurance', 'car-sport-outline', '#14B8A6', 'EXPENSE', 'cat-insurance', NOW()),

  ('cat-emi', NULL, 'EMI & Loans', 'card-outline', '#DC2626', 'EXPENSE', NULL, NOW()),
  ('sub-emi-home', NULL, 'Home Loan EMI', 'home-outline', '#DC2626', 'EXPENSE', 'cat-emi', NOW()),
  ('sub-emi-car', NULL, 'Car Loan EMI', 'car-outline', '#DC2626', 'EXPENSE', 'cat-emi', NOW()),
  ('sub-emi-personal', NULL, 'Personal Loan EMI', 'person-outline', '#DC2626', 'EXPENSE', 'cat-emi', NOW()),

  ('cat-subscriptions', NULL, 'Subscriptions', 'repeat-outline', '#A855F7', 'EXPENSE', NULL, NOW()),
  ('sub-sub-ott', NULL, 'OTT & Streaming', 'tv-outline', '#A855F7', 'EXPENSE', 'cat-subscriptions', NOW()),
  ('sub-sub-software', NULL, 'Software & Apps', 'code-slash-outline', '#A855F7', 'EXPENSE', 'cat-subscriptions', NOW()),
  ('sub-sub-gym', NULL, 'Gym & Memberships', 'barbell-outline', '#A855F7', 'EXPENSE', 'cat-subscriptions', NOW()),

  ('cat-other-exp', NULL, 'Other Expenses', 'ellipsis-horizontal-circle-outline', '#64748B', 'EXPENSE', NULL, NOW()),
  ('sub-oth-personal', NULL, 'Personal Care / Salon', 'cut-outline', '#64748B', 'EXPENSE', 'cat-other-exp', NOW()),
  ('sub-oth-gifts', NULL, 'Gifts & Donations', 'gift-outline', '#64748B', 'EXPENSE', 'cat-other-exp', NOW()),
  ('sub-oth-misc', NULL, 'Miscellaneous', 'help-circle-outline', '#64748B', 'EXPENSE', 'cat-other-exp', NOW()),

  -- ── Income Categories ──
  ('cat-salary', NULL, 'Salary & Wages', 'briefcase-outline', '#3B82F6', 'INCOME', NULL, NOW()),
  ('sub-sal-base', NULL, 'Base Salary', 'cash-outline', '#3B82F6', 'INCOME', 'cat-salary', NOW()),
  ('sub-sal-bonus', NULL, 'Bonus & Incentives', 'trophy-outline', '#3B82F6', 'INCOME', 'cat-salary', NOW()),

  ('cat-investments', NULL, 'Investments & Returns', 'trending-up-outline', '#10B981', 'INCOME', NULL, NOW()),
  ('sub-inv-dividends', NULL, 'Dividends & Interest', 'pie-chart-outline', '#10B981', 'INCOME', 'cat-investments', NOW()),
  ('sub-inv-capital', NULL, 'Capital Gains / Trading', 'stats-chart-outline', '#10B981', 'INCOME', 'cat-investments', NOW()),

  ('cat-freelance', NULL, 'Business & Freelance', 'laptop-outline', '#8B5CF6', 'INCOME', NULL, NOW()),
  ('sub-free-client', NULL, 'Client Invoices', 'document-text-outline', '#8B5CF6', 'INCOME', 'cat-freelance', NOW()),
  ('sub-free-consult', NULL, 'Consulting Fees', 'chatbubble-ellipses-outline', '#8B5CF6', 'INCOME', 'cat-freelance', NOW()),

  ('cat-other-inc', NULL, 'Other Income', 'wallet-outline', '#64748B', 'INCOME', NULL, NOW()),
  ('sub-inc-rent', NULL, 'Rental Income', 'key-outline', '#64748B', 'INCOME', 'cat-other-inc', NOW()),
  ('sub-inc-gifts', NULL, 'Gifts & Reimbursements', 'gift-outline', '#64748B', 'INCOME', 'cat-other-inc', NOW())
ON CONFLICT (id) DO NOTHING;
