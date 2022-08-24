package com.amarland.androidvectorrasterizer

enum class Density(private val qualifier: String) {

    LOW("ldpi"),
    MEDIUM("mdpi"),
    HIGH("hdpi"),
    EXTRA_HIGH("xhdpi"),
    EXTRA_EXTRA_HIGH("xxhdpi"),
    EXTRA_EXTRA_EXTRA_HIGH("xxxhdpi");

    override fun toString() = qualifier
}
