CREATE TABLE routing.route_schedule_rule (
  route_schedule_rule_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  route_plan_id BIGINT NOT NULL REFERENCES routing.route_plan(route_plan_id),
  schedule_type TEXT NOT NULL DEFAULT 'ONE_TIME' CHECK (schedule_type IN ('ONE_TIME','RECURRING')),
  start_at TIMESTAMPTZ NOT NULL,
  end_at TIMESTAMPTZ,
  days_of_week TEXT[] NOT NULL DEFAULT '{}',
  status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','PAUSED','CANCELLED','EXPIRED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (end_at IS NULL OR end_at >= start_at)
);
CREATE INDEX route_schedule_rule_plan_idx ON routing.route_schedule_rule(route_plan_id);
CREATE INDEX route_schedule_rule_status_start_idx ON routing.route_schedule_rule(status, start_at);

CREATE TABLE routing.route_occurrence (
  route_occurrence_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  route_plan_id BIGINT NOT NULL REFERENCES routing.route_plan(route_plan_id),
  scheduled_departure_at TIMESTAMPTZ NOT NULL,
  available_seats INTEGER NOT NULL CHECK (available_seats >= 0),
  status TEXT NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('PUBLISHED','CANCELLED','STARTED','COMPLETED','EXPIRED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(route_plan_id, scheduled_departure_at)
);
CREATE INDEX route_occurrence_status_departure_idx ON routing.route_occurrence(status, scheduled_departure_at);
CREATE INDEX route_occurrence_plan_status_idx ON routing.route_occurrence(route_plan_id, status);

CREATE TABLE routing.route_bucket_cell (
  route_bucket_cell_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  route_plan_id BIGINT NOT NULL REFERENCES routing.route_plan(route_plan_id),
  route_occurrence_id BIGINT REFERENCES routing.route_occurrence(route_occurrence_id),
  bucket_resolution INTEGER NOT NULL CHECK (bucket_resolution BETWEEN 1 AND 12),
  bucket_cell TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(route_occurrence_id, bucket_resolution, bucket_cell)
);
CREATE INDEX route_bucket_cell_lookup_idx ON routing.route_bucket_cell(bucket_resolution, bucket_cell);
CREATE INDEX route_bucket_cell_occurrence_idx ON routing.route_bucket_cell(route_occurrence_id);
