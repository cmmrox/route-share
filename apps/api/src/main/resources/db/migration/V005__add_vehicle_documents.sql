CREATE TABLE vehicle.vehicle_document (
  vehicle_document_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  vehicle_id BIGINT NOT NULL REFERENCES vehicle.vehicle(vehicle_id),
  document_type TEXT NOT NULL,
  storage_key TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
  reviewed_by BIGINT REFERENCES identity.app_user(app_user_id),
  reviewed_at TIMESTAMPTZ,
  rejection_reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX vehicle_document_vehicle_idx ON vehicle.vehicle_document(vehicle_id);
