package com.example.chatx

open class User {
    var userEmail: String = ""
    var userName: String = ""
    var listOfFriends: MutableList<Friend> = mutableListOf()
    constructor(){}
    constructor(userEmail: String, listOfFriends: MutableList<Friend> , userName:String)
    {
        this.userName = userName
        this.userEmail = userEmail
        this.listOfFriends = listOfFriends
    }
}
