-- !Ups
CREATE TABLE TOKEN_PAYMENT (
    ADDRESS VARCHAR(255) NOT NULL,
    ERG_AMOUNT BIGINT NOT NULL,
    TYPE_TOKENS   VARCHAR(255) NOT NULL,
    TXID   VARCHAR(255) NOT NULL,
    USERNAME   VARCHAR(255) NOT NULL,
    CREATED_TIME  TIMESTAMP NOT NULL,
    DONE  BOOLEAN NOT NULL DEFAULT false,
    CHECK (("ERG_AMOUNT" >= 0)),
    CONSTRAINT USER_TOKEN PRIMARY KEY (USERNAME, TYPE_TOKENS)
);

CREATE TABLE USER (
    DISCORD_ID VARCHAR(255) NOT NULL,
    USERNAME VARCHAR(255) NOT NULL,
    EMAIL VARCHAR(255) NOT NULL,
    VERIFIED  BOOLEAN NOT NULL,
    PRIMARY KEY (USERNAME)
);

CREATE TABLE SESSION (
    USERNAME VARCHAR(255) NOT NULL,
    ACCESS_TOKEN VARCHAR(255) NOT NULL,
    REFRESH_TOKEN VARCHAR(255) NOT NULL,
    TOKEN_TYPE VARCHAR(255) NOT NULL,
    EXPIRES_IN  TIMESTAMP NOT NULL,
    SCOPE VARCHAR(255) NOT NULL,
    CONSTRAINT SESSION_USER_TOKEN PRIMARY KEY (USERNAME, ACCESS_TOKEN)
);

-- !Downs
DROP TABLE TOKEN_PAYMENT;
DROP TABLE USER;
DROP TABLE SESSION;
