package com.folioreader.util.parser

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import org.readium.r2.streamer.container.Container
import org.zeroturnaround.zip.commons.IOUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import java.util.zip.ZipInputStream

interface EncryptedZipArchiveContainer : Container {
    var zipFile: File

    val fis: FileInputStream
        get() {
            val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
            val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)

            val encryptedFile = EncryptedFile.Builder(
                zipFile,
                context,
                mainKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            return encryptedFile.openFileInput()
        }

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
        return zipFile.length()
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

        val zis = ZipInputStream(fis)

        var entry = zis.nextEntry
        while (entry != null) {
            if (path.equals(entry.name, true))
                return zis
            entry = zis.nextEntry
        }

        return zis
    }

    val context: Context
}

