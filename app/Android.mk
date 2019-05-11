LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := app
LOCAL_LDFLAGS := -Wl,--build-id
LOCAL_C_INCLUDES += /Users/kasper/Documents/scripts/HDR2EXR/app/src/main/jniLibs

include $(BUILD_SHARED_LIBRARY)
