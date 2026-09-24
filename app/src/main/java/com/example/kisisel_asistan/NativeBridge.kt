package com.example.kisisel_asistan

object NativeBridge {
    init {
        System.loadLibrary("kisiselasistan_native")
    }
    external fun testMesaj(): String
}
