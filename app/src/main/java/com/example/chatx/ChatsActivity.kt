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
import com.example.chatx.R.string
import com.firebase.ui.database.FirebaseListAdapter
import com.github.library.bubbleview.BubbleTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions
import kotlinx.android.synthetic.main.activity_chats.*

class ChatsActivity : AppCompatActivity() {
    private var userEmail = ""
    private var friendEmail = ""
    private var friendName = ""
    private var pathToChat = ""
    private lateinit var emojIconActions: EmojIconActions
    private lateinit var chats_activity: RelativeLayout
    private val myBase = FirebaseDatabase.getInstance().reference
    private var adapter: FirebaseListAdapter<Chats>? = null

    private var cntMsg = (0).toBigInteger()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)
        supportActionBar!!.hide()
        userEmail= intent.getStringExtra("email user")!!
        friendEmail = intent.getStringExtra("email friend")!!
        friendName = intent.getStringExtra("name friend")!!
        chatWith = friendEmail
        chat_with.text = "You are chatting with $friendName"

        //создаём ссылку нового чата
        pathToChat = if(userEmail < friendEmail) userEmail+friendEmail else friendEmail+userEmail

        chats_activity = findViewById(R.id.chat_activity)
        emojIconActions = EmojIconActions(applicationContext,chats_activity,textField,emoji_button)
        emojIconActions.ShowEmojIcon()
        myBase!!.child("new_messages").child(userEmail).child(friendEmail).removeValue()
        submit_button.setOnClickListener {
            myBase.child("chats").child(pathToChat).push()
                .setValue(Chats(FirebaseAuth.getInstance().currentUser!!.displayName!!,
                                textField.text.toString()))
            myBase.child("new_messages").child(friendEmail).child(userEmail).setValue(textField.text.toString())
            textField.setText("")
        }
        //вывод сообщений
        if(isOnline(this)) {
            reloadChatButton.visibility = INVISIBLE
            displayAllMessages()
        }
        else
        {
            Toast.makeText(this,string.InternetError,Toast.LENGTH_SHORT).show()
            reloadChatButton.visibility = VISIBLE
        }
        //обновляем сообщения
        reloadChatButton.setOnClickListener {
            if(isOnline(this)) {
                reloadChatButton.visibility = INVISIBLE
                displayAllMessages()
            }
            else
                Toast.makeText(this,string.InternetError,Toast.LENGTH_SHORT).show()
        }
    }


    //вывод всех сообщений между пользователями
    private fun displayAllMessages()
    {
        val listOfMessages:ListView = findViewById(R.id.list_of_messages)
        adapter = null
        adapter = object : FirebaseListAdapter<Chats>(this,Chats::class.java,R.layout.list_item,myBase.child("chats").child(pathToChat))
        {
            override fun populateView(v: View, model: Chats, position: Int) {
                val mess_name_l: TextView = v.findViewById(R.id.message_name_l)
                val mess_time_l: TextView = v.findViewById(R.id.message_time_l)
                val mess_text_l: BubbleTextView = v.findViewById(R.id.message_text_l)
                val mess_name_r: TextView = v.findViewById(R.id.message_name_r)
                val mess_time_r: TextView = v.findViewById(R.id.message_time_r)
                val mess_text_r: BubbleTextView = v.findViewById(R.id.message_text_r)
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