package dev.ataullah.message

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Minimal activity required for default SMS apps.
 * When another app requests to send a message via an `sms:`/`smsto:`/`mms:`/`mmsto:` URI,
 * the system looks for an activity with the appropriate SENDTO/SEND intent filter.
 * This activity simply forwards the intent to MainActivity (where your UI resides)
 * and then finishes immediately.
 */
class ComposeSmsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        val address = uri?.schemeSpecificPart?.substringBefore('?') ?: ""
        val messageBody = uri?.getQueryParameter("body") ?: intent.getStringExtra("sms_body") ?: ""

        // Forward extras to MainActivity
        val forward = Intent(this, MainActivity::class.java).apply {
            putExtra("initial_address", address)
            putExtra("initial_body", messageBody)
        }
        startActivity(forward)
        finish()
    }

    // Helper extension to extract query parameters from URIs
    private fun Uri.getQueryParameter(key: String): String? {
        return this.queryParameterNames
            .firstOrNull { it == key }
            ?.let { getQueryParameter(it) }
    }
}

