package com.jnetai.puzzle

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jnetai.puzzle.utils.ErrorLogger
import com.jnetai.puzzle.utils.SettingsManager

/**
 * Settings: puzzle grid size, countdown timer and the hidden-images manager.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var spinnerGrid: Spinner
    private lateinit var spinnerTimer: Spinner

    private val settings by lazy { SettingsManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        try {
            spinnerGrid = findViewById(R.id.spinnerGrid)
            spinnerTimer = findViewById(R.id.spinnerTimer)
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.UI_VIEW_BINDING, "Failed to bind settings views", e)
            Toast.makeText(this, "Settings error - E-UI-002", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val gridAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            resources.getStringArray(R.array.grid_options))
        spinnerGrid.adapter = gridAdapter
        spinnerGrid.setSelection(settings.getGridSize() - 3)

        val timerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            resources.getStringArray(R.array.timer_options))
        spinnerTimer.adapter = timerAdapter
        spinnerTimer.setSelection(
            when (settings.getTimerSeconds()) {
                0 -> 0
                60 -> 1
                120 -> 2
                300 -> 3
                else -> 0
            }
        )

        spinnerGrid.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                settings.setGridSize(position + 3)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) { }
        }

        spinnerTimer.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val seconds = when (position) {
                    0 -> 0
                    1 -> 60
                    2 -> 120
                    else -> 300
                }
                settings.setTimerSeconds(seconds)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) { }
        }

        findViewById<Button>(R.id.btnHiddenImages).setOnClickListener {
            startActivity(Intent(this, HiddenImagesActivity::class.java))
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }
}