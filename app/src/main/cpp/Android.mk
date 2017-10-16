APP := EventReader

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE     := $(APP)
LOCAL_SRC_FILES  := $(APP).c

include $(BUILD_EXECUTABLE)
