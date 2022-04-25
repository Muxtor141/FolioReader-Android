package com.folioreader.util.parser

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import org.readium.r2.shared.Link
import org.readium.r2.shared.RootFile
import org.readium.r2.shared.drm.Drm
import org.readium.r2.shared.parser.xml.XmlParser
import org.readium.r2.streamer.container.EpubContainer
import org.readium.r2.streamer.parser.lcplFilePath
import org.readium.r2.streamer.parser.mimetype
import org.zeroturnaround.zip.ZipUtil
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class EncryptedContainerEpub(override val context: Context, private val path: String) : EpubContainer, EncryptedZipArchiveContainer {

    override fun xmlDocumentForFile(relativePath: String): XmlParser {
        val containerData = data(relativePath)
        val document = XmlParser()
        document.parseXml(containerData.inputStream())
        return document
    }

    override fun xmlAsByteArray(link: Link?): ByteArray {
        var pathFile = link?.href ?: throw Exception("Missing Link : ${link?.title}")
        if (pathFile.first() == '/')
            pathFile = pathFile.substring(1)

        return data(pathFile)
    }

    override fun xmlDocumentForResource(link: Link?): XmlParser {
        var pathFile = link?.href ?: throw Exception("Missing Link : ${link?.title}")
        if (pathFile.first() == '/')
            pathFile = pathFile.substring(1)
        return xmlDocumentForFile(pathFile)
    }

    override var rootFile = RootFile(path, mimetype)

    override var drm: Drm? = null
    override var successCreated: Boolean = false

    private val mainKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    override val zipFile: ZipFile

    init {
        if (File(path).exists()) {
            successCreated = true
        }

        val inputStream = EncryptedFile.Builder(
            File(path),
            context,
            mainKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build().openFileInput()

        val file = File(context.cacheDir, "temp")
        file.writeBytes(inputStream.readBytes())
        zipFile = ZipFile(file.path)
    }

    override fun scanForDrm(): Drm? {

        return null
    }
}