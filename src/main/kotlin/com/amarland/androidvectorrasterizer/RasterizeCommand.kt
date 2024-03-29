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

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.Localization
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.ArgumentDelegate
import com.github.ajalt.clikt.parameters.arguments.ProcessedArgument
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.parameters.types.uint
import org.apache.batik.transcoder.TranscoderException
import org.apache.batik.transcoder.TranscoderInput
import org.jetbrains.annotations.VisibleForTesting
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.EnumSet
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString
import kotlin.io.path.useDirectoryEntries
import kotlin.system.exitProcess

private val newLine = System.lineSeparator()
private const val NEXT_LINE = '\u0085'

class RasterizeCommand(
    private val fileSystem: FileSystem = FileSystems.getDefault()
) : CliktCommand(
    name = "java -jar android-vector-rasterizer.jar",
    printHelpOnEmptyArgs = true
) {

    @get:VisibleForTesting
    val source by argument("source")
        .filesOnlyWithExpandedDirectoryContents()
        .validateSource()

    private val destination by option(
        "-d",
        "--destination",
        help = "Location of the generated WebP files$NEXT_LINE(must be a directory).",
        metavar = "<dir>"
    ).path(canBeFile = false, fileSystem = fileSystem)

    private val forceTransparentWhite by option(
        "--force-transparent-white",
        help = "Convert transparent black (#00000000) pixels${NEXT_LINE}" +
            "to transparent white pixels (#00FFFFFF)."
    ).flag()

    private val densityOptions by DensityOptions()

    private val dimensionOptions by DimensionOptions()

    private val dryRun by option("--dry-run", hidden = true).flag()

    init {
        configureHelpFormatter()
    }

    override fun run() {
        val densities = readDensityFlags().also {
            if (it.isEmpty()) throw UsageError("At least one density must be specified.")
        }

        if (dryRun) return

        try {
            for (sourceFile in source) {
                transcode(sourceFile, densities)
            }
        } catch (e: TranscoderException) {
            System.err.println(e.exception?.localizedMessage ?: e.localizedMessage)
            exitProcess(2)
        }
    }

    private fun configureHelpFormatter() {
        context {
            helpFormatter = { context ->
                MordantHelpFormatter(context, showDefaultValues = true)
            }
            localization = object : Localization {

                override fun helpOptionMessage() = super.helpOptionMessage().let { message ->
                    if (message.last() == '.') message else "$message."
                }
            }
        }
    }

    private fun readDensityFlags(): EnumSet<Density> =
        EnumSet.noneOf(Density::class.java).apply {
            with(densityOptions) {
                if (ldpi) add(Density.LOW)
                if (mdpi) add(Density.MEDIUM)
                if (hdpi) add(Density.HIGH)
                if (xhdpi) add(Density.X_HIGH)
                if (xxhdpi) add(Density.XX_HIGH)
                if (xxxhdpi) add(Density.XXX_HIGH)
            }
        }

    private fun transcode(sourcePath: Path, densities: EnumSet<Density>) {
        val (width, height) = dimensionOptions
        sourcePath.inputStream().use { inputStream ->
            WebPTranscoder(densities, width, height, forceTransparentWhite)
                .transcode(
                    TranscoderInput(inputStream),
                    WebPTranscoder.Output(
                        directory = destination ?: sourcePath.parent,
                        fileName = sourcePath.nameWithoutExtension
                    )
                )
        }
    }

    private class DensityOptions : OptionGroup(
        name = "Density options",
        help = "You can enable or disable generation of a version for a specific pixel density" +
            " by specifying one or more of the options below."
    ) {

        val ldpi by densityOption(Density.LOW, defaultValue = false)
        val mdpi by densityOption(Density.MEDIUM, defaultValue = true)
        val hdpi by densityOption(Density.HIGH, defaultValue = true)
        val xhdpi by densityOption(Density.X_HIGH, defaultValue = true)
        val xxhdpi by densityOption(Density.XX_HIGH, defaultValue = true)
        val xxxhdpi by densityOption(Density.XXX_HIGH, defaultValue = true)

        private fun densityOption(
            density: Density,
            defaultValue: Boolean
        ) = option("--$density")
            .flag(
                "--no-$density",
                default = defaultValue,
                defaultForHelp = if (defaultValue) "generated" else "not generated"
            )
    }

    private class DimensionOptions : OptionGroup(
        name = "Dimension options",
        help = buildString {
            append("The desired width and height (in density-independent pixels) of the generated ")
            append("images can be set via the options below.")
            append(NEXT_LINE)
            append("If not set explicitly, the size will be determined by the 'width' and 'height'")
            append(" attributes of the 'svg' element, or by the 'viewBox' attribute if these are")
            append(" not set.")
            append(NEXT_LINE)
            append("If only one of the two is set, then the other one will be computed")
            append(" with respect to the original aspect ratio.")
        }
    ) {

        val width by dimensionOption("width")
        val height by dimensionOption("height")

        operator fun component1() = width
        operator fun component2() = height

        private fun dimensionOption(name: String) =
            option(
                "--$name",
                help = "${name.replaceFirstChar(Char::titlecase)} in dp.",
                metavar = "<int>"
            ).uint().convert { it.toFloat() }
    }

    private fun ProcessedArgument<String, String>.filesOnlyWithExpandedDirectoryContents()
        : ProcessedArgument<Collection<Path>, String> =
        transformAll(nvalues = -1, required = true) { pathStrings ->
            hashSetOf<Path>().apply {
                for (pathString in pathStrings) {
                    val path = fileSystem.getPath(pathString).absolute()

                    with(context.localization) {
                        if (!path.exists())
                            fail(pathDoesNotExist(pathTypeOther(), path.pathString))
                        if (!path.isReadable())
                            fail(pathIsNotReadable(pathTypeOther(), path.pathString))
                    }

                    if (path.isDirectory()) {
                        path.useDirectoryEntries("*.svg", this::addAll)
                    } else {
                        add(path)
                    }
                }
            }
        }

    private fun ProcessedArgument<Collection<Path>, *>.validateSource()
        : ArgumentDelegate<Collection<Path>> =
        validate { files ->
            val nonSvgFiles = files.filterNot { it.extension == "svg" }
            if (nonSvgFiles.isNotEmpty()) {
                val multipleOccurrences = nonSvgFiles.size > 1
                fail(
                    buildString {
                        append("The following ")
                        append(if (multipleOccurrences) "files were" else "file was")
                        append(" not recognized as having the expected extension (.svg):")
                        append(newLine)
                        append(
                            if (multipleOccurrences)
                                nonSvgFiles.joinToString(separator = newLine)
                            else nonSvgFiles[0].pathString
                        )
                    }
                )
            }
        }
}
