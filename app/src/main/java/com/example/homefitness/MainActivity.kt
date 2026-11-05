package com.example.homefitness

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.lifecycle.lifecycleScope
import com.example.homefitness.databinding.ActivityMainBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val credentialManager by lazy { CredentialManager.create(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater);setContentView(binding.root)
        if(auth.currentUser!=null){UserNavigation.afterSignIn(this);return}
        binding.loginButton.setOnClickListener{loginUser()}
        binding.googleSignInButton.setOnClickListener{signInWithGoogle()}
        binding.signUpText.setOnClickListener{startActivity(android.content.Intent(this,SignupActivity::class.java))}
        binding.forgotPasswordText.setOnClickListener { sendPasswordReset() }
    }
    private fun loginUser(){val email=binding.emailCheck.text.toString().trim();val password=binding.passwordCheck.text.toString();if(email.isBlank()||password.isBlank()){Toast.makeText(this,"Enter your email and password",Toast.LENGTH_SHORT).show();return};auth.signInWithEmailAndPassword(email,password).addOnSuccessListener{UserNavigation.afterSignIn(this)}.addOnFailureListener{Toast.makeText(this,"Login failed: ${it.localizedMessage}",Toast.LENGTH_LONG).show()}}
    private fun sendPasswordReset() {
        val email = binding.emailCheck.text.toString().trim()
        if (email.isBlank()) {
            binding.emailCheck.error = getString(R.string.email_label)
            return
        }
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { Toast.makeText(this, "Password reset email sent", Toast.LENGTH_LONG).show() }
            .addOnFailureListener { Toast.makeText(this, "Could not send reset email: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
    }
    private fun signInWithGoogle(){lifecycleScope.launch{try{val option=GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(getString(R.string.default_web_client_id)).setAutoSelectEnabled(false).build();val request=GetCredentialRequest.Builder().addCredentialOption(option).build();val result=credentialManager.getCredential(this@MainActivity,request);val credential=result.credential;if(credential is CustomCredential && credential.type==GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL){val token=GoogleIdTokenCredential.createFrom(credential.data).idToken;auth.signInWithCredential(GoogleAuthProvider.getCredential(token,null)).addOnSuccessListener{UserNavigation.afterSignIn(this@MainActivity)}.addOnFailureListener{Toast.makeText(this@MainActivity,"Google authentication failed: ${it.localizedMessage}",Toast.LENGTH_LONG).show()}}else Toast.makeText(this@MainActivity,"Unsupported Google sign-in credential",Toast.LENGTH_SHORT).show()}catch(e:Exception){Toast.makeText(this@MainActivity,"Google sign-in cancelled or failed: ${e.localizedMessage}",Toast.LENGTH_LONG).show()}}}
}
