package com.nicola.wakemup.model

class Prediction {
    val predictions: List<Description>? = null
    val status: String? = null

    inner class Description {
        val description: String? = null
        val id: String? = null
    }
}