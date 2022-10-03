package com.kapp.call_app.core

import android.content.Context
import androidx.lifecycle.*

class CoreContext(val context: Context) : LifecycleOwner, ViewModelStoreOwner {

    private val _lifecycleRegistry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle {
        return _lifecycleRegistry
    }

    private val _viewModelStore = ViewModelStore()
    override fun getViewModelStore(): ViewModelStore {
        return _viewModelStore
    }

}