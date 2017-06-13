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
    struct uinput_user_dev user_dev;

    system("su -c \"chmod -R 666 /dev/uinput\"");

    fd = open("/dev/uinput", O_WRONLY); // | O_NONBLOCK);
    if (fd < 0) {
        LOGE("error: open @ line %d", __LINE__);
        return -1;
    }

    ioctl(fd, UI_SET_EVBIT, EV_KEY);
    int i;
    for (i = 0; i < codeSize; i++) {
        if (codes[i] > -1) {
            ioctl(fd, UI_SET_KEYBIT, codes[i]);
        }
    }
    ioctl(fd, UI_SET_KEYBIT, KEY_LEFTALT);
    ioctl(fd, UI_SET_KEYBIT, KEY_LEFTCTRL);
    ioctl(fd, UI_SET_KEYBIT, KEY_LEFTSHIFT);
//    for (i = 0; i < KEY_MAX; i++) {
//        if (ioctl(fd, UI_SET_KEYBIT, i) == -1) {
//            return -1;
//        }
//    }


    memset(&user_dev, 0, sizeof(user_dev)); //create an virtual input device node in /dev/input/***
    snprintf(user_dev.name, UINPUT_MAX_NAME_SIZE, "uinput-serialmanager");
    user_dev.id.bustype = BUS_VIRTUAL;
    user_dev.id.vendor = 0x0808;
    user_dev.id.product = 0x0001;
    user_dev.id.version = 1;
    // cat /proc/bus/input/devices

    if (write(fd, &user_dev, sizeof(user_dev)) != sizeof(user_dev)) {
        LOGE("error: write @ line %d", __LINE__);
        return -2;
    }

    if (ioctl(fd, UI_DEV_CREATE) < 0) {
        LOGE("error: ioctl @ line %d", __LINE__);
        return -3;
    }

    /*
     *  The reason for generating a small delay is that creating succesfully
     *  an uinput device does not guarantee that the device is ready to process
     *  input events. It's probably due the asynchronous nature of the udev.
     *  However, my experiments show that the device is not ready to process input
     *  events even after a device creation event is received from udev.
     */
    sleep(2);

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
    send_event(fd, EV_KEY, (uint16_t) code, 1);
    send_event(fd, EV_SYN, 0, 0);

}

JNIEXPORT void JNICALL
Java_kg_delletenebre_serialmanager2_utils_VirtualKeyboard_sendKeyUp(JNIEnv *env, jobject instance,
                                                                    jint fd, jint code) {

    send_event(fd, EV_KEY, (uint16_t) code, 0);
    send_event(fd, EV_SYN, 0, 0);

}