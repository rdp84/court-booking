-- At most one transaction of each type per member per reference (e.g. one BOOKING_PAYMENT and one REFUND per
-- booking), so a retried charge or refund from the Booking Service can't move money twice
CREATE UNIQUE INDEX uq_transaction_member_type_reference
ON account_transactions (member_id, transaction_type, reference_id)
WHERE reference_id IS NOT NULL;
