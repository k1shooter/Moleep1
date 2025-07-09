package com.example.moleep1.ui.added

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moleep1.R
import com.example.moleep1.databinding.ItemPersonToggleBinding // 재사용할 레이아웃의 바인딩
import com.example.moleep1.list_item

class PathPersonAdapter(
    private val onPersonToggled: (personId: String, isSelected: Boolean) -> Unit
) : RecyclerView.Adapter<PathPersonAdapter.ViewHolder>() {

    private var personList: List<list_item> = emptyList()
    private var selectedIds: Set<String> = emptySet()

    fun submitList(newList: List<list_item>, newSelectedIds: Set<String>) {
        personList = newList
        selectedIds = newSelectedIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPersonToggleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(personList[position])
    }

    override fun getItemCount(): Int = personList.size

    inner class ViewHolder(private val binding: ItemPersonToggleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(person: list_item) {
            binding.tvPersonName.text = person.name
            Glide.with(itemView.context)
                .load(Uri.parse(person.imageuri))
                .placeholder(R.drawable.ic_launcher_background)
                .circleCrop()
                .into(binding.ivPersonPhoto)

            binding.cbPersonSelected.setOnCheckedChangeListener(null)
            binding.cbPersonSelected.isChecked = selectedIds.contains(person.id)
            binding.cbPersonSelected.setOnCheckedChangeListener { _, isChecked ->
                onPersonToggled(person.id, isChecked)
            }
            itemView.setOnClickListener {
                binding.cbPersonSelected.toggle()
            }
        }
    }
}