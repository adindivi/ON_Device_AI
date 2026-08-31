#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "llama_cpp/include/llama.h" // llama.cpp 경로 맞춤 조정

#define LOG_TAG "OndeviceAI_Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static llama_model* g_model = nullptr;
static llama_context* g_ctx = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_backend_LlamaNapiBridge_nativeInitModel(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPath
) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    if (!path || strlen(path) == 0) {
        LOGE("🔴 Error: Model path is null or empty. Bypassing load to prevent crash (Issue 3 mitigation).");
        if (path) env->ReleaseStringUTFChars(modelPath, path);
        return JNI_FALSE;
    }
    LOGI("Loading Real Qwen2.5 GGUF Model via Llama.cpp from: %s", path);

    // 이전 모델/컨텍스트가 있으면 먼저 해제 (메모리 누수 방지)
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }

    llama_backend_init();

    auto mparams = llama_model_default_params();
    // [GPU 부스터 완전 해제] 특정 안드로이드 폰(엑시노스 등)에서 Vulkan 드라이버 버그로 인해 
    // GPU에 1계층이라도 올리면 디코딩 중 멈추는 현상이 확인됨. 순수 CPU로만 돌리도록 0으로 설정.
    mparams.n_gpu_layers = 0;
    g_model = llama_model_load_from_file(path, mparams);

    if (!g_model) {
        LOGE("Failed to load model from %s", path);
        env->ReleaseStringUTFChars(modelPath, path);
        return JNI_FALSE;
    }

    auto cparams = llama_context_default_params();
    cparams.n_ctx = 1024; // Context 윈도우 크기
    cparams.n_batch = 1024; // 배치 크기도 넉넉하게
    cparams.n_ubatch = 1024;
    // [중요] 안드로이드 모바일 칩(빅리틀 코어) 스레드 꼬임 완전 방지 (가장 확실한 1개 스레드 강제)
    cparams.n_threads = 1; 
    cparams.n_threads_batch = 1; // 프롬프트 덩어리(청크)를 읽을 때 쓰는 스레드도 반드시 1개로 제한!
    g_ctx = llama_init_from_model(g_model, cparams);

    env->ReleaseStringUTFChars(modelPath, path);
    return g_ctx != nullptr ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_backend_LlamaNapiBridge_nativeGenerateResponseStream(
        JNIEnv* env,
        jobject /* this */,
        jstring promptText,
        jint maxTokens,
        jobject callback
) {
    // JNI Callback Method ID 획득 (에러 콜백용으로 먼저 준비)
    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");

    if (!g_model || !g_ctx) {
        LOGE("🔴 Model NOT loaded! g_model=%p, g_ctx=%p", g_model, g_ctx);
        jstring errMsg = env->NewStringUTF("\n[에러: Llama 엔진이 로드되지 않았습니다.]");
        env->CallVoidMethod(callback, onTokenMethod, errMsg);
        env->DeleteLocalRef(errMsg);
        return;
    }

    const char* prompt = env->GetStringUTFChars(promptText, nullptr);
    if (!prompt || strlen(prompt) == 0) {
        LOGE("🔴 Error: Prompt text is empty. Returning early.");
        if (prompt) env->ReleaseStringUTFChars(promptText, prompt);
        return;
    }
    std::string inputStr(prompt);
    env->ReleaseStringUTFChars(promptText, prompt);

    // KV 캐시 초기화 (이전 추론 기록 삭제)
    llama_memory_t memory = llama_get_memory(g_ctx);
    if (memory) {
        llama_memory_clear(memory, true);
    }

    LOGI("🟢 Starting Streaming inference. Prompt length: %zu chars", inputStr.size());

    const struct llama_vocab * vocab = llama_model_get_vocab(g_model);

    // 1. 토큰화 (Tokenization)
    std::vector<llama_token> tokens_list(inputStr.size() + 4);
    int n_tokens = llama_tokenize(vocab, inputStr.c_str(), inputStr.size(), tokens_list.data(), tokens_list.size(), true, true);
    if (n_tokens < 0) {
        tokens_list.resize(-n_tokens);
        n_tokens = llama_tokenize(vocab, inputStr.c_str(), inputStr.size(), tokens_list.data(), tokens_list.size(), true, true);
    }
    tokens_list.resize(n_tokens);
    LOGI("🟢 Tokenized: %d tokens", n_tokens);

    // 2. 프롬프트 인젝션 및 텍스트 생성 (가장 안전한 API 방식 적용)
    // 한 번에 디코딩할 최대 배치 크기 설정 (용량은 넉넉하게)
    int n_batch_capacity = 512;
    llama_batch batch = llama_batch_init(n_batch_capacity, 0, 1);

    // [중요] 안드로이드 모바일 GPU/CPU 데드락 방지를 위해 한 번에 먹이는 양(Chunk)을 32개로 강제 제한!
    int chunk_size = 32;

    // 프롬프트 전체를 청크(chunk) 단위로 나누어 디코딩
    for (int i = 0; i < n_tokens; i += chunk_size) {
        int eval_count = std::min(chunk_size, n_tokens - i);
        
        // batch 초기화 후 토큰 밀어넣기
        batch.n_tokens = 0;
        for (int j = 0; j < eval_count; j++) {
            batch.token[batch.n_tokens] = tokens_list[i + j];
            batch.pos[batch.n_tokens] = i + j;
            batch.n_seq_id[batch.n_tokens] = 1; // [크래시 해결] 1개의 시퀀스에 속함을 명시
            batch.seq_id[batch.n_tokens][0] = 0;
            // 프롬프트의 맨 마지막 토큰에 대해서만 로짓(다음 단어 예측) 활성화
            batch.logits[batch.n_tokens] = (i + j == n_tokens - 1);
            batch.n_tokens++;
        }

        LOGI("🟢 Decoding prompt chunk [%d ~ %d]", i, i + eval_count - 1);
        if (llama_decode(g_ctx, batch) != 0) {
            LOGE("🔴 llama_decode FAILED at prompt chunk %d", i);
            llama_batch_free(batch);
            return;
        }
    }
    LOGI("🟢 Prompt decoded successfully");

    const int n_vocab = llama_vocab_n_tokens(vocab);
    LOGI("🟢 Vocab size: %d, maxTokens: %d", n_vocab, (int)maxTokens);

    // 3. 텍스트 추론 루프 (Greedy Decoding)
    int generated = 0;
    int current_pos = n_tokens;

    for (int i = 0; i < maxTokens; i++) {
        // 마지막 평가된 배치의 가장 마지막 토큰 로짓 가져오기
        auto* logits = llama_get_logits_ith(g_ctx, batch.n_tokens - 1);
        llama_token new_token_id = 0;

        float max_prob = -1e9;
        for (int v = 0; v < n_vocab; v++) {
            if (logits[v] > max_prob) {
                max_prob = logits[v];
                new_token_id = v;
            }
        }

        if (llama_vocab_is_eog(vocab, new_token_id)) {
            LOGI("🟡 EOS/EOG token hit at step %d (token_id=%d)", i, new_token_id);
            break;
        }

        // 토큰을 문자열로 변환하여 UI로 전송
        char buf[128];
        int n_len = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, true);
        if (n_len > 0) {
            std::string piece(buf, n_len);
            jstring jPiece = env->NewStringUTF(piece.c_str());
            env->CallVoidMethod(callback, onTokenMethod, jPiece);
            env->DeleteLocalRef(jPiece);
            generated++;
        }

        // 새 토큰을 1개짜리 배치로 만들어서 다음 디코딩에 사용
        batch.n_tokens = 0;
        batch.token[batch.n_tokens] = new_token_id;
        batch.pos[batch.n_tokens] = current_pos++;
        batch.n_seq_id[batch.n_tokens] = 1; // [크래시 해결]
        batch.seq_id[batch.n_tokens][0] = 0;
        batch.logits[batch.n_tokens] = true;
        batch.n_tokens++;

        if (llama_decode(g_ctx, batch) != 0) {
            LOGE("🔴 llama_decode FAILED during generation at step %d", i);
            break;
        }
    }

    llama_batch_free(batch);

    LOGI("🟢 Streaming complete. Generated %d tokens.", generated);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_backend_LlamaNapiBridge_nativeReleaseModel(
        JNIEnv* env,
        jobject /* this */
) {
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    llama_backend_free();
    LOGI("Llama resources released.");
}
