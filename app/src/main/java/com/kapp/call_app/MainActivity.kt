package com.kapp.call_app

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.telecom.CallAudioState
import android.telecom.TelecomManager
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.kapp.call_app.call_logs.CallsLogHelper
import com.kapp.call_app.external_call.DialerActivity
import com.kapp.call_app.external_call.ExternalCallActivity
import com.kapp.call_app.telecom.CallManager
import com.kapp.call_app.telecom.NoCall

class MainActivity : AppCompatActivity() {

    private val roleManager: RoleManager by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(ROLE_SERVICE) as RoleManager
        } else {
            getSystemService(ROLE_SERVICE) as RoleManager
        }
    }
    private val TAG: String = MainActivity::class.java.name


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!isDefaultDialer()) {
            requestRoleDefaultDialer()
        }

        /*
        try {
            val callLogList = CallsLogHelper(context = this).fetch()
            val lastMissedCall = CallsLogHelper(this).fetchWithType(1, type = CallLog.Calls.MISSED_TYPE)?.get(0)

            Log.i(TAG, "Calls Log list size is: ${callLogList?.size}")
            Log.i(TAG, "Last Missed Call -> name: ${lastMissedCall?.get(0)?.name}, number: ${lastMissedCall?.get(0)?.number}, date: ${lastMissedCall?.get(0)?.date}")

        }catch (e: Exception){
            Log.i(TAG,"Exception: ${e.message}")
        }
         */
    }


    override fun onResume() {
        super.onResume()

        if(CallManager.getPhoneState() is NoCall){
            val i = Intent(this, DialerActivity::class.java)
            startActivity(i)
        } else {
            startActivity(Intent(this, ExternalCallActivity::class.java))
        }
    }

    private fun isDefaultDialer(): Boolean {
        val manager = getSystemService(TELECOM_SERVICE) as TelecomManager
        val name = manager.defaultDialerPackage
        Log.d(TAG, "isDefault: $name")
        Log.i(TAG, "Package name is: $packageName")
        Log.i(TAG, "isDefaultDialer() returns ${name.equals(packageName)}")
        return name.equals(packageName)
    }

    private fun requestRoleDefaultDialer() {
        Log.i(TAG, "Request for default dialer.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            startForResult.launch(intent)
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName).apply {
                try {
                    startActivityForResult(this, 3)
                } catch (e: ActivityNotFoundException) {
                    Log.i(TAG,"No app found")
                } catch (e: Exception) {
                    Log.i(TAG, "Error: $e")
                }
            }
        }
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            Snackbar.make(this, window.decorView.rootView,"Application set as default dialer", Snackbar.LENGTH_LONG)
        }
    }
}