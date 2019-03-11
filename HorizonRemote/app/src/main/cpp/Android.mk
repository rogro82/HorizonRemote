LOCAL_PATH := $(call my-dir)
MODULE_PATH := $(LOCAL_PATH)

include $(CLEAR_VARS)

LOCAL_MODULE    := horizonremote
LOCAL_CFLAGS	:= \
	-O2 \
	-DANDROID \
#	-Wall \

	
LOCAL_C_INCLUDES := \
	$(MODULE_PATH)/include \
	
LOCAL_SRC_FILES	 := \
	src/vnc/des_local.cpp \
	src/vnc/raw_query.cpp \
	src/vnc/vnc_client.cpp \
	src/jni/jnireference.cpp \
	src/remote.cpp \

LOCAL_STATIC_LIBRARIES := c++_static

include $(BUILD_SHARED_LIBRARY)
