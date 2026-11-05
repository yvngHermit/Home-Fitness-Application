package com.example.homefitness

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.example.homefitness.databinding.ActivitySignupBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val credentialManager by lazy { CredentialManager.create(this) }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState);binding=ActivitySignupBinding.inflate(layoutInflater);setContentView(binding.root);binding.signupButton.setOnClickListener{createAccount()};binding.googleSignUpButton.setOnClickListener{signInWithGoogle()};binding.loginText.setOnClickListener{startActivity(Intent(this,MainActivity::class.java));finish()} }
    private fun createAccount(){val name=binding.nameInput.text.toString().trim();val email=binding.emailInput.text.toString().trim();val password=binding.passwordInput.text.toString();val confirm=binding.confirmPasswordInput.text.toString();if(name.isBlank()||email.isBlank()||password.isBlank()||confirm.isBlank()){Toast.makeText(this,"Complete all fields",Toast.LENGTH_SHORT).show();return};if(password.length<6){binding.passwordInput.error="Use at least 6 characters";return};if(password!=confirm){binding.confirmPasswordInput.error="Passwords do not match";return};auth.createUserWithEmailAndPassword(email,password).addOnSuccessListener{val user=auth.currentUser?:return@addOnSuccessListener;val data=hashMapOf<String,Any>("name" to name,"email" to email,"calorieGoal" to 2000,"caloriesConsumed" to 0,"mealsCalories" to 0,"streak" to 0,"lastWorkoutDate" to "","onboardingCompleted" to false);db.collection("users").document(user.uid).set(data).addOnSuccessListener{UserNavigation.afterSignIn(this)}.addOnFailureListener{Toast.makeText(this,"Account created but profile could not be saved: ${it.localizedMessage}",Toast.LENGTH_LONG).show()}}.addOnFailureListener{Toast.makeText(this,"Registration failed: ${it.localizedMessage}",Toast.LENGTH_LONG).show()}}
    private fun signInWithGoogle(){lifecycleScope.launch{try{val option=GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(getString(R.string.default_web_client_id)).setAutoSelectEnabled(false).build();val request=GetCredentialRequest.Builder().addCredentialOption(option).build();val result=credentialManager.getCredential(this@SignupActivity,request);val credential=result.credential;if(credential is CustomCredential&&credential.type==GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL){val token=GoogleIdTokenCredential.createFrom(credential.data).idToken;auth.signInWithCredential(GoogleAuthProvider.getCredential(token,null)).addOnSuccessListener{val user=auth.currentUser?:return@addOnSuccessListener;val ref=db.collection("users").document(user.uid);ref.get().addOnSuccessListener{doc->if(!doc.exists()){ref.set(mapOf("name" to (user.displayName?:"User"),"email" to (user.email?:""),"calorieGoal" to 2000,"caloriesConsumed" to 0,"mealsCalories" to 0,"streak" to 0,"lastWorkoutDate" to "","onboardingCompleted" to false)).addOnSuccessListener{UserNavigation.afterSignIn(this@SignupActivity)}}else UserNavigation.afterSignIn(this@SignupActivity)}}.addOnFailureListener{Toast.makeText(this@SignupActivity,"Google authentication failed: ${it.localizedMessage}",Toast.LENGTH_LONG).show()}}else Toast.makeText(this@SignupActivity,"Unsupported Google sign-in credential",Toast.LENGTH_SHORT).show()}catch(e:Exception){Toast.makeText(this@SignupActivity,"Google sign-in cancelled or failed: ${e.localizedMessage}",Toast.LENGTH_LONG).show()}}}
}
