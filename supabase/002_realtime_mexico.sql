-- GlycemIQ - Habilitar Realtime
-- Ejecutar en Supabase Dashboard → SQL Editor (después de schema.sql)

ALTER PUBLICATION supabase_realtime ADD TABLE glucose_records;
ALTER PUBLICATION supabase_realtime ADD TABLE medications;
