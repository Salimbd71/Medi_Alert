# মেডি অ্যালার্ট (Medi Alert) — সেটআপ, GitHub Secrets এবং APK/AAB রিলিজ গাইড

এই নির্দেশিকায় আপনি শিখবেন কীভাবে নিজের Android Studio প্রজেক্ট সেটআপ করবেন, রিলিজ সাইনিং কীস্টোর তৈরি করবেন, GitHub Secrets কনফিগার করবেন এবং `.github/workflows/release.yml` ব্যবহার করে স্বয়ংক্রিয়ভাবে Signed APK ও AAB রিলিজ করবেন।

---

## ১. প্রজেক্ট সেটআপ (Android Studio)

1. রিপোজিটরি ক্লোন করুন এবং **Android Studio** (Ladybug বা তার পরবর্তী ভার্সন) দিয়ে ওপেন করুন।
2. **JDK 17** Gradle JDK হিসেবে নির্বাচিত আছে কিনা নিশ্চিত করুন (`Settings -> Build, Execution, Deployment -> Build Tools -> Gradle`)।
3. **Sync Project with Gradle Files** ক্লিক করুন।
4. কোনো Firebase বা API Key ফাইল (`google-services.json` বা `.env`) প্রয়োজন নেই — অ্যাপটি ১০০% অফলাইন।

---

## ২. রিলিজ সাইনিং Keystore তৈরি করা

Google Play Store-এ AAB বা Signed APK প্রকাশ করার জন্য একটি upload keystore প্রয়োজন। টার্মিনালে নিচের কমান্ডটি চালান (অ্যালিয়াস হিসেবে `master_upload` ব্যবহার করুন):

```bash
keytool -genkey -v -keystore my-upload-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias master_upload
```

কমান্ড চালানোর সময়:
- **Keystore password** (`MASTER_STORE_PASSWORD`) এবং **Key password** (`MASTER_KEY_PASSWORD`) নিরাপদ স্থানে সংরক্ষণ করুন।
- Keystore ফাইলটিকে Git-এ কমিট করবেন না।

### Keystore-কে Base64 টেক্সটে রূপান্তর (GitHub Secret-এর জন্য)

**Linux / macOS:**
```bash
base64 -w 0 my-upload-key.jks > keystore.base64.txt
```
*(macOS-এ `-w 0`-এর বদলে `base64 -i my-upload-key.jks | tr -d '\n'` ব্যবহার করুন)*

**Windows (PowerShell):**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("my-upload-key.jks")) | Set-Content keystore.base64.txt
```

`keystore.base64.txt` ফাইলের ভেতরের সম্পূর্ণ লেখাটি কপি করুন।

---

## ৩. GitHub Secrets কনফিগারেশন

আপনার GitHub রিপোজিটরিতে যান:
**Settings → Secrets and variables → Actions → New repository secret**

নিচের তিনটি সিক্রেট যোগ করুন:

| Secret Name | মান (Value) | বিবরণ |
| :--- | :--- | :--- |
| `MASTER_KEYSTORE_BASE64` | Base64 স্ট্রিং | `my-upload-key.jks` ফাইলের Base64 এনকোডেড টেক্সট |
| `MASTER_STORE_PASSWORD` | স্টোর পাসওয়ার্ড | Keystore তৈরির সময় দেওয়া Store Password |
| `MASTER_KEY_PASSWORD` | কী পাসওয়ার্ড | `master_upload` alias-এর Key Password |

> **সতর্কতা:** কখনোই পাসওয়ার্ড বা Base64 কী সরাসরি কোড বা README-তে লিখবেন না।

---

## ৪. GitHub Actions দিয়ে APK ও AAB রিলিজ করার নিয়ম

এই প্রজেক্টে `.github/workflows/release.yml` নামে একটি ম্যানুয়াল (`workflow_dispatch`) ওয়ার্কফ্লো রয়েছে।

### ধাপসমূহ:
1. GitHub রিপোজিটরির **Actions** ট্যাবে যান।
2. বাম পাশ থেকে **Medi Alert Release CI/CD** সিলেক্ট করুন।
3. ডান পাশের **Run workflow** বাটনে ক্লিক করুন এবং অপশনগুলো নির্বাচন করুন:
   - **`version`**: যেমন `1.0.0` লিখুন। খালি রাখলে সর্বশেষ Git ট্যাগ দেখে স্বয়ংক্রিয়ভাবে পরবর্তী patch ভার্সন (যেমন `1.0.1`) তৈরি হবে।
   - **`run_tests`**: বিল্ডের আগে ইউনিট ও Robolectric টেস্ট চালাবে (ডিফল্ট `true`)।
   - **`build_debug_apk`**: Debug APK তৈরি করতে চাইলে টিক দিন (ডিফল্ট `false`)।
   - **`build_release_apk`**: Signed Release APK তৈরি করবে (ডিফল্ট `true`)।
   - **`build_release_aab`**: Play Store-এর জন্য Signed Release AAB তৈরি করবে (ডিফল্ট `true`)।
   - **`publish_release`**: স্বয়ংক্রিয়ভাবে `vVERSION` ট্যাগ ও GitHub Release পাবলিশ করবে (ডিফল্ট `true`)।
4. **Run workflow** চাপুন।

### ফলাফল:
- সব সাইনিং সিক্রেট সঠিক থাকলে ওয়ার্কফ্লো সফলভাবে টেস্ট চালাবে, APK/AAB বিল্ড করবে এবং **Workflow Summary**-তে ভার্সন, টেস্ট স্ট্যাটাস ও ফাইলের তালিকা দেখাবে।
- তৈরি হওয়া `medi-alert-release.apk` ও `medi-alert-vX.Y.Z-release.aab` ফাইলগুলো **Actions Artifacts** এবং **GitHub Releases** পেজে ডাউনলোডের জন্য পাওয়া যাবে। প্রতিটি নতুন রিলিজে ফিক্সড ডাউনলোড লিংক থাকবে: `https://github.com/Salimbd71/Medi_Alert/releases/latest/download/medi-alert-release.apk`।

---

## ৫. স্থানীয়ভাবে (Local) APK / AAB তৈরি

```bash
export KEYSTORE_PATH=./my-upload-key.jks
export STORE_PASSWORD="আপনার_স্টোর_পাসওয়ার্ড"
export KEY_PASSWORD="আপনার_কী_পাসওয়ার্ড"

# Release APK তৈরি
gradle :app:assembleRelease -PappVersionName=1.0.0 -PappVersionCode=10000

# Release AAB তৈরি (Google Play Store-এর জন্য)
gradle :app:bundleRelease -PappVersionName=1.0.0 -PappVersionCode=10000
```

আউটপুট ফাইল লোকেশন:
- **APK:** `app/build/outputs/apk/release/app-release.apk`
- **AAB:** `app/build/outputs/bundle/release/app-release.aab`
