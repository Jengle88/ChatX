package com.example.chatx

class Friend {
    var userEmail: String = ""
    var userName: String = ""
    constructor(){}
    constructor(userEmail: String, userName:String)
    {
        this.userName = userName
        this.userEmail = userEmail
    }
    constructor(addText: String)
    {
        this.userName = addText
        this.userEmail = addText
    }
}