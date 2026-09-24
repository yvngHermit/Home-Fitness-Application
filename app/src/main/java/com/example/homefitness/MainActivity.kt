package com.example.homefitness

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.homefitness.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = Firebase.auth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Modern Activity Result Launcher for Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: Exception) {
            Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is already signed in
        if (auth.currentUser != null) {
            navigateToHome()
            return
        }

        setupGoogleClient()

        binding.loginButton?.setOnClickListener {
            loginUser()
        }

        binding.googleSignInButton?.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        binding.signUpText?.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun setupGoogleClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1032494177571-js26ai028tbv1259cas7vm3eu0e4pcni.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun loginUser() {
        val email = binding.emailCheck?.text.toString().trim()
        val password = binding.passwordCheck?.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                navigateToHome()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                navigateToHome()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Firebase Auth failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}