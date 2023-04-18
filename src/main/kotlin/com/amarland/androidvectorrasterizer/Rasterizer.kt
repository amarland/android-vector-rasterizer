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
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.output.HelpFormatter
import com.github.ajalt.clikt.output.Localization
import com.github.ajalt.clikt.parameters.arguments.ArgumentDelegate
import com.github.ajalt.clikt.parameters.arguments.ProcessedArgument
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.float
import org.apache.batik.anim.dom.SAXSVGDocumentFactory
import org.apache.batik.transcoder.TranscoderException
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.util.XMLResourceDescriptor
import org.w3c.dom.Document
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.EnumSet
import kotlin.system.exitProcess

private const val FILE_EXTENSION_SVG = "svg"

private val NEW_LINE = System.lineSeparator()
private const val NEXT_LINE = '\u0085'

class Rasterizer : CliktCommand(name = "rasterize", printHelpOnEmptyArgs = true) {

    private val source by argument("<source>")
        .filesOnlyWithExpandedDirectoryContents()
        .validateSource()

    private val destination by option(
        "-d",
        "--destination",
        help = "Location of the generated WebP files$NEXT_LINE(must be a directory).",
        metavar = "<dir>"
    ).file(canBeFile = false)

    private val forceTransparentWhite by option(
        "--force-transparent-white",
        help = "Convert transparent black (#00000000) pixels${NEXT_LINE}to white transparent pixels (#00FFFFFF).",
        hidden = true
    ).flag()

    private val densityOptions by DensityOptions()

    private val dimensionOptions by DimensionOptions()

    init {
        configureHelpFormatter()
    }

    override fun run() {
        val densities = readDensityFlags().also {
            if (it.isEmpty()) throw UsageError("At least one density must be specified.")
        }

        try {
            if (source.size == 1) {
                transcode(source[0], densities)
            } else {
                for (sourceFile in source) transcode(sourceFile, densities)
            }
        } catch (e: PreRasterizationException) {
            System.err.println(e.localizedMessage)
            exitProcess(2)
        } catch (e: TranscoderException) {
            System.err.println(e.exception?.localizedMessage ?: e.localizedMessage)
            exitProcess(3)
        }
    }

    private fun configureHelpFormatter() {
        context {
            helpFormatter = object : CliktHelpFormatter(
                localization = object : Localization {

                    override fun optionsMetavar() = super.optionsMetavar().lowercase()
                },
                colSpacing = 4,
                showDefaultValues = true
            ) {

                override fun formatHelp(
                    prolog: String,
                    epilog: String,
                    parameters: List<HelpFormatter.ParameterHelp>,
                    programName: String
                ) = super.formatHelp(prolog, epilog, parameters, programName)
                    .lineSequence()
                    .withIndex().let { linesWithIndex ->
                        buildString {
                            var optionsTitleIndex = Int.MAX_VALUE

                            for ((index, line) in linesWithIndex) {
                                val isPastOptionsTitle = index > optionsTitleIndex
                                val isHelpOptionLine = isPastOptionsTitle &&
                                    currentContext.helpOptionNames.last() in line

                                append(line)

                                if (!isHelpOptionLine) appendLine()
                                if (line == localization.optionsTitle()) optionsTitleIndex = index
                                if (index == optionsTitleIndex ||
                                    isPastOptionsTitle && line.isNotEmpty() && line.last() == '.' && !isHelpOptionLine
                                ) {
                                    appendLine()
                                }
                            }
                        }
                    }
            }
            localization = object : Localization {

                override fun helpOptionMessage() = "${super.helpOptionMessage()}."
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

    private fun transcode(sourceFile: File, densities: EnumSet<Density>) {
        val svgDocument = createSvgDocument(sourceFile.inputStream())
            ?: throw PreRasterizationException(
                "'${sourceFile.path}' could not be interpreted as an SVG document."
            )

        val outputDirectory = destination?.also { directory ->
            if (!directory.exists() && !directory.mkdirs())
                throw DirectoryCreationException(directory)
        } ?: sourceFile.parentFile

        val (width, height) = dimensionOptions
        WebPTranscoder(densities, width, height, forceTransparentWhite).transcode(
            TranscoderInput(svgDocument),
            WebPTranscoder.Output(
                directory = outputDirectory,
                fileName = sourceFile.nameWithoutExtension
            )
        )
    }

    private class DensityOptions : OptionGroup(
        name = "Density options",
        help = "You can enable or disable generation of a version for a specific pixel density" +
            " by specifying one or more of the options below."
    ) {

        val ldpi by densityOption(Density.LOW, "ldpi", defaultValue = false)
        val mdpi by densityOption(Density.MEDIUM, "mdpi", defaultValue = true)
        val hdpi by densityOption(Density.HIGH, "hdpi", defaultValue = true)
        val xhdpi by densityOption(Density.X_HIGH, "xhdpi", defaultValue = true)
        val xxhdpi by densityOption(Density.XX_HIGH, "xxhdpi", defaultValue = true)
        val xxxhdpi by densityOption(Density.XXX_HIGH, "xxxhdpi", defaultValue = true)

        private fun densityOption(
            density: Density,
            help: String,
            defaultValue: Boolean
        ) = option("--$density", help = help)
            .flag(
                "--no-$density",
                default = defaultValue,
                defaultForHelp = if (defaultValue) "enabled" else "disabled"
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
                metavar = "<float>"
            ).float()
    }
}

private fun ProcessedArgument<String, String>.filesOnlyWithExpandedDirectoryContents()
    : ProcessedArgument<List<File>, String> =
    transformAll(nvalues = -1, required = true) { pathStrings ->
        pathStrings.flatMap { pathString ->
            val file = File(pathString)
            val files = if (file.isDirectory)
                file.walk().filter { it.isFile }.asIterable()
            else listOf(file)

            with(context.localization) {
                if (!file.exists())
                    fail(pathDoesNotExist(pathTypeOther(), file.path))
                if (!file.canRead())
                    fail(pathIsNotReadable(pathTypeOther(), file.path))
            }

            return@flatMap files
        }
    }

private fun ProcessedArgument<List<File>, *>.validateSource(): ArgumentDelegate<List<File>> =
    validate { files ->
        val nonSvgFiles = files.filterNot { it.extension == FILE_EXTENSION_SVG }
        if (nonSvgFiles.isNotEmpty()) {
            val multipleOccurrences = nonSvgFiles.size > 1
            fail(
                buildString {
                    append("The following ")
                    append(if (multipleOccurrences) "files were" else "file was")
                    append(" not recognized as having the expected extension (.")
                    append(FILE_EXTENSION_SVG)
                    append("):")
                    append(NEW_LINE)
                    append(
                        if (multipleOccurrences)
                            nonSvgFiles.joinToString(separator = NEW_LINE) { file -> file.path }
                        else nonSvgFiles[0].path
                    )
                }
            )
        }
    }

private fun createSvgDocument(inputStream: InputStream): Document? =
    try {
        SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName())
            .createDocument(null, inputStream)
    } catch (ioe: IOException) {
        null
    }
