CREATE TABLE account_transactions (
       id UUID DEFAULT gen_random_uuid(),
       member_id UUID NOT NULL,
       amount DECIMAL(10, 2) NOT NULL,
       transaction_type VARCHAR(20) NOT NULL,
       reference_id UUID,
       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
       CONSTRAINT pk_account_transactions PRIMARY KEY (id),
       CONSTRAINT fk_account_transactions_members FOREIGN KEY (member_id) REFERENCES members(id),
       CONSTRAINT chk_transaction_type CHECK (transaction_type IN ('TOP_UP', 'BOOKING_PAYMENT', 'REFUND', 'OPPONENT_TRANSFER')),
       CONSTRAINT chk_amount_non_zero CHECK (amount <> 0)
);
