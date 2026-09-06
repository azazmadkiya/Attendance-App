import org.json.JSONObject

fun main() {
    val jsonString = "\uFEFF{\"version\":1}"
    try {
        val root = JSONObject(jsonString)
        println("Success")
    } catch(e: Exception) {
        println(e)
    }
}
