package nicola.dev.com.allarmap.model


class LocationName {
    val results: List<Result>? = null
    val status: String? = null

    inner class Result {
        val address_components: List<AddressComponent>? = null
        val formatted_address: String? = null
    }

    inner class AddressComponent {
        val long_name: String? = null
        val short_name: String? = null
        val types: List<String>? = null
    }
}