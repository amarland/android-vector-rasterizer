package com.amarland.androidvectorrasterizer

import com.github.ajalt.clikt.core.UsageError
import com.google.common.jimfs.Jimfs
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.FileSystem
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.pathString

class RasterizeCommandTest {

    @Test
    fun `fails if none of the source files have the 'svg' extension`() {
        assertThrowsUsageError(listOf(sourceFileWithInvalidExtension))
    }

    @Test
    fun `fails if all but at least one of the source files have the 'svg' extension`() {
        assertThrowsUsageError(sourceFilesWithValidExtension + sourceFileWithInvalidExtension)
    }

    @Test
    fun `fails if any of the source files does not exist`() {
        assertThrowsUsageError(listOf("./non-existing-source.svg"))
    }

    @Test
    fun `fails if all density flags are false`() {
        assertThrowsUsageError(
            Density.values().map { density ->
                "--no-${density.qualifier}"
            } + sourceFilesWithValidExtension
        )
    }

    @Test
    fun `directories passed as source are 'transformed' into a list of the SVG files they contain`() {
        RasterizeCommand(fileSystem).run {
            parse(listOf(sourceDirectory.pathString, "--dry-run"))

            assertTrue(
                source.all { file -> file.absolutePathString() in sourceFilesWithValidExtension }
            )
        }
    }

    private fun assertThrowsUsageError(arguments: List<String>) =
        assertThrows<UsageError> {
            RasterizeCommand().parse(arguments + "--dry-run")
        }

    private companion object {

        @JvmStatic
        lateinit var sourceDirectory: Path

        @JvmStatic
        lateinit var sourceFilesWithValidExtension: List<String>

        @JvmStatic
        lateinit var sourceFileWithInvalidExtension: String

        @JvmStatic
        lateinit var fileSystem: FileSystem

        @BeforeAll
        @JvmStatic
        fun setUp() {
            fileSystem = Jimfs.newFileSystem()

            sourceDirectory = fileSystem.getPath("source")
                .createDirectory()

            sourceFilesWithValidExtension = List(3) { index -> "source-$index.svg" }
                .map { fileName ->
                    sourceDirectory.resolve(fileName)
                        .createFile()
                        .absolutePathString()
                }

            sourceFileWithInvalidExtension = sourceDirectory.resolve("source.png")
                .createFile()
                .absolutePathString()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            fileSystem.close()
        }
    }
}
