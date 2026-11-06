cmake_minimum_required(VERSION 3.22.1)
project(melodia_whisper)

add_library(melodia_whisper SHARED
    whisper.cpp
    melodia_whisper_bridge.cpp
)

find_library(log-lib log)
target_link_libraries(melodia_whisper ${log-lib})