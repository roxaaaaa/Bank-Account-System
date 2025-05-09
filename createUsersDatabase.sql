SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";

DROP DATABASE IF EXISTS `bank_system`;

CREATE DATABASE `bank_system` ;

USE `bank_system`;

CREATE TABLE customers (
  accountNo VARCHAR(25) NOT NULL PRIMARY KEY,
  encrypted_password VARBINARY(256) NOT NULL,
  salt VARBINARY(256) NOT NULL,
  balance FLOAT NOT NULL
);

ALTER TABLE customers 
ADD COLUMN mfa_enabled BOOLEAN DEFAULT FALSE,
ADD COLUMN mfa_secret VARCHAR(255);

ALTER TABLE `customers`
  ADD UNIQUE KEY `accountNo` (`accountNo`);
COMMIT;
SELECT * from customers;