-- GlycemIQ - Realtime + zona horaria México
-- Ejecutar en Supabase Dashboard → SQL Editor (después de schema.sql)

-- Habilitar Realtime en las tablas
ALTER PUBLICATION supabase_realtime ADD TABLE glucose_records;
ALTER PUBLICATION supabase_realtime ADD TABLE medications;

-- Columna legible con hora de México
ALTER TABLE glucose_records
    ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMPTZ;

-- Sincronizar timestamp y recorded_at en zona America/Mexico_City
CREATE OR REPLACE FUNCTION sync_glucose_mexico_time()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.recorded_at IS NULL THEN
        NEW.recorded_at := timezone('America/Mexico_City', NOW());
    END IF;

    IF NEW.timestamp IS NULL OR NEW.timestamp = 0 THEN
        NEW.timestamp := (EXTRACT(EPOCH FROM NEW.recorded_at) * 1000)::BIGINT;
    ELSE
        NEW.recorded_at := timezone(
            'America/Mexico_City',
            to_timestamp(NEW.timestamp / 1000.0)
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS glucose_mexico_time_trigger ON glucose_records;
CREATE TRIGGER glucose_mexico_time_trigger
    BEFORE INSERT OR UPDATE ON glucose_records
    FOR EACH ROW
    EXECUTE FUNCTION sync_glucose_mexico_time();

-- Actualizar filas existentes
UPDATE glucose_records
SET recorded_at = timezone('America/Mexico_City', to_timestamp(timestamp / 1000.0))
WHERE recorded_at IS NULL;
