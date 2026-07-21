CREATE TABLE payment_obligations (
       id UUID DEFAULT gen_random_uuid(),
       booking_id UUID NOT NULL,
       member_id UUID NOT NULL, -- the opponent who owes their share. References members.id in the Member Service; no FK across service boundaries
       amount DECIMAL(10, 2) NOT NULL,
       status VARCHAR(10) NOT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
       CONSTRAINT pk_payment_obligations PRIMARY KEY (id),
       CONSTRAINT fk_payment_obligations_bookings FOREIGN KEY (booking_id) REFERENCES bookings(id),
       CONSTRAINT chk_amount_positive CHECK (amount > 0),
       CONSTRAINT chk_payment_obligations_status CHECK (status IN ('PENDING', 'PAID', 'WAIVED'))
);
