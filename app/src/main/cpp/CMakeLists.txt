cmake_minimum_required(VERSION 3.22.1)
project(kisiselasistan)

set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)

add_subdirectory(llama.cpp)

add_library(kisiselasistan_native SHARED
    native-lib.cpp
)

find_library(log-lib log)

target_link_libraries(kisiselasistan_native
    llama
    ${log-lib}
)
