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
MAPS_API_KEY=tu_clave_de_google_maps
```

La app compila sin ellas, pero no cargará eventos de Ticketmaster ni el mapa.

## Ejecutar

```bash
./gradlew assembleDebug
```
