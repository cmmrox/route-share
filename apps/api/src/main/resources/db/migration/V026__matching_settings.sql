-- Admin-tunable route matching parameters (single-row config). Defaults mirror the previous
-- hard-coded constants so behaviour is unchanged until an admin updates them.
CREATE TABLE routing.matching_settings (
  matching_settings_id            INT PRIMARY KEY DEFAULT 1,
  default_search_radius_meters    INT NOT NULL DEFAULT 1000,
  max_search_radius_meters        INT NOT NULL DEFAULT 5000,
  default_departure_window_minutes INT NOT NULL DEFAULT 120,
  max_departure_window_minutes    INT NOT NULL DEFAULT 720,
  updated_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT matching_settings_singleton CHECK (matching_settings_id = 1)
);

INSERT INTO routing.matching_settings (matching_settings_id) VALUES (1)
ON CONFLICT (matching_settings_id) DO NOTHING;
