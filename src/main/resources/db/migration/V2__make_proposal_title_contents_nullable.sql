-- V2__make_proposal_title_contents_nullable.sql
-- Allow title and contents to be NULL to support blank proposal creation (Google Docs style flow)

ALTER TABLE proposals MODIFY COLUMN title VARCHAR(100) NULL;
ALTER TABLE proposals MODIFY COLUMN contents JSON NULL;
