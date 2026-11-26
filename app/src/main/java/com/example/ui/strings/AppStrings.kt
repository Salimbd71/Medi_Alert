package com.example.ui.strings

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.model.AppLanguage
import com.example.data.model.DoseStatus
import com.example.data.model.MealTiming
import com.example.data.model.MedicineType
import com.example.data.model.ReportPeriod
import com.example.data.model.TimeOfDayBucket

data class AppStrings(
    val appName: String,
    val navToday: String,
    val navMedicines: String,
    val navHistory: String,
    val navSettings: String,
    val allPatients: String,
    val nextDoseTitle: String,
    val noUpcomingDose: String,
    val allDoneSubtext: String,
    val adherenceToday: String,
    val lowStockTitle: String,
    val lowStockAction: String,
    val permissionBannerTitle: String,
    val permissionBannerBody: String,
    val fixNow: String,
    val takeAction: String,
    val snoozeAction: String,
    val skipAction: String,
    val snooze5: String,
    val snooze10: String,
    val snooze15: String,
    val addMedicine: String,
    val editMedicine: String,
    val searchMedicinesHint: String,
    val allTypes: String,
    val medicineNameLabel: String,
    val medicineTypeLabel: String,
    val dosageAmountLabel: String,
    val dosageUnitLabel: String,
    val mealTimingLabel: String,
    val patientLabel: String,
    val startDateLabel: String,
    val endDateLabel: String,
    val ongoingLabel: String,
    val stockCountLabel: String,
    val lowStockThresholdLabel: String,
    val dosePerIntakeLabel: String,
    val schedulesLabel: String,
    val addTimeSlot: String,
    val voiceReminderLabel: String,
    val prescriptionLabel: String,
    val pickImage: String,
    val pickPdf: String,
    val viewPrescription: String,
    val notesLabel: String,
    val save: String,
    val cancel: String,
    val delete: String,
    val refillTitle: String,
    val refillAmountLabel: String,
    val refillButton: String,
    val remainingStock: String,
    val daysLeftSuffix: String,
    val emptyMedicinesTitle: String,
    val emptyMedicinesBody: String,
    val emptyDosesTitle: String,
    val emptyDosesBody: String,
    val calendarTitle: String,
    val exportPdfReport: String,
    val sharePdfReport: String,
    val takenCountLabel: String,
    val skippedCountLabel: String,
    val missedCountLabel: String,
    val pendingCountLabel: String,
    val profilesSection: String,
    val addPatient: String,
    val editPatient: String,
    val patientNameLabel: String,
    val relationLabel: String,
    val ageLabel: String,
    val appearanceSection: String,
    val themeLabel: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val languageLabel: String,
    val alarmsSection: String,
    val customAlarmSound: String,
    val defaultAlarmSound: String,
    val testVoiceReminder: String,
    val missedGraceWindow: String,
    val minutesSuffix: String,
    val permissionsDiagnostics: String,
    val notificationPerm: String,
    val exactAlarmPerm: String,
    val fullScreenPerm: String,
    val batteryOptPerm: String,
    val granted: String,
    val needsAttention: String,
    val securitySection: String,
    val appLockTitle: String,
    val setPinPrompt: String,
    val enterPinPrompt: String,
    val wrongPinError: String,
    val biometricUnlock: String,
    val unlockWithBiometric: String,
    val backupSection: String,
    val exportJsonBackup: String,
    val restoreJsonBackup: String,
    val backupExportedMsg: String,
    val backupRestoredMsg: String,
    val backupErrorMsg: String,
    val pdfSavedMsg: String,
    val activeLabel: String,
    val pausedLabel: String,
    val actionsUnlockHint: String,
    val batteryOptHint: String,
    val aboutSection: String,
    val versionLabel: String,
    val developerCredit: String,
    val callDeveloperDesc: String,
    val whatsappDeveloperDesc: String,
    val facebookDeveloperDesc: String,
    val shareAppDesc: String,
    val shareAppMessage: String,
    val pinLockTitle: String,
    val pinLockSetDesc: String,
    val pinLockUnsetDesc: String,
    val biometricLockDesc: String,
    val biometricUnavailableMsg: String,
    val changePinLabel: String,
    val openPdfLabel: String,
    val openPdfButton: String,
    val pdfSavedToPrefix: String,
    val noPdfReaderMsg: String,
    val legalSectionTitle: String,
    val privacyPolicyTitle: String,
    val termsConditionsTitle: String,
    val privacyPolicyBody: String,
    val termsConditionsBody: String,
    val mailMeLabel: String,
    val closeLabel: String
) {
    fun medicineType(type: MedicineType): String = when (type) {
        MedicineType.TABLET -> if (isBangla) "ট্যাবলেট" else "Tablet"
        MedicineType.CAPSULE -> if (isBangla) "ক্যাপসুল" else "Capsule"
        MedicineType.SYRUP -> if (isBangla) "সিরাপ" else "Syrup"
        MedicineType.INJECTION -> if (isBangla) "ইনজেকশন" else "Injection"
    }

    fun mealTiming(timing: MealTiming): String = when (timing) {
        MealTiming.BEFORE_FOOD -> if (isBangla) "খাবারের আগে" else "Before Food"
        MealTiming.AFTER_FOOD -> if (isBangla) "খাবারের পরে" else "After Food"
        MealTiming.WITH_FOOD -> if (isBangla) "খাবারের সাথে" else "With Food"
        MealTiming.ANYTIME -> if (isBangla) "যেকোনো সময়" else "Anytime"
    }

    fun doseStatus(status: DoseStatus): String = when (status) {
        DoseStatus.PENDING -> if (isBangla) "অপেক্ষমান" else "Pending"
        DoseStatus.TAKEN -> if (isBangla) "নেওয়া হয়েছে" else "Taken"
        DoseStatus.SKIPPED -> if (isBangla) "বাদ দেওয়া হয়েছে" else "Skipped"
        DoseStatus.MISSED -> if (isBangla) "মিসড" else "Missed"
        DoseStatus.SNOOZED -> if (isBangla) "স্নুজ করা" else "Snoozed"
    }

    fun bucket(bucket: TimeOfDayBucket): String = when (bucket) {
        TimeOfDayBucket.MORNING -> if (isBangla) "সকাল" else "Morning"
        TimeOfDayBucket.AFTERNOON -> if (isBangla) "দুপুর" else "Afternoon"
        TimeOfDayBucket.EVENING -> if (isBangla) "বিকাল / সন্ধ্যা" else "Evening"
        TimeOfDayBucket.NIGHT -> if (isBangla) "রাত" else "Night"
    }

    fun period(period: ReportPeriod): String = when (period) {
        ReportPeriod.DAILY -> if (isBangla) "দৈনিক" else "Daily"
        ReportPeriod.WEEKLY -> if (isBangla) "সাপ্তাহিক" else "Weekly"
        ReportPeriod.MONTHLY -> if (isBangla) "মাসিক" else "Monthly"
    }

    val isBangla: Boolean
        get() = appName == "মেডি অ্যালার্ট"
}

