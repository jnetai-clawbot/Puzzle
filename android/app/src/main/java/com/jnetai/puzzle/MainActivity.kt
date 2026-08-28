package com.jnetai.puzzle

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.jnetai.puzzle.utils.ErrorLogger
import com.jnetai.puzzle.utils.ImageLibraryUtils
import com.jnetai.puzzle.utils.SettingsManager
import java.util.concurrent.Executors

/**
 * Main menu: choose, random or upload a puzzle; settings and about.
 */
class MainActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "puzzle-main")
    }

    private val settings by lazy { SettingsManager.getInstance(this) }

    private val uploadLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            handlePickedImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            findViewById<Button>(R.id.btnChoosePuzzle).setOnClickListener {
                startActivity(Intent(this, GalleryActivity::class.java))
            }
            findViewById<Button>(R.id.btnRandomPuzzle).setOnClickListener {
                launchRandomPuzzle()
            }
            findViewById<Button>(R.id.btnUploadImage).setOnClickListener {
                uploadLauncher.launch("image/*")
            }
            findViewById<Button>(R.id.btnSettings).setOnClickListener {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            findViewById<Button>(R.id.btnAbout).setOnClickListener {
                startActivity(Intent(this, AboutActivity::class.java))
            }
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.UI_VIEW_BINDING,
                "Failed to bind main menu buttons", e)
            Toast.makeText(this, "Menu error - E-UI-001", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchRandomPuzzle() {
        val candidates = ImageLibraryUtils.listBuiltInImages(this)
            .filter { !settings.isHidden(it) }
        if (candidates.isEmpty()) {
            Toast.makeText(this, R.string.no_images, Toast.LENGTH_SHORT).show()
            return
        }
        val pick = candidates[kotlin.random.Random.nextInt(candidates.size)]
        startGame(pick)
    }

    private fun startGame(assetName: String) {
        val i = Intent(this, GameActivity::class.java)
        i.putExtra(GameActivity.EXTRA_ASSET, assetName)
        startActivity(i)
    }

    private fun handlePickedImage(uri: android.net.Uri) {
        Toast.makeText(this, R.string.loading_puzzle, Toast.LENGTH_SHORT).show()
        executor.execute {
            val bitmap = ImageLibraryUtils.loadUriImage(this, uri)
            if (bitmap == null) {
                runOnUiThread { toast(R.string.unknown_image_error) }
                return@execute
            }
            val cacheName = "user_" + System.currentTimeMillis() + ".jpg"
            val savedPath = ImageLibraryUtils.saveBitmapToCache(this, bitmap, cacheName)
            if (savedPath == null) {
                runOnUiThread { toast(R.string.image_empty) }
                return@execute
            }
            runOnUiThread {
                val i = Intent(this, GameActivity::class.java)
                i.putExtra(GameActivity.EXTRA_CACHE_IMAGE, cacheName)
                startActivity(i)
            }
        }
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { executor.shutdownNow() } catch (_: Exception) { }
    }
}