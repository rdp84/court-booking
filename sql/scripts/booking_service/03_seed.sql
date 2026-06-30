TRUNCATE TABLE payment_obligations, bookings;

-- Alice booking to play Bob at 06:45 on court 1
WITH inserted_confirmed_booking AS (
     INSERT INTO bookings (court_id, time_slot_id, booking_date, booker_member_id, opponent_member_id, status, court_fee)
     VALUES (
       'ab2e6111-9146-46b8-934d-f175b7aee988',
       '553020f4-79f3-4c9f-aa10-b3caaa55cf9a',
       '2026-07-02',
       '4238780b-f706-45be-96f4-40148ba5e9f8',
       '1aa1eb15-bc93-4e1f-80cb-0fdf81df4800',
       'CONFIRMED',
       3.00
     )
     RETURNING id, opponent_member_id
)
INSERT INTO payment_obligations (booking_id, member_id, amount, status)
SELECT id, opponent_member_id, 1.50, 'PENDING'
FROM inserted_confirmed_booking;

-- Carla booking to play a guest at 18:30 on court 3
INSERT INTO bookings (court_id, time_slot_id, booking_date, booker_member_id, status, court_fee)
VALUES (
       '0ca78aab-6d0e-4c47-b8a4-49aaee7404a7',
       '0337e7e3-1ab4-4787-be3f-3c6332029c56',
       '2026-07-08',
       'c9b4f3ac-b557-487f-bf57-836c9d3f9b6b',
       'CONFIRMED',
       6.00
);

-- David cancelling their booking with Alice on court 4 at 18:30
WITH inserted_cancelled_booking AS (
     INSERT INTO bookings (court_id, time_slot_id, booking_date, booker_member_id, opponent_member_id, status, court_fee, created_at, cancelled_at)
     VALUES (
       'c790769e-ad48-47b5-809e-734218608eec',
       '94033d5d-5191-4cc4-9cac-b178f158c00b',
       '2026-07-09',
       '544902fa-375c-438e-993a-7ea521232e57',
       '4238780b-f706-45be-96f4-40148ba5e9f8',
       'CANCELLED_FULL_REFUND',
       6.00,
       NOW() - INTERVAL '4 HOURS',
       NOW()
     )
     RETURNING id, opponent_member_id
)
INSERT INTO payment_obligations (booking_id, member_id, amount, status)
SELECT id, opponent_member_id, 3.00, 'WAIVED'
FROM inserted_cancelled_booking;

-- Carla booking with Alice, weekday at 18:00 on court 2
WITH inserted_confirmed_booking AS (
     INSERT INTO bookings (court_id, time_slot_id, booking_date, booker_member_id, opponent_member_id, status, court_fee)
     VALUES (
       '2597b7d1-dfe3-4172-a798-747fb57147d4',
       'b4922260-9710-4bc2-ad2a-66b54c622e2b',
       '2026-07-13',
       'c9b4f3ac-b557-487f-bf57-836c9d3f9b6b',
       '4238780b-f706-45be-96f4-40148ba5e9f8',
       'CONFIRMED',
       6.00
     )
     RETURNING id, opponent_member_id
)
INSERT INTO payment_obligations (booking_id, member_id, amount, status)
SELECT id, opponent_member_id, 3.00, 'PENDING'
FROM inserted_confirmed_booking;
