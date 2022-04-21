package com.mahmoud.aesbio

import javax.crypto.Cipher

interface KeyManagerListener {
    fun onUserAuthenticated(decryptedData: String)
}
