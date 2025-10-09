package com.example.vetfinance.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vetfinance.R
import com.example.vetfinance.data.Treatment
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: VetRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val now = Calendar.getInstance()
            val startOfDay = now.timeInMillis

            val endOfDay = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis

            val upcomingTreatments = repository.getUpcomingTreatmentsForRange(startOfDay, endOfDay)

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
            Log.e("ReminderWorker", "Error en doWork", e)
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Recordatorio de Tratamiento")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(appContext)) {
            if (ActivityCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            notify(notificationId, builder.build())
        }
    }
}