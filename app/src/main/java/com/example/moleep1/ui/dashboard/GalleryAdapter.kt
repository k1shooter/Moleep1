package com.example.moleep1.ui.dashboard

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moleep1.databinding.ItemGalleryImageBinding

class GalleryAdapter(
    private val imageUris: MutableList<Uri>,
    private val onItemClick: (Int) -> Unit // 아이템 클릭 시 호출될 함수
    ):
    RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    class GalleryViewHolder(val binding: ItemGalleryImageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val binding =
            ItemGalleryImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        // ✅ 2. ViewHolder가 생성될 때 클릭 리스너를 설정
        val viewHolder = GalleryViewHolder(binding)
        viewHolder.itemView.setOnClickListener {
            val position = viewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onItemClick(position)
            }
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val imageUri = imageUris[position]

        Glide.with(holder.binding.imageView.context)
            .load(imageUri)
            .centerCrop()
            .override(200, 200)
            .into(holder.binding.imageView)
    }

    override fun getItemCount(): Int = imageUris.size

    fun addImage(imageUri: Uri) {
        imageUris.add(imageUri)
        notifyItemInserted(imageUris.size - 1)
    }

    fun updateList(newList: List<Uri>) {
        imageUris.clear()
        imageUris.addAll(newList)
        notifyDataSetChanged()
    }
}
