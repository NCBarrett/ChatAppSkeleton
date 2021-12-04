package com.example.bitamirshafiee.chatappskeleton

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val ANONYMOUS = "anonymous"
    }

    private var userName : String? = null
    private var userPhotoUrl : String? = null

    private var fireBaseAuth : FirebaseAuth? = null
    private var fireBaseUser : FirebaseUser? = null

//    private var googleApiClient : GoogleApiClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        userName = ANONYMOUS

        fireBaseAuth = FirebaseAuth.getInstance()
        fireBaseUser = fireBaseAuth!!.currentUser

        if (fireBaseUser == null) {
            startActivity(Intent (this@MainActivity, SignInActivity::class.java))
            finish()
        } else {
            userName = fireBaseUser!!.displayName

            if (fireBaseUser!!.photoUrl != null) {
                userPhotoUrl = fireBaseUser!!.photoUrl!!.toString()
            }
        }
    }
}
