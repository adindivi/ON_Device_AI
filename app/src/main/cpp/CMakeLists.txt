cmake_minimum_required(VERSION 3.22.1)
project("ondevice_ai_engine" C CXX)

add_library(
    ondevice_ai_engine
    SHARED
    native-lib.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    ondevice_ai_engine
    ${log-lib}
)
