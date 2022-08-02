@file:JvmName("Rasterizer")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
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

class Rasterize : CliktCommand(name = "rasterize", printHelpOnEmptyArgs = true) {

    private val sourceFiles by argument("SOURCE")
        .filesOnlyWithExpandedDirectoryContents()
        .validateSource()

    private val destination by option(
        "-d",
        "--destination",
        help = "Set the location of the generated WebP files\u0085(must be a directory)",
        metavar = "DEST"
    ).file(canBeFile = false)

    private val densityOptions by DensityOptions()

    init {
        context { helpFormatter = CliktHelpFormatter(colSpacing = 3, showDefaultValues = true) }
    }

    override fun run() {
        val densities = readDensityFlags().also {
            if (it.isEmpty()) throw UsageError("At least one density must be specified")
        }

        try {
            if (sourceFiles.size == 1) {
                transcode(sourceFiles[0], densities)
            } else {
                for (sourceFile in sourceFiles) transcode(sourceFile, densities)
            }
        } catch (e: Exception) {
            if (e is PreRasterizationException || e is TranscoderException) {
                System.err.println(
                    (e as? TranscoderException)?.exception?.localizedMessage ?: e.localizedMessage
                )
                exitProcess(1)
            } else throw e
        }
    }

    private fun readDensityFlags(): EnumSet<Density> =
        EnumSet.noneOf(Density::class.java).apply {
            with(densityOptions) {
                if (ldpi) add(Density.LOW)
                if (mdpi) add(Density.MEDIUM)
                if (hdpi) add(Density.HIGH)
                if (xhdpi) add(Density.EXTRA_HIGH)
                if (xxhdpi) add(Density.EXTRA_EXTRA_HIGH)
                if (xxxhdpi) add(Density.EXTRA_EXTRA_EXTRA_HIGH)
            }
        }

    private fun transcode(sourceFile: File, densities: EnumSet<Density>) {
        val svgDocument = createSvgDocument(sourceFile.inputStream())
            ?: throw PreRasterizationException("'${sourceFile.path}' could not be interpreted as an SVG document")

        val outputDirectory = destination?.also { directory ->
            if (!directory.exists() && !directory.mkdirs())
                throw DirectoryCreationException(directory)
        } ?: sourceFile.parentFile

        WebPTranscoder(densities).transcode(
            TranscoderInput(svgDocument),
            WebPTranscoder.Output(
                directory = outputDirectory,
                fileName = sourceFile.nameWithoutExtension
            )
        )
    }

    private class DensityOptions : OptionGroup(
        name = "Density options",
        help = "Enable/disable generation of a version for a specific pixel density"
    ) {

        val ldpi by densityOption(Density.LOW, "low", defaultValue = false)
        val mdpi by densityOption(Density.MEDIUM, "medium", defaultValue = true)
        val hdpi by densityOption(Density.HIGH, "high", defaultValue = true)
        val xhdpi by densityOption(Density.EXTRA_HIGH, "extra-high", defaultValue = true)
        val xxhdpi by densityOption(Density.EXTRA_EXTRA_HIGH, "extra-extra-high", defaultValue = true)
        val xxxhdpi by densityOption(Density.EXTRA_EXTRA_EXTRA_HIGH, "extra-extra-extra-high", defaultValue = true)

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
                    val lineSeparator = System.lineSeparator()
                    append("The following ")
                    append(if (multipleOccurrences) "files were" else "file was")
                    append(" not recognized as having the expected extension (.$FILE_EXTENSION_SVG):")
                    append(lineSeparator)
                    append(
                        if (multipleOccurrences)
                            nonSvgFiles.joinToString(separator = lineSeparator) { file -> file.path }
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
