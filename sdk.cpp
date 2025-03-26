#include <android/log.h>
#include <jni.h>
#include <unistd.h>
#include <string>
#include <pthread.h>

JavaVM* g_VM = nullptr;
static jclass g_pointClass = nullptr;

jint JNI_OnLoad(JavaVM* vm, void *reserved) {
    g_VM = vm;
    JNIEnv* env = nullptr;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK)
        return JNI_ERR;
    // 使用主线程的类加载器查找 Sdk$Point 类
    jclass localPointClass = env->FindClass("com/example/testing/Sdk$Point");
    if (localPointClass == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI_OnLoad", "Unable to find class: com/example/testing/Sdk$Point");
        return JNI_ERR;
    }
    g_pointClass = (jclass)env->NewGlobalRef(localPointClass);
    env->DeleteLocalRef(localPointClass);  // 释放局部引用
    return JNI_VERSION_1_6;
}

void* download(void* p) {
    if (p == nullptr)
        return nullptr;

    JNIEnv* env = nullptr;
    bool needDetach = false;
    // 获取当前 native 线程是否已附加到 jvm 环境中
    if (g_VM->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_EDETACHED) {
        if (g_VM->AttachCurrentThread(&env, nullptr) != 0)
            return nullptr;
        needDetach = true;
    }

    jobject jcallback = (jobject)p;
    jclass javaClass = env->GetObjectClass(jcallback);
    if (javaClass == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "download", "Unable to find class");
        env->DeleteGlobalRef(jcallback);
        if (needDetach)
            g_VM->DetachCurrentThread();
        return nullptr;
    }
    
    jmethodID javaCallbackId = env->GetMethodID(javaClass, "onProgressChange", "(Lcom/example/testing/Sdk$Point;)V");
    if (javaCallbackId == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "download", "Unable to find method:onProgressChange");
        env->DeleteGlobalRef(jcallback);
        if (needDetach)
            g_VM->DetachCurrentThread();
        return nullptr;
    }

    // 使用缓存的全局引用，不在当前线程查找类
    jclass pointClass = g_pointClass;
    if (pointClass == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "download", "Global reference for Sdk$Point is null");
        env->DeleteGlobalRef(jcallback);
        if (needDetach)
            g_VM->DetachCurrentThread();
        return nullptr;
    }

    jfieldID xFieldId = env->GetFieldID(pointClass, "x", "I");
    jfieldID yFieldId = env->GetFieldID(pointClass, "y", "I");

    jobject point = env->AllocObject(pointClass);
    if (point == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "download", "Unable to allocate Sdk$Point object");
        env->DeleteGlobalRef(jcallback);
        if (needDetach)
            g_VM->DetachCurrentThread();
        return nullptr;
    }
    // env->SetIntField(point, xFieldId, 1);
    // env->SetIntField(point, yFieldId, 1);

    for (int i = 0; i <= 100; i += 10) {
        env->SetIntField(point, xFieldId, i);
        env->SetIntField(point, yFieldId, i);
        env->CallVoidMethod(jcallback, javaCallbackId, point);
        sleep(1);
    }

    env->DeleteGlobalRef(jcallback);
    if (needDetach)
        g_VM->DetachCurrentThread();
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_testing_Sdk_download(JNIEnv* env, jobject, jstring jpath, jobject jcallback) {
    env->GetJavaVM(&g_VM);

    // 获取字符串后及时释放，避免泄露资源
    const char* path = env->GetStringUTFChars(jpath, nullptr);
    __android_log_print(ANDROID_LOG_ERROR, "download", "path: %s", path);
    env->ReleaseStringUTFChars(jpath, path);

    jobject callback = env->NewGlobalRef(jcallback);
    pthread_t thread_id;
    if (pthread_create(&thread_id, nullptr, download, callback) != 0) {
        env->DeleteGlobalRef(callback);
        return;
    }
}