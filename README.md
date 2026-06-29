# Aether Cloud

Aplikasi Android untuk sharing modul Root/No Root secara online.

## Fitur
- Login dengan Google & Facebook
- Upload modul dengan file ZIP, screenshot, deskripsi lengkap
- Browse modul dengan filter (Latest, Popular, Root, No Root)
- Download modul ke `/sdcard/Download`
- Sistem komentar
- Profil developer lengkap

## Setup Firebase
1. Buat project di [Firebase Console](https://console.firebase.google.com)
2. Tambahkan Android app dengan package `com.aether.cloud`
3. Download `google-services.json` dan letakkan di folder `app/`
4. Enable Authentication (Google Sign-In, Facebook)
5. Enable Firestore Database
6. Enable Firebase Storage

## Setup Facebook Login
1. Buat aplikasi di [Facebook Developers](https://developers.facebook.com)
2. Dapatkan App ID dan Client Token
3. Update `strings.xml`:
   - `facebook_app_id`
   - `fb_login_protocol_scheme`
   - `facebook_client_token`
4. Tambahkan hash key development di Facebook Console

## Firestore Security Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /modules/{moduleId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.resource.data.authorId == request.auth.uid;
      allow update, delete: if request.auth != null && resource.data.authorId == request.auth.uid;
    }
    match /modules/{moduleId}/comments/{commentId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null && resource.data.userId == request.auth.uid;
    }
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## Storage Rules
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /modules/{moduleId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
    match /users/{userId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## Build
1. Import project ke Android Studio
2. Sync Gradle
3. Run pada device/emulator Android 7.0+
