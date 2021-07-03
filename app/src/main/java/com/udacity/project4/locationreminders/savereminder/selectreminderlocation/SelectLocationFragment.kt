package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val LOCATION_ACCESS_REQUEST = 1
    private val TAG = SelectLocationFragment::class.java.simpleName

    private val defaultZoomLevel = 17f
    private val defaultLocation = LatLng(-34.0, 151.0)

    private lateinit var poiTitle:String
    private  var poiLatitude=0.0
    private  var poiLongitude=0.0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)


        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

//        TODO: add the map setup implementation
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

//        TODO: zoom to the user location after taking his permission
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected





//        TODO: call this function after the user confirms on the selected location
        onLocationSelected()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.saveButton.setOnClickListener {
            findNavController().navigate(SelectLocationFragmentDirections
                .actionSelectLocationFragmentToSaveReminderFragment(poiTitle,
                    poiLatitude.toFloat(), poiLongitude.toFloat()
                ))
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_ACCESS_REQUEST) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                enableUserLocation()

        }
    }

    @SuppressLint("MissingPermission")
    private fun enableUserLocation() {
        if (isPermissionEnabled()) {
            map.isMyLocationEnabled = true
            getDeviceLocation()

        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_ACCESS_REQUEST
            )
        }
    }

    private fun isPermissionEnabled() =
        ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED


    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap!!
        enableUserLocation()
        setMapStyle(map)
        Snackbar.make(requireView().rootView,R.string.select_poi,Snackbar.LENGTH_SHORT).show()
       // setMarkOnLongClick(map)
        setMarkOnPOIClick(map)

    }

    private fun setMarkOnPOIClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.addMarker(MarkerOptions().position(poi.latLng).title(poi.name))
            poiTitle=poi.name
            poiLatitude=poi.latLng.latitude
            poiLongitude=poi.latLng.longitude
        }
    }

   /* private fun setMarkOnLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { LatLng ->
            map.addMarker(MarkerOptions().position(LatLng))
        }
    }*/

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireActivity(),
                    R.raw.map_style
                )
            )
            if (!success) {
                Log.e(TAG, "Parsing failed")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Resource not found $e")
        }
    }

    //Gets device location and move camera to that location

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation() is called")
        try {
            if (isPermissionEnabled()) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener { task ->
                    Log.d(TAG, "last location is ${task.result}")
                    val currentLocation = task.result
                    if (task.isSuccessful && currentLocation != null) {
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(currentLocation.latitude, currentLocation.longitude),
                                defaultZoomLevel
                            )
                        )
                    } else {
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                defaultLocation,
                                defaultZoomLevel
                            )
                        )
                    }

                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
        }
    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


}
