# CityPulse

Aplicación Android para viajeros que muestra los eventos que están ocurriendo **ahora** en la ciudad: conciertos, mercados, ferias y actividades culturales, priorizando lo que está cerca a pie y la hora del día.

Proyecto académico.

## Stack

- Kotlin + Jetpack Compose + Material 3
- Arquitectura MVVM
- Hilt, Retrofit, Room, DataStore
- Fuentes de datos: Ticketmaster Discovery API + catálogo propio en Firestore

## Requisitos

- Android Studio con JDK 17 o superior
- Android SDK 37
- minSdk 26 (Android 8.0)

## Claves de API

Añade tus claves en `local.properties` (este archivo no se sube al repositorio):

```properties
TM_API_KEY=tu_clave_de_ticketmaster
```

La app compila sin ella, pero no cargará eventos de Ticketmaster.

El mapa usa las teselas estándar de OpenStreetMap (osmdroid), que no necesitan clave.

## Firebase (catálogo propio)

1. Crea un proyecto en [Firebase](https://console.firebase.google.com) y registra una app Android con el paquete `com.citypulse`.
2. Descarga `google-services.json` y colócalo en `app/` (no se sube al repositorio).
3. Crea una base de datos Firestore en modo de prueba.
4. Carga el catálogo de ejemplo (Madrid y Bogotá):

   ```bash
   python3 scripts/seed_firestore.py
   ```

5. Publica las reglas de `firestore.rules` (solo lectura) en Firestore → Reglas.

Cada evento del catálogo es recurrente (días de la semana + horario) o de una fecha concreta; la app genera sus fechas para los próximos 7 días.

## Ejecutar

```bash
./gradlew assembleDebug
```
