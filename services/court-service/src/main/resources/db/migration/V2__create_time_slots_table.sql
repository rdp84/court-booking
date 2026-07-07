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
