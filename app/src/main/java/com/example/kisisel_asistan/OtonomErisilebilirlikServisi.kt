package com.example.kisisel_asistan

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class OtonomErisilebilirlikServisi : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Otomatik tıklama ve mesaj gönderme işlemleri burada yapılacak
    }

    override fun onInterrupt() {}
}
