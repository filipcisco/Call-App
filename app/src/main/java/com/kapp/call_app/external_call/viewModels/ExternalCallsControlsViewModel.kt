package com.kapp.call_app.external_call.viewModels


import android.telecom.CallAudioState
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kapp.call_app.telecom.CallManager

class ExternalCallsControlsViewModel : ViewModel() {

    private val TAG: String = ExternalCallsControlsViewModel::class.java.name

    val numpadVisible = MutableLiveData(false)
    val isMicrophoneMuted = MutableLiveData(false)
    val isSpeakerOn = MutableLiveData(false)
    val isExtraButtonsEnabled = MutableLiveData(true)
    val dtmfHistory = MutableLiveData<String>()
    val isBluetoothEnabled = MutableLiveData(false)
    val isCallOnHold = MutableLiveData(false)

    fun handleDtmfClick(key: Char) {
        dtmfHistory.value = "${dtmfHistory.value.orEmpty()}$key"
    }


    fun toggleMuteMicrophone() {
        Log.i(TAG, "ToggleMuteMicrophone triggered !")
        Log.i(TAG, "isMicrophoneMuted value is: ${isMicrophoneMuted.value}")
        isMicrophoneMuted.value = !isMicrophoneMuted.value!!
        Log.i(TAG, "isMicrophoneMuted value is now: ${isMicrophoneMuted.value}")
    }

    fun toggleSpeaker() {
        isSpeakerOn.value = !isSpeakerOn.value!!
    }

    fun showNumpad() {
        isExtraButtonsEnabled.value = false
        numpadVisible.value = true
    }

    fun hideNumpad() {
        numpadVisible.value = false
        isExtraButtonsEnabled.value = true
    }

    fun goToDialerForNewCall() {
        Log.i(TAG, "Go to Dialer fo a new call")
    }

    fun routeToBluetooth() {
        isBluetoothEnabled.value = !isBluetoothEnabled.value!!
    }

    fun toggleHold() {
        Log.i(TAG, "Toggle current call on hold")
        isCallOnHold.value = CallManager.toggleHold()
    }
}