val EnglishStrings = AppStrings(
    appName = "Medi Alert",
    navToday = "Today",
    navMedicines = "Medicines",
    navHistory = "History",
    navSettings = "Settings",
    allPatients = "All Patients",
    nextDoseTitle = "Next Dose Countdown",
    noUpcomingDose = "All caught up for now",
    allDoneSubtext = "No upcoming doses scheduled shortly",
    adherenceToday = "Adherence",
    lowStockTitle = "Low Stock Alert",
    lowStockAction = "Refill",
    permissionBannerTitle = "Ensure Reliable Alarms",
    permissionBannerBody = "Notifications, exact alarms, or battery settings need attention so reminders never miss.",
    fixNow = "Review",
    takeAction = "Take",
    snoozeAction = "Snooze",
    skipAction = "Skip",
    snooze5 = "5 min",
    snooze10 = "10 min",
    snooze15 = "15 min",
    addMedicine = "Add Medicine",
    editMedicine = "Edit Medicine",
    searchMedicinesHint = "Search medicines or patients...",
    allTypes = "All",
    medicineNameLabel = "Medicine Name",
    medicineTypeLabel = "Medicine Form",
    dosageAmountLabel = "Dosage",
    dosageUnitLabel = "Unit",
    mealTimingLabel = "Meal Timing",
    patientLabel = "Patient Profile",
    startDateLabel = "Start Date",
    endDateLabel = "End Date",
    ongoingLabel = "Ongoing (No End Date)",
    stockCountLabel = "Current Stock",
    lowStockThresholdLabel = "Low Stock Alert At",
    dosePerIntakeLabel = "Qty Per Dose",
    schedulesLabel = "Daily Schedules",
    addTimeSlot = "Add Time",
    voiceReminderLabel = "Speak Voice Reminder",
    prescriptionLabel = "Prescription (Image / PDF)",
    pickImage = "Photo",
    pickPdf = "PDF File",
    viewPrescription = "View Prescription",
    notesLabel = "Notes / Instructions",
    save = "Save",
    cancel = "Cancel",
    delete = "Delete",
    refillTitle = "Refill Stock",
    refillAmountLabel = "Quantity to Add",
    refillButton = "Add Stock",
    remainingStock = "Stock",
    daysLeftSuffix = "days left",
    emptyMedicinesTitle = "No Medicines Added Yet",
    emptyMedicinesBody = "Tap the + button to add tablets, capsules, syrups, or injections with smart reminders.",
    emptyDosesTitle = "No Doses Scheduled",
    emptyDosesBody = "Add a medication schedule or switch patient profiles to track daily doses.",
    calendarTitle = "Medication Calendar",
    exportPdfReport = "Save PDF",
    sharePdfReport = "Share PDF",
    takenCountLabel = "Taken",
    skippedCountLabel = "Skipped",
    missedCountLabel = "Missed",
    pendingCountLabel = "Pending",
    profilesSection = "Patient Profiles",
    addPatient = "Add Patient",
    editPatient = "Edit Patient",
    patientNameLabel = "Patient Name",
    relationLabel = "Relation (e.g. Self, Mother)",
    ageLabel = "Age (Optional)",
    appearanceSection = "Appearance & Language",
    themeLabel = "Theme",
    themeSystem = "System",
    themeLight = "Light",
    themeDark = "Dark",
    languageLabel = "Language",
    alarmsSection = "Alarms & Voice",
    customAlarmSound = "Alarm Ringtone",
    defaultAlarmSound = "System Default Alarm",
    testVoiceReminder = "Test Voice Reminder",
    missedGraceWindow = "Missed Dose Window",
    minutesSuffix = "min",
    permissionsDiagnostics = "Alarm & Battery Health",
    notificationPerm = "Notifications (Android 13+)",
    exactAlarmPerm = "Exact Alarms (Android 12+)",
    fullScreenPerm = "Full-Screen Alarm Alert",
    batteryOptPerm = "Unrestricted Battery (Doze)",
    granted = "Optimal",
    needsAttention = "Tap to Fix",
    securitySection = "Privacy & App Lock",
    appLockTitle = "Require PIN / Biometric Lock",
    setPinPrompt = "Set a 4-digit Security PIN",
    enterPinPrompt = "Enter your 4-digit PIN to unlock Medi Alert",
    wrongPinError = "Incorrect PIN. Try again.",
    biometricUnlock = "Use Fingerprint / Face Unlock",
    unlockWithBiometric = "Unlock with Biometrics",
    backupSection = "Offline Backup & Restore",
    exportJsonBackup = "Export JSON Backup",
    restoreJsonBackup = "Restore JSON Backup",
    backupExportedMsg = "Backup JSON saved successfully",
    backupRestoredMsg = "Backup restored & alarms rescheduled",
    backupErrorMsg = "Could not process backup file",
    pdfSavedMsg = "PDF report saved successfully",
    activeLabel = "Active",
    pausedLabel = "Paused",
    actionsUnlockHint = "Take / Snooze / Skip unlock 15 min before dose",
    batteryOptHint = "Tap to select Allow / Unrestricted background usage",
    aboutSection = "About",
    versionLabel = "Version",
    developerCredit = "Developer: SLM Technology",
    callDeveloperDesc = "Call Developer",
    whatsappDeveloperDesc = "WhatsApp Developer",
    facebookDeveloperDesc = "Facebook Profile",
    shareAppDesc = "Share Medi Alert",
    shareAppMessage = "Stay on top of your daily medicines with Medi Alert — smart offline pill reminders, multi-patient tracking, and stock alerts.\n\nDownload Latest APK:\nhttps://github.com/Salimbd71/Medi_Alert/releases/latest/download/medi-alert-release.apk",
    pinLockTitle = "4-Digit PIN Lock",
    pinLockSetDesc = "PIN protection is active (tap Change to update)",
    pinLockUnsetDesc = "Lock app using a 4-digit security PIN",
    biometricLockDesc = "Unlock with fingerprint or face — works with or without PIN",
    biometricUnavailableMsg = "Enable a fingerprint or biometric lock in your device Settings first",
    changePinLabel = "Change",
    openPdfLabel = "Open",
    openPdfButton = "Open PDF",
    pdfSavedToPrefix = "Saved to",
    noPdfReaderMsg = "No PDF reader app installed to open this file",
    legalSectionTitle = "Privacy, Terms & Support",
    privacyPolicyTitle = "Privacy Policy",
    termsConditionsTitle = "Terms & Conditions",
    privacyPolicyBody = """
Medi Alert (developed by SLM Technology) is a 100% offline medication management app. Your privacy and health records stay strictly on your device.

1. Zero Data Collection & Offline Storage
• Medi Alert does NOT collect, transmit, sell, or share any personal or medical data.
• Patient profiles, medication schedules, dose history, PIN hashes, and prescriptions remain stored locally in your device's Room SQLite database and DataStore.

2. Android Permissions & Why We Request Each One
• Notifications (POST_NOTIFICATIONS): Used solely to show timely medication alerts, snooze alerts, and low-stock refill notifications.
• Exact Alarms (SCHEDULE_EXACT_ALARM): Ensures medication reminders ring at the exact prescribed minute, even during idle periods.
• Full-Screen Alarm Alert (USE_FULL_SCREEN_INTENT): Displays the full-screen dose reminder over the lock screen for time-critical medicines.
• Boot & Time Sync (RECEIVE_BOOT_COMPLETED): Automatically restores your scheduled medication alarms after a phone restart, app update, or timezone change.
• Alarm Audio & Voice (FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, WAKE_LOCK, VIBRATE): Keeps alarm sound, vibration, and offline Text-to-Speech voice reminders running reliably until you respond.
• Battery Optimization Exemption (REQUEST_IGNORE_BATTERY_OPTIMIZATIONS): Optional user-approved setting so aggressive battery savers do not silence scheduled pill alarms.
• Files, Prescriptions & PDF Exports: Prescription photos use the zero-permission Android Photo Picker, and PDF reports save directly into Download/Medi_Alert via Android's scoped MediaStore API without requesting broad storage access.
• Biometric Authentication (USE_BIOMETRIC): Handled exclusively by Android's secure BiometricPrompt API; Medi Alert never reads or stores fingerprint data.

3. Contact Developer
If you have privacy questions, tap Mail Me to reach salimbd.ga@gmail.com.
""".trimIndent(),
    termsConditionsBody = """
Welcome to Medi Alert by SLM Technology. By using this application, you agree to the following terms:

1. Medical Disclaimer
• Medi Alert is a personal medication reminder and tracking tool only. It does NOT provide medical advice, diagnosis, or treatment recommendations.
• Always follow instructions from your licensed physician or pharmacist regarding dosages, timing, and refills.

2. Reminder Reliability & User Responsibility
• Alarm delivery depends on device hardware, system power settings, volume levels, and OS permissions (Notifications, Exact Alarms, and Unrestricted Battery).
• Please verify dose schedules and maintain backups via Settings > Offline Backup & Restore before changing or resetting your device.

3. Offline Ownership & Backups
• You retain full control over your exported JSON backups, prescriptions, and PDF reports saved in Download/Medi_Alert.
• Because Medi Alert has no cloud server, SLM Technology cannot recover deleted data if the app is uninstalled without an exported backup.

4. Intellectual Property & Support
• Medi Alert is provided "as is" without warranty of any kind.
• For bug reports, feedback, or support, please contact salimbd.ga@gmail.com.
""".trimIndent(),
    mailMeLabel = "Mail Me",
    closeLabel = "Close"
)

