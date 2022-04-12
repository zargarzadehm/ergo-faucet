-- !Ups
ALTER TABLE TOKEN_PAYMENT_ARCHIVE DROP CONSTRAINT USER_TOKEN_ARCHIVE;
ALTER TABLE TOKEN_PAYMENT_ARCHIVE ADD CONSTRAINT USER_TOKEN_ARCHIVE PRIMARY KEY (USERNAME, TYPE_TOKENS, CREATED_TIME)

-- !Downs
ALTER TABLE TOKEN_PAYMENT_ARCHIVE DROP CONSTRAINT USER_TOKEN_ARCHIVE;
ALTER TABLE TOKEN_PAYMENT_ARCHIVE ADD CONSTRAINT USER_TOKEN_ARCHIVE PRIMARY KEY (USERNAME, TYPE_TOKENS)