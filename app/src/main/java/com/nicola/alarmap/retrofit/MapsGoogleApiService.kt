package com.nicola.alarmap.retrofit

import io.reactivex.Observable
import con.nicola.com.alarmap.BuildConfig
import com.nicola.alarmap.model.Coordinates
import com.nicola.alarmap.model.LocationName
import com.nicola.alarmap.model.Prediction
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.*

interface MapsGoogleApiService {

    companion object {
        private const val URL_AUTOCOMPLETE = "maps/api/place/autocomplete/json"
        private const val API_KEY = BuildConfig.GOOGLE_API_AUTOCOMPLETE_KEY
        private const val URL_GEOCODE = "maps/api/geocode/json"
    }

    @GET(URL_AUTOCOMPLETE)
    fun getPrediction(@Query("input") input: String,
                      @Query("types") types: String = "(cities)",
                      @Query("language") lang: String = Locale.getDefault().language,
                      @Query("key") key: String = API_KEY
    ): Observable<Prediction>

    @GET(URL_GEOCODE)
    fun getLocationName(@Query("latlng") latlng: String): Observable<LocationName>

    @GET(URL_GEOCODE)
    fun getCoordinates(@Query("address") address: String): Observable<Coordinates>
}