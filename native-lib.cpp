#include <jni.h>
#include <string>

#include <android/log.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_testing_MainActivity_stringFromJNI(
    JNIEnv* env,
    jobject /* this */) {
    std::string hello = "Hello from C++";
    int numb = 11;
    // return env->NewStringUTF(hello.c_str());
    return env->NewStringUTF((hello + std::to_string(numb)).c_str());
}

JavaVM* g_VM;



// 在此处跑在子线程中, 并回调到java层
void download(void* p) {
    bool mNeedDetach;

    if (p == nullptr)
        return;

    JNIEnv* env;
    // 获取当前native线程是否有没有被附加到jvm环境中
    int getEnvStat = g_VM->GetEnv((void**)&env, JNI_VERSION_1_6);
    // 如果没有附加到jvm环境中

    if (getEnvStat == JNI_EDETACHED) {
        // 如果没有,  主动附加到jvm环境中, 获取到env
        if (g_VM->AttachCurrentThread(&env, nullptr) != 0) {
            return;
        }

        mNeedDetach = JNI_TRUE;
    }
    // 强转回来
    //  jcallback = (jobject)p;
    jobject jcallback = (jobject)p;

    // 通过强转后的jcallback 获取到要回调的类
    jclass javaClass = env->GetObjectClass(jcallback);

    if (javaClass == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "nativeDownload", "Unable to find class");
        // ???
        g_VM->DetachCurrentThread();
        env = nullptr;
        return;
    }

    // 获取要回调的方法ID
    //  jmethodID javaCallbackId = env->GetMethodID(&env, javaClass, "onProgressChange", "(JJ)I");
    jmethodID javaCallbackId = env->GetMethodID(javaClass, "onProgressChange", "(JJ)I");
    if (javaCallbackId == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "nativeDownload", "Unable to find method");
        return;
    }
    // 执行回调
    env->CallIntMethod(jcallback, javaCallbackId, 1, 1);

    // 释放当前线程
    if (mNeedDetach) {
        g_VM->DetachCurrentThread();
    }
    env = nullptr;

    // 释放你的全局引用的接口, 生命周期自己把控
    env->DeleteGlobalRef(jcallback);
}





extern "C" JNIEXPORT void JNICALL
Java_com_example_testing_Sdk_nativeDownload(JNIEnv* env, jobject, jstring jpath, jobject jcallback) {
    env->GetJavaVM(&g_VM);

    if (jpath == nullptr || jcallback == nullptr) {
        return;
    }
    jobject callback = env->NewGlobalRef(jcallback);

    __android_log_print(ANDROID_LOG_ERROR, "nativeDownload", "path: %s", env->GetStringUTFChars(jpath, nullptr));
    // 把接口传进去, 或者保存在一个结构体里面的属性,  进行传递也可以
    // pthread_create(xxx, xxx, download, callback);
    pthread_t thread;
    pthread_create(&thread, nullptr, reinterpret_cast<void *(*)(void *)>(download), callback);
}

