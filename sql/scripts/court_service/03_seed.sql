
-- This populates the courts table and then returns us the id and name that were inserted
WITH inserted_courts AS (
     INSERT INTO courts (name, is_active)
     VALUES
        ('Court 1', true),
        ('Court 2', true),
        ('Court 3', true),
        ('Court 4', true)
     RETURNING id, name
)

INSERT INTO time_slots (court_id, slot_start)
SELECT
    c.id,
    slot_start::TIME -- referencing the column produced by generate_series via the alias and then casting to TIME
FROM inserted_courts c -- using the result of the Common Table Expression (CTE) above
CROSS JOIN generate_series ( -- this returns a one column table with the generated series for each of the four courts
      -- for court 1 & 2, start at 06:45, 3 & 4 start at 07:15
      CASE
        WHEN c.name IN ('Court 1', 'Court 2')
        THEN '2000-01-01 06:45'::TIMESTAMP
        ELSE '2000-01-01 07:15'::TIMESTAMP
      END,
      -- for court 1 & 2, end at 21:45, 3 & 4 end at 22:30
      CASE
        WHEN c.name IN ('Court 1', 'Court 2')
        THEN '2000-01-01 21:45'::TIMESTAMP
        ELSE '2000-01-01 22:30'::TIMESTAMP
      END,
      -- increment the series by 45 minutes
      INTERVAL '45 minutes'
) AS slot_start;

INSERT INTO court_pricing (day_type, period_start, period_end, fee, valid_from)
VALUES
    ('WEEKDAY', '06:45', '17:00', 3.00, '2026-06-29'),
    ('WEEKDAY', '17:00', '21:00', 6.00, '2026-06-29'),
    ('WEEKDAY', '21:00', '22:45', 3.00, '2026-06-29'),
    ('WEEKEND', '06:45', '22:45', 3.00, '2026-06-29');
