import org.json.JSONObject
import java.io.File

fun main() {
    val jsonString = File("user_json.txt").readText()
    try {
        val cleanJson = jsonString.trim().trim('\uFEFF')
        val root = JSONObject(cleanJson)
        val workersArray = root.optJSONArray("workers")
        println("Workers count: ${workersArray?.length() ?: 0}")
        val attendanceArray = root.optJSONArray("attendanceRecords")
        println("Attendance count: ${attendanceArray?.length() ?: 0}")
        println("Success")
    } catch(e: Exception) {
        e.printStackTrace()
    }
}
