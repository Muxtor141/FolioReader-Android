package com.folioreader.android.sample

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.File

fun encrypt(context: Context) {
    val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
    val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)

    val file = File(context.filesDir, "encrypted.epub")
    if (file.exists()) {
        file.delete()
    }

    val encryptedFile = EncryptedFile.Builder(
        File(context.filesDir, "encrypted.epub"),
        context,
        mainKeyAlias,
        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
    ).build()

    val originalFile = File(context.filesDir, "original.epub")

    encryptedFile.openFileOutput().apply {
        write(originalFile.readBytes())
//        flush()
//        close()
    }
}