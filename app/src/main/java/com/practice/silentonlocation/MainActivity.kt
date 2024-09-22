package com.practice.silentonlocation

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.media.SoundPool
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1010
    }
    private lateinit var btnMap:AppCompatButton
    private lateinit var mapIntent:Intent
    private lateinit var mapRecyclerSavePlace: RecyclerView
    private lateinit var arrSavedMap: ArrayList<MapLocationModel>
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var mapAdapter:MapSavedRecyclerAdapter
    private lateinit var deleteSoundPool:SoundPool
    private  var deleteSound: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnMap = findViewById(R.id.btnMap)
        mapRecyclerSavePlace = findViewById(R.id.mapRecyclerSavePlace)
        geofencingClient = LocationServices.getGeofencingClient(this)
        deleteSoundPool = SoundPool.Builder().setMaxStreams(1).build() // creating delete sound pool
        deleteSound = deleteSoundPool.load(this@MainActivity,R.raw.delete_sound_on_swipe,1)

        mapRecyclerSavePlace.layoutManager = LinearLayoutManager(this)

        btnMap.setOnClickListener {
            mapIntent = Intent(this,MapsActivity::class.java)
            checkLocationPermission()
        }

        showLocations()

        val swipeDelete = object: ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val positionId = mapAdapter.getMapPositionId(position)
                if (positionId == -1) {
                    Toast.makeText(this@MainActivity,"Error Occurred On deleting,Try Again Later!",Toast.LENGTH_SHORT).show()
                    return
                }
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        geofencingClient.removeGeofences(listOf(MapsActivity.GEOFENCE_ID))
                            .addOnSuccessListener {
                                deleteSoundPool.play(deleteSound!!,1f,1f,0,0,1f)
                                mapAdapter.deleteItem(position,positionId)
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@MainActivity,"Try Again Later!",Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean ) {

                RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeLeftBackgroundColor(ContextCompat.getColor(this@MainActivity,R.color.red))
                    .addSwipeLeftActionIcon(R.drawable.delete_icon)
                    .create()
                    .decorate();

                super.onChildDraw(c,recyclerView,viewHolder,dX,dY,actionState,isCurrentlyActive)
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeDelete) // this made object of ItemTouchHelper where we passed the simpleCallBack as parameter
        itemTouchHelper.attachToRecyclerView(mapRecyclerSavePlace) // this helps to attach this swipe function to our RecyclerView

    }

    override fun onResume() {
        super.onResume()
        showLocations()
    }

    private fun showLocations() {
        arrSavedMap = MapDBHelper(this@MainActivity).fetchMaps()
        mapAdapter = MapSavedRecyclerAdapter(this@MainActivity,arrSavedMap)
        mapRecyclerSavePlace.adapter = mapAdapter

    }

    @SuppressLint("InlinedApi")
    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            startActivity(mapIntent)

        } else {
            ActivityCompat.requestPermissions(this@MainActivity,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                PERMISSION_REQUEST_CODE )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,"Permission Granted!!",Toast.LENGTH_SHORT).show()
                    startActivity(mapIntent)
                } else {
                    Toast.makeText(this,"Location Permission Denied!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deleteSoundPool.release() // releasing if sound pool if this activity destroys before onSwipe
    }
}