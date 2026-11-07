cmake_minimum_required(VERSION 3.22.1)
project(melodia_whisper)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)

# Collect all ggml source files
set(GGML_SOURCES
    "third_party/ggml/src/ggml.c"
    "third_party/ggml/src/ggml-alloc.c"
    "third_party/ggml/src/ggml-quants.c"
    "third_party/ggml/src/ggml-backend.cpp"
    "third_party/ggml/src/ggml-backend-reg.cpp"
    "third_party/ggml/src/ggml-threading.cpp"
    "third_party/ggml/src/ggml-cpu/ggml-cpu.c"
    "third_party/ggml/src/ggml-cpu/ggml-cpu.cpp"
    "third_party/ggml/src/ggml-cpu/ggml-cpu-quants.c"
    "third_party/ggml/src/ggml-cpu/ggml-cpu-traits.cpp"
    "third_party/ggml/src/ggml-cpu/ops.cpp"
    "third_party/ggml/src/ggml-cpu/unary-ops.cpp"
    "third_party/ggml/src/ggml-cpu/binary-ops.cpp"
    "third_party/ggml/src/ggml-cpu/vec.cpp"
)

# Collect whisper sources
set(WHISPER_SOURCES
    "third_party/whisper.cpp"
)

add_library(melodia_whisper SHARED
    ${WHISPER_SOURCES}
    ${GGML_SOURCES}
    melodia_whisper_bridge.cpp
)

target_include_directories(melodia_whisper PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/ggml/include
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/ggml/src
)

# Disable GPU/backends not available on Android
target_compile_definitions(melodia_whisper PRIVATE
    -DWHISPER_VERSION=\"2.4.0\"
)

find_library(log-lib log)
target_link_libraries(melodia_whisper ${log-lib})
