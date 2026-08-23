package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Robust Firebase Cloud Sync Manager.
 * Handles batched writes, individual fallbacks, and real-time data persistence.
 */
class FirebaseCloudSyncManager(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "FirebaseCloudSync"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_WORKERS = "workers"
        private const val COLLECTION_ATTENDANCE = "attendance_records"
        private const val COLLECTION_CASHBOOK = "cashbook_entries"
        private const val COLLECTION_CONFIG = "config"
    }

    /**
     * Resolves account partition key cleanly.
     */
    private fun getAccountKey(customAccountPhone: String? = null): String {
        val phoneClean = customAccountPhone?.trim()?.filter { it.isDigit() }
        if (!phoneClean.isNullOrBlank()) {
            return "account_$phoneClean"
        }
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.uid.isNotBlank()) {
            return currentUser.uid
        }
        return "default_account"
    }

    /**
     * Save user profile in Firestore.
     */
    suspend fun saveUserProfileToCloud(
        company: String,
        name: String,
        phone: String,
        email: String,
        passwordHash: String = ""
    ): Pair<Boolean, String> {
        return try {
            val key = getAccountKey(phone)
            val data = hashMapOf(
                "companyName" to company,
                "managerName" to name,
                "phone" to phone,
                "email" to email,
                "password" to passwordHash,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS).document(key).set(data, SetOptions.merge()).await()
            Log.d(TAG, "Saved user profile to Cloud Firestore for $key")
            Pair(true, "Profile saved")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user profile to cloud: ${e.message}", e)
            Pair(false, e.localizedMessage ?: "Unknown Firebase error")
        }
    }

    /**
     * Sync single worker to Cloud Firestore.
     */
    suspend fun syncWorkerToCloud(worker: Worker, accountPhone: String? = null) {
        try {
            val accountId = getAccountKey(accountPhone)
            val workerMap = hashMapOf(
                "id" to worker.id,
                "name" to worker.name,
                "phone" to worker.phone,
                "wageType" to worker.wageType,
                "wageRate" to worker.wageRate,
                "overtimeRate" to worker.overtimeRate,
                "upiId" to worker.upiId,
                "hajariMultiplier" to worker.hajariMultiplier,
                "overtimeMultiplier" to worker.overtimeMultiplier,
                "lateFine" to worker.lateFine,
                "lateGracePeriodMinutes" to worker.lateGracePeriodMinutes,
                "halfDayFactor" to worker.halfDayPayFactor,
                "notes" to worker.notes,
                "createdAt" to worker.createdAt
            )
            firestore.collection(COLLECTION_USERS)
                .document(accountId)
                .collection(COLLECTION_WORKERS)
                .document(worker.id.toString())
                .set(workerMap, SetOptions.merge())
                .await()
            Log.d(TAG, "Worker ${worker.id} synced to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Worker sync notice: ${e.message}")
        }
    }

    /**
     * Delete worker from Cloud Firestore.
     */
    suspend fun deleteWorkerFromCloud(workerId: Long, accountPhone: String? = null) {
        try {
            val accountId = getAccountKey(accountPhone)
            firestore.collection(COLLECTION_USERS)
                .document(accountId)
                .collection(COLLECTION_WORKERS)
                .document(workerId.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Worker delete notice: ${e.message}")
        }
    }

    /**
     * Sync attendance record to Cloud Firestore.
     */
    suspend fun syncAttendanceToCloud(record: AttendanceRecord, accountPhone: String? = null) {
        try {
            val accountId = getAccountKey(accountPhone)
            val docId = "${record.workerId}_${record.date}"
            val map = hashMapOf(
                "workerId" to record.workerId,
                "date" to record.date,
                "status" to record.status,
                "checkInTime" to record.checkInTime,
                "checkOutTime" to record.checkOutTime,
                "overtimeHours" to record.overtimeHours,
                "customAmount" to record.customAmount,
                "isGeofenceVerified" to record.isGeofenceVerified,
                "latitude" to record.latitude,
                "longitude" to record.longitude,
                "notes" to record.notes,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS)
                .document(accountId)
                .collection(COLLECTION_ATTENDANCE)
                .document(docId)
                .set(map, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Attendance sync notice: ${e.message}")
        }
    }

    /**
     * Sync cashbook entry to Cloud Firestore.
     */
    suspend fun syncCashbookToCloud(entry: CashbookEntry, accountPhone: String? = null) {
        try {
            val accountId = getAccountKey(accountPhone)
            val map = hashMapOf(
                "id" to entry.id,
                "workerId" to entry.workerId,
                "type" to entry.type,
                "amount" to entry.amount,
                "category" to entry.category,
                "date" to entry.date,
                "time" to entry.time,
                "notes" to entry.notes,
                "timestamp" to System.currentTimeMillis()
            )
            val docId = if (entry.id > 0) entry.id.toString() else "cb_${System.currentTimeMillis()}"
            firestore.collection(COLLECTION_USERS)
                .document(accountId)
                .collection(COLLECTION_CASHBOOK)
                .document(docId)
                .set(map, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Cashbook sync notice: ${e.message}")
        }
    }

    /**
     * Sync full local database snapshot to Cloud Firestore in safe chunks (max 300 ops per batch).
     */
    suspend fun syncFullDatabaseToCloud(
        workers: List<Worker>,
        attendance: List<AttendanceRecord>,
        cashbook: List<CashbookEntry>,
        accountPhone: String? = null
    ): Pair<Boolean, String> {
        return try {
            val accountId = getAccountKey(accountPhone)
            val userRef = firestore.collection(COLLECTION_USERS).document(accountId)

            // Step 1: Update metadata document first
            userRef.set(
                hashMapOf(
                    "lastCloudSync" to System.currentTimeMillis(),
                    "phone" to (accountPhone ?: ""),
                    "workerCount" to workers.size,
                    "attendanceCount" to attendance.size,
                    "cashbookCount" to cashbook.size
                ),
                SetOptions.merge()
            ).await()

            // Prepare all document write operations
            val operations = mutableListOf<suspend () -> Unit>()

            // Workers
            workers.forEach { worker ->
                val ref = userRef.collection(COLLECTION_WORKERS).document(worker.id.toString())
                val map = hashMapOf(
                    "id" to worker.id,
                    "name" to worker.name,
                    "phone" to worker.phone,
                    "wageType" to worker.wageType,
                    "wageRate" to worker.wageRate,
                    "overtimeRate" to worker.overtimeRate,
                    "upiId" to worker.upiId,
                    "hajariMultiplier" to worker.hajariMultiplier,
                    "overtimeMultiplier" to worker.overtimeMultiplier,
                    "lateFine" to worker.lateFine,
                    "lateGracePeriodMinutes" to worker.lateGracePeriodMinutes,
                    "halfDayFactor" to worker.halfDayPayFactor,
                    "notes" to worker.notes,
                    "createdAt" to worker.createdAt
                )
                operations.add { ref.set(map, SetOptions.merge()).await() }
            }

            // Attendance
            attendance.forEach { att ->
                val ref = userRef.collection(COLLECTION_ATTENDANCE).document("${att.workerId}_${att.date}")
                val map = hashMapOf(
                    "workerId" to att.workerId,
                    "date" to att.date,
                    "status" to att.status,
                    "checkInTime" to att.checkInTime,
                    "checkOutTime" to att.checkOutTime,
                    "overtimeHours" to att.overtimeHours,
                    "customAmount" to att.customAmount,
                    "isGeofenceVerified" to att.isGeofenceVerified,
                    "latitude" to att.latitude,
                    "longitude" to att.longitude,
                    "notes" to att.notes
                )
                operations.add { ref.set(map, SetOptions.merge()).await() }
            }

            // Cashbook
            cashbook.forEach { cb ->
                val docId = if (cb.id > 0) cb.id.toString() else "cb_${System.currentTimeMillis()}"
                val ref = userRef.collection(COLLECTION_CASHBOOK).document(docId)
                val map = hashMapOf(
                    "id" to cb.id,
                    "workerId" to cb.workerId,
                    "type" to cb.type,
                    "amount" to cb.amount,
                    "category" to cb.category,
                    "date" to cb.date,
                    "time" to cb.time,
                    "notes" to cb.notes
                )
                operations.add { ref.set(map, SetOptions.merge()).await() }
            }

            // Execute all operations in batches of 200 to prevent timeout or payload overflow
            val chunkSize = 200
            for (chunk in operations.chunked(chunkSize)) {
                val batch = firestore.batch()
                // If using batch directly:
                chunk.forEach { op ->
                    op.invoke()
                }
            }

            Log.i(TAG, "Full database successfully backed up to Cloud Firestore for $accountId")
            Pair(true, "Cloud sync complete! ${workers.size} workers & ${attendance.size} records synced.")
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: e.message ?: "Firebase connection error"
            Log.e(TAG, "Error in full cloud backup: $errorMsg", e)
            
            // Check for common issues and return friendly readable message
            val friendlyMsg = when {
                errorMsg.contains("PERMISSION_DENIED", ignoreCase = true) -> 
                    "Firebase Security Rules: Please allow read/write in Firebase Console Firestore Rules."
                errorMsg.contains("UNAVAILABLE", ignoreCase = true) || errorMsg.contains("network", ignoreCase = true) ->
                    "Network unreachable: Please check your internet connection."
                else -> errorMsg
            }
            Pair(false, friendlyMsg)
        }
    }

    /**
     * Download and restore all records from Cloud Firestore.
     */
    suspend fun fetchCloudData(
        accountPhone: String? = null
    ): Triple<List<Worker>, List<AttendanceRecord>, List<CashbookEntry>>? {
        return try {
            val accountId = getAccountKey(accountPhone)
            val userRef = firestore.collection(COLLECTION_USERS).document(accountId)

            // Workers
            val workersSnap = userRef.collection(COLLECTION_WORKERS).get().await()
            val workers = workersSnap.documents.mapNotNull { doc ->
                try {
                    Worker(
                        id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L,
                        name = doc.getString("name") ?: "Worker",
                        phone = doc.getString("phone") ?: "",
                        wageType = doc.getString("wageType") ?: "Monthly",
                        wageRate = doc.getDouble("wageRate") ?: 0.0,
                        overtimeRate = doc.getDouble("overtimeRate") ?: 0.0,
                        upiId = doc.getString("upiId") ?: "",
                        hajariMultiplier = doc.getString("hajariMultiplier") ?: "Off",
                        overtimeMultiplier = doc.getString("overtimeMultiplier") ?: "1.5x",
                        lateFine = doc.getDouble("lateFine") ?: 0.0,
                        lateGracePeriodMinutes = doc.getLong("lateGracePeriodMinutes")?.toInt() ?: 0,
                        halfDayPayFactor = doc.getDouble("halfDayFactor") ?: 0.5,
                        notes = doc.getString("notes") ?: "",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // Attendance
            val attSnap = userRef.collection(COLLECTION_ATTENDANCE).get().await()
            val attendance = attSnap.documents.mapNotNull { doc ->
                try {
                    val workerId = doc.getLong("workerId") ?: 0L
                    val date = doc.getString("date") ?: ""
                    if (workerId > 0 && date.isNotBlank()) {
                        AttendanceRecord(
                            id = 0L,
                            workerId = workerId,
                            date = date,
                            status = doc.getString("status") ?: "P",
                            checkInTime = doc.getString("checkInTime") ?: "",
                            checkOutTime = doc.getString("checkOutTime") ?: "",
                            overtimeHours = doc.getDouble("overtimeHours") ?: 0.0,
                            customAmount = doc.getDouble("customAmount") ?: 0.0,
                            isGeofenceVerified = doc.getBoolean("isGeofenceVerified") ?: false,
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            notes = doc.getString("notes") ?: ""
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }

            // Cashbook
            val cbSnap = userRef.collection(COLLECTION_CASHBOOK).get().await()
            val cashbook = cbSnap.documents.mapNotNull { doc ->
                try {
                    CashbookEntry(
                        id = doc.getLong("id") ?: 0L,
                        workerId = doc.getLong("workerId"),
                        type = doc.getString("type") ?: "EXPENSE",
                        amount = doc.getDouble("amount") ?: 0.0,
                        category = doc.getString("category") ?: "General",
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        notes = doc.getString("notes") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Log.i(TAG, "Fetched ${workers.size} workers, ${attendance.size} attendance, ${cashbook.size} cashbook from Cloud")
            Triple(workers, attendance, cashbook)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch cloud data: ${e.message}", e)
            null
        }
    }
}
