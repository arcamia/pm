LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := services.devicepolicy

LOCAL_SRC_FILES += \
      $(call all-java-files-under,java)

LOCAL_JAVA_LIBRARIES := conscrypt services.core com.sentinel.android.services.check

include $(BUILD_STATIC_JAVA_LIBRARY)
