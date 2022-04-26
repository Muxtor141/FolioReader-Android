package com.folioreader.util.parser

import android.content.Context
import android.util.Log
import org.readium.r2.shared.ContentLayoutStyle
import org.readium.r2.shared.drm.Drm
import org.readium.r2.shared.Encryption
import org.readium.r2.shared.LangType
import org.readium.r2.shared.Publication
import org.readium.r2.shared.parser.xml.XmlParser
import org.readium.r2.streamer.container.Container
import org.readium.r2.streamer.container.ContainerEpub
import org.readium.r2.streamer.container.ContainerEpubDirectory
import org.readium.r2.streamer.container.EpubContainer
import org.readium.r2.streamer.fetcher.forceScrollPreset
import org.readium.r2.streamer.fetcher.userSettingsUIPreset
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.PublicationParser
import org.readium.r2.streamer.parser.epub.EncryptionParser
import org.readium.r2.streamer.parser.epub.NCXParser
import org.readium.r2.streamer.parser.epub.NavigationDocumentParser
import org.readium.r2.streamer.parser.epub.OPFParser
import java.io.File

const val defaultEpubVersion = 1.2
const val containerDotXmlPath = "META-INF/container.xml"
const val encryptionDotXmlPath = "META-INF/encryption.xml"
const val lcplFilePath = "META-INF/license.lcpl"
const val mimetype = "application/epub+zip"
const val mimetypeOEBPS = "application/oebps-package+xml"
const val mediaOverlayURL = "media-overlay?resource="

class EncryptedEpubParser(private val context: Context) : PublicationParser {

    private val opfParser = OPFParser()
    private val ndp = NavigationDocumentParser()
    private val ncxp = NCXParser()
    private val encp = EncryptionParser()

    private fun generateContainerFrom(path: String): EpubContainer {
        val isDirectory = File(path).isDirectory

        if (!File(path).exists())
            throw Exception("Missing File")

        val container = when (isDirectory) {
            true -> ContainerEpubDirectory(path)
            false -> EncryptedContainerEpub(context, path)
        }
        if (!container.successCreated)
            throw Exception("Missing File")
        return container
    }

    fun parseEncryption(container: Container, publication: Publication, drm: Drm?): Pair<Container, Publication> {
        container.drm = drm
        fillEncryptionProfile(publication, drm)

        return Pair(container, publication)
    }

    override fun parse(fileAtPath: String, title: String): PubBox? {
        val container = try {
            generateContainerFrom(fileAtPath)
        } catch (e: Exception) {
            Log.e("Error", "Could not generate container", e)
            return null
        }
        val data = try {
            container.data(containerDotXmlPath)
        } catch (e: Exception) {
            Log.e("Error", "Missing File : META-INF/container.xml", e)
            return null
        }

        container.rootFile.mimetype = mimetype
        container.rootFile.rootFilePath = getRootFilePath(data)

        val xmlParser = XmlParser()

        val documentData = try {
            container.data(container.rootFile.rootFilePath)
        } catch (e: Exception) {
            Log.e("Error", "Missing File : ${container.rootFile.rootFilePath}", e)
            return null
        }

        xmlParser.parseXml(documentData.inputStream())

        val epubVersion = xmlParser.root().attributes["version"]!!.toDouble()
        val publication = opfParser.parseOpf(xmlParser, container.rootFile.rootFilePath, epubVersion)
                ?: return null

//        val drm = container.scanForDrm()
//
//        parseEncryption(container, publication, drm)

//        val fetcher = Fetcher(publication, container)
        parseNavigationDocument(container, publication)
        parseNcxDocument(container, publication)


        /*
         * This might need to be moved as it's not really about parsing the Epub
         * but it sets values needed (in UserSettings & ContentFilter)
         */
        setLayoutStyle(publication)

//        container.drm = drm

        File(context.cacheDir, "temp").delete()
        return PubBox(publication, container)
    }

    private fun getRootFilePath(data: ByteArray): String {
        val xmlParser = XmlParser()
        xmlParser.parseXml(data.inputStream())
        return xmlParser.getFirst("container")
                ?.getFirst("rootfiles")
                ?.getFirst("rootfile")
                ?.attributes?.get("full-path")
                ?: "content.opf"
    }

