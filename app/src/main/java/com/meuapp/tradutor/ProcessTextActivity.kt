package com.meuapp.tradutor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

class ProcessTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
        if (selectedText.isNotEmpty() && Settings.canDrawOverlays(this)) {
            val serviceIntent = Intent(this, FloatingService::class.java).apply {
                putExtra("SELECTED_TEXT", selectedText)
            }
            startService(serviceIntent)
        } else {
            Toast.makeText(this, "Ative a bolinha flutuante no app primeiro!", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
