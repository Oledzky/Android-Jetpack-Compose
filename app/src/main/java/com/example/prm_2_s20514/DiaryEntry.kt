package com.example.prm_2_s20514

import android.location.LocationManager
import android.location.LocationRequest
import com.google.android.gms.location.LocationServices
import java.util.Date

data class DiaryEntry(
    var title: String = "",
    var content: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var latitude: Double? = null,
    var longitude: Double? = null,
    var imageUrlEntry: String? = null,
    var audioUrlEntry: String? = null


)