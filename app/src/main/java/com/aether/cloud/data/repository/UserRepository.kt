package com.aether.cloud.data.repository

import com.aether.cloud.data.model.User
import com.aether.cloud.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getUserProfile(uid: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val doc = db.collection("users").document(uid).get().await()
            val user = doc.toObject(User::class.java)
            if (user != null) {
                emit(Resource.Success(user))
            } else {
                emit(Resource.Error("User not found"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load user"))
        }
    }
}
