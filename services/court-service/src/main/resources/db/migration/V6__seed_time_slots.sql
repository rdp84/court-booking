INSERT INTO time_slots (court_id, slot_start)
SELECT
    c.id,
    slot_start::TIME
FROM courts c
CROSS JOIN generate_series (
      CASE
        WHEN c.name IN ('Court 1', 'Court 2')
        THEN '2000-01-01 06:45'::TIMESTAMP
        ELSE '2000-01-01 07:15'::TIMESTAMP
      END,
      CASE
        WHEN c.name IN ('Court 1', 'Court 2')
        THEN '2000-01-01 21:45'::TIMESTAMP
        ELSE '2000-01-01 22:30'::TIMESTAMP
      END,
      INTERVAL '45 minutes'
) AS slot_start;
