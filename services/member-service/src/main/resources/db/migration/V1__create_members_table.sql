CREATE TABLE members (
       id UUID DEFAULT gen_random_uuid(),
       name VARCHAR(100) NOT NULL,
       email VARCHAR(255) NOT NULL,
       password_hash VARCHAR(255) NOT NULL,
       account_balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
       membership_start_date DATE NOT NULL DEFAULT CURRENT_DATE,
       membership_end_date DATE NOT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
       CONSTRAINT pk_members PRIMARY KEY (id),
       CONSTRAINT uq_email UNIQUE (email),
       CONSTRAINT chk_membership_dates CHECK (membership_end_date > membership_start_date),
       CONSTRAINT chk_balance_non_negative CHECK (account_balance >= 0)
);
