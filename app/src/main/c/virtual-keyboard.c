#include <jni.h>
#include <android/log.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <stdio.h>
#include <stdlib.h>
#include "include/linux/uinput.h"


#define LOG_TAG "jni.sm2.virtual-keyboard"
#define LOGE(...) ((void) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))






JNIEXPORT jint JNICALL
Java_kg_delletenebre_serialmanager2_utils_VirtualKeyboard_create(JNIEnv *env, jobject instance,
                                                                 jintArray codes_) {
    jint *codes = (*env)->GetIntArrayElements(env, codes_, NULL);
    int codeSize = (*env)->GetArrayLength(env, codes_);

    int fd;
    struct uinput_user_dev uidev;

    system("su -c \"chmod -R 666 /dev/uinput\"");

    fd = open("/dev/uinput", O_WRONLY | O_NONBLOCK);
    if (fd < 0) {
        LOGE("error: open @ line %d", __LINE__);
        return -1;
    }

    ioctl(fd, UI_SET_EVBIT, EV_KEY);
    int i;
    for (i = 0; i < codeSize; i++) {
        if (codes[i] > 0) {
            ioctl(fd, UI_SET_KEYBIT, codes[i]);
        }
    }
    ioctl(fd, UI_SET_KEYBIT, KEY_LEFTALT);
    ioctl(fd, UI_SET_KEYBIT, KEY_LEFTCTRL);
    ioctl(fd, UI_SET_KEYBIT, KEY_LEFTSHIFT);

    memset(&uidev, 0, sizeof(uidev)); //create an virtual input device node in /dev/input/***
    snprintf(uidev.name, UINPUT_MAX_NAME_SIZE, "uinput-serialmanager");
    uidev.id.bustype = BUS_USB;
    uidev.id.vendor = 0x1;
    uidev.id.product = 0x1;
    uidev.id.version = 1;
    // cat /proc/bus/input/devices

    if (write(fd, &uidev, sizeof(uidev)) < 0) {
        LOGE("error: write @ line %d", __LINE__);
        return -2;
    }

    if (ioctl(fd, UI_DEV_CREATE) < 0) {
        LOGE("error: ioctl @ line %d", __LINE__);
        return -3;
    }

    (*env)->ReleaseIntArrayElements(env, codes_, codes, 0);

    return fd;
}

JNIEXPORT void JNICALL
Java_kg_delletenebre_serialmanager2_utils_VirtualKeyboard_destroy(JNIEnv *env, jobject instance, jint fd) {
    if (ioctl(fd, UI_DEV_DESTROY) >= 0) {
        close(fd);
    } else {
        LOGE("error: fd or ioctl @ line %d", __LINE__);
    }
}

static void send_event(int fd, uint16_t type, uint16_t code, int32_t value) {
    struct input_event event;
    memset(&event, 0, sizeof(event));
    event.type = type;
    event.code = code;
    event.value = value;
    if (write(fd, &event, sizeof(event)) < 0) {
        LOGE("send_event error @ line %d", __LINE__);
    }
}

JNIEXPORT void JNICALL
Java_kg_delletenebre_serialmanager2_utils_VirtualKeyboard_sendEvent(JNIEnv *env, jclass type, jint fd, uint16_t code) {
    send_event(fd, EV_KEY, code, 1);
    send_event(fd, EV_SYN, 0, 0);
    send_event(fd, EV_KEY, code, 0);
    send_event(fd, EV_SYN, 0, 0);
}

JNIEXPORT void JNICALL
Java_kg_delletenebre_serialmanager2_utils_VirtualKeyboard_sendEvents(JNIEnv *env, jclass type, jint fd, uint16_t code1, uint16_t code2) {
    send_event(fd, EV_KEY, code1, 1);
    send_event(fd, EV_SYN, 0, 0);
    send_event(fd, EV_KEY, code2, 1);
    send_event(fd, EV_SYN, 0, 0);
    send_event(fd, EV_KEY, code2, 0);
    send_event(fd, EV_KEY, code1, 0);
    send_event(fd, EV_SYN, 0, 0);
}

JNIEXPORT void JNICALL
Java_kg_delletenebre_serialmanager2_utils_VirtualKeyboard_sendKeyDown(JNIEnv *env, jobject instance,
                                                                      jint fd, jint code) {

    send_event(fd, EV_KEY, code, 1);
    send_event(fd, EV_SYN, 0, 0);

}

JNIEXPORT void JNICALL
Java_kg_delletenebre_serialmanager2_utils_VirtualKeyboard_sendKeyUp(JNIEnv *env, jobject instance,
                                                                    jint fd, jint code) {

    send_event(fd, EV_KEY, code, 0);
    send_event(fd, EV_SYN, 0, 0);

}