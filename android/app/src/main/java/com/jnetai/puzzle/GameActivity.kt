package com.jnetai.puzzle

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jnetai.puzzle.game.PuzzleEngine
import com.jnetai.puzzle.ui.PuzzleBoardView
import com.jnetai.puzzle.utils.ErrorLogger
import com.jnetai.puzzle.utils.ImageLibraryUtils
import com.jnetai.puzzle.utils.SettingsManager
import java.util.Locale

/**
 * GameActivity - the sliding puzzle game.
 *
 * Loads the chosen image (built-in asset or cached user upload), creates a
 * scrambled [PuzzleEngine] and renders it with [PuzzleBoardView]. Handles the
 * optional timer (off / counts up / counts down), move counter and the solved
 * state.
 */
class GameActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ASSET = "extra_asset"
        const val EXTRA_CACHE_IMAGE = "extra_cache_image"

        private const val STATE_BOARD = "state_board"
        private const val STATE_MOVES = "state_moves"
        private const val STATE_STARTED = "state_started"
        private const val STATE_TIME = "state_time_ms"
    }

    private lateinit var boardView: PuzzleBoardView
    private lateinit var tvStatus: TextView
    private lateinit var tvTimer: TextView
    private lateinit var btnNewPuzzle: Button
    private lateinit var btnScramble: Button
    private lateinit var btnHint: Button

    private val settings by lazy { SettingsManager.getInstance(this) }
    private val handler = Handler(Looper.getMainLooper())
    private val ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "puzzle-loader")
    }

    private var engine: PuzzleEngine? = null
    private var currentBitmap: Bitmap? = null

    // Timer state (persists across rotation).
    // For "up" mode this is the elapsed ms; for "down" it is the ms remaining.
    private var timerMs: Long = -1L
    private var gameOver: Boolean = false

    private val timerRunnable = object : Runnable {
        override fun run() {
            tickTimer()
            if (!gameOver) handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        setupViews()
        restoreOrLoadGame(savedInstanceState)
    }

    private fun setupViews() {
        try {
            boardView = findViewById(R.id.boardView)
            tvStatus = findViewById(R.id.tvStatus)
            tvTimer = findViewById(R.id.tvTimer)
            btnNewPuzzle = findViewById(R.id.btnNewPuzzle)
            btnScramble = findViewById(R.id.btnScramble)

            boardView.onTileMoved = { moves ->
                updateStatus()
            }
            boardView.onPuzzleSolved = {
                handleSolved()
            }

            btnNewPuzzle.setOnClickListener { startRandomPuzzle() }
            btnScramble.setOnClickListener { rescramble() }
            btnHint = findViewById(R.id.btnHint)
            btnHint.setOnClickListener { showHint() }
            findViewById<Button>(R.id.btnMenu).setOnClickListener {
                finish()
            }
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.UI_VIEW_BINDING, "Failed to bind game views", e)
            Toast.makeText(this, "Game setup error - E-UI-002", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun restoreOrLoadGame(savedInstanceState: Bundle?) {
        val asset = intent.getStringExtra(EXTRA_ASSET)
        val cacheImage = intent.getStringExtra(EXTRA_CACHE_IMAGE)

        if (savedInstanceState != null) {
            val boardStr = savedInstanceState.getString(STATE_BOARD)
            val moves = savedInstanceState.getInt(STATE_MOVES, 0)
            val started = savedInstanceState.getBoolean(STATE_STARTED, false)
            timerMs = savedInstanceState.getLong(STATE_TIME, -1L)
            if (boardStr != null) {
                val eng = PuzzleEngine(settings.getGridSize())
                if (eng.importState(boardStr, moves, started)) {
                    engine = eng
                } else {
                    ErrorLogger.log(ErrorLogger.Codes.PZL_STATE,
                        "Failed to restore board state from rotation")
                }
            }
        }

        // Load the image either way (needed for the board view).
        val grid = settings.getGridSize()

        if (cacheImage != null && engine == null) {
            val bmp = ImageLibraryUtils.loadCachedBitmap(this, cacheImage)
            if (bmp == null) {
                ErrorLogger.logf(ErrorLogger.Codes.IMG_SAMPLE,
                    "Cached image '%s' no longer exists", cacheImage)
                toast(R.string.image_empty)
                finish()
                return
            }
            finishLoading(bmp, grid, wasRestored = savedInstanceState != null)
        } else if (asset != null && engine == null) {
            val bmp = ImageLibraryUtils.loadAssetImage(this, asset)
            if (bmp == null) {
                ErrorLogger.logf(ErrorLogger.Codes.IMG_ASSET_LOAD,
                    "Could not load asset '%s'", asset)
                toast(R.string.unknown_image_error)
                finish()
                return
            }
            finishLoading(bmp, grid, wasRestored = savedInstanceState != null)
        } else if (engine != null) {
            // Restored engine - bitmap might still need loading.
            val bmp = currentBitmap ?: if (asset != null) {
                ImageLibraryUtils.loadAssetImage(this, asset)
            } else if (cacheImage != null) {
                ImageLibraryUtils.loadCachedBitmap(this, cacheImage)
            } else null

            if (bmp == null) {
                ErrorLogger.logf(ErrorLogger.Codes.IMG_ASSET_LOAD,
                    "No bitmap available after restore; asset='%s' cache='%s'", asset, cacheImage)
                toast(R.string.image_empty)
                finish()
                return
            }
            currentBitmap = bmp
            boardView.attach(bmp, engine!!)
            syncTimerUi()
            updateStatus()
        }
    }

    private fun finishLoading(bmp: Bitmap, grid: Int, wasRestored: Boolean) {
        currentBitmap = bmp
        val eng = engine ?: PuzzleEngine(grid).also {
            it.scramble(if (grid >= 5) 300 else 150)
        }
        if (engine == null) engine = eng
        boardView.attach(bmp, eng)
        syncTimerUi()
        updateStatus()
    }

    private fun rescramble() {
        // Keep the same image but shuffle the tiles again.
        startGameWithCurrentImage()
        Toast.makeText(this, R.string.scramble_again, Toast.LENGTH_SHORT).show()
    }

    /** Scramble a fresh puzzle using the currently displayed image. */
    private fun startGameWithCurrentImage() {
        try {
            val bmp = currentBitmap
            if (bmp == null) {
                ErrorLogger.logf(ErrorLogger.Codes.PZL_IMAGE_NULL,
                    "startGameWithCurrentImage called with no bitmap")
                return
            }
            startGameWithBitmap(bmp)
        } catch (e: Exception) {
            ErrorLogger.logf(ErrorLogger.Codes.PZL_NEW, "Failed to start new game", e)
        }
    }

    /** Load a random non-hidden built-in image and start a new puzzle on it. */
    private fun startRandomPuzzle() {
        val candidates = ImageLibraryUtils.listBuiltInImages(this)
            .filter { !settings.isHidden(it) }
        if (candidates.isEmpty()) {
            ErrorLogger.log(ErrorLogger.Codes.PZL_NEW, "No visible images available for random puzzle")
            toast(R.string.no_images)
            return
        }
        val pick = candidates[kotlin.random.Random.nextInt(candidates.size)]
        btnNewPuzzle.isEnabled = false
        tvStatus.text = getString(R.string.loading_puzzle)
        ioExecutor.execute {
            val bmp = ImageLibraryUtils.loadAssetImage(this, pick)
            runOnUiThread {
                btnNewPuzzle.isEnabled = true
                if (bmp == null) {
                    ErrorLogger.logf(ErrorLogger.Codes.IMG_ASSET_LOAD,
                        "Random puzzle asset '%s' failed to load", pick)
                    toast(R.string.unknown_image_error)
                    return@runOnUiThread
                }
                currentBitmap = bmp
                gameOver = false
                timerMs = -1L
                startGameWithBitmap(bmp)
            }
        }
    }

    /** Attach the given bitmap to a freshly scrambled engine and update the UI. */
    private fun startGameWithBitmap(bmp: Bitmap) {
        val grid = settings.getGridSize()
        val eng = PuzzleEngine(grid)
        eng.scramble(if (grid >= 5) 300 else 150)
        engine = eng
        gameOver = false
        timerMs = -1L
        boardView.attach(bmp, eng)
        syncTimerUi()
        updateStatus()
        ErrorLogger.logf(ErrorLogger.Codes.PZL_NEW,
            "New puzzle started: grid=%dx%d", grid, grid)
    }

    private fun showHint() {
        if (gameOver) return
        boardView.showHint(5000L)
        Toast.makeText(this, R.string.hint_showing, Toast.LENGTH_SHORT).show()
    }

    private fun handleSolved() {
        gameOver = true
        handler.removeCallbacks(timerRunnable)
        val usedTime = formatElapsedTime()
        val moves = engine?.moves ?: 0
        tvStatus.text = getString(R.string.puzzle_solved, moves, usedTime)

        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(getString(R.string.puzzle_solved, moves, usedTime))
            .setPositiveButton(R.string.new_puzzle) { _, _ -> startRandomPuzzle() }
            .setNegativeButton(R.string.back_to_menu) { _, _ -> finish() }
            .setIcon(android.R.drawable.ic_menu_agenda)
            .show()
    }

    /** Human-elapsed-time string for a solved puzzle, from whichever timer mode. */
    private fun formatElapsedTime(): String {
        if (timerMs < 0) return "—"
        return when (settings.getTimerMode()) {
            SettingsManager.TimerMode.UP -> formatClock(timerMs / 1000)
            SettingsManager.TimerMode.DOWN -> {
                val limit = settings.getTimerLimitSeconds() * 1000L
                formatClock(((limit - timerMs).coerceAtLeast(0L)) / 1000)
            }
            else -> "—"
        }
    }

    private fun syncTimerUi() {
        handler.removeCallbacks(timerRunnable)
        val mode = settings.getTimerMode()
        if (mode == SettingsManager.TimerMode.OFF) {
            tvTimer.visibility = android.view.View.GONE
            timerMs = -1L
            return
        }
        tvTimer.visibility = android.view.View.VISIBLE
        if (!gameOver && timerMs < 0) {
            timerMs = when (mode) {
                SettingsManager.TimerMode.UP -> 0L
                SettingsManager.TimerMode.DOWN -> settings.getTimerLimitSeconds() * 1000L
                else -> -1L
            }
        }
        // Guard against a stale down-timer restarted past its limit.
        if (mode == SettingsManager.TimerMode.DOWN) {
            val limit = settings.getTimerLimitSeconds() * 1000L
            if (timerMs > limit) timerMs = limit
        }
        refreshTimerLabel()
        if (!gameOver && engine?.started == true) {
            handler.postDelayed(timerRunnable, 1000)
        }
    }

    private fun refreshTimerLabel() {
        if (timerMs < 0) return
        val display = when (settings.getTimerMode()) {
            SettingsManager.TimerMode.UP -> formatClock(timerMs / 1000)
            SettingsManager.TimerMode.DOWN -> formatClock(timerMs / 1000)
            else -> "-:--"
        }
        tvTimer.text = getString(R.string.time_left, display)
    }

    private fun tickTimer() {
        if (timerMs < 0) return
        when (settings.getTimerMode()) {
            SettingsManager.TimerMode.UP -> timerMs += 1000
            SettingsManager.TimerMode.DOWN -> {
                timerMs -= 1000
                if (timerMs <= 0) {
                    timerMs = 0
                    gameOver = true
                    handler.removeCallbacks(timerRunnable)
                    tvTimer.text = getString(R.string.time_left, formatClock(0))
                    val moves = engine?.moves ?: 0
                    tvStatus.text = getString(R.string.puzzle_paused, formatClock(0))

                    AlertDialog.Builder(this)
                        .setTitle(R.string.app_name)
                        .setMessage("Time's up! You completed $moves moves.")
                        .setPositiveButton(R.string.new_puzzle) { _, _ ->
                            gameOver = false
                            startRandomPuzzle()
                        }
                        .setNegativeButton(R.string.back_to_menu) { _, _ -> finish() }
                        .show()
                    return
                }
            }
            else -> return
        }
        refreshTimerLabel()
    }

    private fun updateStatus() {
        val moves = engine?.moves ?: 0
        tvStatus.text = getString(R.string.game_status, moves)
    }

    private fun formatClock(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        engine?.let {
            outState.putString(STATE_BOARD, it.exportState())
            outState.putInt(STATE_MOVES, it.moves)
            outState.putBoolean(STATE_STARTED, it.started)
        }
        outState.putLong(STATE_TIME, timerMs)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timerRunnable)
    }

    override fun onResume() {
        super.onResume()
        if (settings.isTimerEnabled() && !gameOver && engine?.started == true) {
            handler.postDelayed(timerRunnable, 1000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        boardView.cancelHint()
        try { ioExecutor.shutdownNow() } catch (_: Exception) { }
        currentBitmap?.recycle()
    }
}