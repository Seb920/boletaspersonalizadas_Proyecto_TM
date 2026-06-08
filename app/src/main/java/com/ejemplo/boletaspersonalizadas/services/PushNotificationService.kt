package com.ejemplo.boletaspersonalizadas.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ejemplo.boletaspersonalizadas.R
import com.ejemplo.boletaspersonalizadas.activities.LoginActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushNotificationService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "boletas_channel"
        private const val CHANNEL_NAME = "Notificaciones de Boletas"
        private const val CHANNEL_DESCRIPTION = "Canal para notificaciones de la app de boletas"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token: $token")
        guardarTokenEnFirebase(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Mensaje recibido: ${message.notification?.title}")

        // Mostrar notificación
        message.notification?.let {
            mostrarNotificacion(it.title ?: "Nueva Notificación", it.body ?: "")
        }

        // Manejar datos adicionales si los hay
        message.data.let { data ->
            Log.d("FCM", "Data: $data")
        }
    }

    private fun mostrarNotificacion(titulo: String, mensaje: String) {
        val notificationId = System.currentTimeMillis().toInt()

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun guardarTokenEnFirebase(token: String) {
        // Guardar token en Realtime Database para enviar notificaciones segmentadas
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance().reference
            database.child("tokens").child(uid).setValue(token)
                .addOnSuccessListener {
                    Log.d("FCM", "Token guardado en Firebase")
                }
                .addOnFailureListener {
                    Log.e("FCM", "Error al guardar token", it)
                }
        }
    }
}