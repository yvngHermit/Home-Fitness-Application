package com.example.homefitness.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.homefitness.databinding.ItemExerciseBinding
import com.example.homefitness.models.WgerExercise
import com.bumptech.glide.Glide

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
            binding.exerciseName.text = exercise.name ?: "Exercise"
            // Category IDs and long API descriptions are intentionally not shown.
            binding.exerciseMuscle.visibility = android.view.View.GONE

            // Show equipment details and the exercise image supplied by wger, when available.
            binding.exerciseEquipment.text = exercise.equipment
                ?.mapNotNull { it.name }
                ?.joinToString()
                ?.takeIf { it.isNotBlank() }
                ?.let { "Equipment: $it" } ?: "Bodyweight / equipment not listed"

            val imageUrl = exercise.images.orEmpty()
                .firstOrNull { it.isMain }?.preferredUrl()
                ?: exercise.images.orEmpty().firstOrNull()?.preferredUrl()

            Glide.with(binding.exerciseImage)
                .load(imageUrl)
                .placeholder(com.example.homefitness.R.drawable.ic_workout)
                .error(com.example.homefitness.R.drawable.ic_workout)
                .centerCrop()
                .into(binding.exerciseImage)

            binding.root.setOnClickListener {
                onExerciseClick(exercise)
            }
        }
    }
}