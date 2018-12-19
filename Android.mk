LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

ifneq ($(OTA_DIR),)
    $(shell cp -u $(OTA_DIR)/ota_conf $(LOCAL_PATH)/app/src/main/assets/)
endif

LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := PornOTA
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PRIVATE_PLATFORM_APIS := true

LOCAL_SRC_FILES := $(call all-java-files-under, app/src/main)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/app/src/main/res
LOCAL_ASSET_DIR := $(LOCAL_PATH)/app/src/main/assets

LOCAL_USE_AAPT2 := true
LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true
LOCAL_AAPT_FLAGS := --auto-add-overlay

LOCAL_STATIC_ANDROID_LIBRARIES := \
    android-support-compat \
    android-support-v4 \
    android-support-design \
    android-support-v13 \
    android-support-v7-appcompat \
    android-support-v7-cardview \
    android-support-constraint-layout \
    android-support-constraint-layout-solver \
    android-support-transition

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

include $(BUILD_MULTI_PREBUILT)
