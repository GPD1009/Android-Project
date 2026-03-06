package com.example.nids

import android.R
import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.room.Room
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "nids_db")
        .fallbackToDestructiveMigration().build()
    private val dao = db.threatDao()

    private val api = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:5000/")
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(NidsApiService::class.java)

    // Exposed States
    val activeThreats =
        dao.getAllThreats().map { list -> list.filter { it.userAction == "PENDING" } }
    val history = dao.getAllThreats().map { list -> list.filter { it.userAction != "PENDING" } }

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val remoteData = api.getThreats()
                    dao.insertThreats(remoteData)
                    // Simple check: notify if new threats arrived
                    remoteData.firstOrNull()?.let { sendNotification(it) }
                } catch (e: Exception) {
                }
                delay(3000)
            }
        }
    }

    fun setAction(id: Long, action: String) {
        viewModelScope.launch(Dispatchers.IO) { dao.updateAction(id, action) }
    }

    private fun sendNotification(threat: ThreatEntity) {
        val context = getApplication<Application>()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "nids_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Threats",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val allowIntent = PendingIntent.getBroadcast(
            context, threat.id.toInt(),
            Intent(context, ActionReceiver::class.java).apply {
                action = "ALLOW"; putExtra("threat_id", threat.id)
            }, PendingIntent.FLAG_IMMUTABLE
        )

        val blockIntent = PendingIntent.getBroadcast(
            context, threat.id.toInt() + 1000,
            Intent(context, ActionReceiver::class.java).apply {
                action = "BLOCK"; putExtra("threat_id", threat.id)
            }, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_dialog_alert)
            .setContentTitle("Threat: ${threat.verdict}")
            .setContentText("Confidence: ${threat.confidence}%")
            .addAction(0, "ALLOW", allowIntent)
            .addAction(0, "BLOCK", blockIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(threat.id.toInt(), notification)
    }
}