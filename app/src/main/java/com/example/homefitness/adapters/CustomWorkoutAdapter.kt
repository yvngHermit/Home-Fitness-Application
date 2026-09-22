package com.example.homefitness.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.homefitness.databinding.ItemCustomWorkoutBinding
import com.example.homefitness.models.CustomWorkout

class CustomWorkoutAdapter(
    private val onWorkoutClick:
        (CustomWorkout) -> Unit
) : RecyclerView.Adapter<CustomWorkoutAdapter.WorkoutViewHolder>() {

    private var workouts =
        emptyList<CustomWorkout>()

    fun submitList(
        newWorkouts: List<CustomWorkout>
    ) {
        workouts = newWorkouts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): WorkoutViewHolder {

        val binding =
            ItemCustomWorkoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return WorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: WorkoutViewHolder,
        position: Int
    ) {
        holder.bind(workouts[position])
    }

    override fun getItemCount(): Int =
        workouts.size

    inner class WorkoutViewHolder(
        private val binding: ItemCustomWorkoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(workout: CustomWorkout) {

            binding.customWorkoutName.text =
                workout.name

            binding.customWorkoutDetails.text =
                workout.description

            binding.root.setOnClickListener {
                onWorkoutClick(workout)
            }
        }
    }
}