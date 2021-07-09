package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
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
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
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
    private val TAG = SaveReminderFragment::class.java.simpleName
    private lateinit var reminderDataItem: ReminderDataItem
    private val REQUEST_TURN_DEVICE_LOCATION_ON = 29

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
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            reminderDataItem = ReminderDataItem(title, description, location, latitude, longitude)

            if (_viewModel.validateEnteredData(reminderDataItem)) {
                if (runningQOrLater) {
                    if (isBackgroundPermissionEnabled()) {
                        checkDeviceLocationSettingsAndAddGeoFence()
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
                    checkDeviceLocationSettingsAndAddGeoFence()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeoFence(reminder: ReminderDataItem) {

        val geofence = geofenceHelper.getGeofence(
            reminder.id,
            LatLng(_viewModel.latitude.value!!, _viewModel.longitude.value!!),
            RADIUS,
            Geofence.GEOFENCE_TRANSITION_ENTER
        )
        val geofencingRequest = geofenceHelper.getGeofencingRequest(geofence)
        val pendingIntent = geofenceHelper.getPendingIntent()
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "Geofence added successfully")
                _viewModel.saveReminder(reminder)
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
                ).setAction(R.string.settings) {
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }.show()

            } else {

                checkDeviceLocationSettingsAndAddGeoFence()
                /*Toast.makeText(
                    requireActivity(),
                    "Background Location approved",
                    Toast.LENGTH_SHORT
                ).show()*/
            }
        }
    }

    //checks if background permissin is enabled or not (applicable only for android Q and later)

    private fun isBackgroundPermissionEnabled() = ContextCompat.checkSelfPermission(
        requireContext(), android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    /*
   *  Uses the Location Client to check the current state of location settings, and gives the user
   *  the opportunity to turn on location services within our app.
   */
    private fun checkDeviceLocationSettingsAndAddGeoFence(resolve: Boolean = true) {

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        Log.d(TAG, "Location settings : yes ")
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->

            Log.d(TAG, "is failer : yes ")
            if (exception is ResolvableApiException && resolve) {
                Log.d(TAG, "is resolvable :${exception is ResolvableApiException} ")
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null,
                        0,
                        0,
                        0,
                        null
                    )

                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.root.rootView,
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE
                ).also {
                    val snackbarTextView = (it.view.findViewById(R.id.snackbar_text)) as TextView
                    snackbarTextView.maxLines = 3
                }.setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndAddGeoFence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "Device location is turned on")
                Log.d(TAG, "geofence triggered")
                addGeoFence(reminderDataItem)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndAddGeoFence(false)
            Log.d(TAG,"OnActivity result is called")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
