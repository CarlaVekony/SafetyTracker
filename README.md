# SafetyTracker

## Descriere

SafetyTracker este o aplicație Android dezvoltată în Kotlin cu Jetpack Compose, care detectează și anunță situații de urgență folosind senzori integrați ale dispozitivului.

## Scop

Aplicația oferă un sistem de protecție și alertare în timp real, care monitorizează constant activitatea utilizatorului și trimite alerte către persoanele de contact în cazul detectării unor situații anormale.

## Funcționalități

### Detecție de Urgență

- **Giroscop + Accelerometru**: Detectează căzăturile și mișcări spontane folosind algoritm de fuziune senzorială
- **Microfon**: Monitorizează amplitudinea sunetului pentru detectare mai precisă
- **GPS**: Captează și include locația persoanei în mesajele de alertă
- **Algoritm de detecție**: Analizează datele din toți senzorii și calculează un scor de încredere

### Sistem de Alertare

- **SMS automat**: Trimite alerte către toate contactele de urgență active
- **Înregistrare audio (5 secunde)**: 
  - **Dacă aplicația este SMS-ul implicit**: Trimite MMS cu audio atașat
  - **Altfel**: Încarcă audio pe server (transfer.sh sau 0x0.st) și trimite link în SMS
  - **Fără internet sau upload eșuat**: Trimite doar SMS cu coordonate și scor de încredere
- **Locație**: Include coordonatele GPS în fiecare alertă
- **Cooldown**: Previne spam-ul cu 10 secunde între alerte
- **Popup instant**: Notificare vizuală imediată când este detectată o urgență

### Control și Interfață

- Buton Start/Stop pentru activarea/dezactivarea sistemului de detecție
- Interfață intuitivă pentru gestionarea contactelor de urgență
- Grafice în timp real pentru accelerometru, giroscop și microfon
- Cerere automată de permisiuni la prima deschidere (microfon, SMS, locație)

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
│   │   ├── EmergencyAlertPopup.kt
│   │   ├── EmergencyButton.kt
│   │   ├── SensorGraph.kt
│   │   └── SensorDataManager.kt
│   ├── screens/          # Ecranele aplicației
│   │   ├── HomeScreen.kt
│   │   ├── ContactsScreen.kt
│   │   └── SettingsScreen.kt
│   └── theme/            # Tema aplicației
├── data/                  # Persistența datelor
│   ├── model/           # Modele de date
│   │   ├── EmergencyAlert.kt
│   │   └── EmergencyContact.kt
│   ├── database/        # Room database și DAOs
│   │   ├── SafetyDatabase.kt
│   │   ├── EmergencyContactDao.kt
│   │   └── EmergencyAlertDao.kt
│   ├── repository/      # Repository pentru logica de date
│   │   ├── EmergencyRepository.kt
│   │   └── EmergencyAlertRepository.kt
│   └── preferences/     # Preferințe utilizator
├── sensors/              # Managementul senzorilor
│   ├── AccelerometerManager.kt
│   ├── GyroscopeManager.kt
│   ├── MicrophoneManager.kt (cu buffer circular de 5 secunde)
│   ├── GPSManager.kt
│   └── FallDetectionAlgorithm.kt
├── network/              # Comunicare rețea
│   ├── EmergencyAlertService.kt (coordonare detecție și trimitere)
│   ├── EmergencySMSManager.kt (trimite SMS/MMS)
│   └── AudioUploadClient.kt (upload audio pe server)
├── media/                # Utilitare media
│   └── AudioUtils.kt (conversie PCM la WAV)
├── navigation/           # Navigare în aplicație
│   └── SafetyTrackerNavigation.kt
└── utils/                # Utilitare
    └── PermissionsManager.kt (cerere automată permisiuni)
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
4. Rulează aplicația pe un dispozitiv Android (API 24+)
5. La prima deschidere, aplicația va cere automat permisiunile necesare:
   - Microfon (pentru înregistrare audio)
   - SMS (pentru trimitere alerte)
   - Locație (pentru coordonate GPS)
6. Adaugă contactele de urgență din ecranul "Contacts"
7. Pornește sistemul de detecție din ecranul principal

## Cum Funcționează

### Detecție
- Aplicația monitorizează continuu senzorii când este activată
- Algoritmul analizează datele din accelerometru, giroscop și microfon
- Când detectează o cădere (scor de încredere ridicat), pregătește o alertă

### Trimitere Alertă
1. **Pregătire**: Captează locația GPS și ultimele 5 secunde de audio
2. **Popup instant**: Afișează notificare vizuală imediat
3. **Trimitere SMS/MMS**:
   - Dacă aplicația este SMS-ul implicit: trimite MMS cu audio atașat
   - Altfel: încarcă audio pe server și trimite SMS cu link
   - Mesajul conține: "EMERGENCY ALERT", scor de încredere, coordonate GPS, și link audio (dacă disponibil)
4. **Cooldown**: Previne trimiterea de alerte multiple în 10 secunde

## Note Tehnice

### Înregistrare Audio
- Buffer circular în memorie (5 secunde, 44.1kHz, mono, 16-bit PCM)
- Conversie automată la WAV pentru trimitere/upload
- Funcționează doar când aplicația este în foreground (limitație Android)

### Trimitere Audio
- **MMS**: Doar dacă aplicația este setată ca SMS implicit
- **Upload + Link**: Folosește transfer.sh (fallback: 0x0.st) pentru compatibilitate cu toate SMS-urile
- **Fără internet**: Trimite doar SMS cu coordonate

### Limitări
- Audio nu este înregistrat când aplicația rulează în background (necesită foreground service pentru funcționalitate completă)
- MMS funcționează doar dacă aplicația este SMS-ul implicit
- Upload audio necesită conexiune internet activă

## Licență

Proiect dezvoltat pentru scopuri educaționale.

## Repository

GitHub: https://github.com/CarlaVekony/SafetyTracker

