package com.example.bitamirshafiee.chatappskeleton

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
//import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val ANONYMOUS = "anonymous"
        const val MESSAGE_CHILD = "messages"
        const val LOADING_IMAGE_URL = "https://flevix.com/wp-content/uploads/2019/07/Ring-Loading-feature.gif"
    }

    private var userName : String? = null
    private var userPhotoUrl : String? = null

    private var fireBaseAuth : FirebaseAuth? = null
    private var fireBaseUser : FirebaseUser? = null

//    private var googleApiClient : GoogleApiClient? = null

    lateinit var linearLayoutManager : LinearLayoutManager

    private var firebaseDatabaseReference : DatabaseReference? = null
    private var firebaseAdapter : FirebaseRecyclerAdapter<Message, MessageViewHolder>? = null

    val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data

            if (result.data != null) {
                val uri = data?.data

                val tempMessage = Message(null, userName, userPhotoUrl, LOADING_IMAGE_URL)
                firebaseDatabaseReference!!.child(MESSAGE_CHILD).push().setValue(tempMessage){
                        databaseError, databaseReference ->
                    if (databaseError == null) {
                        val key = databaseReference.key
                        val storageReference = FirebaseStorage.getInstance()
                            .getReference(fireBaseUser!!.uid)
                            .child(key!!)
                            .child(uri?.lastPathSegment!!)

                        putImageInStorage(storageReference, uri, key)
                    } else {
                        Log.e(TAG, "Unable to write message to database ${databaseError.toException()}")
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        linearLayoutManager = LinearLayoutManager(this@MainActivity)
        linearLayoutManager.stackFromEnd = true

        firebaseDatabaseReference = FirebaseDatabase.getInstance().reference

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        userName = ANONYMOUS

        fireBaseAuth = FirebaseAuth.getInstance()
        fireBaseUser = fireBaseAuth!!.currentUser

        if (fireBaseUser == null) {
            Log.d(TAG, "USER IS NULL: $fireBaseUser")

            startActivity(Intent (this@MainActivity, SignInActivity::class.java))
            finish()
            return
        } else {
            userName = fireBaseUser!!.displayName

            if (fireBaseUser!!.photoUrl != null) {
                userPhotoUrl = fireBaseUser!!.photoUrl!!.toString()
            }
            Log.d(TAG, "USER IS NOT NULL")
        }

        val parser = SnapshotParser<Message>{
            snapshot: DataSnapshot ->

            val chatMessage = snapshot.getValue(Message::class.java)

            if (chatMessage != null) {
                chatMessage.id = snapshot.key
            }
            chatMessage!!
        }

        val messageRefs = firebaseDatabaseReference!!.child(MESSAGE_CHILD)

        val options = FirebaseRecyclerOptions.Builder<Message>()
            .setQuery(messageRefs, parser)
            .build()

        firebaseAdapter = object : FirebaseRecyclerAdapter<Message, MessageViewHolder>(options) {
            override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MessageViewHolder {
                val inflater = LayoutInflater.from(viewGroup.context)

                return MessageViewHolder(inflater.inflate(R.layout.item_message, viewGroup, false))
            }

            override fun onBindViewHolder(holder: MessageViewHolder, position: Int, model: Message) {
                progress_bar.visibility = ProgressBar.INVISIBLE

                holder.bind(model)
            }
        }

        firebaseAdapter!!.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val messageCount = firebaseAdapter!!.itemCount
                val lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()

                // if(A || A && B) -> if(A || (A && B))
                if (lastVisiblePosition == -1 || positionStart >= messageCount -1 && lastVisiblePosition == positionStart - 1){
                    recycler_view!!.scrollToPosition(positionStart)
                }
            }
        })

        recycler_view.layoutManager = linearLayoutManager
        recycler_view.adapter = firebaseAdapter

        send_button.setOnClickListener {

            val message = Message(text_message_edit_text!!.text.toString(), userName!!, userPhotoUrl, null)
            firebaseDatabaseReference!!.child(MESSAGE_CHILD).push().setValue(message)

            text_message_edit_text!!.setText("")
        }

        add_image_image_view.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            intent.type = "image/*"
            resultLauncher.launch(intent)
        }
    }

    private fun putImageInStorage(storageReference: StorageReference, uri: Uri?, key: String?) {
        val uploadTask = storageReference.putFile(uri!!)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful){
                throw task.exception!!
            }
            storageReference.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUrl = task.result!!.toString()
                val message = Message(null, userName, userPhotoUrl, downloadUrl)

                firebaseDatabaseReference!!.child(MESSAGE_CHILD).child(key!!).setValue(message)
            } else {
                Log.e(TAG, "Image upload task was not successful ${task.exception}")
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
            messageImageView = itemView.findViewById(R.id.message_image_view)
            nameTextView = itemView.findViewById(R.id.name_text_view)
            userImage = itemView.findViewById(R.id.messenger_image_view)
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
                } else {
                    Glide.with(messageImageView.context)
                        .load(Uri.parse(message.imageUrl))
                        .into(messageImageView)
                }
            }

            nameTextView.text = message.name

            if (message.photoUrl == null) {
                userImage.setImageDrawable(ContextCompat.getDrawable(userImage.context, R.drawable.ic_account_circle_black_36dp))
            } else {
                Glide.with(userImage.context)
                    .load(Uri.parse(message.photoUrl))
                    .into(userImage)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        firebaseAdapter!!.stopListening()
    }

    override fun onResume() {
        super.onResume()
        firebaseAdapter!!.startListening()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return super.onCreateOptionsMenu(menu)
    }
}
