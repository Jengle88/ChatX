package com.example.chatx

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.chatx.R.*
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.database.FirebaseListAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.list_friends.view.*

//проверка, есть ли доступ в интернет
fun isOnline(context: Context) : Boolean
{
    val cm: ConnectivityManager? = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    cm?.let {
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        capabilities?.let {
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }

        }
    }
    return false
}

//проверка, свёрнуто ли приложение
fun isForeground(): Boolean
{
    return LifeCycle.statusCnt > 0
}

var chatWith = ""
var correctUserEmail = ""

class MainActivity : AppCompatActivity() {
    private val SIGN_IN_CODE = 1
    private val SIGN_OUT_CODE = -1
    private val SEARCH_FRIENDS = 2
    private val STOP_CHATING = 3
    private val IMPORTANT_UPDATE = 4
    private var curUser: FirebaseUser? = null
    private var myBase:DatabaseReference? = null
    private var APP_VERSION = ""
    private var checkedVersion = false
    private var TRUE_APP_VERSION = "1.1.1"
    private var CHANNEL_ID: String = ""
    private var actualVers = ""
    private var cntPass = 0
    private var warning_upd = false
    private val listFriendsEmail = mutableListOf<String>()
    private val listFriendsName = mutableListOf<String>()
    private var tablEmailView: MutableMap<String,View> = mutableMapOf()
    private lateinit var notifyBuilder:NotificationCompat.Builder

