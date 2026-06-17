-- GlycemIQ - Simplificar fecha/hora: solo recorded_at (México), editable
-- Ejecutar si ya tenías tablas con timestamp, created_at o el trigger anterior

DROP TRIGGER IF EXISTS glucose_mexico_time_trigger ON glucose_records;
DROP FUNCTION IF EXISTS sync_glucose_mexico_time();

ALTER TABLE glucose_records ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMPTZ;

UPDATE glucose_records
SET recorded_at = timezone('America/Mexico_City', to_timestamp(timestamp / 1000.0))
WHERE recorded_at IS NULL
  AND timestamp IS NOT NULL;

UPDATE glucose_records
SET recorded_at = timezone('America/Mexico_City', now())
WHERE recorded_at IS NULL;

ALTER TABLE glucose_records DROP COLUMN IF EXISTS timestamp;
ALTER TABLE glucose_records DROP COLUMN IF EXISTS created_at;

ALTER TABLE glucose_records
    ALTER COLUMN recorded_at SET NOT NULL,
    ALTER COLUMN recorded_at SET DEFAULT timezone('America/Mexico_City', now());

DROP INDEX IF EXISTS idx_glucose_records_timestamp;
CREATE INDEX IF NOT EXISTS idx_glucose_records_recorded_at ON glucose_records (recorded_at DESC);

ALTER TABLE medications DROP COLUMN IF EXISTS created_at;
