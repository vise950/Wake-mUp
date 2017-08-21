package com.nicola.wakemup.model

data class PlaceAutocomplete(var placeId: CharSequence?, private var description: CharSequence?) {
    override fun toString(): String = description.toString()
}