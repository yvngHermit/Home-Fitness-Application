package com.example.homefitness

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.homefitness.databinding.ActivitySignupBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var credentialManager: CredentialManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        credentialManager = CredentialManager.create(this)

        binding.signupButton.setOnClickListener {
            createAccount()
        }

        binding.googleSignUpButton.setOnClickListener {
            signInWithGoogle()
        }

        binding.loginText.setOnClickListener {
            finish()
        }
    }

    private fun createAccount() {

        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        val confirmPassword =
            binding.confirmPasswordInput.text.toString()

        if (name.isEmpty()) {
            binding.nameInput.error = "Enter your name"
            return
        }

        if (email.isEmpty()) {
            binding.emailInput.error = "Enter your email"
            return
        }

        if (password.length < 6) {
            binding.passwordInput.error =
                "Password must be at least 6 characters"
            return
        }

        if (password != confirmPassword) {
            binding.confirmPasswordInput.error =
                "Passwords do not match"
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    val user = auth.currentUser

                    if (user != null) {
                        createUserDocument(
                            user.uid,
                            name,
                            email
                        )
                    }

                } else {

                    Toast.makeText(
                        this,
                        "Sign up failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun createUserDocument(
        uid: String,
        name: String,
        email: String
    ) {

        val userData = hashMapOf(
            "name" to name,
            "email" to email,
            "calorieGoal" to 2000,
            "caloriesConsumed" to 0,
            "mealsCalories" to 0,
            "streak" to 0,
            "lastWorkoutDate" to "",
            "weight" to 0.0,
            "weightChange" to 0.0
        )

        db.collection("users")
            .document(uid)
            .set(userData)
            .addOnSuccessListener {
                openHome()
            }
            .addOnFailureListener {
                openHome()
            }
    }

    private fun signInWithGoogle() {

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("1032494177571-js26ai028tbv1259cas7vm3eu0e4pcni.apps.googleusercontent.com")
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {

            try {

                val result = credentialManager.getCredential(
                    this@SignupActivity,
                    request
                )

                val credential = result.credential

                val googleCredential =
                    GoogleIdTokenCredential.createFrom(
                        credential.data
                    )

                firebaseAuthWithGoogle(
                    googleCredential.idToken
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this@SignupActivity,
                    "Google sign-in failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {

        val credential =
            GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    val user = auth.currentUser

                    if (user != null) {

                        val userData = hashMapOf(
                            "name" to (
                                    user.displayName ?: "User"
                                    ),
                            "email" to (
                                    user.email ?: ""
                                    ),
                            "calorieGoal" to 2000,
                            "caloriesConsumed" to 0,
                            "mealsCalories" to 0,
                            "streak" to 0,
                            "lastWorkoutDate" to "",
                            "weight" to 0.0,
                            "weightChange" to 0.0
                        )

                        db.collection("users")
                            .document(user.uid)
                            .set(
                                userData,
                                com.google.firebase.firestore.SetOptions.merge()
                            )
                            .addOnCompleteListener {
                                openHome()
                            }

                    } else {
                        openHome()
                    }

                } else {

                    Toast.makeText(
                        this,
                        "Google sign-in failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun openHome() {

        startActivity(
            Intent(this, HomeActivity::class.java)
        )

        finish()
    }
}