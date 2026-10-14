-- Control: Audit Logging
-- Append-only security event log for the MotorPH Payroll System.

CREATE TABLE IF NOT EXISTS `audit_log` (
  `auditId`    INT          NOT NULL AUTO_INCREMENT,
  `eventTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `email`      VARCHAR(100) DEFAULT NULL,
  `employeeId` INT          DEFAULT NULL,
  `eventType`  VARCHAR(50)  NOT NULL,
  `outcome`    ENUM('SUCCESS','FAILURE') NOT NULL,
  `details`    VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`auditId`),
  KEY `idx_audit_eventTime` (`eventTime`),
  KEY `idx_audit_email` (`email`),
  KEY `idx_audit_eventType` (`eventType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Note: employeeId is deliberately NOT a foreign key to employee.
-- Audit records must survive deletion of the account they describe,
-- and failed logins for unknown emails have no employeeId to reference.