package com.jnetai.puzzle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jnetai.puzzle.utils.ErrorLogger
import com.jnetai.puzzle.utils.ImageLibraryUtils
import com.jnetai.puzzle.utils.SettingsManager

/**
 * HiddenImagesActivity - list every built-in image with a checkbox controlling
 * whether it is shown in the gallery / used by the Random button.
 *
 * Default: all visible (checbox ticked). Unticking hides the image.
 */
class HiddenImagesActivity : AppCompatActivity() {

    private lateinit var adapter: HiddenAdapter
    private val settings by lazy { SettingsManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hidden_images)

        try {
            val rv = findViewById<RecyclerView>(R.id.rvHiddenImages)
            rv.layoutManager = LinearLayoutManager(this)
            adapter = HiddenAdapter(ImageLibraryUtils.listBuiltInImages(this))
            rv.adapter = adapter

            findViewById<Button>(R.id.btnDone).setOnClickListener { finish() }
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.UI_VIEW_BINDING, "Failed to set up hidden images screen", e)
            Toast.makeText(this, "Hidden images error - E-UI-001", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private inner class HiddenAdapter(private val items: List<String>) :
        RecyclerView.Adapter<HiddenAdapter.Holder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_hidden_image, parent, false)
            return Holder(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(items[position])
        }

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            private val imgPreview: ImageView = view.findViewById(R.id.imgPreview)
            private val tvName: TextView = view.findViewById(R.id.tvImageName)
            private val chkVisible: CheckBox = view.findViewById(R.id.chkVisible)

            fun bind(name: String) {
                try {
                    Glide.with(this@HiddenImagesActivity)
                        .load(ImageLibraryUtils.assetUri(name))
                        .centerCrop()
                        .into(imgPreview)
                    tvName.text = name
                    chkVisible.isChecked = !settings.isHidden(name)
                    chkVisible.setOnCheckedChangeListener { _, checked ->
                        settings.setHidden(name, !checked)
                    }
                } catch (e: Exception) {
                    ErrorLogger.logf(ErrorLogger.Codes.UI_VIEW_BINDING,
                        "Failed to bind hidden-images row '%s'", e, name)
                }
            }
        }
    }
}