    private fun setLayoutStyle(publication: Publication) {
        var langType = LangType.other

        langTypeLoop@ for (lang in publication.metadata.languages) {
            when (lang) {
                "zh", "ja", "ko" -> {
                    langType = LangType.cjk
                    break@langTypeLoop
                }
                "ar", "fa", "he" -> {
                    langType = LangType.afh
                    break@langTypeLoop
                }
            }
        }

        val pageDirection = publication.metadata.direction
        val contentLayoutStyle = publication.metadata.contentLayoutStyle(langType, pageDirection)

        publication.cssStyle = contentLayoutStyle.name

        userSettingsUIPreset.get(ContentLayoutStyle.layout(publication.cssStyle as String))?.let {
            if (publication.type == Publication.TYPE.WEBPUB) {
                publication.userSettingsUIPreset = forceScrollPreset
            } else {
                publication.userSettingsUIPreset = it
            }
        }
    }

    private fun fillEncryptionProfile(publication: Publication, drm: Drm?): Publication {
        drm?.let {
            for (link in publication.resources) {
                if (link.properties.encryption?.scheme == it.scheme) {
                    link.properties.encryption?.profile = it.profile
                }
            }
            for (link in publication.readingOrder) {
                if (link.properties.encryption?.scheme == it.scheme) {
                    link.properties.encryption?.profile = it.profile
                }
            }
        }
        return publication
    }

    private fun parseEncryption(container: EpubContainer, publication: Publication, drm: Drm?) {
        val documentData = try {
            container.data(encryptionDotXmlPath)
        } catch (e: Exception) {
            return
        }
        val document = XmlParser()
        document.parseXml(documentData.inputStream())
        val encryptedDataElements = document.getFirst("encryption")?.get("EncryptedData") ?: return
        for (encryptedDataElement in encryptedDataElements) {
            val encryption = Encryption()
            val keyInfoUri = encryptedDataElement.getFirst("KeyInfo")?.getFirst("RetrievalMethod")?.let { it.attributes["URI"] }
            if (keyInfoUri == "license.lcpl#/encryption/content_key" && drm?.brand == Drm.Brand.Lcp)
                encryption.scheme = Drm.Scheme.Lcp
            encryption.algorithm = encryptedDataElement.getFirst("EncryptionMethod")?.let { it.attributes["Algorithm"] }
            encp.parseEncryptionProperties(encryptedDataElement, encryption)
            encp.add(encryption, publication, encryptedDataElement)
        }
    }

    private fun parseNavigationDocument(container: EpubContainer, publication: Publication) {
        val navLink = publication.linkWithRel("contents") ?: return

        val navDocument = try {
            container.xmlDocumentForResource(navLink)
        } catch (e: Exception) {
            Log.e("Error", "Navigation parsing", e)
            return
        }

        val navByteArray = try {
            container.xmlAsByteArray(navLink)
        } catch (e: Exception) {
            Log.e("Error", "Navigation parsing", e)
            return
        }

        ndp.navigationDocumentPath = navLink.href ?: return
        publication.tableOfContents.plusAssign(ndp.tableOfContent(navByteArray))
        publication.landmarks.plusAssign(ndp.landmarks(navDocument))
        publication.listOfAudioFiles.plusAssign(ndp.listOfAudiofiles(navDocument))
        publication.listOfIllustrations.plusAssign(ndp.listOfIllustrations(navDocument))
        publication.listOfTables.plusAssign(ndp.listOfTables(navDocument))
        publication.listOfVideos.plusAssign(ndp.listOfVideos(navDocument))
        publication.pageList.plusAssign(ndp.pageList(navDocument))
    }

    private fun parseNcxDocument(container: EpubContainer, publication: Publication) {
        val ncxLink = publication.resources.firstOrNull { it.typeLink == "application/x-dtbncx+xml" }
                ?: return
        val ncxDocument = try {
            container.xmlDocumentForResource(ncxLink)
        } catch (e: Exception) {
            Log.e("Error", "Ncx parsing", e)
            return
        }
        ncxp.ncxDocumentPath = ncxLink.href ?: return
        if (publication.tableOfContents.isEmpty())
            publication.tableOfContents.plusAssign(ncxp.tableOfContents(ncxDocument))
        if (publication.pageList.isEmpty())
            publication.pageList.plusAssign(ncxp.pageList(ncxDocument))
        return
    }

}
