package com.kapp.call_app.external_call.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kapp.call_app.R
import com.kapp.call_app.databinding.FragmentExternalSingleCallBinding
import com.kapp.call_app.databinding.FragmentOutgoingExternalCallBinding
import com.kapp.call_app.external_call.viewModels.ExternalCallsControlsViewModel
import com.kapp.call_app.external_call.viewModels.ExternalCallsViewModel

class OutgoingExternalCallFragment: Fragment() {

    private val extCallsViewModel: ExternalCallsViewModel by activityViewModels()
    private val extControlsViewModel: ExternalCallsControlsViewModel by activityViewModels()

    private var _binding: FragmentOutgoingExternalCallBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentOutgoingExternalCallBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * All business logics are handled here
         */
       binding.apply {
           lifecycleOwner = viewLifecycleOwner
           callsViewModel = extCallsViewModel
           controlsViewModel = extControlsViewModel
       }

    }
}