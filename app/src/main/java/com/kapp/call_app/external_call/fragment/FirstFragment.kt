package com.kapp.call_app.external_call.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.kapp.call_app.R
import com.kapp.call_app.databinding.FragmentFirstBinding
import com.kapp.call_app.external_call.DialerActivity
import com.kapp.call_app.external_call.viewModels.DialerViewModel
import com.kapp.call_app.external_call.viewModels.ExternalCallsControlsViewModel

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {


    private val TAG: String = FirstFragment::class.java.name
    private var _binding: FragmentFirstBinding? = null
    private val viewModel: DialerViewModel by activityViewModels()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            this.dialerViewModel = viewModel
        }

        binding.setCallClickListener {
            Log.i(TAG,"Number is : ${binding.dialerViewModel?.inputNumber?.value.toString()}")
            if (binding.dialerViewModel?.inputNumber?.value != null) {
                (requireActivity() as DialerActivity).initOutgoingCall(binding.dialerViewModel?.inputNumber?.value!!)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}