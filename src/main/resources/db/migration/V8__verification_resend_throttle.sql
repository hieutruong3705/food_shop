-- When the last verification e-mail was issued; a resend is allowed at most once a minute per account.
ALTER TABLE users ADD COLUMN verification_sent_at DATETIME(6) NULL;
