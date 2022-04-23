package com.folioreader.util.parser

import android.content.Context
import org.readium.r2.shared.Link
import org.readium.r2.shared.RootFile
import org.readium.r2.shared.drm.Drm
import org.readium.r2.shared.parser.xml.XmlParser
import org.readium.r2.streamer.container.EpubContainer
import org.readium.r2.streamer.parser.lcplFilePath
import org.readium.r2.streamer.parser.mimetype
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class EncryptedContainerEpub(override val context: Context, path: String) : EpubContainer, EncryptedZipArchiveContainer {

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
    override var zipFile = File(path)
    override var drm: Drm? = null
    override var successCreated: Boolean = false

    init {

        if (File(path).exists()) {
            successCreated = true
        }
    }

    override fun scanForDrm(): Drm? {

//        val zis = ZipInputStream(fis)
//
//        var entry = zis.nextEntry
//        while (entry != null) {
//            if (entry.name == lcplFilePath) return Drm(Drm.Brand.Lcp)
//            entry = zis.nextEntry
//        }

        return null
    }
}