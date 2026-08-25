# PawAlert 🐾 — Android App

A native Android application connecting local communities with volunteer dog feeders and rescuers. Anyone can report a stray or injured dog nearby with a photo, problem category, description, and GPS location. Nearby volunteer feeders view active alerts sorted dynamically by proximity and can claim them to assist.

---

## 📱 Tech Stack & Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM + Repository Pattern + Kotlin Coroutines & Flow
- **Backend & Cloud**:
  - **Firebase Authentication**: Anonymous 1-click community login and Email/Password auth
  - **Cloud Firestore**: Real-time reactive stream of reports (`reports` collection)
  - **Firebase Cloud Storage**: Secure storage for dog alert photos
- **Location & Maps**:
  - **Google Play Services FusedLocationProviderClient**: Precise GPS capture and local distance calculation
  - **Android Geocoder**: Reverse geocoding of coordinates into human-readable street addresses
  - **Google Maps SDK & Maps Compose**: Interactive in-app map pin + turn-by-turn navigation intent
- **Image Loading**: Coil Compose

---

## 🗂️ Project Structure

```
PawAlert/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/pawalert/
│   │   │   ├── data/
│   │   │   │   ├── DogReport.kt            # Data model, ProblemType & ReportStatus enums
│   │   │   │   └── ReportRepository.kt     # Firestore real-time observation, CRUD & Storage upload
│   │   │   ├── ui/
│   │   │   │   ├── auth/
│   │   │   │   │   ├── AuthScreen.kt       # Onboarding, profile & authentication UI
│   │   │   │   │   └── AuthViewModel.kt    # Auth state management
│   │   │   │   ├── detail/
│   │   │   │   │   ├── DetailScreen.kt     # Hero photo, Google Map, navigation & claim action
│   │   │   │   │   └── DetailViewModel.kt  # Report observation, claim/resolve/unclaim logic
│   │   │   │   ├── feed/
│   │   │   │   │   ├── FeedScreen.kt       # Proximity-sorted alerts feed with category chips
│   │   │   │   │   └── FeedViewModel.kt    # Client-side distance sorting & filtering
│   │   │   │   ├── navigation/
│   │   │   │   │   ├── PawAlertNavGraph.kt # Compose navigation graph
│   │   │   │   │   └── Screen.kt           # Route destinations
│   │   │   │   ├── report/
│   │   │   │   │   ├── ReportScreen.kt     # Photo capture, category picker & GPS location capture
│   │   │   │   │   └── ReportViewModel.kt  # Report submission workflow
│   │   │   │   └── theme/
│   │   │   │       ├── Color.kt            # Amber/Brown theme & status colors (Red/Amber/Green)
│   │   │   │       ├── Theme.kt            # Material 3 theme & dark/light color schemes
│   │   │   │       └── Type.kt             # Typography
│   │   │   ├── util/
│   │   │   │   └── LocationHelper.kt       # GPS capture, distance calculation, geocoding & time utils
│   │   │   └── MainActivity.kt             # Single activity entry point
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── xml/
│   │   │       └── file_paths.xml          # FileProvider camera cache paths
│   │   └── AndroidManifest.xml             # Permissions, FileProvider & Maps metadata
│   └── build.gradle.kts
├── firestore.rules                         # Firestore security rules
├── storage.rules                           # Storage security rules
├── build.gradle.kts                        # Top-level build file
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Setup & Firebase Configuration

### 1. Create a Firebase Project
1. Go to the [Firebase Console](https://console.firebase.google.com/) and click **Add Project**.
2. Name your project (e.g., `PawAlert`).

### 2. Add Android App to Firebase
1. Register an Android app in your Firebase project:
   - **Android package name**: `com.example.pawalert`
   - **App nickname**: `PawAlert`
2. Download the generated `google-services.json` file.
3. Place `google-services.json` into the `PawAlert/app/` directory:
   ```
   PawAlert/app/google-services.json
   ```

### 3. Enable Firebase Services
1. **Authentication**:
   - Navigate to **Authentication** > **Sign-in method**.
   - Enable **Anonymous** authentication (allows fast 1-click community onboarding).
   - (Optional) Enable **Email/Password** authentication.
2. **Cloud Firestore**:
   - Navigate to **Firestore Database** > **Create database**.
   - Start in test mode or paste the rules from `firestore.rules`.
   - **Composite Index**:
     Firestore will automatically provide a link in Android Logcat if an index is required for querying active reports (`status` IN `['OPEN', 'IN_PROGRESS']` ordered by `createdAt DESC`).
     Or create it manually under **Indexes**:
     - Collection: `reports`
     - Fields:
       - `status` (Ascending)
       - `createdAt` (Descending)
3. **Cloud Storage**:
   - Navigate to **Storage** > **Get Started**.
   - Paste the rules from `storage.rules`.

### 4. Configure Google Maps API Key
1. Go to the [Google Cloud Console](https://console.cloud.google.com/) and create/select your project.
2. Enable **Maps SDK for Android**.
3. Create an API key in **Credentials**.
4. In `PawAlert/app/src/main/AndroidManifest.xml`, replace `YOUR_MAPS_API_KEY_HERE` with your key:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="AIzaSyYourActualKeyHere..." />
   ```

---

## 💡 Key Features & User Flows

1. **Nearby Alerts Feed**:
   - Alerts are streamed live from Firestore.
   - User GPS location is acquired and the distance (e.g. `250 m away`, `1.4 km away`) is calculated locally for each report.
   - Alerts are sorted nearest-first so volunteer feeders can quickly find dogs in their vicinity.
   - Filter chips allow instant filtering by need category (`Hungry`, `Injured`, `Sick`, `Stuck`, etc.).

2. **Reporting a Stray Dog**:
   - Snap a photo directly using the camera (`FileProvider`) or pick from device gallery.
   - Select issue category and add notes/landmarks.
   - 1-tap "Get Current Location" grabs high-accuracy GPS coordinates and reverse geocodes the address.
   - Image is uploaded to Firebase Storage and document saved to Firestore.

3. **Detail & Feeder Claim Flow**:
   - Visual status badges:
     - 🔴 **OPEN** (Needs help)
     - 🟡 **BEING HANDLED** (A volunteer has claimed it)
     - 🟢 **RESOLVED** (Dog is safe/fed/treated)
   - Embedded Google Map showing exact pin + button to launch turn-by-turn navigation in the Google Maps app.
   - **"I'll help this dog"** button assigns the current user as the feeder and transitions status to `IN_PROGRESS`.
   - The claiming feeder can easily mark the case as **Resolved** or release the alert if they cannot make it.
