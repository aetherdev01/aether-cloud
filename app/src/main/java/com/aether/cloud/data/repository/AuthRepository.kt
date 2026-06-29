package com.aether.cloud.data.repository

import android.content.Context
import com.aether.cloud.R
import com.aether.cloud.data.model.User
import com.aether.cloud.util.Resource
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getGoogleSignInIntent() = googleSignInClient.signInIntent

    suspend fun firebaseAuthWithGoogle(idToken: String): Resource<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            Resource.Success(result.user!!)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Google sign in failed")
        }
    }

    suspend fun checkUserProfile(uid: String): Boolean {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createUserProfile(user: User): Resource<Unit> {
        return try {
            db.collection("users").document(user.uid).set(user).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create profile")
        }
    }

    fun logout() {
        auth.signOut()
        googleSignInClient.signOut()
    }
}
