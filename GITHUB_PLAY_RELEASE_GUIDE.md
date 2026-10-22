# GitHub se Play Store tak: automatic release ki guide

## Ye kaise kaam karta hai (aasaan alfaaz me)

Pehle aap khud: Android Studio me `.aab` file banate, phir Play Console me ja kar upload karte the.

Ab ye kaam GitHub ka ek "robot" karega (isay **GitHub Actions workflow** kehte hain):

1. Aap GitHub par **"Run workflow"** ka button dabate hain (ya ek naya version tag push karte hain).
2. GitHub ka ek khali computer chalta hai. Wo aap ka code leta hai.
3. Wo aap ki **signing key** (jo GitHub ke "Secrets" me band hai) laga kar `.aab` banata hai.
4. Har dafa khud ek naya **version number** deta hai (aap ko yaad nahi rakhna).
5. File seedhi **Play Console** me upload kar deta hai, jis track par aap ne kaha ho (internal / alpha / beta / production).
6. Kaam ke baad us computer se saari keys mita deta hai.

Is me lagbhag **10-15 minute** lagte hain. Aap is dauran kuch aur kar sakte hain.

Workflow file: [.github/workflows/play-release.yml](.github/workflows/play-release.yml)

---

## Ek dafa ki setup (sirf pehli baar)

### 1) GitHub par private repository

- github.com par **New repository** banayein, **Private** zaroor rakhein (code me NRSP ka kaam hai).
- Code push ho jayega (Claude ya aap `git push` se).
- Ye files GitHub par **nahi** jati (`.gitignore` me hain): `local.properties`, `keystore.properties`,
  `.jks` key, `app/google-services.json`, presentation/video, purani scripts aur `backend_fix`.

### 2) Upload key (signing key) banayein

Project folder me ek dafa:

```
keytool -genkeypair -v -keystore asaan-upload.jks -alias asaan-upload -keyalg RSA -keysize 2048 -validity 10000
```

- Passwords aur ye `.jks` file **sambhal kar** rakhein (do jagah backup). Ye NRSP ki milkiyat hai.
- Play Console me **Play App Signing** on rakhein: agar upload key kabhi gum ho jaye to Google se reset ho sakti hai.

### 3) Play Console me app aur pehla upload (khud)

Google ka rule hai ke **nayi app ka pehla upload haath se** hota hai:

1. Play Console me app banayein (package name: `com.assan.finance`).
2. Apne PC par `keystore.properties` banayein (`keystore.properties.example` dekhein), phir:
   ```
   gradlew.bat :app:bundleRelease
   ```
3. `app/build/outputs/bundle/release/app-release.aab` ko **Internal testing** me upload kar ke release kar dein.

Is ke baad har agla upload GitHub khud karega.

### 4) Google Play ke liye "service account" (robot ka login)

Ye ek alag account hai jis se GitHub, Play Console me upload kar sakta hai. Aap ka apna password GitHub ko nahi diya jata.

1. **Google Cloud Console** (console.cloud.google.com) me ek project chunein ya banayein.
2. **APIs & Services → Library** me **"Google Play Android Developer API"** enable karein.
3. **IAM & Admin → Service Accounts → Create service account** (naam: `github-play-upload`).
4. Us service account me **Keys → Add key → JSON**: ek `.json` file download hogi. Isay sambhal kar rakhein, ye password jaisi hai.
5. **Play Console → Users and permissions → Invite new user** me us service account ka email daalein
   (`...@....iam.gserviceaccount.com`) aur sirf **Asaan Finance app** ki ye permissions dein:
   *Release apps to testing tracks*, aur production chahiye to *Release to production*.

### 5) GitHub Secrets (sab chabiyan ek band tijori me)

GitHub repo → **Settings → Secrets and variables → Actions → New repository secret**. Ye 7 secret banayein:

| Secret ka naam | Is me kya daalna hai |
|---|---|
| `KEYSTORE_BASE64` | `.jks` file ka base64 (neeche command) |
| `KEYSTORE_PASSWORD` | keystore ka password |
| `KEY_ALIAS` | `asaan-upload` (jo alias aap ne rakha) |
| `KEY_PASSWORD` | key ka password |
| `GOOGLE_SERVICES_JSON_BASE64` | `app/google-services.json` ka base64 |
| `GEMINI_API_KEY` | Gemini ki key (`local.properties` me `GEMINI_API_KEY=` wali) |
| `PLAY_SERVICE_ACCOUNT_JSON` | Step 4 wali `.json` file ka **poora text** (Notepad me khol kar copy) |

Base64 banane ke liye PowerShell (project folder me):

```
[Convert]::ToBase64String([IO.File]::ReadAllBytes("asaan-upload.jks")) | Set-Clipboard
[Convert]::ToBase64String([IO.File]::ReadAllBytes("app\google-services.json")) | Set-Clipboard
```

Har command ke baad clipboard me text aa jata hai, GitHub me paste kar dein.

---

## Rozana ka kaam: naya version Play Store par bhejna

**Tareeqa A: button se (sab se aasaan)**

1. Code GitHub par push karein.
2. GitHub repo → **Actions** → **Play Store release** → **Run workflow**.
3. **track** chunein:
   - `internal`: sirf aap ki testing team (pehle hamesha yahan bhejein)
   - `alpha` / `beta`: closed / open testing
   - `production`: sab users
4. **status**: `completed` (release foran nikal jaye) ya `draft` (aap Play Console me khud "Release" dabayein).
5. Hara tick ✅ aa jaye to kaam ho gaya. Laal ❌ ho to us par click kar ke dekhein kis step me masla hai.

**Tareeqa B: version tag se** (seedha `internal` par jata hai)

```
git tag v1.0.1
git push origin v1.0.1
```

Version ka naam tag se (`1.0.1`) aur version number khud ban jata hai.

**Mashwara:** pehle `internal` par bhejein, phone par check karein, phir Play Console me usi release ko
**Promote** kar ke production par le jayein. Is tarah ghalat build kabhi seedha users tak nahi jata.

Har run ki `.aab` file aur `mapping.txt` us run ke page par **Artifacts** me 30 din tak download ki ja sakti hai.

---

## Aam masle aur un ka hal

| Error | Matlab | Hal |
|---|---|---|
| `Only releases with status draft may be created on draft app` | App abhi tak kabhi release nahi hui | Pehla upload haath se karein (Step 3), ya `status = draft` chunein |
| `APK specifies a version code that has already been used` | Ye version number pehle istemal ho chuka | Workflow dobara chalayein (naya run = naya number) |
| `The caller does not have permission` | Service account ko Play Console me permission nahi | Step 4.5 dobara dekhein; permission milne me kuch der lag sakti hai |
| `Keystore was tampered with, or password was incorrect` | Key ya password ghalat | `KEYSTORE_BASE64` aur passwords wale secrets check karein |
| `File google-services.json is missing` | Secret khali hai | `GOOGLE_SERVICES_JSON_BASE64` secret dobara daalein |
| Assistants kaam nahi karte | Gemini key nahi lagi | `GEMINI_API_KEY` secret check karein |

---

## Hifazat ke usool

- Repository **Private** rahe. Sirf zaroori logon ko access dein.
- Keys aur passwords **sirf GitHub Secrets** me. Kabhi code, chat ya email me nahi.
- `.jks` file aur service account `.json` ka backup kisi mehfooz jagah (NRSP IT ke paas) rakhein.
- Koi key leak ho jaye to foran badlein: Gemini key Google AI Studio me, service account key Google Cloud me
  (purani delete kar ke nayi banayein, phir GitHub secret update karein).
