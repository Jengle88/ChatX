package com.example.chatx

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.example.chatx.R.*
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.database.FirebaseListAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.list_friends.view.*


class MainActivity : AppCompatActivity() {
    private val SIGN_IN_CODE = 1
    private val SIGN_OUT_CODE = -1
    private val SEARCH_FRIENDS = 2
    private var curUser: FirebaseUser? = null
    private var myBase:DatabaseReference? = null
    private var correctUserEmail = ""
    private var rest = false
    private val listFriendsEmail = mutableListOf<String>()
    private val listFriendsName = mutableListOf<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_main)
        //Проверка авторизации
        if (FirebaseAuth.getInstance().currentUser == null) {
            rest = true
           startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), SIGN_IN_CODE)
        }
        else {
            Toast.makeText(this, string.authIN, Toast.LENGTH_SHORT).show()
        }

        myBase = FirebaseDatabase.getInstance().reference
        if(FirebaseAuth.getInstance().currentUser != null) {
            curUser = FirebaseAuth.getInstance().currentUser!!
            correctUserEmail = curUser?.email.toString().replace('.','_')
            //Добавляем пользователя в базу, если его ещё там нет
            createProfile()
        }
        //Выводим список друзей на Activity Main
        displayAllFriends()
        listOfUserFriends.setOnItemClickListener { parent, view, pos, id ->
            when (pos)
            {
                0 ->
                {
                    val intent = Intent(this,FindFriend::class.java)
                    listFriendsEmail.clear()
                    startActivityForResult(intent,SEARCH_FRIENDS)
                }
                else ->
                {
                    val intent = Intent(this,ChatsActivity::class.java)
                    intent.putExtra("email friend", listFriendsEmail[pos].replace('.','_'))
                    intent.putExtra("email user", curUser?.email!!.replace('.','_'))
                    intent.putExtra("name friend", listFriendsName[pos])
                    startActivityForResult(intent, SEARCH_FRIENDS)
                }
            }
        }


    }
    private fun displayAllFriends()
    {
        val listOfFriends: ListView = findViewById(R.id.listOfUserFriends)
        val adapter = object : FirebaseListAdapter<Friend>(this,Friend::class.java,R.layout.list_friends,myBase!!
            .child("users").child(correctUserEmail).child("listOfFriends"))
        {
            override fun populateView(v: View, model: Friend, position: Int) {
                val friend_name: TextView = v.findViewById(R.id.nameFriend)
                friend_name.text = model.userName
                listFriendsEmail.add(model.userEmail)
                listFriendsName.add(model.userName)
            }

        }
        listOfUserFriends.adapter = adapter
    }



    //Создаём нового пользователя в базе
    private fun createProfile()
    {
        myBase!!.child("users").child(correctUserEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(p0: DataSnapshot) {
                    if (!p0.hasChildren())
                        myBase!!.child("users").child(correctUserEmail)
                            .setValue(User(curUser?.email.toString(), mutableListOf(Friend("+ Add Friend")), curUser?.displayName.toString()))
                }
                override fun onCancelled(p0: DatabaseError) {}
            })
    }



    //Обработка регистрации
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode)
        {
            SIGN_IN_CODE ->//Если пользователь входил
            {
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, string.authIN, Toast.LENGTH_SHORT).show()
                    
                    displayAllFriends()
                }
                else
                {
                    Toast.makeText(this, string.authOUT, Toast.LENGTH_SHORT).show()
                    startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(),SIGN_IN_CODE)
                }
            }
            SIGN_OUT_CODE ->//Если пользователь выходил
            {
                Toast.makeText(this, string.authINagain, Toast.LENGTH_SHORT).show()
                recreate()
            }
            SEARCH_FRIENDS ->
            {
                displayAllFriends()
            }

        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //Обработка выхода из профиля
        if(item.itemId == id.sign_out)
        {
            AuthUI.getInstance().signOut(this)
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), SIGN_OUT_CODE)
           // recreate()
        }
        return super.onOptionsItemSelected(item)
    }
}