package com.example.vetfinance.viewmodel

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BackupImportWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: VetRepository,
    private val appEventBus: AppEventBus
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val uriString = inputData.getString(KEY_URI)
            ?: return Result.failure(messageData("No se encontro el archivo de respaldo."))

        return try {
            setProgress(workDataOf(KEY_PROGRESS to 10, KEY_MESSAGE to "Leyendo respaldo..."))
            val result = repository.importarDatosDesdeZIP(Uri.parse(uriString), appContext)
            setProgress(workDataOf(KEY_PROGRESS to 95, KEY_MESSAGE to "Actualizando pantallas..."))
            appEventBus.emitPagingRefresh()
            Result.success(workDataOf(KEY_PROGRESS to 100, KEY_MESSAGE to result))
        } catch (error: Exception) {
            Result.failure(messageData(error.message ?: "No se pudo importar el respaldo."))
        }
    }

    private fun messageData(message: String): Data {
        return workDataOf(KEY_MESSAGE to message, KEY_PROGRESS to 0)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "backup_import_work"
        const val KEY_URI = "backup_uri"
        const val KEY_MESSAGE = "backup_message"
        const val KEY_PROGRESS = "backup_progress"
    }
}
