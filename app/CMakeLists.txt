# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.

add_library( virtual-keyboard
             SHARED
             src/main/c/virtual-keyboard.c )

find_library( log-lib
              log )

target_link_libraries( virtual-keyboard
                       ${log-lib} )
