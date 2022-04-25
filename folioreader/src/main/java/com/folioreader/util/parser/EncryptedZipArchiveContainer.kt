package com.folioreader.util.parser

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import org.readium.r2.streamer.container.Container
import org.zeroturnaround.zip.commons.IOUtils
import java.io.*
import java.net.URI
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

interface EncryptedZipArchiveContainer : Container {

    val zipFile: ZipFile

    override fun data(relativePath: String): ByteArray {

        val inputStream = getEntry(relativePath)
        val outputStream = ByteArrayOutputStream()
        var readLength = 0
        val buffer = ByteArray(16384)

        while (inputStream.read(buffer).let { readLength = it; it != -1 })
            outputStream.write(buffer, 0, readLength)

        inputStream.close()
        return outputStream.toByteArray()
    }

    override fun dataLength(relativePath: String): Long {
            return zipFile.size().toLong()
    }

    override fun dataInputStream(relativePath: String): InputStream {
        return getEntry(relativePath)
    }

    fun getEntry(relativePath: String): InputStream {

        val path: String = try {
            URI(relativePath).path
        } catch (e: Exception) {
            relativePath
        }

        var zipEntry = zipFile.getEntry(path)
        if (zipEntry == null) {
            val zipEntries = zipFile.entries()
            while (zipEntries.hasMoreElements()) {
                zipEntry = zipEntries.nextElement()
                if (path.equals(zipEntry.name, true))
                    break
            }
        }

        return zipFile.getInputStream(zipEntry)
    }

    val context: Context
}

