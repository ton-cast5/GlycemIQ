-- GlycemIQ - Esquema inicial para Supabase
-- Ejecutar en: Supabase Dashboard → SQL Editor

-- Tabla de registros de glucosa
CREATE TABLE IF NOT EXISTS glucose_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL,
    value INTEGER NOT NULL CHECK (value >= 20 AND value <= 600),
    context TEXT NOT NULL CHECK (context IN ('FASTING', 'BEFORE_MEAL', 'AFTER_MEAL')),
    timestamp BIGINT NOT NULL,
    recorded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Tabla de medicamentos
CREATE TABLE IF NOT EXISTS medications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL,
    name TEXT NOT NULL,
    dose TEXT NOT NULL,
    scheduled_hour INTEGER NOT NULL CHECK (scheduled_hour >= 0 AND scheduled_hour <= 23),
    scheduled_minute INTEGER NOT NULL CHECK (scheduled_minute >= 0 AND scheduled_minute <= 59),
    interval_hours INTEGER NOT NULL DEFAULT 24 CHECK (interval_hours IN (6, 8, 12, 24)),
    recommend_for_high_glucose BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Índices para consultas por dispositivo
CREATE INDEX IF NOT EXISTS idx_glucose_records_device ON glucose_records (device_id);
CREATE INDEX IF NOT EXISTS idx_glucose_records_timestamp ON glucose_records (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_medications_device ON medications (device_id);

-- Habilitar Row Level Security
ALTER TABLE glucose_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE medications ENABLE ROW LEVEL SECURITY;

-- Políticas de acceso (clave pública / publishable)
-- La app filtra por device_id en cada consulta
CREATE POLICY "glucose_select" ON glucose_records FOR SELECT USING (true);
CREATE POLICY "glucose_insert" ON glucose_records FOR INSERT WITH CHECK (true);
CREATE POLICY "glucose_update" ON glucose_records FOR UPDATE USING (true);
CREATE POLICY "glucose_delete" ON glucose_records FOR DELETE USING (true);

CREATE POLICY "medications_select" ON medications FOR SELECT USING (true);
CREATE POLICY "medications_insert" ON medications FOR INSERT WITH CHECK (true);
CREATE POLICY "medications_update" ON medications FOR UPDATE USING (true);
CREATE POLICY "medications_delete" ON medications FOR DELETE USING (true);
