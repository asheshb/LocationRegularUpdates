package com.example.locationregularupdates

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.text.DateFormat
import java.util.*

const val PERMISSION_REQUEST_FINE_LOCATION = 0
const val REQUEST_LOCATION_SETTINGS = 1

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationSettingsRequest: LocationSettingsRequest
    private lateinit var settingsClient: SettingsClient

    private var locationUpdating: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationUpdates()

        start_location_updates.setOnClickListener {
            location_updates.text = ""
            locationUpdating = true

            startLocationUpdates()
        }

        stop_location_updates.setOnClickListener {
            stopLocationUpdates()
            locationUpdating = false
        }
    }

    private fun setupLocationUpdates(){

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                val currentLocation = locationResult.lastLocation
                val lastUpdateTime = DateFormat.getTimeInstance().format(Date())
                location_updates.append(getString(R.string.location_info,
                    lastUpdateTime, currentLocation.latitude, currentLocation.longitude))
            }
        }

        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        locationSettingsRequest = builder.build()

        settingsClient= LocationServices.getSettingsClient(this)

    }

    private fun startLocationUpdates() {


        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission()
            return
        }


        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener(this){
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback, Looper.myLooper()
                )
            }
            .addOnFailureListener(this){ e ->
                when ((e as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(
                                this@MainActivity,
                                REQUEST_LOCATION_SETTINGS
                            )
                        } catch (sie: SendIntentException) {

                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        Toast.makeText(this@MainActivity,
                            getString(R.string.insufficient_location_settings), Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
    }

    override fun onResume() {
        super.onResume()
        if (locationUpdating) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()

        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            .addOnCompleteListener(this){

            }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_LOCATION_SETTINGS -> when (resultCode) {
                Activity.RESULT_OK -> {
                    // All good nothing to do
                }
                Activity.RESULT_CANCELED -> {
                    // User didn't change the location settings. We may show a message to the user
                }
            }
        }
    }


    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            val snack = Snackbar.make(container, R.string.location_permission_rationale,
                Snackbar.LENGTH_INDEFINITE)
            snack.setAction(getString(R.string.ok)) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_FINE_LOCATION)
            }
            snack.show()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, getString(R.string.location_permission_denied),
                    Toast.LENGTH_SHORT). show()
            }
        }
    }

}
