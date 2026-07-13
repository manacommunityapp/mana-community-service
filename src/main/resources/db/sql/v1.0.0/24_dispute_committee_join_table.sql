-- Replace dispute_committee_ids comma-separated string with a proper join table.
-- Required for prod (ddl-auto: validate); local=create / dev=create auto-create it.

CREATE TABLE IF NOT EXISTS sports_event_dispute_committee (
    sports_event_id BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    PRIMARY KEY (sports_event_id, user_id),
    CONSTRAINT fk_sedc_event FOREIGN KEY (sports_event_id) REFERENCES sports_event(id) ON DELETE CASCADE,
    CONSTRAINT fk_sedc_user  FOREIGN KEY (user_id)         REFERENCES app_user(id)     ON DELETE CASCADE
);

-- Migrate existing comma-separated data into the join table.
-- MySQL procedure to split the comma string per event row.
DROP PROCEDURE IF EXISTS migrate_dispute_committee;
DELIMITER $$
CREATE PROCEDURE migrate_dispute_committee()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_event_id BIGINT;
    DECLARE v_ids VARCHAR(1000);
    DECLARE cur CURSOR FOR
        SELECT id, dispute_committee_ids FROM sports_event
        WHERE dispute_committee_ids IS NOT NULL AND dispute_committee_ids <> '';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_event_id, v_ids;
        IF done THEN LEAVE read_loop; END IF;

        SET @pos = 1;
        SET @len = LENGTH(v_ids);
        WHILE @pos <= @len DO
            SET @comma = LOCATE(',', v_ids, @pos);
            IF @comma = 0 THEN SET @comma = @len + 1; END IF;
            SET @uid = CAST(TRIM(SUBSTRING(v_ids, @pos, @comma - @pos)) AS UNSIGNED);
            IF @uid > 0 THEN
                INSERT IGNORE INTO sports_event_dispute_committee (sports_event_id, user_id)
                VALUES (v_event_id, @uid);
            END IF;
            SET @pos = @comma + 1;
        END WHILE;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

CALL migrate_dispute_committee();
DROP PROCEDURE IF EXISTS migrate_dispute_committee;

-- Drop the old column after migration
ALTER TABLE sports_event DROP COLUMN dispute_committee_ids;
