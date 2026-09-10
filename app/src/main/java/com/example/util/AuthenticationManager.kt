package com.example.util

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest
import java.util.UUID

/**
 * User profile details stored persistently in Firebase Cloud.
 */
data class UserProfile(
    val uid: String = "",
    val companyName: String = "",
    val managerName: String = "",
    val phone: String = "",
    val email: String = ""
)

/**
 * Result state for authentication actions.
 */
sealed class AuthResult {
    data class Success(val user: FirebaseUser, val profile: UserProfile? = null) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Cancelled : AuthResult()
}

/**
 * User profile details derived from Firebase Auth.
 */
data class AuthUserInfo(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean = false
)

/**
 * Manages Firebase Authentication, Email/Phone Sign-Up/Login, and Cloud Firestore user persistence.
 */
class AuthenticationManager(
    private val context: Context
) {
    private val auth: FirebaseAuth? by lazy { 
        try { FirebaseAuth.getInstance() } catch (e: Exception) { 
            Log.e(TAG, "Failed to initialize FirebaseAuth", e)
            null 
        } 
    }

    private val firestore: FirebaseFirestore? by lazy {
        try { FirebaseFirestore.getInstance() } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FirebaseFirestore", e)
            null
        }
    }

    companion object {
        private const val TAG = "AuthManager"
        
        // Client ID from google-services.json (OAuth Client Type 3 / Web Client)
        const val DEFAULT_WEB_CLIENT_ID = "429402498400-shfkqr1gn3vntos3jlib49p41frpqfmd.apps.googleusercontent.com"
        const val EMAIL_DOMAIN = "attendanceapp.com"
    }

    private val credentialManager = CredentialManager.create(context)

    private val _currentUserState = MutableStateFlow<FirebaseUser?>(auth?.currentUser)
    val currentUserState: StateFlow<FirebaseUser?> = _currentUserState.asStateFlow()

    private val _authUserInfo = MutableStateFlow<AuthUserInfo?>(getCurrentUserInfo())
    val authUserInfo: StateFlow<AuthUserInfo?> = _authUserInfo.asStateFlow()

    init {
        // Keep flow synchronized with Firebase Auth state
        try {
            auth?.addAuthStateListener { firebaseAuth ->
                _currentUserState.value = firebaseAuth.currentUser
                _authUserInfo.value = getCurrentUserInfo()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching auth state listener", e)
        }
    }

    /**
     * Checks if a user is currently signed in.
     */
    fun isUserSignedIn(): Boolean = auth?.currentUser != null

    /**
     * Returns current FirebaseUser instance.
     */
    fun getCurrentUser(): FirebaseUser? = auth?.currentUser

    /**
     * Returns current user details formatted for UI consumption.
     */
    fun getCurrentUserInfo(): AuthUserInfo? {
        val user = auth?.currentUser ?: return null
        return AuthUserInfo(
            uid = user.uid,
            displayName = user.displayName ?: user.email?.substringBefore("@") ?: "User",
            email = user.email,
            photoUrl = user.photoUrl?.toString(),
            isAnonymous = user.isAnonymous
        )
    }

    /**
     * Registers a new business account in Firebase Authentication and stores user profile in Firestore.
     */
    suspend fun registerUserInFirebase(
        company: String,
        name: String,
        phone: String,
        passwordOrPin: String,
        email: String = ""
    ): AuthResult {
        if (auth == null) {
            return AuthResult.Error("Firebase Authentication is not available. Please verify Google Play Services.")
        }

        val cleanPhone = phone.filter { it.isDigit() }.trim()
        val authEmail = if (email.isNotBlank() && email.contains("@")) {
            email.trim().lowercase()
        } else {
            "${cleanPhone}@$EMAIL_DOMAIN"
        }

        return try {
            val result = auth?.createUserWithEmailAndPassword(authEmail, passwordOrPin)?.await()
            val user = result?.user
            if (user != null) {
                // 1. Update Firebase Auth Display Name
                try {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name.trim())
                        .build()
                    user.updateProfile(profileUpdates).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase display name update failed: ${e.message}")
                }

                val userProfile = UserProfile(
                    uid = user.uid,
                    companyName = company.trim(),
                    managerName = name.trim(),
                    phone = cleanPhone,
                    email = if (email.isNotBlank()) email.trim() else authEmail
                )

                // 2. Persist profile to Cloud Firestore
                saveProfileToFirestore(userProfile, authEmail)

                _currentUserState.value = user
                _authUserInfo.value = getCurrentUserInfo()
                AuthResult.Success(user, userProfile)
            } else {
                AuthResult.Error("Sign up failed: User creation returned empty profile")
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            Log.w(TAG, "Account already exists for $authEmail. Attempting auto-login...")
            try {
                val signInResult = auth?.signInWithEmailAndPassword(authEmail, passwordOrPin)?.await()
                val existingUser = signInResult?.user
                if (existingUser != null) {
                    val profile = fetchProfileFromFirestore(existingUser.uid, authEmail)
                    _currentUserState.value = existingUser
                    _authUserInfo.value = getCurrentUserInfo()
                    AuthResult.Success(existingUser, profile)
                } else {
                    AuthResult.Error("Account already exists. Please switch to Login tab.")
                }
            } catch (signInErr: Exception) {
                Log.w(TAG, "Auto-login failed: ${signInErr.message}")
                AuthResult.Error("An account with this mobile number or email already exists. Please switch to the Login tab.")
            }
        } catch (e: FirebaseAuthWeakPasswordException) {
            AuthResult.Error("Password must be at least 6 characters long for cloud security.")
        } catch (e: FirebaseNetworkException) {
            AuthResult.Error("Network error: Please connect to the internet to create your account.")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Registration failed", e)
            AuthResult.Error(e.localizedMessage ?: "Account registration failed")
        }
    }

    /**
     * Signs in with either 10-digit mobile number or email address, and recovers profile from Cloud Firestore.
     */
    suspend fun loginUserInFirebase(
        phoneOrEmail: String,
        passwordOrPin: String
    ): AuthResult {
        if (auth == null) {
            return AuthResult.Error("Firebase Authentication is not available. Please verify Google Play Services.")
        }

        val trimmed = phoneOrEmail.trim()
        val authEmail: String

        if (trimmed.contains("@")) {
            authEmail = trimmed.lowercase()
        } else {
            val cleanPhone = trimmed.filter { it.isDigit() }
            // Try to lookup registered auth email for this phone in Firestore
            val lookedUpEmail = lookupPhoneAuthEmail(cleanPhone)
            authEmail = lookedUpEmail ?: "${cleanPhone}@$EMAIL_DOMAIN"
        }

        return try {
            val result = auth?.signInWithEmailAndPassword(authEmail, passwordOrPin)?.await()
            val user = result?.user
            if (user != null) {
                // Retrieve user profile from Firestore
                val profile = fetchProfileFromFirestore(user.uid, authEmail)

                _currentUserState.value = user
                _authUserInfo.value = getCurrentUserInfo()
                AuthResult.Success(user, profile)
            } else {
                AuthResult.Error("Sign in failed: Empty user profile received from Firebase")
            }
        } catch (e: FirebaseAuthInvalidUserException) {
            AuthResult.Error("No account found with this mobile number or email. Please create an account via Sign-Up.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("Incorrect password. Please verify your password and try again.")
        } catch (e: FirebaseNetworkException) {
            AuthResult.Error("Network error: Please check your internet connection and try again.")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Login failed", e)
            AuthResult.Error(e.localizedMessage ?: "Login failed. Please check your credentials.")
        }
    }

    /**
     * Look up registered email from Firestore phone_lookup collection with quick timeout.
     */
    private suspend fun lookupPhoneAuthEmail(phone: String): String? {
        if (firestore == null || phone.isBlank()) return null
        return try {
            withTimeoutOrNull(3000) {
                val doc = firestore?.collection("phone_lookup")?.document(phone)?.get()?.await()
                if (doc != null && doc.exists()) {
                    doc.getString("authEmail")
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error looking up phone in Firestore: ${e.message}")
            null
        }
    }

    /**
     * Save profile details in Firestore users collection and phone_lookup collection.
     */
    suspend fun saveProfileToFirestore(profile: UserProfile, authEmail: String) {
        if (firestore == null) return
        try {
            withTimeoutOrNull(5000) {
                val userMap = hashMapOf(
                    "uid" to profile.uid,
                    "companyName" to profile.companyName,
                    "managerName" to profile.managerName,
                    "phone" to profile.phone,
                    "email" to profile.email,
                    "authEmail" to authEmail,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore?.collection("users")?.document(profile.uid)?.set(userMap, SetOptions.merge())?.await()

                if (profile.phone.isNotBlank()) {
                    val lookupMap = hashMapOf(
                        "phone" to profile.phone,
                        "authEmail" to authEmail,
                        "uid" to profile.uid,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    firestore?.collection("phone_lookup")?.document(profile.phone)?.set(lookupMap, SetOptions.merge())?.await()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save profile to Firestore: ${e.message}")
        }
    }

    /**
     * Fetch profile details from Firestore users collection.
     */
    suspend fun fetchProfileFromFirestore(uid: String, fallbackEmail: String): UserProfile {
        if (firestore == null) {
            val user = auth?.currentUser
            return UserProfile(
                uid = uid,
                companyName = "My Business",
                managerName = user?.displayName ?: "Admin",
                phone = "",
                email = user?.email ?: fallbackEmail
            )
        }

        return try {
            val doc = withTimeoutOrNull(4000) {
                firestore?.collection("users")?.document(uid)?.get()?.await()
            }
            if (doc != null && doc.exists()) {
                UserProfile(
                    uid = uid,
                    companyName = doc.getString("companyName") ?: "My Business",
                    managerName = doc.getString("managerName") ?: auth?.currentUser?.displayName ?: "Admin",
                    phone = doc.getString("phone") ?: "",
                    email = doc.getString("email") ?: auth?.currentUser?.email ?: fallbackEmail
                )
            } else {
                val user = auth?.currentUser
                UserProfile(
                    uid = uid,
                    companyName = "My Business",
                    managerName = user?.displayName ?: "Admin",
                    phone = "",
                    email = user?.email ?: fallbackEmail
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch profile from Firestore: ${e.message}")
            val user = auth?.currentUser
            UserProfile(
                uid = uid,
                companyName = "My Business",
                managerName = user?.displayName ?: "Admin",
                phone = "",
                email = user?.email ?: fallbackEmail
            )
        }
    }

    /**
     * Registers a new user with Email, Password and Display Name.
     */
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): AuthResult {
        return registerUserInFirebase(
            company = "My Business",
            name = displayName,
            phone = "",
            passwordOrPin = password,
            email = email
        )
    }

    /**
     * Signs in with Email and Password.
     */
    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return loginUserInFirebase(email, password)
    }

    /**
     * Sends a password reset email to the specified address.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Boolean> {
        val currentAuth = auth ?: return Result.failure(Exception("Firebase Authentication is not available."))
        return try {
            currentAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed", e)
            Result.failure(e)
        }
    }

    /**
     * Initiates Google Sign-In flow using Android Jetpack CredentialManager
     * and signs into Firebase with the resulting Google ID token.
     */
    suspend fun signInWithGoogle(serverClientId: String = DEFAULT_WEB_CLIENT_ID): AuthResult {
        return try {
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            handleSignInResponse(result)
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Sign-in was cancelled by user: ${e.message}")
            AuthResult.Cancelled
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            Log.w(TAG, "No credentials found on device", e)
            AuthResult.Error("No Google account found on this device. Please add an account in device settings, or use Email/Mobile login.")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager sign-in failed", e)
            AuthResult.Error(e.localizedMessage ?: "Google Sign-In failed")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sign-in", e)
            AuthResult.Error(e.localizedMessage ?: "An unexpected error occurred")
        }
    }

    /**
     * Handles credential response and exchanges it for Firebase Auth token.
     */
    private suspend fun handleSignInResponse(result: GetCredentialResponse): AuthResult {
        return when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth?.signInWithCredential(authCredential)?.await()
                        val user = authResult?.user
                        if (user != null) {
                            val profile = fetchProfileFromFirestore(user.uid, user.email ?: "")

                            // Handle Google Sign-In missing fields mapping
                            val updatedProfile = if (profile.companyName == "My Business" && profile.phone.isBlank()) {
                                val nameFallback = user.displayName ?: "User"
                                val newProfile = UserProfile(
                                    uid = user.uid,
                                    companyName = nameFallback,
                                    managerName = nameFallback,
                                    phone = "",
                                    email = user.email ?: ""
                                )
                                saveProfileToFirestore(newProfile, user.email ?: "")
                                newProfile
                            } else {
                                profile
                            }

                            _currentUserState.value = user
                            _authUserInfo.value = getCurrentUserInfo()
                            Log.i(TAG, "Successfully signed in user: ${user.uid} (${user.email})")
                            AuthResult.Success(user, updatedProfile)
                        } else {
                            AuthResult.Error("Firebase returned empty user profile")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Firebase credential authentication failed", e)
                        AuthResult.Error(e.localizedMessage ?: "Authentication with Firebase failed")
                    }
                } else {
                    Log.e(TAG, "Unexpected credential type: ${credential.type}")
                    AuthResult.Error("Unsupported credential type")
                }
            }
            else -> {
                Log.e(TAG, "Unsupported credential class: ${credential.javaClass.name}")
                AuthResult.Error("Invalid credential received")
            }
        }
    }

    /**
     * Signs out the current user from Firebase and clears Jetpack Credential state.
     */
    suspend fun signOut(): Boolean {
        return try {
            auth?.signOut()
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Error clearing credential state: ${e.message}")
            }
            _currentUserState.value = null
            _authUserInfo.value = null
            Log.i(TAG, "User signed out successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during sign out", e)
            false
        }
    }
}
