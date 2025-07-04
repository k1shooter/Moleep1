package com.example.moleep1.ui.dashboard

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moleep1.databinding.ItemImagePaperBinding

class ImageDetailAdapter(private val imageUris: List<Uri>) :
    RecyclerView.Adapter<ImageDetailAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(private val binding: ItemImagePaperBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: Uri) {
            Glide.with(itemView)
                .load(uri)
                .into(binding.pagerImageView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImagePaperBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageUris[position])
    }

    override fun getItemCount(): Int = imageUris.size
}