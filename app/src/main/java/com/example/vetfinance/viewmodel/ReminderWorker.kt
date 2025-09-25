package com.example.vetfinance.viewmodel

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vetfinance.R
import com.example.vetfinance.data.Treatment
import com.example.vetfinance.viewmodel.VetRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReminderWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
    private val repository: VetRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val upcomingTreatments = repository.getUpcomingTreatments().first()
            if (upcomingTreatments.isEmpty()) {
                return Result.success()
            }

            val petsWithOwners = repository.getAllPetsWithOwners().first()
            val petsMap = petsWithOwners.associateBy { it.pet.petId }

            upcomingTreatments.forEach { treatment ->
                val petName = petsMap[treatment.petIdFk]?.pet?.name ?: "una mascota"
                sendNotification(treatment, petName)
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun sendNotification(treatment: Treatment, petName: String) {
        val notificationId = treatment.treatmentId.hashCode()

        val date = treatment.nextTreatmentDate?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Fecha no especificada"

        val contentText = "Recordatorio para $petName: ${treatment.description} el $date."

        val builder = NotificationCompat.Builder(appContext, "TREATMENT_REMINDERS")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Reemplazar con un ícono adecuado
            .setContentTitle("Recordatorio de Tratamiento")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(appContext)) {
            notify(notificationId, builder.build())
        }
    }
}
