package com.kapp.call_app.external_call.viewModels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DialerViewModel : ViewModel() {

    private val TAG: String = DialerViewModel::class.java.name

    val inputNumber = MutableLiveData<String>()

}