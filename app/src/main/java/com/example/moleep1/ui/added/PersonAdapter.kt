// PersonAdapter.kt
package com.example.moleep1.ui.added

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Glide 사용을 위해 import (또는 Coil)
import com.example.moleep1.R
import com.example.moleep1.databinding.ItemPersonToggleBinding
import com.example.moleep1.list_item

class PersonAdapter(
    private val onPersonToggled: (personId: String, isSelected: Boolean) -> Unit
) : RecyclerView.Adapter<PersonAdapter.PersonViewHolder>() {

    private var personList: List<list_item> = emptyList()
    private var selectedIds: Set<String> = emptySet()

    fun submitList(newList: List<list_item>, newSelectedIds: Set<String>) {
        personList = newList
        selectedIds = newSelectedIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val binding = ItemPersonToggleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PersonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        holder.bind(personList[position])
    }

    override fun getItemCount(): Int = personList.size

    inner class PersonViewHolder(private val binding: ItemPersonToggleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(person: list_item) {
            // ❗ [수정] Java 클래스의 getter를 사용하여 데이터에 접근합니다.
            binding.tvPersonName.text = person.name // public 필드는 직접 접근 가능

            // Glide 라이브러리를 사용해 이미지 로드 (build.gradle에 추가 필요)
            // person.imageuri는 "android.resource://..." 형태의 문자열 URI입니다.
            Glide.with(itemView.context)
                .load(Uri.parse(person.imageuri))
                .placeholder(R.drawable.ic_launcher_background) // 로딩 중 이미지
                .circleCrop() // 원형으로 자르기
                .into(binding.ivPersonPhoto)


            binding.cbPersonSelected.setOnCheckedChangeListener(null)
            // ❗ [수정] person.id 대신 person.getId() 사용
            binding.cbPersonSelected.isChecked = selectedIds.contains(person.id)

            binding.cbPersonSelected.setOnCheckedChangeListener { _, isChecked ->
                // ❗ [수정] person.id 대신 person.getId() 사용
                onPersonToggled(person.id, isChecked)
            }

            itemView.setOnClickListener {
                binding.cbPersonSelected.toggle()
            }
        }
    }
}