package com.example.moleep1.ui.added

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moleep1.R
import com.example.moleep1.databinding.ItemPersonSimpleBinding
import com.example.moleep1.list_item

class PathPersonAdapter(
    private val onPersonClicked: (list_item) -> Unit
) : RecyclerView.Adapter<PathPersonAdapter.ViewHolder>() {

    private var personList: List<list_item> = emptyList()

    fun submitList(newList: List<list_item>) {
        personList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPersonSimpleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val person = personList[position]
        holder.bind(person)
        holder.itemView.setOnClickListener { onPersonClicked(person) }
    }

    override fun getItemCount(): Int = personList.size

    class ViewHolder(private val binding: ItemPersonSimpleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(person: list_item) {
            binding.tvPersonNameSimple.text = person.name
            Glide.with(itemView.context)
                .load(Uri.parse(person.imageuri))
                .placeholder(R.drawable.ic_launcher_background)
                .into(binding.ivPersonPhotoSimple)
        }
    }
}