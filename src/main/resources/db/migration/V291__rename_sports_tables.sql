-- Sports-table renames already handled by V119 with conditional IF-EXISTS logic.
-- V119 runs at version 119, before this script, and covers every rename listed here.
-- This script is a no-op to avoid failing on a fresh database where the old names never existed.
SELECT 1;
