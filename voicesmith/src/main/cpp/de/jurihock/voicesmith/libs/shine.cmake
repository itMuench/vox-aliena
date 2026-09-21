# https://github.com/toots/shine

CPMAddPackage(
  NAME shine
  GIT_TAG ab5e3526b64af1a2eaa43aa6f441a7312e013519
  GITHUB_REPOSITORY toots/shine
  DOWNLOAD_ONLY YES)

if(shine_ADDED)

  add_library(shine STATIC
    "${shine_SOURCE_DIR}/src/lib/bitstream.c"
    "${shine_SOURCE_DIR}/src/lib/huffman.c"
    "${shine_SOURCE_DIR}/src/lib/l3bitstream.c"
    "${shine_SOURCE_DIR}/src/lib/l3loop.c"
    "${shine_SOURCE_DIR}/src/lib/l3mdct.c"
    "${shine_SOURCE_DIR}/src/lib/l3subband.c"
    "${shine_SOURCE_DIR}/src/lib/layer3.c"
    "${shine_SOURCE_DIR}/src/lib/reservoir.c"
    "${shine_SOURCE_DIR}/src/lib/tables.c")

  target_include_directories(shine
    PUBLIC "${shine_SOURCE_DIR}/src/lib")

  set_target_properties(shine PROPERTIES
    POSITION_INDEPENDENT_CODE ON)

  target_link_libraries(shine PUBLIC m)

endif()
