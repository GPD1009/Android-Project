package com.example.nids

import android.content.*
import androidx.room.Room
import kotlinx.coroutines.*

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("threat_id", -1L)
        val action = intent.action ?: return // "ALLOW" or "BLOCK"

        val db = Room.databaseBuilder(context, AppDatabase::class.java, "nids_db").build()

        CoroutineScope(Dispatchers.IO).launch {
            if (id != -1L) {
                db.threatDao().updateAction(id, action)
            }
        }
    }
}