# Tab - Minimalist Expense Tracker

<div align="center">
  <h3>📱 Track your expenses effortlessly with SMS automation</h3>
  <p>An Android-only expense tracker that syncs to Google Sheets</p>
</div>

---

## ✨ Features

### 🖊️ Manual Entry
- **Date Picker**: Calendar selection with today as default
- **Category Dropdown**: Predefined categories with emojis
- **Description Field**: Free text for expense details
- **Amount Input**: Numeric input with currency support

### 💱 Currency Toggle
- **MVR** (Maldivian Rufiyaa) - Default
- **USD** (US Dollar) - Auto-converts to MVR (×15.42)
- Real-time conversion display

### 📲 SMS Automation
- **Permission-based**: Requires READ_SMS and RECEIVE_SMS
- **Allowed Senders**: User-defined whitelist of sender numbers
- **Smart Parsing**: Regex-based extraction of Date, Description, and Amount
- **Notifications**: Local notifications with pre-filled expense details
- **One-tap Confirmation**: Click notification to review and save

### ☁️ Google Sheets Integration
- **Custom Spreadsheet**: User provides Spreadsheet ID
- **Sheet Name**: Flexible sheet targeting
- **Service Account Auth**: JSON credentials for API access
- **Auto-sync**: Background sync of unsynced expenses
- **Header Management**: Automatically creates column headers

### 🎨 Design
- **Color Palette**:
  - Primary (Deep Slate): `#2C3E50`
  - Accent (Mint Green): `#27AE60`
  - Background (Cool Gray): `#F4F7F6`
  - Highlight (Soft Gold): `#F1C40F`
- **Light & Dark Mode**: System-based theme switching
- **Material Design 3**: Modern, clean UI with Jetpack Compose

---

## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room (Local caching)
- **Cloud Storage**: Google Sheets API v4
- **DI**: Hilt (Dependency Injection)
- **Background Tasks**: BroadcastReceiver + WorkManager
- **Preferences**: DataStore

### Project Structure
```
app/src/main/java/com/tab/expense/
├── data/
│   ├── local/
│   │   ├── database/        # Room database & DAOs
│   │   └── entity/          # Data entities
│   ├── remote/              # Google Sheets service
│   └── repository/          # Repository pattern
├── service/
│   ├── SmsReceiver.kt       # SMS broadcast receiver
│   ├── SmsParser.kt         # Regex-based parser
│   └── NotificationService.kt
├── ui/
│   ├── screens/             # Compose screens
│   ├── components/          # Reusable UI components
│   ├── theme/               # Color, Typography, Theme
│   └── navigation/          # Navigation graph
├── util/                    # Utilities & constants
└── MainActivity.kt
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/SillyXilly/tabs.git
   cd tabs
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Sync Gradle**
   - Android Studio will automatically sync dependencies
   - If not, click "Sync Project with Gradle Files"

4. **Build the APK** (via GitHub Actions)
   - Push to `main` or `develop` branch
   - GitHub Actions will automatically build the APK
   - Download from the Actions tab → Artifacts

---

## 📋 Configuration

### Google Sheets Setup

1. **Create a Google Cloud Project**
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project
   - Enable "Google Sheets API"

2. **Create Service Account**
   - Navigate to "IAM & Admin" → "Service Accounts"
   - Create a service account
   - Generate a JSON key file

3. **Share Your Google Sheet**
   - Create a Google Sheet for expenses
   - Share it with the service account email (found in JSON)
   - Give "Editor" permissions

4. **Configure in App**
   - Open Settings in the app
   - Enter your Spreadsheet ID (from the sheet URL)
   - Enter your Sheet Name (e.g., "Expenses")
   - Paste the JSON credentials

### SMS Automation Setup

1. **Grant Permissions**
   - Allow "Read SMS" and "Receive SMS" when prompted

2. **Add Allowed Senders**
   - Go to Settings → SMS Automation
   - Add phone numbers of your bank/payment providers
   - Format: `+960 7301000, +960 9876543`

---

## 📱 Usage

### Manual Entry
1. Tap the **+** button on the Summary screen
2. Select a date (default: today)
3. Choose a category from the dropdown
4. Enter description and amount
5. Toggle currency if needed (MVR ↔ USD)
6. Tap "Add Expense"

### SMS Auto-Detection
1. Receive a transaction SMS from an allowed sender
2. Get a notification with parsed expense details
3. Tap the notification to review
4. Confirm or edit the details
5. Tap "Save Expense"

### View Summary
- See monthly total at the top
- Browse recent expenses below
- Each expense shows category, description, date, and amount

---

## 🔨 Build & Deploy

### Local Build
```bash
# Debug APK
./gradlew assembleDebug

# Release APK (unsigned)
./gradlew assembleRelease
```

### GitHub Actions CI/CD
- **Automatic Builds**: On push to main/develop
- **Artifacts**: Debug and Release APKs uploaded
- **Releases**: Tag with `v*` to create a GitHub release

```bash
# Create a release
git tag v1.0.0
git push origin v1.0.0
```

---

## 📦 SMS Patterns Supported

The app uses regex patterns to extract expenses from various SMS formats:

```
Pattern 1: "You spent Rs.1,250.00 at MERCHANT_NAME on 15-Jan-2026"
Pattern 2: "Debit of USD 45.50 from your account for SHOP_NAME"
Pattern 3: "MVR 1250.00 debited for MERCHANT on 15/01/2026"
Pattern 4: "Transaction of Rs 500 at MERCHANT_NAME"
Pattern 5: "Your card ending 1234 was used for Rs.750.00 at SHOP"
Pattern 6: Generic amount and merchant detection
```

You can extend patterns in `SmsParser.kt`.

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

## 🙏 Acknowledgments

- **Material Design 3** for UI guidelines
- **Google Sheets API** for cloud sync
- **Jetpack Compose** for modern UI development

---

## 📞 Support

For issues, questions, or feature requests, please [open an issue](https://github.com/SillyXilly/tabs/issues).

---

<div align="center">
  <p>Made with ❤️ for effortless expense tracking</p>
  <p>🔧 Built with Kotlin & Jetpack Compose</p>
</div>
