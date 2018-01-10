package com.nicola.wakemup.adapter

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.places.AutocompleteFilter
import com.google.android.gms.location.places.Places
import com.nicola.wakemup.model.PlaceAutocomplete
import java.util.*
import java.util.concurrent.TimeUnit


class PlaceAutocompleteAdapter(context: Context, resource: Int, private val googleApiClient: GoogleApiClient?)
    : ArrayAdapter<PlaceAutocomplete>(context, resource), Filterable {

    private var resultList: ArrayList<PlaceAutocomplete>? = null

    override fun getCount(): Int = resultList?.size ?: 0

    override fun getItem(position: Int): PlaceAutocomplete? = resultList?.get(position)

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults? {
            val results = FilterResults()
            constraint?.let {
                getPredictions(it)?.let {
                    resultList = it
                    results.values = it
                    results.count = it.size
                }
            }
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            results?.let {
                if (it.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }

    private fun getPredictions(constraint: CharSequence?): ArrayList<PlaceAutocomplete>? {
        val typeFilter = AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES)
                .build()

        googleApiClient?.let {
            val results = Places.GeoDataApi.getAutocompletePredictions(it, constraint.toString(), null, typeFilter)
            val autocompletePredictions = results.await(60, TimeUnit.SECONDS)
            return if (autocompletePredictions.status?.isSuccess == true) {
                val resultList = ArrayList<PlaceAutocomplete>(autocompletePredictions.count)
                autocompletePredictions.iterator().forEach {
                    resultList.add(PlaceAutocomplete(it.placeId.toString(), it.getFullText(null)))
                }
                autocompletePredictions.release()
                resultList
            } else {
                autocompletePredictions.release()
                null
            }
        } ?: run {
            return null
        }
    }
}