package com.example.chatx

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

class LifeCycle : Application.ActivityLifecycleCallbacks {
    companion object {
        var statusCnt: Int = 0
    }
    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        Log.d("TAG","CREAT")
    }

    override fun onActivityStarted(p0: Activity) {
        Log.d("TAG","START")
        statusCnt++
        if(statusCnt == 0)
            Log.d("TAG","Приложение свёрнуто или закрыто")
    }

    override fun onActivityResumed(p0: Activity) {
        Log.d("TAG","RESUME")
    }

    override fun onActivityPaused(p0: Activity) {
        Log.d("TAG","PAUSE")
    }

    override fun onActivityStopped(p0: Activity) {
        Log.d("TAG","STOP")
        statusCnt--
        if(statusCnt == 0 && chatWith != "") {
            FirebaseDatabase.getInstance().reference!!.child("new_messages").child(correctUserEmail).child(chatWith).removeValue()
            chatWith = ""
        }
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        Log.d("TAG","SAVE")
    }

    override fun onActivityDestroyed(p0: Activity) {
        Log.d("TAG","DESTROY")
    }
}