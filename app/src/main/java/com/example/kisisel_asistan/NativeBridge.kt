package com.example.kisisel_asistan

object NativeBridge {
    init {
        System.loadLibrary("kisiselasistan_native")
    }
    external fun testMesaj(): String
    external fun modelYukle(modelYolu: String): Boolean
    external fun yanitUret(girdiMetni: String): String
    external fun modelKapat()
}
