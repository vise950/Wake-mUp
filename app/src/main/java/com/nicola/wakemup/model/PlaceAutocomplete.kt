package com.nicola.wakemup.model

data class PlaceAutocomplete(var placeId: CharSequence?, var description: CharSequence?) {
    override fun toString(): String {
        return description.toString()
    }
}