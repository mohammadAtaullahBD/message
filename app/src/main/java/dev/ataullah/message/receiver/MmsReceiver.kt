package dev.ataullah.message.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Minimal implementation – just log receipt.  The ViewModel re-queries the provider.
        Log.d("MmsReceiver", "MMS delivered: ${intent?.action}")
    }
}
