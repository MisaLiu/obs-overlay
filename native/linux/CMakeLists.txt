cmake_minimum_required(VERSION 3.10)
project(glx_hook C)

set(CMAKE_C_STANDARD 99)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_package(X11 REQUIRED)
find_package(OpenGL REQUIRED)

add_library(glx_hook SHARED glx_hook.c)
target_link_libraries(glx_hook ${X11_LIBRARIES} ${OPENGL_LIBRARIES} dl)
target_include_directories(glx_hook PRIVATE ${X11_INCLUDE_DIR} ${OPENGL_INCLUDE_DIR})

set_target_properties(glx_hook PROPERTIES PREFIX "lib")
