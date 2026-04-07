package com.soundtag.data

data class AnnotationData(
    val noiseType: String = "",
    val isNoise: Boolean = true,
    val severity: String = "medium",
    val environment: String = "outdoor",
    val locationContext: String = "",
    val fileName: String = "",
    val notes: String = ""
)
