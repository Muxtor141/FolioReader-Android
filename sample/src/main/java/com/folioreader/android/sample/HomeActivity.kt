/*
 * Copyright (C) 2016 Pedro Paulo de Amorim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.folioreader.android.sample

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.folioreader.Config
import com.folioreader.FolioReader
import com.folioreader.FolioReader.OnClosedListener
import com.folioreader.android.sample.HomeActivity
import com.folioreader.model.HighLight
import com.folioreader.model.HighLight.HighLightAction
import com.folioreader.model.locators.ReadLocator
import com.folioreader.model.locators.ReadLocator.Companion.fromJson
import com.folioreader.util.AppUtil.Companion.getSavedConfig
import com.folioreader.util.OnHighlightListener
import com.folioreader.util.ReadLocatorListener
import kotlinx.android.synthetic.main.activity_home.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class HomeActivity : AppCompatActivity(), OnHighlightListener, ReadLocatorListener,
    OnClosedListener {

    private lateinit var folioReader: FolioReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        folioReader = FolioReader.get()
            .setOnHighlightListener(this)
            .setReadLocatorListener(this)
            .setOnClosedListener(this)
            .setConfig(Config().setThemeColorRes(R.color.colorPrimary), true)
        highlightsAndSave

        parseBooks()
        root.setOnClickListener {
            parseBooks()
        }
    }

    private fun parseBooks() {
        books.removeAllViews()
        filesDir.listFiles()?.forEach { book ->
            val textView = TextView(this)
            textView.text = book.name
            textView.setOnClickListener {

                var config = getSavedConfig(applicationContext)
                if (config == null) config = Config()
                config.allowedDirection = Config.AllowedDirection.VERTICAL_AND_HORIZONTAL

                folioReader.setConfig(config, true)
                    .openBook(book.path)
            }
            books.addView(textView)
        }
    }

    private val lastReadLocator: ReadLocator?
        private get() {
            val jsonString =
                loadAssetTextAsString("Locators/LastReadLocators/last_read_locator_1.json")
            return fromJson(jsonString)
        }

    override fun saveReadLocator(readLocator: ReadLocator) {
        Log.i(LOG_TAG, "-> saveReadLocator -> " + readLocator.toJson())
    }//You can do anything on successful saving highlight list

    /*
       * For testing purpose, we are getting dummy highlights from asset. But you can get highlights from your server
       * On success, you can save highlights to FolioReader DB.
       */
    private val highlightsAndSave: Unit
        private get() {
            Thread {
                var highlightList: ArrayList<HighLight?>? = null
                val objectMapper = ObjectMapper()
                try {
                    highlightList = objectMapper.readValue(
                        loadAssetTextAsString("highlights/highlights_data.json"),
                        object : TypeReference<List<HighlightData?>?>() {})
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                if (highlightList == null) {
                    folioReader!!.saveReceivedHighLights(highlightList) {
                        //You can do anything on successful saving highlight list
                    }
                }
            }.start()
        }

    private fun loadAssetTextAsString(name: String): String? {
        var `in`: BufferedReader? = null
        try {
            val buf = StringBuilder()
            val `is` = assets.open(name)
            `in` = BufferedReader(InputStreamReader(`is`))
            var str: String?
            var isFirst = true
            while (`in`.readLine().also { str = it } != null) {
                if (isFirst) isFirst = false else buf.append('\n')
                buf.append(str)
            }
            return buf.toString()
        } catch (e: IOException) {
            Log.e("HomeActivity", "Error opening asset $name")
        } finally {
            if (`in` != null) {
                try {
                    `in`.close()
                } catch (e: IOException) {
                    Log.e("HomeActivity", "Error closing asset $name")
                }
            }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        FolioReader.clear()
    }

    override fun onHighlight(highlight: HighLight, type: HighLightAction) {
//        Toast.makeText(this,
//                "highlight id = " + highlight.getUUID() + " type = " + type,
//                Toast.LENGTH_SHORT).show();
    }

    override fun onFolioReaderClosed() {
        Log.v(LOG_TAG, "-> onFolioReaderClosed")
    }

    companion object {
        private val LOG_TAG = HomeActivity::class.java.simpleName
    }
}