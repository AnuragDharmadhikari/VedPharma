-- V6__add_email_to_chemists_and_stockists.sql
-- Adds optional email field to chemists and stockists
-- Used for sending invoice emails
-- Nullable — existing records don't have email, and email is optional

ALTER TABLE chemists
    ADD COLUMN email VARCHAR(255);

ALTER TABLE stockists
    ADD COLUMN email VARCHAR(255);