-- Snapshot of the time slot's start and end from the Court Service at time of booking, so overlapping
-- bookings (e.g. 06:45 - 07:30 and 07:15 - 08:00) can be detected without calling the Court Service
ALTER TABLE bookings
      ADD COLUMN slot_start TIME NOT NULL,
      ADD COLUMN slot_end TIME NOT NULL;
