package com.nicola.wakemup.retrofit

import com.nicola.wakemup.model.LocationName
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface MapsGoogleApiService {

    companion object {
        private const val URL_GEOCODE = "maps/api/geocode/json"
    }

    @GET(URL_GEOCODE)
    fun getLocationName(@Query("latlng") latlng: String): Observable<LocationName>
}