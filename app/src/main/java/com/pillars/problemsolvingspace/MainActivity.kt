package com.pillars.problemsolvingspace

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.mahmoud.aesbio.KeyManager
import com.mahmoud.aesbio.KeyManagerListener
import com.pillars.problemsolvingspace.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var keyManagerListener = object : KeyManagerListener {
        override fun onUserAuthenticated(decryptedData: String) {
            println("this is the decrypted data $decryptedData")
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val keyManager =
            KeyManager(fragmentActivity = this, listener = keyManagerListener)
        keyManager.encryptData(keyAlias = "alias", dataToEncrypt = "1234 4321 1234 4321")

        binding.button.setOnClickListener {
            keyManager.authenticateUser()
        }
    }

}
