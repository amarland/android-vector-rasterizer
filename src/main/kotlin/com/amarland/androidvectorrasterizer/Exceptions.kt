package com.amarland.androidvectorrasterizer

import java.io.File

open class PreRasterizationException(message: String) : Exception(message)

class DirectoryCreationException(
    directory: File
) : PreRasterizationException("Directory '${directory.path}' could not be created")
