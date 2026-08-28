package com.jnetai.puzzle

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jnetai.puzzle.utils.ErrorLogger
import com.jnetai.puzzle.utils.ImageLibraryUtils
import com.jnetai.puzzle.utils.SettingsManager

/**
 * Gallery - grid of built-in puzzle images. Supports a favourites toggle
 * (star icon per tile) and an all / favourites-only filter. Hidden images are
 * excluded. Tapping an image starts the game on it.
 */
class GalleryActivity : AppCompatActivity() {

    private lateinit var adapter: ImageAdapter
    private var favouritesOnly = false

    private val settings by lazy { SettingsManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        try {
            val rv = findViewById<RecyclerView>(R.id.rvImages)
            val btnFilter = findViewById<Button>(R.id.btnFavFilter)

            val spanCount = 3
            rv.layoutManager = GridLayoutManager(this, spanCount)
            adapter = ImageAdapter()
            rv.adapter = adapter

            loadImages().let { adapter.submit(it) }

            btnFilter.setOnClickListener {
                favouritesOnly = !favouritesOnly
                btnFilter.setText(
                    if (favouritesOnly) R.string.gallery_favorites_only else R.string.gallery_all
                )
                adapter.submit(currentList())
            }
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.UI_VIEW_BINDING, "Failed to set up gallery", e)
            Toast.makeText(this, "Gallery error - E-UI-001", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh after returning from Settings changes.
        adapter.submit(currentList())
    }

    private fun allImageNames(): List<String> = ImageLibraryUtils.listBuiltInImages(this)
        .filter { !settings.isHidden(it) }

    private fun currentList(): List<String> {
        val all = allImageNames()
        return if (favouritesOnly) all.filter { settings.isFavourite(it) } else all
    }

    private fun loadImages(): List<String> = currentList()

    private inner class ImageAdapter :
        RecyclerView.Adapter<ImageAdapter.ImageHolder>() {

        private val items = mutableListOf<String>()

        fun submit(newItems: List<String>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gallery, parent, false)
            return ImageHolder(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ImageHolder, position: Int) {
            holder.bind(items[position])
        }
    }

    private inner class ImageHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imgThumb: ImageView = view.findViewById(R.id.imgThumb)
        private val imgFavorite: ImageView = view.findViewById(R.id.imgFavorite)
        private val hideOverlay: View = view.findViewById(R.id.hideOverlay)

        fun bind(name: String) {
            try {
                Glide.with(this@GalleryActivity)
                    .load(ImageLibraryUtils.assetUri(name))
                    .thumbnail(0.3f)
                    .centerCrop()
                    .into(imgThumb)

                if (settings.isFavourite(name)) {
                    imgFavorite.setImageResource(R.drawable.ic_star_fill)
                } else {
                    imgFavorite.setImageResource(R.drawable.ic_star_outline)
                }

                imgFavorite.setOnClickListener {
                    val added = settings.toggleFavourite(name)
                    Toast.makeText(
                        this@GalleryActivity,
                        if (added) R.string.favourite_added else R.string.favourite_removed,
                        Toast.LENGTH_SHORT
                    ).show()
                    bind(name)
                    if (favouritesOnly && !added) {
                        adapter.submit(currentList())
                    }
                }

                hideOverlay.visibility = View.GONE

                itemView.setOnClickListener {
                    val i = Intent(this@GalleryActivity, GameActivity::class.java)
                    i.putExtra(GameActivity.EXTRA_ASSET, name)
                    startActivity(i)
                }
            } catch (e: Exception) {
                ErrorLogger.logf(ErrorLogger.Codes.UI_VIEW_BINDING,
                    "Failed to bind gallery item '%s'", e, name)
            }
        }
    }
}