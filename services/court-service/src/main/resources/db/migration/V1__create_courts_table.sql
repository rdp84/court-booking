CREATE TABLE courts (
       id UUID DEFAULT gen_random_uuid(),
       name VARCHAR(50) NOT NULL,
       is_active BOOLEAN NOT NULL DEFAULT true,
       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
       CONSTRAINT pk_courts PRIMARY KEY (id)
);

