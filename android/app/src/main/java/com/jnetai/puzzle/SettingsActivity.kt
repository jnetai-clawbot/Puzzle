package com.jnetai.puzzle

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jnetai.puzzle.utils.ErrorLogger
import com.jnetai.puzzle.utils.SettingsManager

/**
 * Settings: puzzle grid size, timer mode (off / count up / count down),
 * count-down time limit, and the hidden-images manager.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var spinnerGrid: Spinner
    private lateinit var spinnerTimerMode: Spinner
    private lateinit var spinnerTimerLimit: Spinner
    private lateinit var tvTimerLimitLabel: TextView

    private val settings by lazy { SettingsManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        try {
            spinnerGrid = findViewById(R.id.spinnerGrid)
            spinnerTimerMode = findViewById(R.id.spinnerTimerMode)
            spinnerTimerLimit = findViewById(R.id.spinnerTimerLimit)
            tvTimerLimitLabel = findViewById(R.id.tvTimerLimitLabel)
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

        val modeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            resources.getStringArray(R.array.timer_mode_options))
        spinnerTimerMode.adapter = modeAdapter
        spinnerTimerMode.setSelection(
            when (settings.getTimerMode()) {
                SettingsManager.TimerMode.UP -> 1
                SettingsManager.TimerMode.DOWN -> 2
                else -> 0
            }
        )

        val limitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            resources.getStringArray(R.array.timer_limit_options))
        spinnerTimerLimit.adapter = limitAdapter
        spinnerTimerLimit.setSelection(limitIndexFor(settings.getTimerLimitSeconds()))

        updateTimerLimitVisibility()

        spinnerGrid.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                settings.setGridSize(position + 3)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) { }
        }

        spinnerTimerMode.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                val mode = when (position) {
                    1 -> SettingsManager.TimerMode.UP
                    2 -> SettingsManager.TimerMode.DOWN
                    else -> SettingsManager.TimerMode.OFF
                }
                settings.setTimerMode(mode)
                updateTimerLimitVisibility()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) { }
        }

        spinnerTimerLimit.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                val seconds = when (position) {
                    0 -> 60
                    1 -> 120
                    2 -> 180
                    3 -> 300
                    else -> 600
                }
                settings.setTimerLimitSeconds(seconds)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) { }
        }

        findViewById<Button>(R.id.btnHiddenImages).setOnClickListener {
            startActivity(Intent(this, HiddenImagesActivity::class.java))
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun updateTimerLimitVisibility() {
        val isDown = settings.getTimerMode() == SettingsManager.TimerMode.DOWN
        val vis = if (isDown) View.VISIBLE else View.GONE
        tvTimerLimitLabel.visibility = vis
        spinnerTimerLimit.visibility = vis
    }

    private fun limitIndexFor(seconds: Int): Int {
        return when (seconds) {
            60 -> 0
            120 -> 1
            180 -> 2
            300 -> 3
            else -> 4
        }
    }
}