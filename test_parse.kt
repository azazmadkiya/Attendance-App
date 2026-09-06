import org.json.JSONObject

fun main() {
    val jsonString = """{
  "version": 1,
  "exportTimestamp": 1788685720296,
  "workers": [],
  "attendanceRecords": [],
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
