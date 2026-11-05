package com.example.homefitness.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.homefitness.databinding.ItemSelectExerciseBinding
import com.example.homefitness.models.WgerExercise

class SelectExerciseAdapter(
    private val onExerciseSelected: (WgerExercise, Boolean) -> Unit
) : RecyclerView.Adapter<SelectExerciseAdapter.ViewHolder>() {

    private var exercises = emptyList<WgerExercise>()
    private val selectedIds = mutableSetOf<Int>()

    fun submitList(newList: List<WgerExercise>) {
        exercises = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSelectExerciseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(exercises[position])
    }

    override fun getItemCount(): Int = exercises.size

    inner class ViewHolder(
        private val binding: ItemSelectExerciseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: WgerExercise) {
            binding.exerciseName.text = exercise.name ?: "Exercise"

            val isSelected = exercise.id != null && selectedIds.contains(exercise.id)
            binding.exerciseCheckBox.isChecked = isSelected

            binding.root.setOnClickListener {
                exercise.id?.let { id ->
                    if (selectedIds.contains(id)) {
                        selectedIds.remove(id)
                        binding.exerciseCheckBox.isChecked = false
                        onExerciseSelected(exercise, false)
                    } else {
                        selectedIds.add(id)
                        binding.exerciseCheckBox.isChecked = true
                        onExerciseSelected(exercise, true)
                    }
                }
            }
        }
    }
}