package nicola.dev.com.allarmap.model

import com.google.gson.annotations.SerializedName

class Prediction {
    @SerializedName("predictions")
    val predictions: List<Description>? = null
    @SerializedName("status")
    val status: String? = null


    inner class Description {
        @SerializedName("description")
        val description: String? = null
        @SerializedName("id")
        val id: String? = null
    }
}