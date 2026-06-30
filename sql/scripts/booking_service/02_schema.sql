DROP TABLE IF EXISTS payment_obligations;
DROP TABLE IF EXISTS bookings;

CREATE TABLE bookings (
       id UUID DEFAULT gen_random_uuid(),
       court_id UUID NOT NULL, -- references courts.id in the Court Service; no FK across service boundaries
       time_slot_id UUID NOT NULL, -- references time_slots.id in the Court Service; no FK across service boundaries
       booking_date DATE NOT NULL,
       booker_member_id UUID NOT NULL, -- references members.id in the Member Service; no FK across service boundaries
       opponent_member_id UUID, -- if opponent is also a member, references members.id in the Member Service; no FK across service boundaries
       status VARCHAR(25) NOT NULL,
       court_fee DECIMAL(10, 2) NOT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
       cancelled_at TIMESTAMP,
       CONSTRAINT pk_bookings PRIMARY KEY (id),
       CONSTRAINT chk_booking_status CHECK (status IN ('CONFIRMED', 'CANCELLED_FULL_REFUND', 'CANCELLED_NO_REFUND')),
       CONSTRAINT chk_court_fee_positive CHECK (court_fee > 0),
       CONSTRAINT chk_cancelled_at_consistency CHECK (
                  (status = 'CONFIRMED' AND cancelled_at IS NULL) OR
                  (status != 'CONFIRMED' AND cancelled_at IS NOT NULL)
       )
);

-- Partial unique index: prevents two different members both booking the same court/slot/date,
-- but only among CONFIRMED bookings so a cancelled slot can be rebooked
CREATE UNIQUE INDEX uq_court_slot_date_active
ON bookings (court_id, time_slot_id, booking_date)
WHERE status = 'CONFIRMED';

-- Partial unique index: prevents a member from booking more than one court for the same exact time slot
-- but does not catch overlapping slots, e.g, 06:45 - 07:30 and 07:15 - 08:00
CREATE UNIQUE INDEX uq_member_slot_date_active
ON bookings (booker_member_id, time_slot_id, booking_date)
WHERE status = 'CONFIRMED';

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
