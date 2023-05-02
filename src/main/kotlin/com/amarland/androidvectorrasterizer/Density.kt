/*
 * Copyright 2022 Anthony Marland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amarland.androidvectorrasterizer

enum class Density(private val qualifier: String, val scaleFactor: Float) {

    LOW("ldpi", 0.75F),
    MEDIUM("mdpi", 1F),
    HIGH("hdpi", 1.5F),
    X_HIGH("xhdpi", 2F),
    XX_HIGH("xxhdpi", 3F),
    XXX_HIGH("xxxhdpi", 4F);

    val directoryName: String = "drawable-$qualifier"

    override fun toString() = qualifier
}
