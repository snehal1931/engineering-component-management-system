-- ============================================================
-- EDCMS Database Setup Script
-- Run this ONCE before starting the application.
--
-- Usage:
--   mysql -u root -p < create_database.sql
--   OR paste into MySQL Workbench / DBeaver and execute.
-- ============================================================

-- Create the database (if it doesn't exist)
CREATE DATABASE IF NOT EXISTS edcms_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Create a dedicated application user (recommended for security)
-- Replace 'your_secure_password' with a real password
-- Skip this block if you're using root for local development
CREATE USER IF NOT EXISTS 'edcms_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON edcms_db.* TO 'edcms_user'@'localhost';
FLUSH PRIVILEGES;

-- Switch to the application database
USE edcms_db;

-- Verify setup
SELECT 'Database edcms_db created and user configured successfully.' AS status;

-- NOTE: Hibernate will auto-generate all tables on first startup
-- because application.yml has:  jpa.hibernate.ddl-auto: update
--
-- The expected tables after first startup:
--   users
--   projects
--   components           ← base table for JOINED inheritance
--   mechanical_components
--   electrical_components
--   software_components
--   component_versions
--   assemblies
--   assembly_components
--   dependencies
--   compatibility_rules
