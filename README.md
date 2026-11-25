# মেডি অ্যালার্ট (Medi Alert) — স্মার্ট অফলাইন ওষুধ ব্যবস্থাপনা অ্যাপ

**মেডি অ্যালার্ট (Medi Alert / Pill Tracker)** একটি সম্পূর্ণ অফলাইন, প্রোডাকশন-রেডি অ্যান্ড্রয়েড অ্যাপ্লিকেশন যা কটলিন (Kotlin), জেটপ্যাক কম্পোজ (Jetpack Compose), Material Design 3 এবং SQLite/Room ডাটাবেস ব্যবহার করে তৈরি করা হয়েছে। এতে কোনো লগইন, ফায়ারবেস বা ইন্টারনেট সংযোগের প্রয়োজন হয় না।

---

## প্রধান বৈশিষ্ট্যসমূহ

### ১. সম্পূর্ণ ওষুধ ও রোগী ব্যবস্থাপনা
- **মাল্টি-পেশেন্ট প্রোফাইল:** পরিবারের একাধিক সদস্যের জন্য আলাদা প্রোফাইল তৈরি, সম্পাদনা এবং প্রোফাইল সুইচ করার সুবিধা।
- **ওষুধের ধরন:** ট্যাবলেট (Tablet), ক্যাপসুল (Capsule), সিরাপ (Syrup) এবং ইনজেকশন (Injection)।
- **সময়সূচী ও ডোজ:** শুরুর তারিখ, শেষের তারিখ (অথবা চলমান), প্রতিদিন একাধিক সময় এবং **খাবারের আগে / পরে / সাথে** নির্দেশনা।
- **লাইভ কাউন্টডাউন:** পরবর্তী ডোজের জন্য ড্যাশবোর্ডে সেকেন্ড-বাই-সেকেন্ড কাউন্টডাউন।
- **স্টক ও রিফিল সতর্কতা:** ডোজ নেওয়ার সাথে সাথে স্টক কমে যাওয়া, কত দিন চলবে তার হিসাব, কম স্টক নোটিফিকেশন এবং দ্রুত রিফিল।
- **প্রেসক্রিপশন আপলোড ও ভিউয়ার:** Photo Picker দিয়ে ছবি বা SAF দিয়ে PDF প্রেসক্রিপশন সংযুক্তি এবং অফলাইন বিল্ট-ইন PDF/Image ভিউয়ার।

### ২. নির্ভরযোগ্য অ্যালার্ম ও নোটিফিকেশন সিস্টেম
- **AlarmManager (`setAlarmClock`):** Doze মোডেও নির্ভুল সময়ে অ্যালার্ম ডেলিভারি।
- **রিবুট ও টাইমজোন রিকভারি:** ফোন রিস্টার্ট, অ্যাপ আপডেট, সময় বা টাইমজোন পরিবর্তনে স্বয়ংক্রিয়ভাবে সব অ্যালার্ম রিশিডিউল (`BOOT_COMPLETED`, `TIMEZONE_CHANGED`, `TIME_SET`, `MY_PACKAGE_REPLACED`)।
- **নোটিফিকেশন অ্যাকশন ও ফুল-স্ক্রিন অ্যালার্ম:** লক-স্ক্রিন ফুল-স্ক্রিন অ্যালার্ম UI এবং সরাসরি নোটিফিকেশন থেকে **Taken**, **Snooze (5/10/15 মিনিট)** এবং **Skip** করার সুবিধা।
- **মিসড ডোজ অ্যালার্ট:** নির্ধারিত সময়সীমার পরে ডোজ না নিলে স্বয়ংক্রিয়ভাবে Missed হিসেবে চিহ্নিত ও সতর্কতা।
- **ভয়েস রিমাইন্ডার ও কাস্টম রিংটোন:** বাংলা/ইংরেজি Text-to-Speech (TTS) ভয়েস ঘোষণা ও কাস্টম অ্যালার্ম সাউন্ড নির্বাচন।
- **ডায়াগনস্টিকস:** Android 13+ Notification, Android 12+ Exact Alarm, Full-Screen Intent এবং Battery Optimization অবস্থা যাচাই ও ১-ট্যাপ সেটিংস গাইড।

### ৩. ইতিহাস, অ্যানালিটিক্স, নিরাপত্তা ও ব্যাকআপ
- **ক্যালেন্ডার ও রিপোর্ট:** দৈনিক / সাপ্তাহিক / মাসিক Adherence শতাংশ, অ্যানিমেটেড রিং ও বার চার্ট এবং মাসিক ক্যালেন্ডার ভিউ।
- **PDF রিপোর্ট:** অফলাইনে রোগীর ওষুধ ও Adherence রিপোর্ট PDF হিসেবে সেভ এবং শেয়ার।
- **অ্যাপ লক (PIN + Biometric):** SHA-256 হ্যাশড ৪-সংখ্যার পিন এবং ফিঙ্গারপ্রিন্ট/ফেস আনলক।
- **JSON ব্যাকআপ ও রিস্টোর:** সম্পূর্ণ ডাটাবেস অফলাইন JSON ফাইলে ব্যাকআপ ও পুনরুদ্ধার।
- **হোম স্ক্রিন উইজেট:** পরবর্তী ডোজ ও পেন্ডিং সংখ্যা দেখার জন্য হোম স্ক্রিন উইজেট (১-ট্যাপ Taken অ্যাকশন সহ)।
- **UI ও ভাষা:** গ্লাসমরফিজম (Glassmorphism) UI, Light / Dark / System থিম এবং **বাংলা ও ইংরেজি** ভাষা সাপোর্ট।

---

## টেকনিক্যাল আর্কিটেকচার

- **প্যাকেজ আইডি:** `com.slm.medialert`
- **স্থাপত্য:** MVVM + Repository Pattern + Clean Domain Calculators
- **UI:** Jetpack Compose + Material 3 + Bundled Local Fonts (`Outfit`, `Plus Jakarta Sans`, `Noto Sans Bengali`)
- **ডাটাবেস ও স্টোরেজ:** Room Database (SQLite) + Jetpack DataStore Preferences + Internal Prescription Storage
- **ব্যাকগ্রাউন্ড ও অ্যালার্ম:** `AlarmManager`, `AlarmService` (Foreground `mediaPlayback`), `BroadcastReceiver`
- **টেস্টিং:** JUnit 4 + Robolectric + Room In-Memory Tests

---

## লোকাল বিল্ড ও টেস্ট নির্দেশনা

```bash
# ইউনিট এবং Robolectric টেস্ট চালানো
gradle :app:testDebugUnitTest

# Debug APK তৈরি
gradle :app:assembleDebug

# Signed Release APK ও AAB তৈরি (পরিবেশ ভ্যারিয়েবল সেট করে)
export KEYSTORE_PATH=/path/to/my-upload-key.jks
export STORE_PASSWORD=your_store_password
export KEY_PASSWORD=your_key_password
gradle :app:assembleRelease :app:bundleRelease
```

---

## GitHub Secrets ও রিলিজ গাইড

GitHub Actions ওয়ার্কফ্লো (`.github/workflows/release.yml`), কীস্টোর তৈরি এবং APK/AAB রিলিজ সংক্রান্ত বিস্তারিত বাংলা গাইডের জন্য দেখুন:
- [`docs/SETUP_AND_SECRETS_BN.md`](docs/SETUP_AND_SECRETS_BN.md)
