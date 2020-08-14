package com.example.chatx

import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
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

class ChatsActivity : AppCompatActivity() {
    private var userEmail = ""
    private var friendEmail = ""
    private var friendName = ""
    private var pathToChat = ""
    private lateinit var emojIconActions: EmojIconActions
    private lateinit var chats_activity: RelativeLayout
    val myBase = FirebaseDatabase.getInstance().reference
    private lateinit var adapter: FirebaseListAdapter<Chats>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)
        supportActionBar!!.hide()
        //если не удалось загрузить email
        if(intent.getStringExtra("email user") == null || intent.getStringExtra("email friend") == null) {
            Toast.makeText(this, "Ошибка", Toast.LENGTH_LONG).show()
            finish()
        }
        userEmail= intent.getStringExtra("email user")!!
        friendEmail = intent.getStringExtra("email friend")!!
        friendName = intent.getStringExtra("name friend")!!
        chat_with.text = "You are chatting with $friendName"
        //создаём ссылку нового чата
        pathToChat = if(userEmail < friendEmail) userEmail+friendEmail else friendEmail+userEmail
        chats_activity = findViewById(R.id.chat_activity)
        emojIconActions = EmojIconActions(applicationContext,chats_activity,textField,emoji_button)
        emojIconActions.ShowEmojIcon()
        submit_button.setOnClickListener {
            myBase.child("chats").child(pathToChat).push()
                .setValue(Chats(FirebaseAuth.getInstance().currentUser!!.displayName!!,
                                textField.text.toString()))
            textField.setText("")
        }
        //вывод сообщений
        myBase.child("chats").child(pathToChat).addListenerForSingleValueEvent(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
                displayAllMessages()
            }
        })
    }

    //вывод всех сообщений между пользователями
    private fun displayAllMessages()
    {
        val listOfMessages:ListView = findViewById(R.id.list_of_messages)
        adapter = object : FirebaseListAdapter<Chats>(this,Chats::class.java,R.layout.list_item,myBase.child("chats").child(pathToChat))
        {
            override fun populateView(v: View, model: Chats, position: Int) {
                var mess_name_l: TextView = v.findViewById(R.id.message_name_l)
                var mess_time_l: TextView = v.findViewById(R.id.message_time_l)
                var mess_text_l: BubbleTextView = v.findViewById(R.id.message_text_l)
                var mess_name_r: TextView = v.findViewById(R.id.message_name_r)
                var mess_time_r: TextView = v.findViewById(R.id.message_time_r)
                var mess_text_r: BubbleTextView = v.findViewById(R.id.message_text_r)
                //изменяем видимость и добавляем текст
                if(model.nameUser == friendName)
                {
                    mess_name_r.visibility = INVISIBLE
                    mess_time_r.visibility = INVISIBLE
                    mess_text_r.visibility = INVISIBLE
                    mess_name_l.visibility = VISIBLE
                    mess_time_l.visibility = VISIBLE
                    mess_text_l.visibility = VISIBLE
                    mess_name_l.text = model.nameUser
                    mess_text_l.text = model.textMessage
                    mess_time_l.text = DateFormat.format("dd-MM-yyyy HH:mm", model.messageTime)
                }
                else
                {
                    mess_name_r.visibility = VISIBLE
                    mess_time_r.visibility = VISIBLE
                    mess_text_r.visibility = VISIBLE
                    mess_name_l.visibility = INVISIBLE
                    mess_time_l.visibility = INVISIBLE
                    mess_text_l.visibility = INVISIBLE
                    mess_name_r.text = model.nameUser
                    mess_text_r.text = model.textMessage
                    mess_time_r.text = DateFormat.format("dd-MM-yyyy HH:mm", model.messageTime)
                }
            }
        }
        listOfMessages.adapter = adapter
    }
}