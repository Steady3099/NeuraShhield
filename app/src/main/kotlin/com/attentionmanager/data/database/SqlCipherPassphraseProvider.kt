package com.attentionmanager.data.database

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SqlCipherPassphraseProvider(private val context: Context) {
    private val keyAlias = "neurashhield_db_key"
    private val keyFile: File
        get() = File(context.noBackupFilesDir, "attention_db_key.bin")

    fun getPassphrase(): ByteArray = synchronized(this) {
        if (!keyFile.exists()) {
            val rawPassphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
            keyFile.parentFile?.mkdirs()
            keyFile.writeBytes(encrypt(rawPassphrase))
            rawPassphrase
        } else {
            decrypt(keyFile.readBytes())
        }
    }

    private fun encrypt(value: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(value)
        return byteArrayOf(iv.size.toByte()) + iv + encrypted
    }

    private fun decrypt(value: ByteArray): ByteArray {
        val ivSize = value.first().toInt()
        val iv = value.copyOfRange(1, 1 + ivSize)
        val encrypted = value.copyOfRange(1 + ivSize, value.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.getKey(keyAlias, null)?.let { return it as SecretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }
}
