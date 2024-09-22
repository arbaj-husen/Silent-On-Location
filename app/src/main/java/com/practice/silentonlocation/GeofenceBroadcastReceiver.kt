package com.practice.silentonlocation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private lateinit var audioManager: AudioManager
    private lateinit var transitionType:String

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (geofencingEvent == null) {
            Log.e("GeofenceReceiver", "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("GeofenceReceiver", "Geofencing error: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            transitionType =  "User Entered!"
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        }
        else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            transitionType = "User Exited!"
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
         else {
            // Log an error if the transition type is invalid
            Log.e("GeofenceReceiver", "Invalid geofence transition type: $geofenceTransition")
        }
        Toast.makeText(context,"Geofence : $transitionType",Toast.LENGTH_SHORT).show()

        Log.d("GeofenceReceiver", "Geofence transition: $transitionType")
    }
}