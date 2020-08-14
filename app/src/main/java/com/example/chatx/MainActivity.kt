package com.example.chatx

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.example.chatx.R.*
import com.example.chatx.R.string
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.database.FirebaseListAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private val SIGN_IN_CODE = 1
    private val SIGN_OUT_CODE = -1
    private val SEARCH_FRIENDS = 2
    private var curUser: FirebaseUser? = null
    private var myBase:DatabaseReference? = null
    private var correctUserEmail = ""
    private var APP_VERSION = ""
    private var checkedVersion = false
    private var TRUE_APP_VERSION = "1.0.0"
    private var actualVers = ""
    private val listFriendsEmail = mutableListOf<String>()
    private val listFriendsName = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_main)

        //Проверка авторизации
        if (FirebaseAuth.getInstance().currentUser == null) {
           startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), SIGN_IN_CODE)
        }
        else {
            Toast.makeText(this, string.authIN, Toast.LENGTH_SHORT).show()
        }

        myBase = FirebaseDatabase.getInstance().reference
        //создаём нового пользователя, если его раньше не было
        if(FirebaseAuth.getInstance().currentUser != null) {
            curUser = FirebaseAuth.getInstance().currentUser!!
            correctUserEmail = curUser?.email.toString().replace('.','_')
            createProfile()
        }

        //проверяем наличие обновления при запуске
        if(!checkedVersion) {
            APP_VERSION = TRUE_APP_VERSION
            updateApp()
        }
        checkedVersion = true

        //Выводим список друзей на Activity Main
        displayAllFriends()

        //действия, связанные с выбором чата или добавлением нового друга
        listOfUserFriends.setOnItemClickListener { parent, view, pos, id ->
            when (pos)
            {
                0 ->
                {
                    val intent = Intent(this,FindFriend::class.java)
                    listFriendsName.clear()
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

    //вывод списка друзей
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

    //проверка обновлений
    private fun updateApp()
    {
        myBase!!.child("actual_vers").addListenerForSingleValueEvent(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError) {}
            override fun onDataChange(p0: DataSnapshot)
            {
                actualVers = p0.getValue(String::class.java)!!
                //получаем версию приложения из папки
                if(this@MainActivity.fileList().isEmpty())
                    this@MainActivity.openFileOutput("apkVersion", Context.MODE_PRIVATE).write(TRUE_APP_VERSION.toByteArray())
                this@MainActivity.openFileInput("apkVersion").use {
                    APP_VERSION = it.readBytes().toString(Charsets.UTF_8)
                }
                if(APP_VERSION < TRUE_APP_VERSION)
                {
                    this@MainActivity.openFileOutput("apkVersion", Context.MODE_PRIVATE)
                        .write(TRUE_APP_VERSION.toByteArray())
                    APP_VERSION = TRUE_APP_VERSION
                }
                if(actualVers != APP_VERSION)
                {
                    val update_mess = AlertDialog.Builder(this@MainActivity)
                    update_mess.setTitle(string.update)
                    update_mess.setMessage(string.update_mess)
                    update_mess.setPositiveButton("Открыть браузер"){dialog, which ->
                        myBase!!.child("update_site").addListenerForSingleValueEvent(object : ValueEventListener
                        {
                            override fun onCancelled(p0: DatabaseError) {}
                            override fun onDataChange(p0: DataSnapshot) {
                                val address = p0.getValue(String::class.java)!!
                                val uri = Uri.parse(address)
                                val intent = Intent(Intent.ACTION_VIEW,uri)
                                startActivity(intent)
                            }
                        })
                    }
                    update_mess.setNegativeButton("Позже"){dialog,which ->}
                    update_mess.setNeutralButton("Больше не показывать"){dialog,which ->
                        this@MainActivity.openFileOutput("apkVersion", Context.MODE_PRIVATE).write(actualVers.toByteArray())
                    }
                    val dialog: AlertDialog = update_mess.create()
                    dialog.show()
                }
                else
                    Toast.makeText(this@MainActivity,string.lastVersion,Toast.LENGTH_SHORT).show()
            }
        })
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode)
        {
            SIGN_IN_CODE ->//Если пользователь входил
            {
                if (resultCode == Activity.RESULT_OK)
                {
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
            SEARCH_FRIENDS ->//заново выводим список друзей
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

        when(item.itemId)
        {
            id.sign_out ->//Обработка выхода из профиля
            {
                AuthUI.getInstance().signOut(this)
                startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), SIGN_OUT_CODE)
            }
            id.updateApp ->//обработка обновления
            {
                this@MainActivity.openFileOutput("apkVersion", Context.MODE_PRIVATE).write(TRUE_APP_VERSION.toByteArray())
                updateApp()
            }

        }
        return super.onOptionsItemSelected(item)
    }
}