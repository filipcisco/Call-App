package com.kapp.call_app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kapp.call_app.external_call.ExternalCallActivity
import com.kapp.call_app.telecom.CallManager


class GSMNotificationBroadcastReceiver : BroadcastReceiver() {

    private val TAG = GSMNotificationBroadcastReceiver::class.java.name


    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG,"onReceive")

        val notificationId = intent.getIntExtra(GSMCallNotificationManager.INTENT_NOTIF_ID, 0)
        Log.i(TAG,"[Notification Broadcast Receiver] Got notification broadcast for ID [$notificationId]")
        handleCallIntent(context,intent)
    }

    private fun handleCallIntent(context: Context, intent: Intent) {

        val call = CallManager.getPrimaryCall()
        if (call == null) {
            Log.i(TAG,"[Notification Broadcast Receiver] Couldn't find call from InCallService")
            return
        }

        if (intent.action == GSMCallNotificationManager.INTENT_ANSWER_CALL_NOTIF_ACTION) {
            context.startActivity(ExternalCallActivity.getStartIntent(context))
            CallManager.accept()
        } else {
            CallManager.reject()
        }
    }
}
