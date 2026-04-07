package com.soundtag.data

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

sealed class UploadStatus {
    data object Idle : UploadStatus()
    data class Uploading(val progress: Int) : UploadStatus()
    data object Success : UploadStatus()
    data class Failed(val message: String) : UploadStatus()
}

object FirebaseUploader {

    private const val ANNOTATOR_ID = "HML-01"

    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    suspend fun uploadRecording(
        audioFile: File,
        jsonContent: String,
        filename: String
    ): UploadStatus {
        return try {
            val basePath = "SoundTag/$ANNOTATOR_ID"

            // Upload audio file
            val audioRef = storage.reference.child("$basePath/$filename.m4a")
            audioRef.putFile(Uri.fromFile(audioFile)).await()

            // Upload JSON sidecar
            val jsonRef = storage.reference.child("$basePath/$filename.json")
            val jsonBytes = jsonContent.toByteArray(Charsets.UTF_8)
            jsonRef.putBytes(jsonBytes).await()

            UploadStatus.Success
        } catch (e: Exception) {
            UploadStatus.Failed(e.message ?: "Upload failed")
        }
    }
}
