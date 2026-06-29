DROP TABLE IF EXISTS time_slots;
DROP TABLE IF EXISTS court_pricing;
DROP TABLE IF EXISTS courts;

CREATE TABLE courts (
       id UUID DEFAULT gen_random_uuid(),
       name VARCHAR(50) NOT NULL,
       is_active BOOLEAN NOT NULL DEFAULT true,
       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
       CONSTRAINT pk_courts PRIMARY KEY (id)
);

CREATE TABLE time_slots (
       id UUID DEFAULT gen_random_uuid(),
       court_id UUID NOT NULL,
       slot_start TIME NOT NULL,
       --     slot_end TIME NOT NULL,
       slot_end TIME GENERATED ALWAYS AS (slot_start + INTERVAL '45 minutes') STORED,
       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
       CONSTRAINT pk_time_slots PRIMARY KEY (id),
       CONSTRAINT fk_time_slots_court FOREIGN KEY (court_id) REFERENCES courts(id),
       CONSTRAINT uq_court_slot UNIQUE (court_id, slot_start)
       --       CONSTRAINT chk_slot_duration CHECK (slot_end = slot_start + INTERVAL '45 minutes')
);

CREATE TABLE court_pricing (
       id UUID DEFAULT gen_random_uuid(),
       day_type VARCHAR(10) NOT NULL,
       period_start TIME NOT NULL,
       period_end TIME NOT NULL,
       fee DECIMAL(10, 2) NOT NULL,
       valid_from DATE NOT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
       CONSTRAINT pk_court_pricing PRIMARY KEY (id),
       CONSTRAINT uq_pricing_period UNIQUE (day_type, period_start, valid_from),
       CONSTRAINT chk_day_type CHECK (day_type IN ('WEEKDAY', 'WEEKEND')),
       CONSTRAINT chk_period_end CHECK (period_end > period_start),
       CONSTRAINT chk_fee CHECK (fee > 0)
);
