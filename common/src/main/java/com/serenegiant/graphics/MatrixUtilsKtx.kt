package com.serenegiant.graphics
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2021 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

import android.graphics.Matrix
import kotlin.math.*

fun Matrix.getRotationDegree() = FloatArray(9)
    .apply { getValues(this) }
    .let { -round(atan2(it[Matrix.MSKEW_X], it[Matrix.MSCALE_X]) * (180 / PI)) }

fun Matrix.getScale() = FloatArray(9)
    .apply { getValues(this) }
    .let { sqrt(it[Matrix.MSKEW_Y] * it[Matrix.MSKEW_Y] + it[Matrix.MSCALE_X] * it[Matrix.MSCALE_X]) }