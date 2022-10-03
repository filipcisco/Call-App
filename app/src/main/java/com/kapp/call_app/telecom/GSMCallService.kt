package com.kapp.call_app.telecom

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.kapp.call_app.external_call.ExternalCallActivity
import com.kapp.call_app.notifications.GSMCallNotificationManager

class GSMCallService : InCallService() {
    private val callNotificationManager by lazy { GSMCallNotificationManager(this) }
    private var isRinging = false
    private val Context.powerManager: PowerManager get() = getSystemService(Context.POWER_SERVICE) as PowerManager

    private val outgoingCallStates = arrayOf(
        Call.STATE_CONNECTING,
        Call.STATE_DIALING,
        Call.STATE_SELECT_PHONE_ACCOUNT
    )


    private val callListener = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            Log.i(GSMCallService::class.java.name, "Call state is : $state")
            if (state == Call.STATE_RINGING){
                isRinging = true
            }
            if(state == Call.STATE_ACTIVE){
                isRinging = false
            }
            if (state != Call.STATE_DISCONNECTED) {
                callNotificationManager.setupNotification()
            }
            if(state == Call.STATE_DISCONNECTED && isRinging) {
                callNotificationManager.displayMissedCallNotification(call)
            }
        }
    }

    @SuppressLint("NewApi")
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.onCallAdded(call)
        CallManager.inCallService = this
        call.registerCallback(callListener)

        val isScreenLocked = (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceLocked
        if (!powerManager.isInteractive || call.details.callDirection == Call.Details.DIRECTION_OUTGOING || outgoingCallStates.contains(call.state) || isScreenLocked) {
            Log.i(GSMCallService::class.java.name,"New Call added. Starting External Call Activity.")
            val i = Intent(this, ExternalCallActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
            if(call.details.state == Call.STATE_RINGING){
                Log.i(GSMCallService::class.java.name,"isRinging set to true.")
                isRinging = true
            }
            if (call.details.state == Call.STATE_RINGING || call.state == Call.STATE_RINGING) {
                Log.i(GSMCallService::class.java.name,"GSM Screen Locked. Starting notification.")
                callNotificationManager.setupNotification()
            }
        } else {
            Log.i(GSMCallService::class.java.name, "Incoming call received. State is: ${call.details.state}")
            if(call.details.state == Call.STATE_RINGING){
                Log.i(GSMCallService::class.java.name,"isRinging set to true.")
                isRinging = true
            }
            callNotificationManager.setupNotification()
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callListener)
        val wasPrimaryCall = call == CallManager.getPrimaryCall()
        CallManager.onCallRemoved(call)
        if (CallManager.getPhoneState() == NoCall) {
            CallManager.inCallService = null

            if(isRinging){
                callNotificationManager.displayMissedCallNotification(call)
            }else {
                callNotificationManager.cancelNotification()
            }
        } else {
            callNotificationManager.setupNotification()
            if (wasPrimaryCall) {
                startActivity(ExternalCallActivity.getStartIntent(this))
            }
        }
    }

    /**
     * Method to check if call in parameter matches last missed call in Calls Log
     **/
    /*
    private fun checkIfCallWasMissed(call: Call): Boolean {
        // TODO("Check if call was missed to display a missed call notification.")
        val lastMissedCall = CallsLogHelper(this).fetchWithType(1, type = CallLog.Calls.MISSED_TYPE)?.get(0)
        Log.i(GSMCallService::class.java.name,"Last missed call Date is: ${lastMissedCall?.date}.")
        Log.i(GSMCallService::class.java.name,"Removed Call Date is: ${call.details.creationTimeMillis}.")
        return call.details.creationTimeMillis == lastMissedCall?.date?.toLong()
    }
     */

    override fun onDestroy() {
        super.onDestroy()
        callNotificationManager.cancelNotification()
    }

}
