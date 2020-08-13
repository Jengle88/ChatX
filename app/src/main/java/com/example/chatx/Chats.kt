package com.example.chatx

import java.util.*


open class Chats {
    var nameUser = ""
    var textMessage:String = ""
    var messageTime: Long = 0
    constructor() {}
    constructor(nameUser:String, textMessage:String)
    {
        this.nameUser = nameUser
        this.textMessage = textMessage
        this.messageTime = Date().time
    }
    constructor(nobody: String)
    {
        this.nameUser = nobody
    }


}