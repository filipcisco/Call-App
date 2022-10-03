package com.kapp.call_app.external_call

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.CallAudioState
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.kapp.call_app.R
import com.kapp.call_app.databinding.ActivityDialerBinding
import com.kapp.call_app.external_call.fragment.FirstFragment
import com.kapp.call_app.external_call.viewModels.DialerViewModel
import com.kapp.call_app.external_call.viewModels.ExternalCallsViewModel
import com.kapp.call_app.telecom.CallManager

class DialerActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    companion object {
        fun getStartIntent(context: Context): Intent {
            val openAppIntent = Intent(context, DialerActivity::class.java)
            openAppIntent.flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            return openAppIntent
        }
    }

    private val TAG: String = DialerActivity::class.java.name

    private val roleManager: RoleManager by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(ROLE_SERVICE) as RoleManager
        } else {
            getSystemService(ROLE_SERVICE) as RoleManager
        }
    }
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityDialerBinding
    private val telecomManager: TelecomManager get() = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    private val viewModel: DialerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this,R.layout.activity_dialer)

        setSupportActionBar(binding.toolbar)

        binding.apply {
            lifecycleOwner = this@DialerActivity
            this.dialerViewModel = viewModel
        }

        val navController = findNavController(R.id.nav_host_fragment_content_dialer)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        if (!isDefaultDialer()) {
            requestRoleDefaultDialer()
        }

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_dialer)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    fun initOutgoingCall(number: String?){
        Log.i(TAG, "Phone number is: $number")
        if (number != null){
            val callNumber = Uri.parse("tel:$number")
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(TAG,"No permission to call.")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE),2)
                return
            } else {
                val handle = telecomManager.getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_TEL)
                if (handle != null) {
                    val bundle = Bundle().apply {
                        putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                        putBoolean(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, false)
                        putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
                    }
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CALL_PHONE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Log.i(TAG,"No permission to call.")
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE),1)
                        return
                    }
                    telecomManager.placeCall(callNumber, bundle)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            1 -> {
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission has been granted, preview can be displayed
                    Log.i(TAG, "Call permission has now been granted. Showing preview.");
                    Snackbar.make(this, binding.root,"Permission granted.",
                        Snackbar.LENGTH_LONG).show()
                    initOutgoingCall(viewModel.inputNumber.value)
                } else {
                    Log.i(TAG, "Call permission was NOT granted.");
                    Snackbar.make(this, binding.root,"Permission not granted.",
                        Snackbar.LENGTH_LONG).show()

                }
            }
            2 -> {
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission has been granted, preview can be displayed
                    Log.i(TAG, "Read phone state permission has now been granted. Showing preview.");
                    Snackbar.make(this, binding.root,"Permission granted.",
                        Snackbar.LENGTH_LONG).show()
                    initOutgoingCall(viewModel.inputNumber.value)
                } else {
                    Log.i(TAG, "Read phone state permission was NOT granted.");
                    Snackbar.make(this, binding.root,"Permission not granted.",
                        Snackbar.LENGTH_LONG).show()

                }
            }
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