cmake_minimum_required(VERSION 3.1...3.19)

project(voicesmith)

file(GLOB_RECURSE HDR "${CMAKE_CURRENT_LIST_DIR}/*.h")
file(GLOB_RECURSE SRC "${CMAKE_CURRENT_LIST_DIR}/*.cpp")

add_library(${PROJECT_NAME} SHARED)
target_sources(${PROJECT_NAME} PRIVATE "${HDR}" "${SRC}")
target_include_directories(${PROJECT_NAME} PRIVATE "${CMAKE_CURRENT_LIST_DIR}/..")

target_compile_features(${PROJECT_NAME} PRIVATE cxx_std_20)
target_compile_options(${PROJECT_NAME} PRIVATE $<$<CONFIG:RELEASE>:-Ofast>)
target_compile_options(${PROJECT_NAME} PRIVATE -Wall -Werror)

# NDK r27 and older do not produce 16 KB-aligned ELF LOAD segments by default.
# Android 15+ devices may use 16 KB memory pages, so explicitly request the
# alignment recommended by Android for CMake builds.
# https://developer.android.com/guide/practices/page-sizes
target_link_options(${PROJECT_NAME} PRIVATE
  "-Wl,-z,max-page-size=16384"
  "-Wl,-z,common-page-size=16384")

include("${CMAKE_CURRENT_LIST_DIR}/libs/cpm.cmake")
include("${CMAKE_CURRENT_LIST_DIR}/libs/eventpp.cmake")
include("${CMAKE_CURRENT_LIST_DIR}/libs/fmt.cmake")
include("${CMAKE_CURRENT_LIST_DIR}/libs/oboe.cmake")
include("${CMAKE_CURRENT_LIST_DIR}/libs/pffft.cmake")
include("${CMAKE_CURRENT_LIST_DIR}/libs/pocketfft.cmake")
include("${CMAKE_CURRENT_LIST_DIR}/libs/readerwriterqueue.cmake")
include("${CMAKE_CURRENT_LIST_DIR}/libs/shine.cmake")
include("${CMAKE_CURRENT_LIST_DIR}/libs/stftpitchshift.cmake")
include("${CMAKE_CURRENT_LIST_DIR}/libs/vad.cmake")

target_link_libraries(${PROJECT_NAME} PRIVATE
  android
  eventpp
  fmt
  log
  oboe
  pffft
  pocketfft
  readerwriterqueue
  shine
  stftpitchshift
  vad)
