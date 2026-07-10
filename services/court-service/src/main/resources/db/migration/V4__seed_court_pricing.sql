INSERT INTO court_pricing (day_type, period_start, period_end, fee, valid_from)
VALUES
    ('WEEKDAY', '06:45', '17:00', 3.00, '2000-01-01'),
    ('WEEKDAY', '17:00', '21:00', 6.00, '2000-01-01'),
    ('WEEKDAY', '21:00', '22:45', 3.00, '2000-01-01'),
    ('WEEKEND', '06:45', '22:45', 3.00, '2000-01-01');
