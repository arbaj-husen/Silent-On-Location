package com.practice.silentonlocation

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MapDBHelper(context : Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "MapDB"
        const val DATABASE_VERSION = 1
        const val TABLE_MAP = "maps"
        const val KEY_ID = "id"
        const val KEY_NAME = "name"
        const val KEY_LAT = "latitude"
        const val KEY_LNG = "longitude"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE $TABLE_MAP ($KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,$KEY_NAME TEXT, $KEY_LAT REAL,$KEY_LNG REAL)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

    fun addMap(name:String,lat:Double,lng:Double) {
        val db = this.writableDatabase

        val value = ContentValues()
        value.put(KEY_NAME,name)
        value.put(KEY_LAT,lat)
        value.put(KEY_LNG,lng)
        db.insert(TABLE_MAP,null,value)

    }

    fun fetchMaps() : ArrayList<MapLocationModel> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MAP",null)
        val arrMaps = ArrayList<MapLocationModel>()

        while (cursor.moveToNext()) {
            val map = MapLocationModel()
            map.id = cursor.getInt(0)
            map.name = cursor.getString(1)
            map.lat = cursor.getDouble(2)
            map.lng = cursor.getDouble(3)

            arrMaps.add(map)
        }
        cursor.close()
        return arrMaps
    }

    fun updateMaps(mapLocationModel: MapLocationModel) {
        val db = this.writableDatabase

        val value = ContentValues()
        value.put(KEY_NAME,mapLocationModel.name)
        value.put(KEY_LAT,mapLocationModel.lat)
        value.put(KEY_LNG,mapLocationModel.lng)

        db.update(TABLE_MAP, value,"$KEY_ID = ${mapLocationModel.id}", null)

    }

    fun deleteMap(id:Int) {
        val db = this.writableDatabase
        db.delete(TABLE_MAP, "$KEY_ID =?", arrayOf(id.toString()))

    }

}