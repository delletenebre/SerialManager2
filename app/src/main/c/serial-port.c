#include <jni.h>
#include <android/log.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
//#include "include/termios.h"
#include <termios.h>


#define LOG_TAG "serial-port"
#define LOGD(...) ((void) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))
#define LOGE(...) ((void) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))

static speed_t getBaudRateCode(jint baudRate)
{
    switch (baudRate) {
        case 0: return B0;
        case 50: return B50;
        case 75: return B75;
        case 110: return B110;
        case 134: return B134;
        case 150: return B150;
        case 200: return B200;
        case 300: return B300;
        case 600: return B600;
        case 1200: return B1200;
        case 1800: return B1800;
        case 2400: return B2400;
        case 4800: return B4800;
        case 9600: return B9600;
        case 19200: return B19200;
        case 38400: return B38400;
        case 57600: return B57600;
        case 115200: return B115200;
        case 230400: return B230400;
        case 460800: return B460800;
        case 500000: return B500000;
        case 576000: return B576000;
        case 921600: return B921600;
        default: return 0;
    }
}

JNIEXPORT jobject JNICALL
Java_kg_delletenebre_serialmanager2_communication_impl_SerialCommunicator_openSerial (JNIEnv *env, jclass instance, jstring path, jint baudRate) {
    int fd;
    speed_t speed;
    jobject fileDescriptor;
    char command[PATH_MAX];

    /* Check arguments */
    speed = getBaudRateCode(baudRate);
    if (speed == -1) {
        LOGE("Invalid baudrate @ line %d", __LINE__);
        return NULL;
    }

    /* Opening device */
    const char* _path = (*env)->GetStringUTFChars(env, path, (jboolean *)0);
    snprintf(command, sizeof(command), "su -c \"chmod -R 646 %s\"", _path);
    system(command);
    fd = open(_path, O_RDWR);
    (*env)->ReleaseStringUTFChars(env, path, _path);
    if (fd == -1) {
        LOGE("Cannot open serial port %s @ line %d", _path, __LINE__);
        return NULL;
    }


    /* Configure device */
    struct termios cfg;
    LOGD("Configuring serial port @ line %d", __LINE__);
    if (tcgetattr(fd, &cfg)) {
        LOGE("tcgetattr() failed @ line %d", __LINE__);
        close(fd);
        return NULL;
    }

    cfmakeraw(&cfg);
    cfsetispeed(&cfg, speed);
    cfsetospeed(&cfg, speed);

    if (tcsetattr(fd, TCSANOW, &cfg)) {
        LOGE("tcsetattr() failed @ line %d", __LINE__);
        close(fd);
        return NULL;
    }


    /* Create a corresponding file descriptor */
    jclass classFileDescriptor = (*env)->FindClass(env, "java/io/FileDescriptor");
    jmethodID iFileDescriptor = (*env)->GetMethodID(env, classFileDescriptor, "<init>", "()V");
    jfieldID descriptorID = (*env)->GetFieldID(env, classFileDescriptor, "descriptor", "I");
    fileDescriptor = (*env)->NewObject(env, classFileDescriptor, iFileDescriptor);
    (*env)->SetIntField(env, fileDescriptor, descriptorID, (jint) fd);


    return fileDescriptor;
}


JNIEXPORT void JNICALL
Java_kg_delletenebre_serialmanager2_communication_impl_SerialCommunicator_closeSerial (JNIEnv *env, jobject instance) {
    jclass SerialPortClass = (*env)->GetObjectClass(env, instance);
    jclass FileDescriptorClass = (*env)->FindClass(env, "java/io/FileDescriptor");

    jfieldID fdID = (*env)->GetFieldID(env, SerialPortClass, "fileDescriptor", "Ljava/io/FileDescriptor;");
    jfieldID descriptorID = (*env)->GetFieldID(env, FileDescriptorClass, "descriptor", "I");

    jobject fd = (*env)->GetObjectField(env, instance, fdID);
    jint descriptor = (*env)->GetIntField(env, fd, descriptorID);
    close(descriptor);
}