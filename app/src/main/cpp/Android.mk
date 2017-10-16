APP := EventReader

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE     := $(APP)
LOCAL_SRC_FILES  := input.h-labels.h $(APP).c

include $(BUILD_EXECUTABLE)
