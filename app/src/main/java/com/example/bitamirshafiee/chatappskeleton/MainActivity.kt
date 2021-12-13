package com.example.bitamirshafiee.chatappskeleton

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
//import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

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

    private var firebaseDatabaseReference : DatabaseReference? = null
    private var firebaseAdapter : FirebaseRecyclerAdapter<Message, MessageViewHolder>? = null

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

    class MessageViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        lateinit var message: Message

        var messageTextView : TextView
        var messageImageView : ImageView
        var nameTextView : TextView
        var userImage : CircleImageView

        init {
            messageTextView = itemView.findViewById(R.id.message_text_view)
            messageImageView = itemView.findViewById(R.id.messenger_image_view)
            nameTextView = itemView.findViewById(R.id.name_text_view)
            userImage = itemView.findViewById(R.id.message_image_view)
        }

        fun bind(message: Message) {
            this.message = message

            if (message.text != null) {
                messageTextView.text = message.text

                messageTextView.visibility = View.VISIBLE

                messageImageView.visibility = View.GONE
            } else if (message.imageUrl != null) {
                messageTextView.visibility = View.GONE
                messageImageView.visibility = View.VISIBLE

                val imageUrl = message.imageUrl

                if (imageUrl!!.startsWith("gs://")) {
                    val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)

                    storageReference.downloadUrl.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val downloadUrl = task.result!!.toString()

                            Glide.with(messageImageView.context)
                                .load(downloadUrl)
                                .into(messageImageView)

                        } else {
                            Log.e(TAG, "Getting Download url was not successful ${task.exception}")
                        }
                    }
                }
            }
        }
    }
}
