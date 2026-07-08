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
