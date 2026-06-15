# GlycemIQ

Aplicación Android profesional para el monitoreo inteligente de glucosa, orientada a personas mayores.

## Características

- Registro de glucosa con clasificación automática (bajo, normal, alto)
- Gestión de medicamentos con alarmas exactas (AlarmManager)
- Recomendaciones inteligentes según niveles glucémicos
- Gráficas por registro, promedio diario y semanal
- Generación de reportes clínicos en PDF
- Interfaz accesible con tipografía grande y alto contraste

## Stack tecnológico

- Kotlin
- MVVM + StateFlow
- Jetpack Compose + Material Design 3
- Supabase (PostgreSQL REST API)
- Hilt
- AlarmManager
- PdfDocument (Android PDF)

## Requisitos

- Android Studio Ladybug o superior
- JDK 17
- Android SDK 35
- minSdk 26

## Base de datos Supabase

1. Abre el [SQL Editor de Supabase](https://supabase.com/dashboard) en tu proyecto
2. Ejecuta el script `supabase/schema.sql`
3. Configura en `local.properties` (opcional):

```
SUPABASE_URL=https://gggfcecvukanwogbinaf.supabase.co
SUPABASE_KEY=sb_publishable_k0m-twsbHF_NKauP0WPvTw_Ec9VEOTw
```

## Compilación

```bash
./gradlew assembleDebug
```

## Configuración regional

- Idioma: Español
- Zona horaria: America/Mexico_City
- Formato de fecha: dd/MM/yyyy
- Formato de hora: HH:mm

## Estructura

```
app/src/main/java/com/glycemiq/
├── data/          # Room, DAOs, Repositories
├── di/            # Hilt modules
├── domain/        # Modelos de dominio
├── notification/  # Alarmas y notificaciones
├── pdf/           # Generador de reportes
├── ui/            # Compose UI, ViewModels, Theme
└── util/          # Utilidades de fecha/hora
```

## Licencia

Proyecto académico — GlycemIQ © 2026
