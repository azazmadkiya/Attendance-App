import org.json.JSONObject

fun main() {
    val jsonString = """{
  "version": 1,
  "exportTimestamp": 1788685720296,
  "workers": [
    {
      "id": 6,
      "name": "Aslam Khureshi ",
      "phone": "9875117251",
      "wageType": "Daily",
      "wageRate": 500,
      "overtimeRate": 0,
      "upiId": "",
      "hajariMultiplier": "Off",
      "overtimeMultiplier": "1.5x",
      "lateFine": 0,
      "lateGracePeriodMinutes": 0,
      "halfDayPayFactor": 0.5,
      "notes": "",
      "createdAt": 1786445751428
    }
  ],
  "attendanceRecords": [
    {
      "id": 16,
      "workerId": 6,
      "date": "2026-08-01",
      "status": "1\/2",
      "checkInTime": "09:00 AM",
      "checkOutTime": "",
      "overtimeHours": 0,
      "customAmount": 0,
      "isGeofenceVerified": false,
      "latitude": 0,
      "longitude": 0,
      "notes": ""
    }
  ],
  "cashbookEntries": [],
  "notificationSetting": {
    "id": 1,
    "dailyReminderEnabled": true,
    "reminderTime": "10:00 AM",
    "missedCheckoutNudge": true,
    "weeklyReportEnabled": true,
    "hideAmounts": true
  }
}"""
    try {
        val root = JSONObject(jsonString)
        println("Success")
    } catch(e: Exception) {
        println(e)
    }
}
