package com.client.printerutil

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.client.printerutil.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.buttonRefresh.setOnClickListener { refreshStatus() }
        binding.buttonPrintSettings.setOnClickListener { openPrintSettings() }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        binding.textStatus.text = PrintingDiagnostics.buildReport(this)
    }

    private fun openPrintSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            startActivity(Intent(Settings.ACTION_PRINT_SETTINGS))
        } else {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
