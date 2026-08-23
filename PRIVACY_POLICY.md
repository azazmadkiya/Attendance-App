# Privacy Policy for Attendance App (Haazri Pro)

**Effective Date:** August 23, 2026  
**Developer:** Azaz Madkiya  
**Contact Email:** [azazmadkiya@gmail.com](mailto:azazmadkiya@gmail.com)  
**Application:** Attendance App (Haazri Pro)

---

## 1. Overview
Attendance App ("Haazri Pro", "we", "us", or "our"), developed by Azaz Madkiya, is an Android mobile application designed to simplify staff attendance logging, wage calculations, overtime tracking, and cashbook bookkeeping for small businesses, contractors, and site supervisors.

This Privacy Policy explains what data the app collects, how it is stored, how permissions are utilized, and how you can exercise your data rights in compliance with **Google Play Developer Policies**, GDPR, and local privacy regulations.

---

## 2. Information We Collect and Process
Depending on the features you use:
- **User Profile:** Full name, firm/company name, email address, and phone number.
- **Worker & Staff Data:** Worker full names, phone numbers, wage types (daily, monthly, hourly, piece-rate), and daily attendance statuses.
- **Cashbook Transactions:** Advance salary payments, expense entries, and notes.
- **Authentication Identifiers:** When choosing to sign in via Google Sign-In, authentication tokens are validated through Firebase Auth without saving your account passwords.

---

## 3. Location & Geofencing Policy
- **Purpose:** Optional Geofence Attendance Verification allows supervisors to ensure attendance is recorded only when within the configured work site radius.
- **Scope:** GPS coordinates are read strictly in the foreground during an active check-in action.
- **No Background Tracking:** The application **does not** track user location in the background or when closed.
- **No Monetization:** Location coordinates are never transmitted to advertisement networks or commercial brokers.

---

## 4. Android Device Permissions
| Permission | Classification | Purpose |
| :--- | :--- | :--- |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Optional | Verifies geofenced work site perimeter during check-in. |
| `POST_NOTIFICATIONS` | Configurable | Daily attendance reminder alerts and backup nudges. |
| `READ_CONTACTS` | Optional | Allows selecting worker phone numbers from phonebook. |
| `STORAGE / FILE_PROVIDER` | Local File Access | Generating and sharing PDF salary slips and Excel reports. |

---

## 5. Data Storage & Security
- **Local SQLite Database (Room):** All worker and ledger data are sandboxed on your device.
- **App Lock:** 4-digit PIN security lock protects access to financial records and worker profiles.
- **Encrypted Transmission:** Any cloud operations (Google Sign-In, Firebase backup sync) are encrypted using standard HTTPS/TLS.

---

## 6. User Rights & Data Deletion
In accordance with Google Play's Account and Data Deletion policies:
1. **Local Data Wipe:** You can wipe all local databases and records instantly by going to **Settings > Legal & App Policies > Data Safety & Permissions > Wipe All Local App Data**.
2. **Cloud Deletion:** If you wish to delete synchronized cloud backups or your Google account association, submit an email request to [azazmadkiya@gmail.com](mailto:azazmadkiya@gmail.com) with the subject `Data Deletion Request`. We process all requests within 48 hours.

---

## 7. Third-Party Services
- **Google Play Services:** Core Android platform APIs.
- **Firebase Authentication:** Secure OAuth user login.
- **Google Maps Platform:** Interactive map visualization for geofence selection.

---

## 8. Children's Privacy
The app is not directed to children under the age of 13. We do not knowingly collect personal data from minors.

---

## 9. Contact Developer
If you have questions or suggestions regarding this policy, please contact:
- **Developer:** Azaz Madkiya
- **Email:** [azazmadkiya@gmail.com](mailto:azazmadkiya@gmail.com)
