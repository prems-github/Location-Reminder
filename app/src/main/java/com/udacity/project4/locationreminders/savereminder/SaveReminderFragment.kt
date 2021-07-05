package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceHelper
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
    private val BACKGROUND_LOCATION_REQUEST = 2
    private val RADIUS = 100f
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofenceHelper: GeofenceHelper
    private val TAG=SaveReminderFragment::class.java.simpleName


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        geofenceHelper = GeofenceHelper(requireActivity())
        val args = SaveReminderFragmentArgs.fromBundle(requireArguments())
        _viewModel.reminderSelectedLocationStr.value = args.locationName
        _viewModel.latitude.value = args.poiLatitude.toDouble()
        _viewModel.longitude.value = args.poiLongitude.toDouble()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        /** requests background permission with educated UI if running android Q or later.
         *   If permissions are in place, proceed with saving reminder and creating geofence
         * */

        binding.saveReminder.setOnClickListener {
            if (!isFieldsAreEmpty()) {
                if (runningQOrLater) {
                    if (isBackgroundPermissionEnabled()) {
                        saveReminderInDB()
                    } else {
                        AlertDialog.Builder(requireActivity())
                            .setTitle(R.string.all_time_permission_title)
                            .setMessage(R.string.background_permission_explanation)
                            .setPositiveButton("OK", { dialogInterface, i ->
                                requestPermissions(
                                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                    BACKGROUND_LOCATION_REQUEST
                                )
                            })
                            .setNegativeButton("Cancel", { dialogInterface, i ->
                                dialogInterface.dismiss()
                            }).create().show()
                    }
                } else {
                    saveReminderInDB()

                }
            }


        }
    }

    private fun saveReminderInDB() {
        val title = _viewModel.reminderTitle.value
        val description = _viewModel.reminderDescription.value
        val location = _viewModel.reminderSelectedLocationStr.value
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value
        val newReminder = ReminderDataItem(title, description, location, latitude, longitude)
        _viewModel.saveReminder(newReminder)
        addGeoFence(newReminder.id)
 //       _viewModel.onClear()
        Toast.makeText(requireActivity(), "Reminder saved!", Toast.LENGTH_SHORT).show()
        _viewModel.navigationCommand.value =
            NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToReminderListFragment())

    }

    @SuppressLint("MissingPermission")
    private fun addGeoFence(geofenceID:String) {

        val geofence = geofenceHelper.getGeofence(
            geofenceID,
            LatLng(_viewModel.latitude.value!!, _viewModel.longitude.value!!),
            RADIUS,
            Geofence.GEOFENCE_TRANSITION_ENTER
        )
        val geofencingRequest = geofenceHelper.getGeofencingRequest(geofence)
        val pendingIntent = geofenceHelper.getPendingIntent()
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
            .addOnSuccessListener {
            Log.d(TAG,"Geofence added successfully")
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireActivity(), R.string.geofences_not_added,
                    Toast.LENGTH_SHORT
                ).show()
                if ((it.message != null)) {
                    Log.w(TAG, "${it.message}")
                }
            }
    }

    /**
     * If permission are denied, show indefinite snackbar with a message to user
     * plus action settings button to permission page
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == BACKGROUND_LOCATION_REQUEST) {
            if (grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Snackbar.make(
                    binding.root,
                    R.string.background_permission_denied_explanation,
                    Snackbar.LENGTH_INDEFINITE
                ).also {
                    val snackbarTextView = (it.view.findViewById(R.id.snackbar_text)) as TextView
                    snackbarTextView.maxLines = 3
                }
                    .setAction(R.string.settings) {
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }.show()

            } else {
                Toast.makeText(
                    requireActivity(),
                    "Background Location approved",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    //checks if background permissin is enabled or not (applicable only for android Q and later)

    private fun isBackgroundPermissionEnabled() = ContextCompat.checkSelfPermission(
        requireContext(), android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
    ) == PackageManager.PERMISSION_GRANTED


    //checks the user input fields empty or null and if it so displays toast to fill the same

    private fun isFieldsAreEmpty(): Boolean {
        var isEmpty = true
        if (binding.selectedLocation.text.isNullOrEmpty()) {
            Toast.makeText(requireActivity(), R.string.select_location, Toast.LENGTH_SHORT)
                .show()

        } else if (binding.reminderTitle.text.isNullOrEmpty() || binding.reminderDescription.text.isNullOrEmpty()
        ) {
            Toast.makeText(requireActivity(), R.string.enter_title_description, Toast.LENGTH_SHORT)
                .show()
        } else {
            isEmpty = false
        }
        return isEmpty
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
