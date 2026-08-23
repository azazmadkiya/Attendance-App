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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

/**
 * Result state for authentication actions.
 */
sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
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
 * Manages Firebase Authentication, Email Sign-Up/Login, and Google Sign-In via Jetpack CredentialManager.
 */
class AuthenticationManager(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "AuthManager"
        
        // Client ID from google-services.json (OAuth Client Type 3 / Web Client)
        const val DEFAULT_WEB_CLIENT_ID = "429402498400-shfkqr1gn3vntos3jlib49p41frpqfmd.apps.googleusercontent.com"
    }

    private val credentialManager = CredentialManager.create(context)

    private val _currentUserState = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUserState: StateFlow<FirebaseUser?> = _currentUserState.asStateFlow()

    private val _authUserInfo = MutableStateFlow<AuthUserInfo?>(getCurrentUserInfo())
    val authUserInfo: StateFlow<AuthUserInfo?> = _authUserInfo.asStateFlow()

    init {
        // Keep flow synchronized with Firebase Auth state
        auth.addAuthStateListener { firebaseAuth ->
            _currentUserState.value = firebaseAuth.currentUser
            _authUserInfo.value = getCurrentUserInfo()
        }
    }

    /**
     * Checks if a user is currently signed in.
     */
    fun isUserSignedIn(): Boolean = auth.currentUser != null

    /**
     * Returns current FirebaseUser instance.
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Returns current user details formatted for UI consumption.
     */
    fun getCurrentUserInfo(): AuthUserInfo? {
        val user = auth.currentUser ?: return null
        return AuthUserInfo(
            uid = user.uid,
            displayName = user.displayName ?: user.email?.substringBefore("@") ?: "User",
            email = user.email,
            photoUrl = user.photoUrl?.toString(),
            isAnonymous = user.isAnonymous
        )
    }

    /**
     * Registers a new user with Email, Password and Display Name.
     */
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
            if (user != null) {
                if (displayName.isNotBlank()) {
                    try {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName.trim())
                            .build()
                        user.updateProfile(profileUpdates).await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Profile name update failed: ${e.message}")
                    }
                }
                _currentUserState.value = user
                _authUserInfo.value = getCurrentUserInfo()
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Sign up failed: User profile creation failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-up failed", e)
            AuthResult.Error(e.localizedMessage ?: "Registration failed")
        }
    }

    /**
     * Signs in with Email and Password.
     */
    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
            if (user != null) {
                _currentUserState.value = user
                _authUserInfo.value = getCurrentUserInfo()
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Sign in failed: Empty user profile")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-in failed", e)
            AuthResult.Error(e.localizedMessage ?: "Email sign-in failed")
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
                        val authResult = auth.signInWithCredential(authCredential).await()
                        val user = authResult.user
                        if (user != null) {
                            _currentUserState.value = user
                            _authUserInfo.value = getCurrentUserInfo()
                            Log.i(TAG, "Successfully signed in user: ${user.uid} (${user.email})")
                            AuthResult.Success(user)
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
            auth.signOut()
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
