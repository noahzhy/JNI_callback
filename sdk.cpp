#include <android/log.h>
#include <jni.h>
#include <unistd.h>
#include <string>

JavaVM* g_VM;

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_testing_MainActivity_stringFromJNI(
    JNIEnv* env,
    jobject /* this */) {
    std::string hello = "Hello from C++";
    int numb = 11;
    return env->NewStringUTF((hello + std::to_string(numb)).c_str());
}

void* download(void* p) {
    bool mNeedDetach = JNI_FALSE;

    if (p == nullptr)
        return nullptr;

    JNIEnv* env;
    // 获取当前native线程是否有没有被附加到jvm环境中
    int getEnvStat = g_VM->GetEnv((void**)&env, JNI_VERSION_1_6);
    if (getEnvStat == JNI_EDETACHED) {
        // 如果没有， 主动附加到jvm环境中，获取到env
        if (g_VM->AttachCurrentThread(&env, nullptr) != 0) {
            return nullptr;
        }
        mNeedDetach = JNI_TRUE;
    }
    // 强转回来
    auto jcallback = (jobject)p;

    // 通过强转后的jcallback 获取到要回调的类
    jclass javaClass = env->GetObjectClass(jcallback);
    if (javaClass == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "download", "Unable to find class");
        if (mNeedDetach) {
            __android_log_print(ANDROID_LOG_ERROR, "download", "DetachCurrentThread");
            g_VM->DetachCurrentThread();
        }
        env->DeleteGlobalRef(jcallback);  // 释放全局引用
        return nullptr;                   // 添加返回，防止继续执行
    }

    // 获取要回调的方法ID
    jmethodID javaCallbackId = env->GetMethodID(javaClass, "onProgressChange", "(JJ)I");
    if (javaCallbackId == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "download", "Unable to find method:onProgressCallBack");
        if (mNeedDetach) {
            g_VM->DetachCurrentThread();
        }
        env->DeleteGlobalRef(jcallback);  // 释放全局引用
        return nullptr;                   // 添加返回，防止继续执行
    }

    for (int i = 0; i <= 100; i += 10) {
        // 可以定期向Java层报告进度
        env->CallIntMethod(jcallback, javaCallbackId, i, 100);

        // 模拟耗时工作
        sleep(1);  // 如需要暂停，可以使用sleep

        // 检查是否需要中断操作
        // if (shouldCancel) break;
    }

    // // 执行回调
    // env->CallIntMethod(jcallback, javaCallbackId, 1, 1);
    jcallback = nullptr;

    // 释放当前线程
    if (mNeedDetach) {
        g_VM->DetachCurrentThread();
    }
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_testing_Sdk_download(JNIEnv* env, jobject, jstring jpath, jobject jcallback) {
    // JavaVM是虚拟机在JNI中的表示，等下再其他线程回调java层需要用到
    env->GetJavaVM(&g_VM);
    // 只创建一个全局引用
    jobject callback = env->NewGlobalRef(jcallback);

    __android_log_print(ANDROID_LOG_ERROR, "download", "path: %s", env->GetStringUTFChars(jpath, nullptr));

    // 把接口传进去, 或者保存在一个结构体里面的属性,  进行传递也可以
    pthread_t thread_id;
    if ((pthread_create(&thread_id, nullptr, download, callback)) != 0) {
        env->DeleteGlobalRef(callback);  // 如果创建线程失败，释放全局引用
        return;
    }
}