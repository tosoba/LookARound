cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Werror")

add_library(
        opengl_renderer_jni SHARED
        jni_hooks.cpp
        opengl_renderer_jni.cpp)

find_library(log-lib log)
find_library(android-lib android)
find_library(opengl-lib GLESv2)
find_library(egl-lib EGL)


target_link_libraries(opengl_renderer_jni ${log-lib} ${android-lib} ${opengl-lib} ${egl-lib})