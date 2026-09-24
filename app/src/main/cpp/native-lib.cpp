#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_kisisel_1asistan_NativeBridge_testMesaj(JNIEnv* env, jobject /* this */) {
    std::string mesaj = "Native kopru calisiyor!";
    return env->NewStringUTF(mesaj.c_str());
}
