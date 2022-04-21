package com.mahmoud.aesbio


import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.nio.charset.Charset
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class KeyManager(
    private val fragmentActivity: FragmentActivity,
    private val listener: KeyManagerListener
) {
    private lateinit var cipherEnc: Cipher
    private lateinit var cipherDec: Cipher
    private lateinit var encryptedBytes: ByteArray

    private lateinit var keyAlias: String
    private lateinit var dataToEncrypt: String

    private var callbackDec: BiometricPrompt.AuthenticationCallback =
        @RequiresApi(Build.VERSION_CODES.P)
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {

                val cipher = result.cryptoObject?.cipher
                if (cipher != null) {
                    cipherDec = cipher
                }
                decrypt()
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
            }

            override fun onAuthenticationFailed() {}

        }

    private var callbackEnc: BiometricPrompt.AuthenticationCallback =
        @RequiresApi(Build.VERSION_CODES.P)
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                encrypt()
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
            }

            override fun onAuthenticationFailed() {}

        }


    @RequiresApi(Build.VERSION_CODES.M)
    fun encryptData(keyAlias: String, dataToEncrypt: String) {
        this.keyAlias = keyAlias
        this.dataToEncrypt = dataToEncrypt
        initializeKeyGenParameterSpec()
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun initializeKeyGenParameterSpec() {
        val keySpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(KeyConfigConstants.KEY_SIZE_IN_BITS)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)
            .build()

        generateKey(keySpec)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun generateKey(keySpec: KeyGenParameterSpec) {
        val keygen = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KeyConfigConstants.ANDROID_KEYSTORE
        ).apply { init(keySpec) }
        val key: SecretKey = keygen.generateKey()
        createCiphers(key)
    }

    private fun createCiphers(key: Key) {
        cipherEnc = Cipher.getInstance(KeyConfigConstants.CIPHER_TRANSFORMATION)
            .apply { init(Cipher.ENCRYPT_MODE, key) }

        cipherDec = Cipher.getInstance(KeyConfigConstants.CIPHER_TRANSFORMATION)
            .apply { init(Cipher.DECRYPT_MODE, key, IvParameterSpec(cipherEnc.iv)) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            initializeBiometricPromptForEnc()
        }

    }

    private fun encrypt() {
        val unencryptedBytes =
            dataToEncrypt.toByteArray(Charset.forName("UTF-8"))
        encryptedBytes = cipherEnc.doFinal(unencryptedBytes)
        var encryptedString = ""
        for (item in encryptedBytes) {
            encryptedString += item
        }
    }

    fun authenticateUser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            initializeBiometricPromptForDec()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun initializeBiometricPromptForEnc() {
        val biometricPrompt =
            BiometricPrompt(fragmentActivity, fragmentActivity.mainExecutor, callbackEnc)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("We need your fingerprint to encrypt this sensitive data")
            .setNegativeButtonText("cancel")
            .build()

        biometricPrompt.authenticate(
            promptInfo,
            BiometricPrompt.CryptoObject(cipherEnc)
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun initializeBiometricPromptForDec() {
        val biometricPrompt =
            BiometricPrompt(fragmentActivity, fragmentActivity.mainExecutor, callbackDec)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("This data is sensitive we need your fingerprint")
            .setNegativeButtonText("cancel")
            .build()

        biometricPrompt.authenticate(
            promptInfo,
            BiometricPrompt.CryptoObject(cipherDec)
        )
    }

    private fun decrypt() {
        val unencryptedBytes = cipherDec.doFinal(encryptedBytes)
        var decryptedString = String(unencryptedBytes)
        listener.onUserAuthenticated(decryptedString)
    }


}
