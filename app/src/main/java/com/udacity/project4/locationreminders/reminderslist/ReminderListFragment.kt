package com.udacity.project4.locationreminders.reminderslist

import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentRemindersBinding
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setTitle
import com.udacity.project4.utils.setup
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReminderListFragment : BaseFragment() {
    //use Koin to retrieve the ViewModel instance
    override val _viewModel: RemindersListViewModel by viewModel()
    private lateinit var binding: FragmentRemindersBinding
    var previoustime: Long = 0
    private val TAG = ReminderListFragment::class.java.simpleName

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_reminders, container, false
            )
        binding.viewModel = _viewModel

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(false)
        setTitle(getString(R.string.app_name))

        binding.refreshLayout.setOnRefreshListener { _viewModel.loadReminders() }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        setupRecyclerView()
        binding.addReminderFAB.setOnClickListener {
            navigateToAddReminder()
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (System.currentTimeMillis() <= previoustime + 2000) {
                requireActivity().finishAffinity()
            } else {
                Toast.makeText(
                    requireActivity(),
                    "Press once again to Logout", Toast.LENGTH_SHORT
                ).show()
                previoustime = System.currentTimeMillis()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //load the reminders list on the ui
        _viewModel.loadReminders()
        chekDeviceLocationSettings()
    }

    private fun chekDeviceLocationSettings() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
            .checkLocationSettings(locationSettingsRequest.build())
        settingsClient.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
                        1
                    )
                } catch (ex: IntentSender.SendIntentException) {
                    Log.d(TAG, "Request failed ${ex.message}")
                }
            }
        }
        settingsClient.addOnCompleteListener {
            if (it.isSuccessful) {
                if (it.isSuccessful) {
                    Log.d(TAG, "Device location is turned on")
                }
            }
        }
    }

    private fun navigateToAddReminder() {
        //use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                ReminderListFragmentDirections.toSaveReminder()
            )
        )
    }

    private fun setupRecyclerView() {
        val adapter = RemindersListAdapter {
        }

//        setup the recycler view using the extension function
        binding.reminderssRecyclerView.setup(adapter)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                AuthUI.getInstance().signOut(requireContext())
                requireActivity().finishAffinity()
            }
        }
        return super.onOptionsItemSelected(item)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
//        display logout as menu item
        inflater.inflate(R.menu.main_menu, menu)
    }


}
