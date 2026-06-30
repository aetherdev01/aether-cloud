package com.aether.cloud.data.repository

import android.net.Uri
import com.aether.cloud.data.model.Comment
import com.aether.cloud.data.model.Module
import com.aether.cloud.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ModuleRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun getModules(filter: String): Flow<Resource<List<Module>>> = flow {
        emit(Resource.Loading())
        try {
            val query = when (filter) {
                "POPULAR" -> db.collection("modules")
                    .orderBy("downloadCount", Query.Direction.DESCENDING)
                    .limit(50)
                "ROOT" -> db.collection("modules")
                    .whereEqualTo("type", "ROOT")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50)
                "NO_ROOT" -> db.collection("modules")
                    .whereEqualTo("type", "NO_ROOT")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50)
                else -> db.collection("modules")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50)
            }
            val snapshot = query.get().await()
            val modules = snapshot.toObjects(Module::class.java)
            emit(Resource.Success(modules))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load modules"))
        }
    }

    fun searchModules(query: String): Flow<Resource<List<Module>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = db.collection("modules")
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(20)
                .get()
                .await()
            val modules = snapshot.toObjects(Module::class.java)
            emit(Resource.Success(modules))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Search failed"))
        }
    }

    fun getModuleById(moduleId: String): Flow<Resource<Module>> = flow {
        emit(Resource.Loading())
        try {
            val doc = db.collection("modules").document(moduleId).get().await()
            val module = doc.toObject(Module::class.java)
            if (module != null) {
                emit(Resource.Success(module))
            } else {
                emit(Resource.Error("Module not found"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load module"))
        }
    }

    fun getMyModules(userId: String): Flow<Resource<List<Module>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = db.collection("modules")
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val modules = snapshot.toObjects(Module::class.java)
            emit(Resource.Success(modules))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load your modules"))
        }
    }

    suspend fun uploadModule(module: Module, zipUri: Uri?, screenshots: List<Uri>): Resource<String> {
        return try {
            var fileUrl = module.fileUrl
            var fileSize = module.fileSize

            if (zipUri != null) {
                android.util.Log.d("ModuleRepository", "Uploading ZIP for module ${module.id}")
                // Use a fixed, sanitized file name instead of the raw module name.
                // module.name is free-text user input and can contain '/', spaces,
                // or other characters that break the Storage path (and don't match
                // storage.rules, which only allows a single path segment under
                // modules/{moduleId}/). That mismatch is what causes
                // "Object does not exist at location" when getDownloadUrl() is called.
                val zipRef = storage.reference.child("modules/${module.id}/module.zip")
                zipRef.putFile(zipUri).await()
                fileUrl = zipRef.downloadUrl.await().toString()
                val metadata = zipRef.metadata.await()
                fileSize = metadata.sizeBytes
            }

            val screenshotUrls = mutableListOf<String>()
            screenshots.forEachIndexed { index, uri ->
                android.util.Log.d("ModuleRepository", "Uploading screenshot $index for module ${module.id}")
                val ssRef = storage.reference.child("modules/${module.id}/screenshot_$index.jpg")
                ssRef.putFile(uri).await()
                screenshotUrls.add(ssRef.downloadUrl.await().toString())
            }

            val finalModule = module.copy(
                fileUrl = fileUrl,
                fileSize = fileSize,
                screenshots = screenshotUrls
            )

            android.util.Log.d("ModuleRepository", "Writing module document ${module.id} to Firestore, authorId=${module.authorId}")
            db.collection("modules").document(module.id).set(finalModule).await()
            android.util.Log.d("ModuleRepository", "Module ${module.id} uploaded successfully")
            Resource.Success(module.id)
        } catch (e: Exception) {
            android.util.Log.e("ModuleRepository", "Upload failed for module ${module.id}", e)
            val msg = when {
                e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
                    "Upload ditolak server (Firestore/Storage rules). Pastikan kamu login dan rules sudah benar."
                else -> e.message ?: "Upload failed"
            }
            Resource.Error(msg)
        }
    }

    suspend fun incrementDownload(moduleId: String) {
        try {
            db.collection("modules").document(moduleId)
                .update("downloadCount", FieldValue.increment(1)).await()
        } catch (_: Exception) {}
    }

    suspend fun incrementView(moduleId: String) {
        try {
            db.collection("modules").document(moduleId)
                .update("viewCount", FieldValue.increment(1)).await()
        } catch (_: Exception) {}
    }

    fun getComments(moduleId: String): Flow<Resource<List<Comment>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = db.collection("modules").document(moduleId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val comments = snapshot.toObjects(Comment::class.java)
            emit(Resource.Success(comments))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load comments"))
        }
    }

    suspend fun addComment(comment: Comment): Resource<Unit> {
        return try {
            db.collection("modules").document(comment.moduleId)
                .collection("comments").document(comment.id).set(comment).await()
            db.collection("modules").document(comment.moduleId)
                .update("commentCount", FieldValue.increment(1)).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add comment")
        }
    }
}
