package com.practice.silentonlocation

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.TranslateAnimation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MapsActivity : AppCompatActivity(){
    companion object {
        const val GEOFENCE_ID = "geofence_id"
    }
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofenceHelper: GeofenceHelper
    private lateinit var notificationManager: NotificationManager
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var autoCompleteFragment:AutocompleteSupportFragment
    private lateinit var fabSaveLocation:FloatingActionButton
    private  var userLat:Double? = null
    private var userLng:Double? = null
    private var geofenceLat:Double? = null
    private var geofenceLng:Double? = null
    private var locationName:String = ""
    private var geofenceSet = false
    private val MAP_GEOFENCE_RADIUS = 50.0f
    private lateinit var  mapDB: MapDBHelper
    private lateinit var  arrMaps: ArrayList<MapLocationModel>
    private lateinit var imgBtnMapType1:ImageButton
    private lateinit var imgBtnMapType2:ImageButton
    private lateinit var imgBtnMapType3:ImageButton
    private lateinit var secondBtnCard:CardView
    private lateinit var thirdBtnCard:CardView
    private var areBtnVisible = false

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fabSaveLocation = findViewById(R.id.fabSaveLocation)
        imgBtnMapType1 = findViewById(R.id.imgBtnMapType1)
        imgBtnMapType2 = findViewById(R.id.imgBtnMapType2)
        imgBtnMapType3 = findViewById(R.id.imgBtnMapType3)
        secondBtnCard = findViewById(R.id.secondBtnCard)
        thirdBtnCard = findViewById(R.id.thirdBtnCard)

        geofencingClient = LocationServices.getGeofencingClient(this)
        geofenceHelper = GeofenceHelper(this)
        mapDB = MapDBHelper(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        Places.initialize(applicationContext,getString(R.string.maps_key))
        autoCompleteFragment = supportFragmentManager.findFragmentById(R.id.autoCompleteFragment)
                as AutocompleteSupportFragment

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        if (!notificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        }

        val fromUpdateIntent = intent
        val updateKey = fromUpdateIntent.getBooleanExtra("updateKey",false)
        val updateName = fromUpdateIntent.getStringExtra("name")
        val updateLat = fromUpdateIntent.getDoubleExtra("lat",0.0)
        val updateLng = fromUpdateIntent.getDoubleExtra("lng",0.0)
        val updatePosition = fromUpdateIntent.getIntExtra("position",-1)

        mapFragment.getMapAsync {
            mMap = it
            mMap.isMyLocationEnabled = true
            if (updateKey) {
                geofenceLat = updateLat
                geofenceLng = updateLng
                CoroutineScope(Dispatchers.IO).launch {
                    arrMaps = mapDB.fetchMaps()
                }
                addCircle(geofenceLat,geofenceLng,MAP_GEOFENCE_RADIUS)
                val markerOption = MarkerOptions()
                    .position(LatLng(geofenceLat!!,geofenceLng!!))
                    .title(updateName)
                Handler(Looper.getMainLooper()).postDelayed({
                    mMap.addMarker(markerOption)
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(geofenceLat!!, geofenceLng!!)))
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(geofenceLat!!, geofenceLng!!), 18f)
                    )
                },1000)
            } else {
                getUserLocation()
            }

            mMap.setOnMapClickListener { googleMap ->
                mMap.clear()
                geofenceLat = googleMap.latitude
                geofenceLng = googleMap.longitude

                CoroutineScope(Dispatchers.IO).launch {
                    getLocationName(geofenceLat,geofenceLng)
                    withContext(Dispatchers.Main) {
                        addCircle(geofenceLat,geofenceLng,MAP_GEOFENCE_RADIUS)
                        val markerOption = MarkerOptions().position(LatLng(geofenceLat!!,geofenceLng!!)).title("$geofenceLat,$geofenceLng")
                        mMap.addMarker(markerOption)
                    }
                }
            }
            mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
        }
        secondBtnCard.visibility = View.GONE
        thirdBtnCard.visibility = View.GONE

        moveTOLocation() // for everytime user search it gives result

        fabSaveLocation.setOnClickListener {
            finish()
        }

        fabSaveLocation.setOnLongClickListener {
            if (geofenceLat == null || geofenceLng == null) {
                Toast.makeText(this,"Location is not Selected",Toast.LENGTH_SHORT).show()
                return@setOnLongClickListener true
            }
            CoroutineScope(Dispatchers.IO).launch {
                addGeofence(geofenceLat,geofenceLng,MAP_GEOFENCE_RADIUS)
                withContext(Dispatchers.Main) {
                    if (locationName.isNotEmpty() && geofenceSet && !updateKey) {
                        mapDB.addMap(locationName, geofenceLat!!, geofenceLng!!)
                        mapDB.close()
                    }
                    if (locationName.isNotEmpty() && geofenceSet
                        && updateKey && updatePosition>=0 && updatePosition<arrMaps.size) {
                        val updateMapLocation = arrMaps[updatePosition]

                        updateMapLocation.lat = geofenceLat
                        updateMapLocation.lng = geofenceLng
                        updateMapLocation.name = locationName
                        mapDB.updateMaps(updateMapLocation)
                    }
                }
            }
            true
        }

        imgBtnMapType1.setOnClickListener {
            if (!areBtnVisible) {
                secondBtnCard.visibility = View.VISIBLE
                thirdBtnCard.visibility = View.VISIBLE
            }
            else {
                secondBtnCard.visibility = View.GONE
                thirdBtnCard.visibility = View.GONE
            }
            areBtnVisible = !areBtnVisible
        }

        imgBtnMapType2.setOnClickListener {
            mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
        }
        imgBtnMapType3.setOnClickListener {
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        val task: Task<Location> = fusedLocationProviderClient.lastLocation
        task.addOnSuccessListener { location ->
            if (location != null) {
                userLat = location.latitude
                userLng = location.longitude
                val userLocation = LatLng(userLat!!, userLng!!)

                mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(28.171787213694518, 83.9856864115855)))

                Handler(Looper.getMainLooper()).postDelayed({
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation))
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation,18f))
                },1000)
            } else {
            }
        }
    }

    private suspend fun getLocationName(lat: Double?, lng: Double?) {
        if (lat == null && lng == null) {
            return
        }
        val geocoder = Geocoder(this@MapsActivity, Locale.getDefault())
        try {
            val addresses = withContext(Dispatchers.IO){
                geocoder.getFromLocation(lat!!, lng!!, 1)
            }
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                locationName = address.getAddressLine(0)
            } else {
                locationName = "Failed To find Address"
            }
        } catch (e: IOException) {
            locationName = "Address Fetching Error"
        }
    }

    private fun moveTOLocation() {
        autoCompleteFragment.setPlaceFields(listOf(Place.Field.ID,Place.Field.ADDRESS,Place.Field.LAT_LNG))

        autoCompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onError(status: Status) {
                Toast.makeText(this@MapsActivity,"error: $status",Toast.LENGTH_SHORT).show()
            }

            override fun onPlaceSelected(place: Place) {
                // val address = place.address
                // val id = place.id
//                val name = place.name
                val latlng = place.latLng
                if (latlng != null) {
//                    mMap.addMarker(MarkerOptions().position(latlng).title(name))
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng))
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng , 16f))
                }
            }
        })
    }

    private fun addCircle(lat:Double?,lng:Double?, radius: Float) {
        if (lat == null || lng == null) return
        val latlng = LatLng(lat,lng)
        val circleOptions = CircleOptions()
        circleOptions.center(latlng)
        circleOptions.radius(radius.toDouble())
        circleOptions.strokeColor(Color.argb(255,255,0,0))
        circleOptions.fillColor(Color.argb(64,255,0,0))
        circleOptions.strokeWidth(4f)
        mMap.addCircle(circleOptions)
    }

    @SuppressLint("MissingPermission")
    private suspend fun addGeofence(lat:Double?, lng: Double?, radius: Float) {
        if (lat == null || lng == null) return
        val geofence = geofenceHelper.getGeofence(GEOFENCE_ID, lat, lng, radius,
            Geofence.GEOFENCE_TRANSITION_ENTER
                    or Geofence.GEOFENCE_TRANSITION_EXIT )
        val geofencingRequest = geofenceHelper.getGeofencingRequest(geofence)
        val pendingIntent = geofenceHelper.getPendingIntent()

        suspendCancellableCoroutine { continuation ->
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener {
                    Toast.makeText(this, "Location added...", Toast.LENGTH_SHORT).show()
                    geofenceSet = true
                    continuation.resume(Unit)
                }
                .addOnFailureListener { // it = Exception
                    val errorMessage = geofenceHelper.getErrorString(it)
                    Toast.makeText(this, "Error! try again later..", Toast.LENGTH_SHORT).show()
                    geofenceSet = false
                    continuation.resumeWithException(it)
                }
        }
    }

}