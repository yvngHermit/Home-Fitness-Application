package com.example.homefitness

import android.app.Activity
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object UserNavigation {
    fun afterSignIn(activity: Activity) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val destination = if (doc.getBoolean("onboardingCompleted") == true) HomeActivity::class.java else OnboardingActivity::class.java
                activity.startActivity(Intent(activity, destination).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                activity.finish()
            }
            .addOnFailureListener {
                activity.startActivity(Intent(activity, OnboardingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                activity.finish()
            }
    }
}
