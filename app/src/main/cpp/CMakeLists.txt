# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

set(lib_dir ${CMAKE_SOURCE_DIR}/../../../libs/armeabi-v7a)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall")

add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             native-lib.cpp FFMpegMusic.cpp)

add_library(
        avcodec-57
        SHARED
        IMPORTED
)
set_target_properties(
        avcodec-57
        PROPERTIES IMPORTED_LOCATION
        ${lib_dir}/libavcodec-57.so
)

add_library(
        postproc-54
        SHARED
        IMPORTED
)
set_target_properties(
        postproc-54
        PROPERTIES IMPORTED_LOCATION
        ${lib_dir}/libpostproc-54.so
)

add_library(
        avfilter-6
        SHARED
        IMPORTED
)
set_target_properties(
        avfilter-6
        PROPERTIES IMPORTED_LOCATION
        ${lib_dir}/libavfilter-6.so
)

add_library(
        avformat-57
        SHARED
        IMPORTED
)
set_target_properties(
        avformat-57
        PROPERTIES IMPORTED_LOCATION
        ${lib_dir}/libavformat-57.so
)

add_library(
        avutil-55
        SHARED
        IMPORTED
)
set_target_properties(
        avutil-55
        PROPERTIES IMPORTED_LOCATION
        ${lib_dir}/libavutil-55.so
)

add_library(
        swresample-2
        SHARED
        IMPORTED
)
set_target_properties(
        swresample-2
        PROPERTIES IMPORTED_LOCATION
        ${lib_dir}/libswresample-2.so
)

add_library(
        swscale-4
        SHARED
        IMPORTED
)
set_target_properties(
        swscale-4
        PROPERTIES IMPORTED_LOCATION
        ${lib_dir}/libswscale-4.so
)

include_directories(../../../libs/include)
include_directories(header)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       native-lib
                       avcodec-57
                        postproc-54
                       avfilter-6
                        avformat-57
                        avutil-55
                        swresample-2
                        swscale-4
                        EGL
                        GLESv2
                        android
                        OpenSLES
                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )