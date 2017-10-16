
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE     := EventReader
LOCAL_SRC_FILES  := $(LOCAL_MODULE).c

include $(BUILD_EXECUTABLE)
