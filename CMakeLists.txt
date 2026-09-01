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

# Llama.cpp 빌드 옵션 최소화 (안드로이드 환경 용)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "Disable llama tests")
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "Disable llama examples")
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "Disable llama server")

# [중요] 안드로이드 모바일 GPU 가속 (Vulkan) 활성화
set(GGML_VULKAN OFF CACHE BOOL "Enable Vulkan GPU acceleration")

add_subdirectory(llama_cpp)

target_link_libraries(
    ondevice_ai_engine
    ${log-lib}
    llama
)
