package com.soundtag.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.soundtag.data.DriveUploader
import com.soundtag.data.RecordingRepository
import com.soundtag.data.UploadQueueManager
import com.soundtag.data.UploadResult

class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val queueManager = UploadQueueManager(applicationContext)
        val repo = RecordingRepository(applicationContext as android.app.Application)

        if (!DriveUploader.isSignedIn(applicationContext)) {
            return Result.retry()
        }

        val pending = queueManager.getPendingFiles()
        if (pending.isEmpty()) return Result.success()

        var allSucceeded = true

        pending.forEach { upload ->
            val jsonContent = upload.jsonFile.readText(Charsets.UTF_8)
            val result = DriveUploader.uploadRecording(
                context = applicationContext,
                audioFile = upload.audioFile,
                jsonContent = jsonContent,
                filename = upload.filename,
                annotatorId = upload.annotatorId,
                customFolderId = upload.customFolderId.ifEmpty { null }
            )

            when (result) {
                is UploadResult.Success -> {
                    queueManager.removePending(upload.filename)
                    repo.updateUploadStatus(upload.filename, "uploaded")
                }
                is UploadResult.Failed -> {
                    repo.updateUploadStatus(upload.filename, "failed")
                    allSucceeded = false
                }
            }
        }

        return if (allSucceeded) Result.success() else Result.retry()
    }

    companion object {
        private const val WORK_NAME = "soundtag_upload_sync"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun enqueueIfPending(context: Context) {
            val queueManager = UploadQueueManager(context)
            if (queueManager.hasPending()) {
                enqueue(context)
            }
        }
    }
}
