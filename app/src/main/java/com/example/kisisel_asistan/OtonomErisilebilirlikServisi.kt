package com.example.kisisel_asistan

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class OtonomErisilebilirlikServisi : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
