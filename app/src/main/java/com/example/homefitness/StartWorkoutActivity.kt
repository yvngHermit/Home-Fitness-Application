package com.example.homefitness

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.homefitness.api.RetrofitInstance
import com.example.homefitness.databinding.ActivityStartWorkoutBinding
import com.example.homefitness.data.ExerciseCache
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class StartWorkoutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartWorkoutBinding
    private val auth by lazy { FirebaseAuth.getInstance() }; private val db by lazy { FirebaseFirestore.getInstance() }
    private var timer:CountDownTimer?=null; private var timeRemaining=60_000L; private var isPaused=false
    private var exerciseIds= mutableListOf<Int>(); private var exerciseNames= mutableListOf<String>(); private var currentIndex=0; private var workoutName="Workout"
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);binding=ActivityStartWorkoutBinding.inflate(layoutInflater);setContentView(binding.root);workoutName=intent.getStringExtra("exerciseName")?:"Workout";val ids=intent.getIntArrayExtra("exerciseIds");if(ids!=null&&ids.isNotEmpty())exerciseIds=ids.toMutableList() else {val id=intent.getIntExtra("exerciseId",-1);if(id>=0)exerciseIds.add(id)};binding.workoutTitle.text=workoutName;binding.backButton.setOnClickListener{timer?.cancel();finish()};binding.pauseButton.setOnClickListener{toggleTimer()};binding.nextButton.setOnClickListener{finishCurrentExercise()};if(exerciseIds.isEmpty()){exerciseNames.add(workoutName);showCurrentExercise()}else loadExerciseNames()}
    private fun loadExerciseNames(){lifecycleScope.launch{val cached=run { val cache = ExerciseCache(applicationContext); val items = cache.read("wger_all"); cache.close(); items };exerciseNames=mutableListOf();for(id in exerciseIds){val cachedName=cached.firstOrNull{it.id==id}?.name;val name=try{val result=RetrofitInstance.wgerApi.getExerciseById(id);result.body()?.name?.takeIf{it.isNotBlank()}?:cachedName?:"Exercise #$id"}catch(_:Exception){cachedName?:"Exercise #$id"};exerciseNames.add(name)};showCurrentExercise()}}
    private fun showCurrentExercise(){if(currentIndex>=exerciseNames.size){completeWorkout();return};val name=exerciseNames[currentIndex];binding.currentExerciseName.text=name;binding.exerciseProgressText.text="Exercise ${currentIndex+1} of ${exerciseNames.size}";binding.workoutProgressBar.max=60;timeRemaining=60_000L;updateTimerText();startTimer()}
    private fun startTimer(){timer?.cancel();isPaused=false;binding.pauseButton.setImageResource(android.R.drawable.ic_media_pause);timer=object:CountDownTimer(timeRemaining,1000){override fun onTick(ms:Long){timeRemaining=ms;updateTimerText();binding.workoutProgressBar.progress=((60_000L-ms)/1000).toInt()};override fun onFinish(){timeRemaining=0;binding.workoutProgressBar.progress=60;updateTimerText();finishCurrentExercise()}}.start()}
    private fun toggleTimer(){if(isPaused)startTimer()else{timer?.cancel();isPaused=true;binding.pauseButton.setImageResource(android.R.drawable.ic_media_play)}}
    private fun updateTimerText(){val seconds=timeRemaining/1000;binding.timerText.text=String.format(Locale.getDefault(),"%02d:%02d",seconds/60,seconds%60)}
    private fun finishCurrentExercise(){timer?.cancel();currentIndex++;if(currentIndex<exerciseNames.size)showCurrentExercise()else completeWorkout()}
    private fun completeWorkout(){timer?.cancel();val uid=auth.currentUser?.uid;if(uid!=null){val today=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date());val userRef=db.collection("users").document(uid);db.collection("users").document(uid).collection("workoutLogs").document(UUID.randomUUID().toString()).set(mapOf("workoutName" to workoutName,"exerciseCount" to exerciseNames.size,"date" to today,"completedAt" to FieldValue.serverTimestamp()));userRef.get().addOnSuccessListener{d->val last=d.getString("lastWorkoutDate")?:"";val streak=d.getLong("streak")?.toInt()?:0;if(last!=today){val cal=Calendar.getInstance();cal.add(Calendar.DAY_OF_YEAR,-1);val yesterday=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(cal.time);userRef.set(mapOf("streak" to if(last==yesterday)streak+1 else 1,"lastWorkoutDate" to today),com.google.firebase.firestore.SetOptions.merge())}}};Toast.makeText(this,"Workout completed!",Toast.LENGTH_SHORT).show();finish()}
    override fun onDestroy(){timer?.cancel();super.onDestroy()}
}
