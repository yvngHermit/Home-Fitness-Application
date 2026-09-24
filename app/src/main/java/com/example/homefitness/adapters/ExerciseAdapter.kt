package com.example.homefitness.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.homefitness.databinding.ItemExerciseBinding
import com.example.homefitness.models.WgerExercise

class ExerciseAdapter(
    private val onExerciseClick: (WgerExercise) -> Unit
) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    private var exercises = emptyList<WgerExercise>()

    fun submitList(newExercises: List<WgerExercise>) {
        exercises = newExercises
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemExerciseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(exercises[position])
    }

    override fun getItemCount(): Int = exercises.size

    inner class ExerciseViewHolder(
        private val binding: ItemExerciseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: WgerExercise) {
            // Display valid exercise name, or fall back to ID / default title if empty or null
            val displayName = if (!exercise.name.isNullOrBlank()) {
                exercise.name
            } else if (exercise.id != null) {
                "Exercise #${exercise.id}"
            } else {
                "Fitness Exercise"
            }
            binding.exerciseName.text = displayName

            // Clean HTML tags safely from description
            val cleanDescription = (exercise.description ?: "")
                .replace(Regex("<[^>]*>"), "")
                .trim()

            // Bind description or Category ID to exerciseMuscle view
            binding.exerciseMuscle.text = if (cleanDescription.isNotEmpty()) {
                cleanDescription
            } else {
                "Category ID: ${exercise.category ?: "N/A"}"
            }

            // Set default text for equipment view
            binding.exerciseEquipment.text = "Tap to view details"

            binding.root.setOnClickListener {
                onExerciseClick(exercise)
            }
        }
    }
}