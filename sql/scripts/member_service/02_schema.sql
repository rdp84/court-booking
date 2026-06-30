DROP TABLE IF EXISTS account_transactions;
DROP TABLE IF EXISTS members;

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

CREATE TABLE account_transactions (
       id UUID DEFAULT gen_random_uuid(),
       member_id UUID NOT NULL,
       amount DECIMAL(10, 2) NOT NULL,
       transaction_type VARCHAR(20) NOT NULL,
       reference_id UUID, -- references bookings.id in the Booking Service; no FK across service boundaries
       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
       CONSTRAINT pk_account_transactions PRIMARY KEY (id),
       CONSTRAINT fk_account_transactions_members FOREIGN KEY (member_id) REFERENCES members(id),
       CONSTRAINT chk_transaction_type CHECK (transaction_type IN ('TOP_UP', 'BOOKING_PAYMENT', 'REFUND', 'OPPONENT_TRANSFER')),
       CONSTRAINT chk_amount_non_zero CHECK (amount <> 0)
);
