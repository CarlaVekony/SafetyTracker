# SafetyTracker

## Descriere

SafetyTracker este o aplicație Android dezvoltată în Kotlin cu Jetpack Compose, care detectează și anunță situații de urgență folosind senzori integrați ale dispozitivului.

## Scop

Aplicația oferă un sistem de protecție și alertare în timp real, care monitorizează constant activitatea utilizatorului și trimite alerte către persoanele de contact în cazul detectării unor situații anormale.

## Funcționalități

### Detecție de Urgență

- **Giroscop + Accelerometru**: Detectează căzăturile și mișcări spontane de fugarire
- **Microfon**: Determină situații ambigue pe baza amplitudinii sunetului
- **GPS**: Include locația persoanei în mesajele de alertă

### Sistem de Alertare

- **SMS**: Trimite alerte către persoanele de contact (suport VoLTE/VoNR)
- **Audio**: 
  - Cu internet: înregistrează și încarcă audio pe server, trimite link prin SMS
  - Fără internet: salvează audio local
- **Locație**: Include coordonatele GPS în fiecare alertă

### Control

- Buton Start/Stop pentru activarea/dezactivarea sistemului de detecție
- Interfață intuitivă pentru gestionarea contactelor de urgență
- Monitorizare în timp real a statusului senzorilor

## Tehnologii

- **Limbaj**: Kotlin
- **UI**: Jetpack Compose
- **Baza de date**: Room (funcționare offline)
- **Arhitectură**: Clean Architecture
- **Min SDK**: 24
- **Target SDK**: 36

## Structura Proiectului

```
app/src/main/java/com/example/safetytracker/
├── ui/                    # Interfața utilizatorului
│   ├── components/       # Componente Compose reutilizabile
│   ├── screens/          # Ecranele aplicației
│   └── AppTheme.kt       # Tema aplicației
├── data/                  # Persistența datelor
│   ├── model/           # Modele de date
│   ├── database/        # Room database și DAOs
│   └── repository/      # Repository pentru logica de date
├── sensors/              # Managementul senzorilor
│   ├── AccelerometerManager.kt
│   ├── GyroscopeManager.kt
│   ├── MicrophoneManager.kt
│   ├── GPSManager.kt
│   ├── FallDetectionAlgorithm.kt
│   └── SensorFusionManager.kt
├── network/              # Comunicare rețea
│   ├── AudioUploadClient.kt
│   ├── SMSManager.kt
│   ├── NetworkChecker.kt
│   └── EmergencyAlertService.kt
└── utils/                # Utilitare
    ├── Constants.kt
    ├── PermissionsManager.kt
    ├── LocationFormatter.kt
    ├── TimeUtils.kt
    └── FileUtils.kt
```

## Cerințe de Permisiuni

- **LOCATION**: Acces la locație GPS pentru alerte
- **RECORD_AUDIO**: Înregistrare audio
- **SEND_SMS**: Trimitere SMS către contacte
- **INTERNET**: Upload audio și comunicare rețea
- **SENSORS**: Acces la accelerometru și giroscop

## Instalare și Configurare

1. Clonează repository-ul
2. Deschide proiectul în Android Studio
3. Sincronizează cu Gradle
4. Configurează contactele de urgență în aplicație
5. Acordă permisiunile necesare
6. Pornește sistemul de detecție

## Dezvoltare

Proiectul este în dezvoltare activă. Fișierele conțin TODO-uri pentru implementarea funcționalităților planificate.

## Licență

Proiect dezvoltat pentru scopuri educaționale.

## Repository

GitHub: https://github.com/CarlaVekony/SafetyTracker

