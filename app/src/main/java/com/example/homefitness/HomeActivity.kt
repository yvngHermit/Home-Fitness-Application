package com.example.homefitness

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.homefitness.api.RetrofitInstance
import com.example.homefitness.databinding.ActivityHomepageBinding
import com.example.homefitness.data.ExerciseCache
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomepageBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var todayWorkoutId=-1
    private var todayWorkoutName=""
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);binding=ActivityHomepageBinding.inflate(layoutInflater);setContentView(binding.root);binding.signoutButton.setOnClickListener{auth.signOut();startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));finish()};binding.startWorkoutButton.setOnClickListener{launchWorkout()};setupNavigation();loadUser()}
    override fun onResume(){super.onResume();if(::binding.isInitialized)loadUser()}
    private fun setupNavigation(){val nav=binding.root.findViewById<BottomNavigationView>(R.id.bottomNavigation);nav?.selectedItemId=R.id.nav_home;nav?.setOnItemSelectedListener{item->when(item.itemId){R.id.nav_home->true;R.id.nav_workouts->{startActivity(Intent(this,WorkoutCategoriesActivity::class.java));true};R.id.nav_nutrition->{startActivity(Intent(this,NutritionActivity::class.java));true};R.id.nav_progress->{startActivity(Intent(this,ProgressActivity::class.java));true};R.id.nav_profile->{startActivity(Intent(this,ProfileActivity::class.java));true};else->false}}}
    private fun loadUser(){val user=auth.currentUser?:return;db.collection("users").document(user.uid).get().addOnSuccessListener{d->val name=d.getString("name")?:user.displayName?:"User";binding.root.findViewById<TextView>(R.id.greetingText)?.text=getString(R.string.hello_name,name);val goal=(d.getLong("calorieGoal")?:2000).toInt().coerceAtLeast(1);val streak=(d.getLong("streak")?:0).toInt();val weight=d.getDouble("weight");val change=d.getDouble("weightChange");binding.root.findViewById<TextView>(R.id.calorieGoalText)?.text="/ $goal Kcal";binding.root.findViewById<TextView>(R.id.mealGoalText)?.text="/ $goal Kcal";loadDailyCalories(goal);binding.root.findViewById<TextView>(R.id.streakText)?.text="$streak Days";binding.root.findViewById<TextView>(R.id.weightText)?.text=weight?.let{"$it kg"}?:"Not set";binding.root.findViewById<TextView>(R.id.weightChangeText)?.text=change?.let{"$it kg this week"}?:"Track your weight";loadTodayWorkout(d)}.addOnFailureListener{Toast.makeText(this,"Could not load your profile",Toast.LENGTH_SHORT).show()}}

    private fun loadDailyCalories(goal:Int){val uid=auth.currentUser?.uid?:return;val today=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date());db.collection("users").document(uid).collection("mealLogs").whereEqualTo("date",today).get().addOnSuccessListener{docs->val total=docs.documents.sumOf{it.getLong("calories")?:0L}.toInt();binding.root.findViewById<TextView>(R.id.calorieValueText)?.text=total.toString();binding.root.findViewById<ProgressBar>(R.id.calorieProgress)?.apply{max=goal;progress=total.coerceAtMost(goal)};binding.root.findViewById<TextView>(R.id.mealValueText)?.text=total.toString();binding.root.findViewById<ProgressBar>(R.id.mealProgress)?.apply{max=goal;progress=total.coerceAtMost(goal)}}}
    private fun loadTodayWorkout(d:com.google.firebase.firestore.DocumentSnapshot){val today=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date());if(d.getString("todayWorkoutDate")==today&&d.contains("todayWorkoutId")){todayWorkoutId=d.getLong("todayWorkoutId")?.toInt()?:-1;todayWorkoutName=d.getString("todayWorkoutName")?:"Daily bodyweight session";showWorkout();return};val goal=d.getString("fitnessGoal")?:"general_fitness";val equipment=d.getString("equipment")?:"none";lifecycleScope.launch{try{val response=RetrofitInstance.wgerApi.getExercises(language=2,limit=100);val remote=response.body()?.results.orEmpty().filter{it.id!=null&&!it.name.isNullOrBlank()};if(remote.isNotEmpty())run { val cache = ExerciseCache(applicationContext); cache.save("wger_all", remote); cache.close() };val cached=run { val cache = ExerciseCache(applicationContext); val items = cache.read("wger_all"); cache.close(); items };val all=remote.ifEmpty{cached};val pool=when(equipment){"none"->all.filter{it.equipment.isNullOrEmpty()};"home"->all.filter{exercise->exercise.equipment.isNullOrEmpty()||exercise.equipment.orEmpty().any{it.name.orEmpty().contains("dumbbell",true)||it.name.orEmpty().contains("band",true)||it.name.orEmpty().contains("mat",true)}};else->all};val keywords=when(goal){"weight_loss","endurance"->listOf("jump","burpee","mountain climber","high knees","run","cardio");"build_muscle"->listOf("push","press","squat","row","curl","pull");else->listOf("squat","push","plank","lunge","row")};val goalMatches=pool.filter{exercise->keywords.any{key->exercise.name.orEmpty().contains(key,true)}};val recommendationPool=goalMatches.ifEmpty{pool};if(recommendationPool.isNotEmpty()){val chosen=recommendationPool.random();todayWorkoutId=chosen.id!!;todayWorkoutName=chosen.name!!;db.collection("users").document(auth.currentUser!!.uid).set(mapOf("todayWorkoutId" to todayWorkoutId,"todayWorkoutName" to todayWorkoutName,"todayWorkoutDate" to today),com.google.firebase.firestore.SetOptions.merge());showWorkout()}else{todayWorkoutId=-1;todayWorkoutName="Explore your workout library";showWorkout()}}catch(_:Exception){val cached=run { val cache = ExerciseCache(applicationContext); val items = cache.read("wger_all"); cache.close(); items };val candidates=cached.filter{it.id!=null&&!it.name.isNullOrBlank()};if(candidates.isNotEmpty()){val chosen=candidates.random();todayWorkoutId=chosen.id!!;todayWorkoutName=chosen.name!!;showWorkout()}else{todayWorkoutId=-1;todayWorkoutName="Explore your workout library";showWorkout()}}}}
    private fun showWorkout(){binding.root.findViewById<TextView>(R.id.todayWorkoutName)?.text=todayWorkoutName;binding.root.findViewById<TextView>(R.id.todayWorkoutDetails)?.text=getString(R.string.recommendation_details)}
    private fun launchWorkout(){if(todayWorkoutId<0){startActivity(Intent(this,WorkoutCategoriesActivity::class.java));return};startActivity(Intent(this,StartWorkoutActivity::class.java).putExtra("exerciseId",todayWorkoutId).putExtra("exerciseName",todayWorkoutName))}
}
