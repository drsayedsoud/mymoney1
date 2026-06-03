package com.example.util

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build

class DeviceAuthenticator {
    
    fun isDeviceSecure(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.isDeviceSecure
    }

    fun createAuthenticationIntent(context: Context): Intent? {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.createConfirmDeviceCredentialIntent(
            "تأكيد الهوية",
            "يرجى التحقق من بصمتك أو رمز الدخول لفتح التطبيقات الحساسة"
        )
    }
}
