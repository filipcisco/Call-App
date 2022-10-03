package com.kapp.call_app.external_call

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.util.Log
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import com.kapp.call_app.R
import com.kapp.call_app.contacts.getCallContact
import com.kapp.call_app.databinding.ActivityExternalCallBinding
import com.kapp.call_app.external_call.viewModels.ExternalCallsControlsViewModel
import com.kapp.call_app.external_call.viewModels.ExternalCallsViewModel
import com.kapp.call_app.telecom.CallManager
import com.kapp.call_app.telecom.NoCall

class ExternalCallActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    companion object {
        //private const val ANIMATION_DURATION = 250L
        fun getStartIntent(context: Context): Intent {
            val openAppIntent = Intent(context, ExternalCallActivity::class.java)
            openAppIntent.flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            return openAppIntent
        }
    }

    private val TAG: String = ExternalCallActivity::class.java.name

    private lateinit var binding: ActivityExternalCallBinding
    private val callsViewModel: ExternalCallsViewModel by viewModels()
    private val externalControlsViewModel: ExternalCallsControlsViewModel by viewModels()
    private val audioManager: AudioManager get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var proximityWakeLock: PowerManager.WakeLock? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "Phone State is NoCall : ${CallManager.getPhoneState() == NoCall}")
        if (CallManager.getPhoneState() == NoCall) {
            finish()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if(CallManager.getPrimaryCall()?.details?.callDirection == Call.Details.DIRECTION_INCOMING){
                    showWhenLockedAndTurnScreenOn()
                }
        } else {
            val OUTGOING_CALL_STATES = arrayOf(
                Call.STATE_CONNECTING,
                Call.STATE_DIALING,
                Call.STATE_SELECT_PHONE_ACCOUNT
            )
            if(!OUTGOING_CALL_STATES.contains(CallManager.getPrimaryCall()?.state)){
                showWhenLockedAndTurnScreenOn()
            }
        }

        binding = DataBindingUtil.setContentView(this,R.layout.activity_external_call)

        audioManager.mode = AudioManager.MODE_IN_CALL

        binding.apply {
            lifecycleOwner = this@ExternalCallActivity
            callViewModel = callsViewModel
            controlsViewModel = externalControlsViewModel
        }

        callsViewModel.permissionRequest.observe(this) {
            Log.i(TAG,"Permission request value changed.")
        }

        callsViewModel.isCallRinging.observe(this) {
            if (it) {
                Log.i(TAG, "Call is ringing navigate to incoming call fragment")
                findNavController(R.id.nav_host_fragment_content_external_call).navigate(R.id.action_global_externalIncomingCallFragment)
            }
        }
        callsViewModel.isCallStarted.observe(this) {
            if(it) {
                Log.i(TAG, "Call has started navigate to outgoing call fragment")
                audioManager.mode = AudioManager.MODE_NORMAL
                //enableProximitySensor()
                findNavController(R.id.nav_host_fragment_content_external_call).navigate(R.id.action_global_externalSingleCallFragment)
            }
        }

        callsViewModel.isCallEnded.observe(this) {
            if (it){
                Log.i(TAG, "Call disconnected navigate to main activity")
                CallManager.reject()
                disableProximitySensor()
                finish()
            }
        }
        callsViewModel.initOutgoingCallUI.observe(this) {
            if(it) {
                Log.i(TAG, "Call is connecting or dialing navigate to outgoing call fragment")
                findNavController(R.id.nav_host_fragment_content_external_call).navigate(R.id.action_global_externalOutgoingCallFragment)
            }
        }
        callsViewModel.showPhoneAccountPicker.observe(this) {
            if(it) {
                Log.i(TAG, "Show Phone Account Picker Dialog")
            }
        }

        externalControlsViewModel.isBluetoothEnabled.observe(this){
            toggleBluetooth(it)
        }

        externalControlsViewModel.isMicrophoneMuted.observe(this) {
            toggleMicrophone(it)
        }

        externalControlsViewModel.isSpeakerOn.observe(this) {
            toggleSpeaker(it)
        }

        callsViewModel.currentCall.observe(this) {
            if (it != null) {
                getCallContact(this, it) {
                    callContact -> callsViewModel.callContact.postValue(callContact)
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()

        if (CallManager.getPhoneState() == NoCall) {
            finish()
            return
        }

    }

    private fun toggleBluetooth(it: Boolean) {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) -> {
                if(it) {
                    Log.i(TAG,"Putting Bluetooth on: $it")
                    audioManager.isBluetoothScoOn = it
                    audioManager.startBluetoothSco()
                    CallManager.inCallService?.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH)
                }
                else {
                    audioManager.isBluetoothScoOn = !it
                    audioManager.startBluetoothSco()
                    CallManager.inCallService?.setAudioRoute(CallAudioState.ROUTE_EARPIECE)
                }
            }
            else -> {
                Log.i(TAG,"Bluetooth Connect Permission is not granted. Requesting for permission!")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.BLUETOOTH_CONNECT),6)
                }else{
                    ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.BLUETOOTH),6)
                }
            }
        }
    }

    private fun toggleMicrophone(it: Boolean) {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.WAKE_LOCK
            ) -> {
                Log.i(TAG, "IsMicrophoneMuted value is: $it")
                audioManager.isMicrophoneMute = it
                CallManager.inCallService?.setMuted(it)
            }
            else -> {
                ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.WAKE_LOCK),5)
            }
        }
    }

    private fun toggleSpeaker(it: Boolean) {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.WAKE_LOCK
            ) -> {
                if(it) {
                    Log.i(TAG,"Putting Speaker on: $it")
                    CallManager.inCallService?.setAudioRoute(CallAudioState.ROUTE_SPEAKER)
                    audioManager.isSpeakerphoneOn = it
                    disableProximitySensor()
                } else {
                    CallManager.inCallService?.setAudioRoute(CallAudioState.ROUTE_EARPIECE)
                    audioManager.isSpeakerphoneOn = it
                    enableProximitySensor()
                }
            }
            else -> {
                ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.WAKE_LOCK),4)
            }
        }
    }

    private fun enableProximitySensor() {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            proximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "com.kapp.call_app.pro:wake_lock")
            proximityWakeLock!!.acquire(60 * 60 * 1000L)
    }

    private fun disableProximitySensor() {
        if (proximityWakeLock?.isHeld == true) {
            proximityWakeLock!!.release()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(TAG, "onRequestPermissionsResult triggered.");
        Log.i(TAG, "Request code is: $requestCode.");
        when(requestCode){
            4 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission has been granted, preview can be displayed
                    Log.i(TAG, "Wake Lock permission has now been granted. Showing preview.")
                    Log.i(TAG, "isSpeakerOn value is : ${externalControlsViewModel.isSpeakerOn.value == true}.")
                    Snackbar.make(this, binding.root,"Permission granted.",
                        Snackbar.LENGTH_LONG).show()
                    toggleSpeaker(externalControlsViewModel.isSpeakerOn.value == true)
                } else {
                    Log.i(TAG, "Wake Lock permission was NOT granted.");
                    Snackbar.make(this, binding.root,"Permission not granted.",
                        Snackbar.LENGTH_LONG).show()

                }
            }
            5 -> {
                Log.i(TAG, "Wake Lock permission from microphone onRequestPermissionsResult triggered.");
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission has been granted, preview can be displayed
                    Log.i(TAG, "Wake Lock permission has now been granted. Showing preview.")
                    Log.i(TAG, "isSpeakerOn value is : ${externalControlsViewModel.isSpeakerOn.value == true}.")
                    Snackbar.make(this, binding.root,"Permission granted.",
                        Snackbar.LENGTH_LONG).show()
                    toggleMicrophone(externalControlsViewModel.isMicrophoneMuted.value == true)
                } else {
                    Log.i(TAG, "Wake Lock permission was NOT granted.");
                    Snackbar.make(this, binding.root,"Permission not granted.",
                        Snackbar.LENGTH_LONG).show()

                }
            }
            6 -> {
                Log.i(TAG, "Bluetooth Connect permission result is: ${grantResults[0] == PackageManager.PERMISSION_GRANTED}.")
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission has been granted, preview can be displayed
                    Log.i(TAG, "Bluetooth Connect permission has now been granted. Showing preview.")
                    Log.i(TAG, "isBluetoothEnabled value is : ${externalControlsViewModel.isBluetoothEnabled.value == true}.")
                    Snackbar.make(this, binding.root,"Permission granted.",
                        Snackbar.LENGTH_LONG).show()
                    toggleBluetooth(externalControlsViewModel.isBluetoothEnabled.value == true)
                } else {
                    Log.i(TAG, "Bluetooth Connect permission was NOT granted.");
                    Snackbar.make(this, binding.root,"Permission not granted.",
                        Snackbar.LENGTH_LONG).show()

                }
            }
        }
    }

    private fun showWhenLockedAndTurnScreenOn() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    }

}