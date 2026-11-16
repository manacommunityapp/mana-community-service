-- Phase 5: Trainer Programs + Sports–Workout Bridge
-- trainer_program: curated or user-created multi-week workout programs
-- trainer_program_day: one day in the program (exercises stored as JSON text)
-- user_program_enrollment: tracks a user's progress through a program
-- sports_workout_link: links a sports booking to an auto-created workout log

CREATE TABLE IF NOT EXISTS manacommunity.trainer_program (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(120)  NOT NULL,
    description         TEXT,
    difficulty          VARCHAR(20)   NOT NULL DEFAULT 'BEGINNER',  -- BEGINNER | INTERMEDIATE | ADVANCED
    target_goal         VARCHAR(40),                                 -- WEIGHT_LOSS | STRENGTH | ENDURANCE | FLEXIBILITY | GENERAL
    duration_weeks      INT           NOT NULL DEFAULT 4,
    is_public           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by_user_id  BIGINT,
    thumbnail_url       VARCHAR(255),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS manacommunity.trainer_program_day (
    id                  BIGSERIAL PRIMARY KEY,
    program_id          BIGINT        NOT NULL REFERENCES manacommunity.trainer_program(id) ON DELETE CASCADE,
    week_number         INT           NOT NULL,
    day_number          INT           NOT NULL,
    day_name            VARCHAR(80),
    is_rest_day         BOOLEAN       NOT NULL DEFAULT FALSE,
    focus               VARCHAR(80),
    exercises           TEXT,         -- JSON array: [{exerciseName,sets,reps,durationMin,notes}]
    total_duration_min  INT,
    notes               TEXT,
    CONSTRAINT uq_program_week_day UNIQUE (program_id, week_number, day_number)
);

CREATE TABLE IF NOT EXISTS manacommunity.user_program_enrollment (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    program_id      BIGINT        NOT NULL REFERENCES manacommunity.trainer_program(id),
    start_date      DATE          NOT NULL DEFAULT CURRENT_DATE,
    current_week    INT           NOT NULL DEFAULT 1,
    current_day     INT           NOT NULL DEFAULT 1,
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | PAUSED | COMPLETED | ABANDONED
    enrolled_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    CONSTRAINT uq_user_program UNIQUE (user_id, program_id)
);

CREATE TABLE IF NOT EXISTS manacommunity.sports_workout_link (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    booking_id      BIGINT,
    workout_log_id  BIGINT        REFERENCES manacommunity.workout_log(id) ON DELETE SET NULL,
    sport_type      VARCHAR(60),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trainer_program_public ON manacommunity.trainer_program (is_public, difficulty);
CREATE INDEX IF NOT EXISTS idx_program_day_program ON manacommunity.trainer_program_day (program_id, week_number, day_number);
CREATE INDEX IF NOT EXISTS idx_enrollment_user ON manacommunity.user_program_enrollment (user_id, status);
CREATE INDEX IF NOT EXISTS idx_sports_link_user ON manacommunity.sports_workout_link (user_id);

-- Seed: 3 starter programs
INSERT INTO manacommunity.trainer_program (title, description, difficulty, target_goal, duration_weeks, is_public)
VALUES
  ('7-Day Beginner Strength', 'A one-week intro program focusing on bodyweight strength fundamentals. Perfect for first-timers.', 'BEGINNER', 'STRENGTH', 1, TRUE),
  ('21-Day Fat Burn', 'Three weeks of progressive cardio and HIIT to maximise calorie burn and boost metabolism.', 'INTERMEDIATE', 'WEIGHT_LOSS', 3, TRUE),
  ('Yoga & Flexibility', 'A four-week yoga flow program to improve flexibility, posture, and mindfulness.', 'BEGINNER', 'FLEXIBILITY', 4, TRUE)
ON CONFLICT DO NOTHING;

-- Days for Program 1 (7-Day Beginner Strength, program_id=1)
INSERT INTO manacommunity.trainer_program_day (program_id, week_number, day_number, day_name, is_rest_day, focus, exercises, total_duration_min)
SELECT p.id, 1, d.day_num, d.day_name, d.rest, d.focus, d.exercises::text, d.dur
FROM manacommunity.trainer_program p
CROSS JOIN (VALUES
  (1,'Monday',  FALSE,'Upper Body','[{"exerciseName":"Push-ups","sets":3,"reps":10,"notes":"Keep core tight"},{"exerciseName":"Dumbbell Row","sets":3,"reps":12,"notes":"Each side"},{"exerciseName":"Plank","sets":3,"durationMin":1}]',30),
  (2,'Tuesday', FALSE,'Lower Body','[{"exerciseName":"Bodyweight Squat","sets":4,"reps":15},{"exerciseName":"Reverse Lunge","sets":3,"reps":10,"notes":"Each leg"},{"exerciseName":"Glute Bridge","sets":3,"reps":15}]',30),
  (3,'Wednesday',TRUE, 'Rest Day',  '[]',0),
  (4,'Thursday',FALSE,'Core & Abs', '[{"exerciseName":"Crunches","sets":3,"reps":20},{"exerciseName":"Bicycle Crunch","sets":3,"reps":15,"notes":"Each side"},{"exerciseName":"Dead Bug","sets":3,"reps":10}]',25),
  (5,'Friday',  FALSE,'Full Body',  '[{"exerciseName":"Burpee","sets":3,"reps":8},{"exerciseName":"Mountain Climber","sets":3,"durationMin":1},{"exerciseName":"Jump Squat","sets":3,"reps":10}]',35),
  (6,'Saturday',FALSE,'Cardio',     '[{"exerciseName":"Brisk Walk","durationMin":30,"notes":"Steady pace"},{"exerciseName":"High Knees","sets":3,"durationMin":1}]',35),
  (7,'Sunday',  TRUE, 'Rest Day',   '[]',0)
) AS d(day_num, day_name, rest, focus, exercises, dur)
WHERE p.title = '7-Day Beginner Strength'
ON CONFLICT DO NOTHING;
