package com.kapp.call_app.external_call.viewModels

import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kapp.call_app.R
import com.kapp.call_app.models.CallContact
import com.kapp.call_app.telecom.CallManager
import com.kapp.call_app.telecom.CallManagerListener
import com.kapp.call_app.telecom.SingleCall
import com.kapp.call_app.telecom.TwoCalls
import java.util.*

class ExternalCallsViewModel : ViewModel(){

    private val TAG: String = ExternalCallsViewModel::class.java.name

    val currentCall = MutableLiveData<Call>()
    val callContact = MutableLiveData<CallContact>()
    val callsList = MutableLiveData<MutableList<Call>>()
    val contact = MutableLiveData<String>()
    val isMuteMicrophoneEnabled = MutableLiveData<Boolean>()
    val permissionRequest = MutableLiveData<String>()
    val isCallRinging = MutableLiveData(false)
    val isCallStarted = MutableLiveData(false)
    val isCallEnded = MutableLiveData(false)
    val initOutgoingCallUI = MutableLiveData(false)
    val showPhoneAccountPicker = MutableLiveData(false)
    private val callDurationHandler = Handler(Looper.getMainLooper())
    private var callDuration = MutableLiveData<String>()
    val inactiveCallsCount = MutableLiveData(0)


    private val callCallback = object : CallManagerListener {
        override fun onStateChanged() {
            updateState()
        }

        override fun onPrimaryCallChanged(call: Call) {
            callDurationHandler.removeCallbacks(updateCallDurationTask)
            //updateCallContactInfo(call)
            updateState()
        }
    }

    override fun onCleared() {
        super.onCleared()
        CallManager.removeListener(callCallback)
    }

    init {


        val primaryCall = CallManager.getPrimaryCall()

        if (
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (primaryCall != null && primaryCall.details.state != Call.STATE_DISCONNECTED)
                ||
                (primaryCall != null && primaryCall.details.state != Call.STATE_DISCONNECTING)
            } else {
                (primaryCall != null && primaryCall.state != Call.STATE_DISCONNECTED)
                ||
                (primaryCall != null && primaryCall.state != Call.STATE_DISCONNECTING)
            }
        ) {

            currentCall.value = primaryCall!!
            Log.i(TAG, "Contact value is: ${Uri.decode(primaryCall.details.handle.toString())}")
            contact.value = Uri.decode(primaryCall.details.handle.toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when (currentCall.value!!.details.state) {
                    Call.STATE_RINGING -> isCallRinging.value = true
                    Call.STATE_ACTIVE -> isCallStarted.value = true
                    Call.STATE_DISCONNECTED -> isCallEnded.value = true
                    Call.STATE_CONNECTING, Call.STATE_DIALING -> initOutgoingCallUI.value = true
                    Call.STATE_SELECT_PHONE_ACCOUNT -> showPhoneAccountPicker.value = true
                }
            }else{
                when (currentCall.value!!.state) {
                    Call.STATE_RINGING -> isCallRinging.value = true
                    Call.STATE_ACTIVE -> isCallStarted.value = true
                    Call.STATE_DISCONNECTED -> isCallEnded.value = true
                    Call.STATE_CONNECTING, Call.STATE_DIALING -> initOutgoingCallUI.value = true
                    Call.STATE_SELECT_PHONE_ACCOUNT -> showPhoneAccountPicker.value = true
                }
            }

        }
        CallManager.addListener(callCallback)
        initCallList()
    }

    private fun updateState() {
        val phoneState = CallManager.getPhoneState()
        if (phoneState is SingleCall) {
            updateCallState(phoneState.call)
            updateCallOnHoldState(null)
        } else if (phoneState is TwoCalls) {
            updateCallState(phoneState.active)
            updateCallOnHoldState(phoneState.onHold)
        }
    }

    private fun updateCallOnHoldState(call: Call?) {
        val hasCallOnHold = call != null
        if (hasCallOnHold) {
            inactiveCallsCount.value = 1
        }
    }

    private fun updateCallState(call: Call) {
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details.state
        } else {
            call.state
        }
        when (state) {
            Call.STATE_RINGING -> isCallRinging.value = true
            Call.STATE_ACTIVE -> isCallStarted.value = true
            Call.STATE_DISCONNECTED -> isCallEnded.value = true
            Call.STATE_CONNECTING, Call.STATE_DIALING -> initOutgoingCallUI.value = true
            Call.STATE_SELECT_PHONE_ACCOUNT -> showPhoneAccountPicker.value = true
        }

        val statusTextId = when (state) {
            Call.STATE_RINGING -> R.string.is_calling
            Call.STATE_CONNECTING, Call.STATE_DIALING -> R.string.dialing
            else -> 0
        }
    }

    private fun initCallList() {
        val calls = arrayListOf<Call>()

        for (call in CallManager.getCalls()) {
            Log.i(TAG,"[Calls] Adding call with caller's display name as: ${Uri.decode(call.details.handle.toString())}  to calls list")
            calls.add(call)
        }

        callsList.value = calls
    }

    private val updateCallDurationTask = object : Runnable {
        override fun run() {
            val connectTime = getCallDuration(CallManager.getPrimaryCall())
            if (isCallEnded.value != true) {
                /*
                * Set call duration on view
                */
                if(connectTime >= 3600){
                    callDuration.value = getFormattedDuration(true,connectTime)
                } else {
                    callDuration.value = getFormattedDuration(false,connectTime)
                }
                callDurationHandler.postDelayed(this, 1000)
            }
        }
    }

    fun getCallsListSize(): Int {
        return callsList.value?.size ?: 0
    }

    fun getCallDuration(call: Call?): Int {
        return if (call != null) {
            val connectTimeMillis = call.details.connectTimeMillis
            if (connectTimeMillis == 0L) {
                return 0
            }
            ((System.currentTimeMillis() - connectTimeMillis) / 1000).toInt()
        } else {
            0
        }
    }

    fun hangUp(){
        Log.i(TAG, "Hang up on call.")
        CallManager.reject()
    }

    fun answer() {
        Log.i(TAG, "Answer call.")
        CallManager.accept()
    }

    fun getFormattedDuration(forceShowHours: Boolean = false, value: Int): String {
        val sb = StringBuilder(8)
        val hours = value / 3600
        val minutes = value % 3600 / 60
        val seconds = value % 60

        if (value >= 3600) {
            sb.append(String.format(Locale.getDefault(), "%02d", hours)).append(":")
        } else if (forceShowHours) {
            sb.append("0:")
        }

        sb.append(String.format(Locale.getDefault(), "%02d", minutes))
        sb.append(":").append(String.format(Locale.getDefault(), "%02d", seconds))
        return sb.toString()
    }

}