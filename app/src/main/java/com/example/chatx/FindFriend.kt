package com.example.chatx

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatx.R.string
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_find_friend.*
import java.util.*

class FindFriend : AppCompatActivity() {
    //проверка, правильный ли email
    private fun EditText.isEmailValid(): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(this.text.toString()).matches()
    }
    private val correctUserEmail = FirebaseAuth.getInstance().currentUser!!.email.toString().replace('.','_')
    private val myBase = FirebaseDatabase.getInstance().reference
    private val returnList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_friend)

        val setOfFriends: SortedSet<String> = sortedSetOf()
        val resultList:ArrayAdapter<String> = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,returnList)
        //ищем друга в базе и добавляем, если такой есть
        search.setOnClickListener {
            if(!isOnline(this)) {
                Toast.makeText(this, string.InternetError, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(enterFriend.isEmailValid())
            {
                val emailFriend = enterFriend.text.toString().replace('.','_')
                myBase.child("users").child(emailFriend)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {
                            enterFriend.setTextColor(Color.RED)
                        }

                        override fun onDataChange(p0: DataSnapshot) {
                            if(p0.hasChildren() && !setOfFriends.contains(p0.getValue(Friend::class.java)!!.userEmail))
                            {//если правильный
                                val UserFriend = p0.getValue(Friend::class.java)
                                if(UserFriend == null) {//если ничего нет
                                    Toast.makeText(applicationContext, R.string.Error, Toast.LENGTH_SHORT).show()
                                    return
                                }
                                //если всё правильно
                                val str = UserFriend.userEmail + " (" + UserFriend.userName + ")"
                                returnList.add(str)
                                setOfFriends.add(str)
                                enterFriend.setTextColor(resources.getColor(R.color.colorGreenGood))
                                searchResult.adapter = resultList
                            }
                            else//если такого пользователя нет
                                enterFriend.setTextColor(Color.RED)
                        }
                    })
            }
            else    enterFriend.setTextColor(Color.RED)//если неправильный email
        }

        enterFriend.addTextChangedListener(object : TextWatcher
        {
            override fun afterTextChanged(p0: Editable?) {
                enterFriend.setTextColor(Color.BLACK)//если изменился текст
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

    }
    //загружаем список друзей в БД в профиль пользователя
    override fun onBackPressed() {
        super.onBackPressed()

        for (str in returnList)
        {
            val emailFriend = str.substringBefore(" ")
            val nameFriend = str.subSequence(emailFriend.length+2,str.length-1).toString()
             myBase.child("users").child(correctUserEmail)
             .child("listOfFriends").child(emailFriend.replace('.','_')).setValue(Friend(emailFriend,nameFriend))
        }
    }
}