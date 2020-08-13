package com.example.chatx

import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.library.bubbleview.BubbleTextView
import com.firebase.ui.database.FirebaseListAdapter as FirebaseListAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions
import kotlinx.android.synthetic.main.activity_chats.*
import kotlinx.android.synthetic.main.list_item.*

class ChatsActivity : AppCompatActivity() {
    private var useremail = ""
    private var friendemail = ""
    private var pathToChat = ""
    private lateinit var emojIconActions: EmojIconActions
    private lateinit var chats_activity: RelativeLayout
    val myBase = FirebaseDatabase.getInstance().reference
    private lateinit var adapter: FirebaseListAdapter<Chats>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)
        supportActionBar!!.hide()
        if(intent.getStringExtra("email user") == null || intent.getStringExtra("email friend") == null) {
            Toast.makeText(this, "Ошибка", Toast.LENGTH_LONG).show()
            finish()
        }
        useremail= intent.getStringExtra("email user")!!
        friendemail = intent.getStringExtra("email friend")!!
        chat_with.text = "You are chatting with ${intent.getStringExtra("name friend")}"
        pathToChat = if(useremail < friendemail) useremail+friendemail else friendemail+useremail
        chats_activity = findViewById(R.id.chat_activity)
        emojIconActions = EmojIconActions(applicationContext,chats_activity,textField,emoji_button)
        emojIconActions.ShowEmojIcon()
        submit_button.setOnClickListener {
            myBase.child("chats").child(pathToChat).push()
                .setValue(Chats(FirebaseAuth.getInstance().currentUser!!.displayName!!,
                                textField.text.toString()))
            textField.setText("")
        }
        myBase.child("chats").child(pathToChat).addListenerForSingleValueEvent(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
                displayAllMessages()
            }
        })
    }
    private fun displayAllMessages()
    {
        val listOfMessages:ListView = findViewById(R.id.list_of_messages)
        adapter = object : FirebaseListAdapter<Chats>(this,Chats::class.java,R.layout.list_item,myBase.child("chats").child(pathToChat))
        {
            override fun populateView(v: View, model: Chats, position: Int) {
                if(model.nameUser != "Nobody") {
                    val mess_name: TextView = v.findViewById(R.id.message_name)
                    val mess_time: TextView = v.findViewById(R.id.message_time)
                    val mess_text: BubbleTextView = v.findViewById(R.id.message_text)
                    mess_name.text = model.nameUser
                    mess_text.text = model.textMessage
                    mess_time.text = DateFormat.format("dd-MM-yyyy HH:mm", model.messageTime)
                }
            }
        }
        listOfMessages.adapter = adapter
    }

}