    private lateinit var notifyManager: NotificationManagerCompat


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_main)

        //регистрируем этапы жизненного цикла приложения
        application.registerActivityLifecycleCallbacks(LifeCycle())

        //создаём канал уведомлений для API >= 26
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
                val channelId = "Channel_id"
                val channelName = "Application_name"
                val channelDescription = "Application_name Alert"
                val channelImportance = NotificationManager.IMPORTANCE_HIGH
                val channelEnableVibrate = true
                val notificationChannel = NotificationChannel(channelId, channelName, channelImportance)
                notificationChannel.description = channelDescription
                notificationChannel.enableVibration(channelEnableVibrate)
                val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(notificationChannel)
                CHANNEL_ID =  channelId
            }
            else
                CHANNEL_ID =  "null"
        notifyBuilder = NotificationCompat.Builder(this,CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_message_24)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if(isOnline(this)) {
            //Проверка авторизации
            if (FirebaseAuth.getInstance().currentUser == null) {
                startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(),SIGN_IN_CODE)
            } else {
                Toast.makeText(this, string.authIN, Toast.LENGTH_SHORT).show()
            }
            myBase = FirebaseDatabase.getInstance().reference
            //создаём нового пользователя, если его раньше не было
            if (FirebaseAuth.getInstance().currentUser != null) {
                curUser = FirebaseAuth.getInstance().currentUser!!
                correctUserEmail = curUser?.email.toString().replace('.', '_')
                createProfile()
            }

            //проверяем наличие обновления при запуске
            if (!checkedVersion) {
                APP_VERSION = TRUE_APP_VERSION
                updateApp()
            }
            checkedVersion = true

            //Выводим список друзей на Activity Main
            displayAllFriends()
            cntPass++
            notifyManager = NotificationManagerCompat.from(this)

            //проверяем новые сообщения
            checkNewMessages()
        }
        else
        {
            Toast.makeText(this,string.InternetError,Toast.LENGTH_SHORT).show()
        }
        //действия, связанные с выбором чата или добавлением нового друга
        listOfUserFriends.setOnItemClickListener { parent, view, pos, id ->
            when (pos)
            {
                0 ->
                {
                    val intent = Intent(this,FindFriend::class.java)
                    listFriendsEmail.clear()
                    listFriendsName.clear()
                    startActivityForResult(intent,SEARCH_FRIENDS)
                }
                else ->
                {
                    val intent = Intent(this,ChatsActivity::class.java)
                    intent.putExtra("email friend", listFriendsEmail[pos].replace('.','_'))
                    intent.putExtra("email user", curUser?.email!!.replace('.','_'))
                    intent.putExtra("name friend", listFriendsName[pos])
                    tablEmailView[listFriendsEmail[pos].replace('.','_')]!!.findViewById<ImageView>(R.id.new_mess).visibility = INVISIBLE
                    startActivityForResult(intent,STOP_CHATING)
                }
            }
        }
    }

    //вывод списка друзей
    private fun displayAllFriends()
    {
        val adapter = object : FirebaseListAdapter<Friend>(this,Friend::class.java,R.layout.list_friends,myBase!!
            .child("users").child(correctUserEmail).child("listOfFriends"))
        {
            override fun populateView(v: View, model: Friend, position: Int)
            {
                val friend_name: TextView = v.findViewById(R.id.nameFriend)
                tablEmailView[model.userEmail.replace('.', '_')] = v
                listFriendsEmail.add(model.userEmail)
                listFriendsName.add(model.userName)
                if(cntPass <= 1)
                {
                    myBase!!.child("new_messages").child(correctUserEmail).addListenerForSingleValueEvent(object : ValueEventListener
                    {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.hasChild(model.userEmail.replace('.', '_')))
                                    v.new_mess.visibility = VISIBLE
                            }

                            override fun onCancelled(error: DatabaseError) {}
                    })
                }
                friend_name.text = model.userName
            }
        }
        listOfUserFriends.adapter = adapter
    }
    //проверяем наличие новых сообщений
    private fun checkNewMessages()
    {
        myBase!!.child("new_messages").child(correctUserEmail).addChildEventListener(object : ChildEventListener
        {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if(tablEmailView.contains(snapshot.key!!))
                {
                    if (!isForeground() || isForeground() && chatWith != snapshot.key!! && chatWith != "")
                    {
                        val intent = Intent(this@MainActivity, ChatsActivity::class.java)
                        intent.putExtra("email friend", snapshot.key!!.replace('.', '_'))
                        intent.putExtra("email user", curUser?.email!!.replace('.', '_'))
                        intent.putExtra("name friend",tablEmailView[snapshot.key!!]!!.findViewById<TextView>(R.id.nameFriend).text)
                        val penintent = PendingIntent.getActivities(this@MainActivity, STOP_CHATING, arrayOf(intent), PendingIntent.FLAG_CANCEL_CURRENT)
                        notifyBuilder.setContentText("У вас новое сообщение от ${tablEmailView[snapshot.key!!]!!.findViewById<TextView>(R.id.nameFriend).text}")
                            .setContentIntent(penintent)
                            .setAutoCancel(true)
                        notifyManager.notify(snapshot.key!!.hashCode(), notifyBuilder.build())
                    }
                    else if(chatWith == "")
                        tablEmailView[snapshot.key!!]!!.findViewById<ImageView>(R.id.new_mess).visibility = VISIBLE
                }

            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?)
            {
                onChildAdded(snapshot,previousChildName)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }


    //проверка обновлений
    private fun updateApp()
    {
        myBase!!.child("update_warning").addListenerForSingleValueEvent(object : ValueEventListener
        {
            override fun onDataChange(snapshot: DataSnapshot) {
                warning_upd = snapshot.getValue(Boolean::class.java)!!
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        myBase!!.child("actual_vers").addListenerForSingleValueEvent(object : ValueEventListener
        {
            override fun onCancelled(error: DatabaseError) {}
            override fun onDataChange(snapshot: DataSnapshot)
            {
                actualVers = snapshot.getValue(String::class.java)!!
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
                    update_mess.setPositiveButton("Открыть браузер"){dialog, which ->
                        myBase!!.child("update_site").addListenerForSingleValueEvent(object : ValueEventListener
                        {
                            override fun onCancelled(p0: DatabaseError) {}
                            override fun onDataChange(p0: DataSnapshot) {
                                val address = p0.getValue(String::class.java)!!
                                val uri = Uri.parse(address)
                                val intent = Intent(Intent.ACTION_VIEW,uri)
                                startActivityForResult(intent,IMPORTANT_UPDATE)
                            }
                        })
                    }
                    //проверка важности обновления
                    if(warning_upd)
                    {
                        update_mess.setMessage(string.update_important_mess)
                        update_mess.setNegativeButton("Позже"){dialog,which -> finish()}
                        update_mess.setNeutralButton("Пропустить версию"){dialog,which -> finish()}
                        update_mess.setOnCancelListener { finish() }
                    }
                    else {
                        update_mess.setMessage(string.update_local_mess)
                        update_mess.setNegativeButton("Позже") { dialog, which -> }
                        update_mess.setNeutralButton("Пропустить версию") { dialog, which ->
                            this@MainActivity.openFileOutput("apkVersion", Context.MODE_PRIVATE).write(actualVers.toByteArray())
                        }
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
                else //Если пользователь не вошёл
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
            STOP_CHATING -> //завершаем диалог
            {
                myBase!!.child("new_messages").child(correctUserEmail).child(chatWith).removeValue()
                chatWith = ""
            }
            IMPORTANT_UPDATE -> //выходим, если пользователь не обновил приложение
            {
                finish()
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
            id.reloadBase ->//обработка перезагрузки данных
            {
                recreate()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}