val BanglaStrings = AppStrings(
    appName = "মেডি অ্যালার্ট",
    navToday = "আজকের ডোজ",
    navMedicines = "ওষুধসমূহ",
    navHistory = "ইতিহাস",
    navSettings = "সেটিংস",
    allPatients = "সব রোগী",
    nextDoseTitle = "পরবর্তী ডোজ কাউন্টডাউন",
    noUpcomingDose = "আপাতত সব ডোজ সম্পন্ন",
    allDoneSubtext = "নিকটবর্তী সময়ে কোনো ডোজ বাকি নেই",
    adherenceToday = "নিয়মানুবর্তিতা",
    lowStockTitle = "কম স্টক সতর্কতা",
    lowStockAction = "রিফিল",
    permissionBannerTitle = "সঠিক সময়ে অ্যালার্ম নিশ্চিত করুন",
    permissionBannerBody = "অ্যালার্ম মিস না হতে নোটিফিকেশন, একজ্যাক্ট অ্যালার্ম ও ব্যাটারি সেটিংস যাচাই করুন।",
    fixNow = "ঠিক করুন",
    takeAction = "খেয়েছি",
    snoozeAction = "স্নুজ",
    skipAction = "বাদ দিন",
    snooze5 = "৫ মি.",
    snooze10 = "১০ মি.",
    snooze15 = "১৫ মি.",
    addMedicine = "নতুন ওষুধ যোগ করুন",
    editMedicine = "ওষুধ সম্পাদনা করুন",
    searchMedicinesHint = "ওষুধ বা রোগীর নাম খুঁজুন...",
    allTypes = "সব",
    medicineNameLabel = "ওষুধের নাম",
    medicineTypeLabel = "ওষুধের ধরন",
    dosageAmountLabel = "ডোজের পরিমাণ",
    dosageUnitLabel = "একক (যেমন mg, ml)",
    mealTimingLabel = "খাবারের সময়",
    patientLabel = "রোগীর প্রোফাইল",
    startDateLabel = "শুরুর তারিখ",
    endDateLabel = "শেষ তারিখ",
    ongoingLabel = "চলমান (শেষ তারিখ নেই)",
    stockCountLabel = "বর্তমান স্টক",
    lowStockThresholdLabel = "কম স্টক সতর্কতা সীমা",
    dosePerIntakeLabel = "প্রতি ডোজে ব্যবহৃত পরিমাণ",
    schedulesLabel = "দৈনিক সময়সূচী",
    addTimeSlot = "সময় যোগ করুন",
    voiceReminderLabel = "ভয়েস রিমাইন্ডার চালু রাখুন",
    prescriptionLabel = "প্রেসক্রিপশন (ছবি / পিডিএফ)",
    pickImage = "ছবি",
    pickPdf = "পিডিএফ ফাইল",
    viewPrescription = "প্রেসক্রিপশন দেখুন",
    notesLabel = "নোট / নির্দেশনা",
    save = "সংরক্ষণ করুন",
    cancel = "বাতিল",
    delete = "মুছে ফেলুন",
    refillTitle = "স্টক রিফিল করুন",
    refillAmountLabel = "যোগ করার পরিমাণ",
    refillButton = "স্টক যোগ করুন",
    remainingStock = "স্টক",
    daysLeftSuffix = "দিন চলবে",
    emptyMedicinesTitle = "এখনো কোনো ওষুধ যোগ করা হয়নি",
    emptyMedicinesBody = "স্মার্ট রিমাইন্ডার সহ ট্যাবলেট, ক্যাপসুল, সিরাপ বা ইনজেকশন যোগ করতে + বাটনে ট্যাপ করুন।",
    emptyDosesTitle = "কোনো ডোজ নির্ধারিত নেই",
    emptyDosesBody = "দৈনিক ডোজ ট্র্যাক করতে নতুন ওষুধ যোগ করুন বা রোগীর প্রোফাইল পরিবর্তন করুন।",
    calendarTitle = "ওষুধের ক্যালেন্ডার",
    exportPdfReport = "PDF সেভ",
    sharePdfReport = "PDF শেয়ার",
    takenCountLabel = "খাওয়া হয়েছে",
    skippedCountLabel = "বাদ",
    missedCountLabel = "মিসড",
    pendingCountLabel = "বাকি",
    profilesSection = "রোগীর প্রোফাইলসমূহ",
    addPatient = "রোগী যোগ করুন",
    editPatient = "রোগী সম্পাদনা",
    patientNameLabel = "রোগীর নাম",
    relationLabel = "সম্পর্ক (যেমন আমি, মা, বাবা)",
    ageLabel = "বয়স (ঐচ্ছিক)",
    appearanceSection = "থিম ও ভাষা",
    themeLabel = "থিম",
    themeSystem = "সিস্টেম",
    themeLight = "লাইট",
    themeDark = "ডার্ক",
    languageLabel = "ভাষা",
    alarmsSection = "অ্যালার্ম ও ভয়েস",
    customAlarmSound = "অ্যালার্ম রিংটোন",
    defaultAlarmSound = "সিস্টেম ডিফল্ট অ্যালার্ম",
    testVoiceReminder = "ভয়েস রিমাইন্ডার পরীক্ষা",
    missedGraceWindow = "মিসড ডোজ সময়সীমা",
    minutesSuffix = "মি.",
    permissionsDiagnostics = "অ্যালার্ম ও ব্যাটারি পারমিশন",
    notificationPerm = "নোটিফিকেশন (Android 13+)",
    exactAlarmPerm = "একজ্যাক্ট অ্যালার্ম (Android 12+)",
    fullScreenPerm = "ফুল-স্ক্রিন অ্যালার্ম অ্যালার্ট",
    batteryOptPerm = "ব্যাটারি অপটিমাইজেশন মুক্ত",
    granted = "সক্রিয়",
    needsAttention = "ঠিক করুন",
    securitySection = "প্রাইভেসি ও অ্যাপ লক",
    appLockTitle = "পিন / বায়োমেট্রিক লক চালু করুন",
    setPinPrompt = "৪-সংখ্যার সিকিউরিটি পিন সেট করুন",
    enterPinPrompt = "মেডি অ্যালার্ট আনলক করতে পিন দিন",
    wrongPinError = "ভুল পিন! আবার চেষ্টা করুন।",
    biometricUnlock = "ফিঙ্গারপ্রিন্ট / ফেস আনলক ব্যবহার করুন",
    unlockWithBiometric = "বায়োমেট্রিক দিয়ে আনলক",
    backupSection = "অফলাইন ব্যাকআপ ও রিস্টোর",
    exportJsonBackup = "JSON ব্যাকআপ সেভ করুন",
    restoreJsonBackup = "JSON ব্যাকআপ রিস্টোর করুন",
    backupExportedMsg = "JSON ব্যাকআপ সফলভাবে সংরক্ষিত হয়েছে",
    backupRestoredMsg = "ব্যাকআপ রিস্টোর এবং অ্যালার্ম পুনরায় সেট হয়েছে",
    backupErrorMsg = "ব্যাকআপ ফাইল পড়া সম্ভব হয়নি",
    pdfSavedMsg = "PDF রিপোর্ট সফলভাবে সংরক্ষিত হয়েছে",
    activeLabel = "সক্রিয়",
    pausedLabel = "বিরতি",
    actionsUnlockHint = "নির্ধারিত সময়ের ১৫ মি. আগে বাটনগুলো চালু হবে",
    batteryOptHint = "ট্যাপ করে Allow বা Unrestricted নির্বাচন করুন",
    aboutSection = "অ্যাপ সম্পর্কে",
    versionLabel = "ভার্সন",
    developerCredit = "Developer: SLM Technology",
    callDeveloperDesc = "ডেভেলপারকে কল করুন",
    whatsappDeveloperDesc = "হোয়াটসঅ্যাপে যোগাযোগ",
    facebookDeveloperDesc = "ফেসবুক প্রোফাইল",
    shareAppDesc = "অ্যাপ শেয়ার করুন",
    shareAppMessage = "সঠিক সময়ে ওষুধ খেতে ব্যবহার করুন Medi Alert — স্মার্ট অফলাইন পিল রিমাইন্ডার, একাধিক রোগীর প্রোফাইল ও স্টক অ্যালার্ট।\n\nসর্বশেষ ভার্সন ডাউনলোড লিংক:\nhttps://github.com/Salimbd71/Medi_Alert/releases/latest/download/medi-alert-release.apk",
    pinLockTitle = "৪-সংখ্যার পিন লক",
    pinLockSetDesc = "পিন লক চালু আছে (পরিবর্তন করতে ট্যাপ করুন)",
    pinLockUnsetDesc = "৪-সংখ্যার পিন কোড দিয়ে অ্যাপ সুরক্ষিত রাখুন",
    biometricLockDesc = "পিন ছাড়াই শুধু ফিঙ্গারপ্রিন্ট বা ফেস দিয়ে আনলক করুন",
    biometricUnavailableMsg = "প্রথমে ফোনের সেটিংসে ফিঙ্গারপ্রিন্ট বা বায়োমেট্রিক লক সেট করুন",
    changePinLabel = "পরিবর্তন",
    openPdfLabel = "খুলুন",
    openPdfButton = "PDF খুলুন",
    pdfSavedToPrefix = "সেভ হয়েছে",
    noPdfReaderMsg = "ফাইলে খোলার জন্য ফোনে কোনো PDF রিডার অ্যাপ পাওয়া যায়নি",
    legalSectionTitle = "প্রাইভেসি পলিসি, শর্তাবলি ও সাপোর্ট",
    privacyPolicyTitle = "প্রাইভেসি পলিসি (Privacy Policy)",
    termsConditionsTitle = "শর্তাবলি (Terms & Conditions)",
    privacyPolicyBody = """
Medi Alert (ডেভেলপার: SLM Technology) একটি ১০০% অফলাইন ওষুধ ব্যবস্থাপনা অ্যাপ। আপনার সকল স্বাস্থ্য তথ্য শুধুমাত্র আপনার ফোনেই সংরক্ষিত থাকে।

১. কোনো তথ্য সংগ্রহ করা হয় না (Zero Data Collection)
• Medi Alert আপনার কোনো ব্যক্তিগত বা চিকিৎসা তথ্য সংগ্রহ, বিক্রি বা কোনো সার্ভারে পাঠায় না।
• রোগীর প্রোফাইল, ওষুধের রুটিন, পিন হ্যাশ এবং প্রেসক্রিপশন সম্পূর্ণ অফলাইনে আপনার ফোনের লোকাল ডাটাবেজে থাকে।

২. ব্যবহৃত অ্যান্ড্রয়েড পারমিশনগুলোর ব্যাখ্যা
• নোটিফিকেশন (POST_NOTIFICATIONS): শুধুমাত্র নির্ধারিত সময়ে ওষুধের অ্যালার্ম, স্নুজ ও কম স্টক সতর্কতা দেখানোর জন্য।
• একজ্যাক্ট অ্যালার্ম (SCHEDULE_EXACT_ALARM): ফোন আইডল অবস্থায় থাকলেও ঠিক নির্ধারিত মিনিটে ওষুধের রিমাইন্ডার বাজানোর জন্য।
• ফুল-স্ক্রিন অ্যালার্ম (USE_FULL_SCREEN_INTENT): জরুরি ওষুধের সময় লক স্ক্রিনের উপরে পূর্ণ পর্দার অ্যালার্ম স্ক্রিন দেখানোর জন্য।
• বুট ও সময় সিঙ্ক (RECEIVE_BOOT_COMPLETED): ফোন রিস্টার্ট, অ্যাপ আপডেট বা টাইমজোন পরিবর্তনের পর সব অ্যালার্ম স্বয়ংক্রিয়ভাবে পুনরায় চালু রাখার জন্য।
• অ্যালার্ম সাউন্ড ও ভয়েস (FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, WAKE_LOCK, VIBRATE): অ্যালার্মের শব্দ, ভাইব্রেশন ও অফলাইন ভয়েস রিমাইন্ডার নিরবচ্ছিন্নভাবে চালানোর জন্য।
• ব্যাটারি অপটিমাইজেশন (REQUEST_IGNORE_BATTERY_OPTIMIZATIONS): ঐচ্ছিক সেটিংস—যাতে ফোনের ব্যাটারি সেভার ওষুধের অ্যালার্ম বন্ধ না করে দেয়।
• ফাইল, প্রেসক্রিপশন ও PDF সেভ: প্রেসক্রিপশনের ছবির জন্য পারমিশন-মুক্ত Android Photo Picker এবং PDF সেভ করার জন্য ফোনের সুরক্ষিত Download/Medi_Alert (MediaStore) ফোল্ডার ব্যবহার করা হয়।
• বায়োমেট্রিক লক (USE_BIOMETRIC): অ্যান্ড্রয়েডের নিজস্ব সুরক্ষিত BiometricPrompt সিস্টেম ব্যবহার করে; অ্যাপ আপনার ফিঙ্গারপ্রিন্টের কোনো তথ্য সংরক্ষণ করে না।

৩. যোগাযোগ
প্রাইভেসি সংক্রান্ত যেকোনো প্রশ্নে Mail Me বাটনে ট্যাপ করে salimbd.ga@gmail.com-এ মেইল করুন।
""".trimIndent(),
    termsConditionsBody = """
SLM Technology-র তৈরি Medi Alert ব্যবহার করার মাধ্যমে আপনি নিচের শর্তাবলিতে সম্মত হচ্ছেন:

১. চিকিৎসা সংক্রান্ত সতর্কীকরণ (Medical Disclaimer)
• Medi Alert শুধুমাত্র সময়মতো ওষুধ মনে করিয়ে দেওয়ার একটি সহায়ক টুল। এটি কোনো ডাক্তারি পরামর্শ বা চিকিৎসা সেবা প্রদান করে না।
• ওষুধের মাত্রা ও সময়সূচির জন্য সর্বদা নিবন্ধিত চিকিৎসক বা ফার্মাসিস্টের পরামর্শ অনুসরণ করুন।

২. অ্যালার্ম ও ব্যবহারকারীর দায়িত্ব
• অ্যালার্ম সঠিক সময়ে বাজার জন্য ফোনের নোটিফিকেশন, একজ্যাক্ট অ্যালার্ম, ব্যাটারি ও সাউন্ড সেটিংস ঠিক রাখা প্রয়োজন।
• ফোন পরিবর্তন বা অ্যাপ আনইনস্টল করার আগে Settings > Offline Backup & Restore থেকে ব্যাকআপ নিয়ে রাখুন।

৩. অফলাইন মালিকানা ও সাপোর্ট
• Download/Medi_Alert ফোল্ডারে সংরক্ষিত PDF এবং JSON ব্যাকআপের সম্পূর্ণ নিয়ন্ত্রণ আপনার কাছেই থাকে।
• যেকোনো মতামত বা সহায়তার জন্য ইমেইল করুন: salimbd.ga@gmail.com।
""".trimIndent(),
    mailMeLabel = "Mail Me",
    closeLabel = "বন্ধ করুন"
)

fun stringsFor(language: AppLanguage): AppStrings =
    if (language == AppLanguage.BANGLA) BanglaStrings else EnglishStrings

val LocalAppStrings = staticCompositionLocalOf { EnglishStrings }
