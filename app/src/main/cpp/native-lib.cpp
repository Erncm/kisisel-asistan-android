#include <jni.h>
#include <string>
#include <vector>
#include "llama.h"

static llama_model* g_model = nullptr;
static llama_context* g_ctx = nullptr;

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_kisisel_1asistan_NativeBridge_testMesaj(JNIEnv* env, jobject) {
    return env->NewStringUTF("Native kopru calisiyor!");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_kisisel_1asistan_NativeBridge_modelYukle(JNIEnv* env, jobject, jstring modelYolu) {
    const char* yol = env->GetStringUTFChars(modelYolu, nullptr);

    llama_backend_init();

    llama_model_params modelParams = llama_model_default_params();
    modelParams.n_gpu_layers = 0;

    g_model = llama_model_load_from_file(yol, modelParams);
    env->ReleaseStringUTFChars(modelYolu, yol);

    if (g_model == nullptr) {
        return JNI_FALSE;
    }

    llama_context_params ctxParams = llama_context_default_params();
    ctxParams.n_ctx = 2048;
    ctxParams.n_threads = 4;
    ctxParams.n_threads_batch = 4;

    g_ctx = llama_init_from_model(g_model, ctxParams);

    if (g_ctx == nullptr) {
        llama_model_free(g_model);
        g_model = nullptr;
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_kisisel_1asistan_NativeBridge_yanitUret(JNIEnv* env, jobject, jstring girdiMetni) {
    if (g_model == nullptr || g_ctx == nullptr) {
        return env->NewStringUTF("[Hata: model yuklu degil]");
    }

    const char* girdi = env->GetStringUTFChars(girdiMetni, nullptr);
    const llama_vocab* vocab = llama_model_get_vocab(g_model);

    std::vector<llama_token> tokenler(512);
    int tokenSayisi = llama_tokenize(vocab, girdi, (int)strlen(girdi), tokenler.data(), (int)tokenler.size(), true, true);
    env->ReleaseStringUTFChars(girdiMetni, girdi);

    if (tokenSayisi < 0) {
        return env->NewStringUTF("[Hata: tokenizasyon basarisiz]");
    }
    tokenler.resize(tokenSayisi);

    llama_batch batch = llama_batch_get_one(tokenler.data(), tokenSayisi);

    std::string sonuc;
    int uretilenTokenSayisi = 0;
    const int maxYeniToken = 128;

    while (uretilenTokenSayisi < maxYeniToken) {
        if (llama_decode(g_ctx, batch) != 0) {
            break;
        }

        auto* logitler = llama_get_logits_ith(g_ctx, batch.n_tokens - 1);
        int vocabBoyutu = llama_vocab_n_tokens(vocab);

        llama_token enIyiToken = 0;
        float enIyiSkor = logitler[0];
        for (int i = 1; i < vocabBoyutu; i++) {
            if (logitler[i] > enIyiSkor) {
                enIyiSkor = logitler[i];
                enIyiToken = i;
            }
        }

        if (llama_vocab_is_eog(vocab, enIyiToken)) {
            break;
        }

        char tamponMetin[256];
        int uzunluk = llama_token_to_piece(vocab, enIyiToken, tamponMetin, sizeof(tamponMetin), 0, true);
        if (uzunluk > 0) {
            sonuc.append(tamponMetin, uzunluk);
        }

        llama_token yeniTokenDizisi[1] = { enIyiToken };
        batch = llama_batch_get_one(yeniTokenDizisi, 1);

        uretilenTokenSayisi++;
    }

    return env->NewStringUTF(sonuc.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_kisisel_1asistan_NativeBridge_modelKapat(JNIEnv*, jobject) {
    if (g_ctx != nullptr) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model != nullptr) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
}
