package com.practice.silentonlocation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlin.math.ln

class MapSavedRecyclerAdapter (
    private val context: Context,
    private val arrMap:ArrayList<MapLocationModel>): RecyclerView.Adapter<MapSavedRecyclerAdapter.MapViewHolder>(){

    private val mapDB = MapDBHelper(context)

    class MapViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        val mapImageButton:AppCompatImageButton = itemView.findViewById(R.id.mapImageButton)
        val locationName:TextView = itemView.findViewById(R.id.locationName)
        val latitudeTextView:TextView = itemView.findViewById(R.id.latitudeTextView)
        val longitudeTextView:TextView = itemView.findViewById(R.id.longitudeTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.map_saved_recycler_design, parent, false)
        return MapViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrMap.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MapViewHolder, position: Int) {
        val mapDetail = arrMap[position]
//        val mapUrl = getStaticMapUrl(mapDetail.lat,mapDetail.lng)

        holder.mapImageButton.setImageResource(R.drawable.placeholder_img)
        holder.locationName.text = mapDetail.name
        holder.latitudeTextView.text = "Latitude: ${mapDetail.lat}"
        holder.longitudeTextView.text = "Longitude: ${mapDetail.lng}"



        holder.mapImageButton.setOnClickListener{
            updateProcess(mapDetail,position)
        }

        // loading static map api using Glide Library
//        Glide.with(holder.mapImageButton.context)
//            .load(mapUrl)
//            .error(R.drawable.placeholder_img)
//            .into(holder.mapImageButton)
    }

//    private fun getStaticMapUrl(lat: Double?, lng: Double?): String {
//        val apiKey = "AIzaSyDYQJMSsxDKugOAm53vN7wDswb0El9A6d4"
//
//        Log.d("check", "getStaticMapUrl: working ")
//
//        return "https://maps.googleapis.com/maps/api/staticmap?" +
//                "center=$lat,$lng" +
//                "&zoom=16" +
//                "&size=600x300" +
//                "&maptype=roadmap" +
//                "&key=$apiKey"
//    }

    private fun updateProcess(mapDetail: MapLocationModel, position: Int) {
        val name = mapDetail.name
        val lat = mapDetail.lat
        val lng = mapDetail.lng
        val updateKey = true

        val mapUpdateIntent = Intent(context,MapsActivity::class.java)
        val bundle = Bundle()
        bundle.putString("name",name)
        bundle.putDouble("lat",lat!!)
        bundle.putDouble("lng",lng!!)
        bundle.putBoolean("updateKey",updateKey)
        bundle.putInt("position",position)

        mapUpdateIntent.putExtras(bundle)
        startActivity(context,mapUpdateIntent,bundle)
    }

    fun getMapPositionId(position: Int):Int {
        val location = arrMap[position]
        if (location.id == null) return -1
        return location.id!!
    }

    fun deleteItem(position: Int,positionId: Int) {
        mapDB.deleteMap(positionId)
        arrMap.removeAt(position)
        notifyItemRemoved(position)
    }


}