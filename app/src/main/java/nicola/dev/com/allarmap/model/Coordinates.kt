package nicola.dev.com.allarmap.model


class Coordinates {
    val results: List<Result>? = null
    val status: String? = null

    inner class Result {
        val geometry: Geometry? = null
    }

    inner class Geometry {
        val location: Location? = null
    }

    inner class Location {
        val lat: Double? = null
        val lng: Double? = null
    }
}




