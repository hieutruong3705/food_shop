-- Password reset tokens and e-mail verification codes are stored as SHA-256 hashes from now on.
-- Plain-text values already in the table cannot be hashed into something a mailed link still
-- matches, so they are retired: reset tokens are cleared, and every pending verification gets an
-- unguessable placeholder (it stays "pending", so the account can request a new link).
UPDATE users SET reset_password_token = NULL, reset_password_expires_at = NULL
 WHERE reset_password_token IS NOT NULL;
UPDATE users SET verification_code = SHA2(CONCAT(UUID(), UUID(), RAND()), 256)
 WHERE verification_code IS NOT NULL;
