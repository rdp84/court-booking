TRUNCATE TABLE account_transactions, members;

WITH inserted_members AS (
     INSERT INTO members (name, email, password_hash, membership_start_date, membership_end_date)
     VALUES
        ('Alice Johnson', 'alice.johnson@example.com', 'placeholder_hash', '2026-01-14', '2027-01-13'),
        ('Bob Smith', 'bob.smith@example.com', 'placeholder_hash', '2025-09-15', '2026-09-14'),
        ('Carla Diaz', 'carla.diaz@example.com', 'placeholder_hash', '2026-02-18', '2027-02-17'),
        ('David Chen', 'david.chen@example.com', 'placeholder_hash', '2025-12-02', '2026-12-01'),
        ('Emma Wright', 'emma.wright@example.com', 'placeholder_hash', '2025-05-27', '2026-05-26')
     RETURNING id, name
)
INSERT INTO account_transactions (member_id, amount, transaction_type)
SELECT id, 20.00, 'TOP_UP'
FROM inserted_members;

UPDATE members m
SET account_balance = (
    SELECT COALESCE(SUM(amount), 0)
    FROM account_transactions t
    WHERE t.member_id = m.id
);
