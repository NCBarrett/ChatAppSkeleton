package com.example.bitamirshafiee.chatappskeleton

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.bitamirshafiee.chatappskeleton.databinding.ActivitySignInBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class SignInActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SignInActivity"
    }

    private lateinit var binding : ActivitySignInBinding

    private var googleSignInClient : GoogleSignInClient? = null
    private var fireBaseAuth : FirebaseAuth? = null

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data : Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)
                fireBaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign in failed $e")
                Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        binding = ActivitySignInBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        fireBaseAuth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this@SignInActivity, gso)

        binding.signInButton.setOnClickListener() {
            signIn()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient!!.signInIntent

//        resultLauncher.launch(intent)
        resultLauncher.launch(signInIntent)
    }

    private fun fireBaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

//        try {
            fireBaseAuth!!.signInWithCredential(credential)
                .addOnCompleteListener(this@SignInActivity) { task ->
                    if (task.isSuccessful) {
                        startActivity(Intent(this@SignInActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this@SignInActivity,
                            "Authentication failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
//        } catch (e: ApiException) {
//            Log.e(TAG, "Firebase sign in failed $e")
//            Toast.makeText(this, "Firebase sign in failed", Toast.LENGTH_SHORT).show()
//        }
    }
}