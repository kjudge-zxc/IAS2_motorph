-- Control: Account Lockout
-- Adds failed-attempt tracking and lock state to the employee table.

ALTER TABLE `employee`
  ADD COLUMN `failedAttempts` INT NOT NULL DEFAULT 0 AFTER `lastLogin`,
  ADD COLUMN `lockedUntil`    DATETIME DEFAULT NULL AFTER `failedAttempts`;