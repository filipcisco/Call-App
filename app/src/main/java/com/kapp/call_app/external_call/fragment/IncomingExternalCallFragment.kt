package com.kapp.call_app.external_call.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kapp.call_app.R
import com.kapp.call_app.databinding.FragmentExternalSingleCallBinding
import com.kapp.call_app.databinding.FragmentIncomingExternalCallBinding
import com.kapp.call_app.external_call.viewModels.ExternalCallsControlsViewModel
import com.kapp.call_app.external_call.viewModels.ExternalCallsViewModel

class IncomingExternalCallFragment : Fragment() {

    private val extCallsViewModel: ExternalCallsViewModel by activityViewModels()
    private val extControlsViewModel: ExternalCallsControlsViewModel by activityViewModels()

    private lateinit var binding: FragmentIncomingExternalCallBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_incoming_external_call, container, false)

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            controlsViewModel = extControlsViewModel
            callsViewModel = extCallsViewModel
        }

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}