project(tox4j-c)
cmake_minimum_required(VERSION 3.22.1)
enable_testing()
set(CMAKE_MODULE_PATH "${CMAKE_SOURCE_DIR}/tools/cmake")

if (NOT DEFINED CMAKE_MACOSX_RPATH)
  set(CMAKE_MACOSX_RPATH true)
endif()

include(ExternalProject)

include(${PROJECT_SOURCE_DIR}/cmake/protogen.cmake)

set(PBUF_DIR ${PROJECT_BINARY_DIR}/protobuf)

set(THIRDPARTY_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/thirdparty)
set(THIRDPARTY_INSTALL_DIR ${CMAKE_CURRENT_BINARY_DIR}/install)
set(THIRDPARTY_INCLUDE_DIR ${THIRDPARTY_INSTALL_DIR}/include)
set(THIRDPARTY_LINK_DIR ${THIRDPARTY_INSTALL_DIR}/lib)
set(THIRDPARTY_BIN_DIR ${THIRDPARTY_INSTALL_DIR}/bin)


set(SODIUM_SOURCE_DIR ${THIRDPARTY_SOURCE_DIR}/libsodium)
set(OPUS_SOURCE_DIR ${THIRDPARTY_SOURCE_DIR}/opus)
set(VPX_SOURCE_DIR ${THIRDPARTY_SOURCE_DIR}/libvpx)
set(TOXCORE_SOURCE_DIR ${THIRDPARTY_SOURCE_DIR}/c-toxcore)
set(PROTOBUF_SOURCE_DIR ${THIRDPARTY_SOURCE_DIR}/protobuf)

set(PROTOC_HOST_EXE ${CMAKE_CURRENT_SOURCE_DIR}/_host/install/bin/protoc)


if (${ANDROID_ABI} STREQUAL "armeabi-v7a")
  set(VPX_TARGET armv7-android-gcc)
  set(TARGET armv7a-linux-androideabi)
  set(BASE_TARGET arm-linux-androideabi)
elseif (${ANDROID_ABI} STREQUAL "arm64-v8a")
  set(VPX_TARGET arm64-android-gcc)
  set(TARGET aarch64-linux-android)
  set(BASE_TARGET ${TARGET})
elseif (${ANDROID_ABI} STREQUAL "x86")
  set(VPX_TARGET x86-android-gcc)
  set(TARGET i686-linux-android)
  set(BASE_TARGET ${TARGET})
  set(UNSET_AS_IF_NEED unset AS &&)
elseif (${ANDROID_ABI} STREQUAL "x86_64")
  set(VPX_TARGET x86_64-android-gcc)
  set(TARGET x86_64-linux-android)
  set(BASE_TARGET ${TARGET})
  set(UNSET_AS_IF_NEED unset AS &&)
endif()

set(TOOLCHAIN ${ANDROID_NDK}/toolchains/llvm/prebuilt/linux-x86_64)
set(TOOLCHAIN_CLANG_BIN ${TOOLCHAIN}/bin/${TARGET}${ANDROID_NATIVE_API_LEVEL})
set(TOOLCHAIN_BIN ${TOOLCHAIN}/bin/${BASE_TARGET})

#
# Dependencies
#

find_package(PkgConfig REQUIRED)

set(ENV_VARS export CC=${CMAKE_C_COMPILER}\ --target=${TARGET}${ANDROID_NATIVE_API_LEVEL}\ -fPIC && export CXX=${CMAKE_CXX_COMPILER}\ --target=${TARGET}${ANDROID_NATIVE_API_LEVEL}\ -fPIC && export AR=${TOOLCHAIN}/bin/llvm-ar && export LD=${TOOLCHAIN}/bin/ld && export AS=${TOOLCHAIN}/bin/${BASE_TARGET}-as && export STRIP=${TOOLCHAIN}/bin/llvm-strip && export NM=${TOOLCHAIN}/bin/llvm-nm && export RANLIB=${TOOLCHAIN}/bin/llvm-ranlib &&)

ExternalProject_Add(
        libsodium
        PREFIX ${THIRDPARTY_INSTALL_DIR}
        SOURCE_DIR ${SODIUM_SOURCE_DIR}
        CONFIGURE_COMMAND ${ENV_VARS} autoreconf -fi ${SODIUM_SOURCE_DIR} && ${SODIUM_SOURCE_DIR}/configure --srcdir=${SODIUM_SOURCE_DIR} --prefix=${THIRDPARTY_INSTALL_DIR} --host=${TARGET}${ANDROID_NATIVE_API_LEVEL} --disable-shared
        BUILD_COMMAND make
        INSTALL_COMMAND make install
)

ExternalProject_Add(
        libopus
        PREFIX ${THIRDPARTY_INSTALL_DIR}
        SOURCE_DIR ${OPUS_SOURCE_DIR}
        CONFIGURE_COMMAND ${ENV_VARS} autoreconf -fi ${OPUS_SOURCE_DIR} && ${OPUS_SOURCE_DIR}/configure --srcdir=${OPUS_SOURCE_DIR} --prefix=${THIRDPARTY_INSTALL_DIR} --host=${TARGET}${ANDROID_NATIVE_API_LEVEL} --disable-shared
        BUILD_COMMAND make
        INSTALL_COMMAND make install
)

ExternalProject_Add(
        libvpx
        PREFIX ${THIRDPARTY_INSTALL_DIR}
        SOURCE_DIR ${VPX_SOURCE_DIR}
        PATCH_COMMAND git checkout ${VPX_SOURCE_DIR} && patch -p1 < ${THIRDPARTY_SOURCE_DIR}/libvpx.patch
        CONFIGURE_COMMAND ${ENV_VARS} ${UNSET_AS_IF_NEED} ${VPX_SOURCE_DIR}/configure --prefix=${THIRDPARTY_INSTALL_DIR} --libc=${CMAKE_SYSROOT} --target=${VPX_TARGET} --disable-examples --disable-unit-tests --enable-pic
        BUILD_COMMAND make
        INSTALL_COMMAND make install
)


ExternalProject_Add(
        libtoxcore
        PREFIX ${THIRDPARTY_INSTALL_DIR}
        SOURCE_DIR ${TOXCORE_SOURCE_DIR}
        CONFIGURE_COMMAND ${ENV_VARS} export CFLAGS=-I${THIRDPARTY_INSTALL_DIR}/include && export LDFLAGS=-L${THIRDPARTY_INSTALL_DIR}/lib && autoreconf -fi ${TOXCORE_SOURCE_DIR} && ${TOXCORE_SOURCE_DIR}/configure --prefix=${THIRDPARTY_INSTALL_DIR} --srcdir=${TOXCORE_SOURCE_DIR} --host=${TARGET}${ANDROID_NATIVE_API_LEVEL} --disable-rt --disable-shared
        BUILD_COMMAND make
        INSTALL_COMMAND make install
)

ExternalProject_Add(
        libprotobuf
        PREFIX ${THIRDPARTY_INSTALL_DIR}
        SOURCE_DIR ${PROTOBUF_SOURCE_DIR}
        CONFIGURE_COMMAND ${ENV_VARS} autoreconf -fi ${PROTOBUF_SOURCE_DIR} && ${PROTOBUF_SOURCE_DIR}/configure --srcdir=${PROTOBUF_SOURCE_DIR} --prefix=${THIRDPARTY_INSTALL_DIR} --host=${TARGET}${ANDROID_NATIVE_API_LEVEL} --disable-shared --with-protoc=${PROTOC_HOST_EXE}
        BUILD_COMMAND make
        INSTALL_COMMAND make install
)

find_package(JNI)
if(JAVA_INCLUDE_PATH)
  include_directories(${JAVA_INCLUDE_PATH})
endif()
if(JAVA_INCLUDE_PATH2)
  include_directories(${JAVA_INCLUDE_PATH2})
endif()

include_directories(".")
include_directories(${CMAKE_BINARY_DIR})

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++14")

#
# C++ standard library features
#

include(CheckCXXSourceCompiles)

# ::gets
check_cxx_source_compiles("
#include <cstdio>
using ::gets;
int main() {}
" HAVE_GETS)
if(HAVE_GETS)
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DHAVE_GETS=1")
endif()

# std::make_unique
check_cxx_source_compiles("
#include <memory>
using std::make_unique;
int main() {}
" HAVE_MAKE_UNIQUE)
if(HAVE_MAKE_UNIQUE)
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DHAVE_MAKE_UNIQUE=1")
endif()

# std::to_string
check_cxx_source_compiles("
#include <string>
using std::to_string;
int main() {}
" HAVE_TO_STRING)
if(HAVE_TO_STRING)
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DHAVE_TO_STRING=1")
endif()

#
# Build
#

set(PBUF_PROTOC ${PROTOC_HOST_EXE})
custom_protobuf_generate_cpp(PROTO_SRCS PROTO_HDRS
  ./ToxAv/Av.proto
  ./ToxCore/Core.proto
  ./util/ProtoLog.proto
)

set(PROTOBUF_INCLUDE_DIR ${CMAKE_CURRENT_BINARY_DIR})
include_directories(${PROTOBUF_INCLUDE_DIR})
include_directories(${THIRDPARTY_INCLUDE_DIR})
add_dependencies(libtoxcore libsodium libopus libvpx libprotobuf)

#execute_process(COMMAND "echo" "${THIRDPARTY_BIN_DIR}/protoc")

add_library(${PROJECT_NAME} SHARED
  ${ANDROID_CPU_FEATURES}
  ${PROTO_SRCS}
  ${PROTO_HDRS}
  ./ToxAv/generated/enums.cpp
  ./ToxAv/generated/errors.cpp
  ./ToxAv/generated/impls.h
  ./ToxAv/generated/im_tox_tox4j_impl_jni_ToxAvJni.h
  ./ToxAv/generated/natives.h
  ./ToxAv/av.cpp
  ./ToxAv/debug.cpp
  ./ToxAv/lifecycle.cpp
  ./ToxAv/ToxAv.cpp
  ./ToxAv/ToxAv.h
  ./ToxCore/generated/constants.h
  ./ToxCore/generated/enums.cpp
  ./ToxCore/generated/errors.cpp
  ./ToxCore/generated/impls.h
  ./ToxCore/generated/im_tox_tox4j_impl_jni_ToxCoreJni.h
  ./ToxCore/generated/natives.h
  ./ToxCore/clientinfo.cpp
  ./ToxCore/connection.cpp
  ./ToxCore/custom.cpp
  ./ToxCore/debug.cpp
  ./ToxCore/filetransfer.cpp
  ./ToxCore/friendlist.cpp
  ./ToxCore/interaction.cpp
  ./ToxCore/lifecycle.cpp
  ./ToxCore/ToxCore.cpp
  ./ToxCore/ToxCore.h
  ./ToxCrypto/generated/constants.h
  ./ToxCrypto/generated/errors.cpp
  ./ToxCrypto/generated/im_tox_tox4j_impl_jni_ToxCryptoJni.h
  ./ToxCrypto/generated/natives.h
  ./ToxCrypto/debug.cpp
  ./ToxCrypto/encryptsave.cpp
  ./ToxCrypto/hash.cpp
  ./ToxCrypto/ToxCrypto.cpp
  ./ToxCrypto/ToxCrypto.h
  ./Tox4j.cpp
  ./cpp14compat.h
  ./tox4j/ToxInstances.h
  ./tox4j/Tox4j.h
  ./tox/av.cpp
  ./tox/av.h
  ./tox/common.h
  ./tox/core.cpp
  ./tox/core.h
  ./tox/generated/av.h
  ./tox/generated/core.h
  ./util/jni/ArrayFromJava.cpp
  ./util/jni/ArrayFromJava.h
  ./util/jni/ArrayToJava.cpp
  ./util/jni/ArrayToJava.h
  ./util/jni/Enum.h
  ./util/jni/UTFChars.cpp
  ./util/jni/UTFChars.h
  ./util/debug_log.cpp
  ./util/debug_log.h
  ./util/exceptions.cpp
  ./util/exceptions.h
  ./util/instance_manager.h
  ./util/logging.cpp
  ./util/logging.h
  ./util/pp_attributes.h
  ./util/pp_cat.h
  ./util/to_bytes.cpp
  ./util/to_bytes.h
  ./util/unused.h
  ./util/wrap_void.h
)

add_dependencies(${PROJECT_NAME} libtoxcore)

target_link_directories(${PROJECT_NAME} PRIVATE ${THIRDPARTY_LINK_DIR})
target_link_libraries(${PROJECT_NAME} toxcore toxav toxencryptsave opus vpx sodium protobuf protoc log)

if(ANDROID_CPU_FEATURES)
  target_compile_definitions(${PROJECT_NAME} PRIVATE -Dtypeof=__typeof__)
endif()

# Windows and OSX don't have this linker functionality.
if(NOT WIN32 AND NOT APPLE)
  set_target_properties(${PROJECT_NAME} PROPERTIES LINK_FLAGS
    "-Wl,-z,defs -Wl,--version-script,${CMAKE_SOURCE_DIR}/libtox4j-c.ld")
endif()

#
# Install
#

install(TARGETS ${PROJECT_NAME} LIBRARY DESTINATION "lib")

#
# Testing
#

find_package(GTest)
if(GTEST_FOUND)
  add_executable(tox4j-test
    test/util/jni/ArrayFromJava_test.cpp
    test/util/jni/ArrayToJava_test.cpp
    test/util/jni/UTFChars_test.cpp
    test/util/debug_log_test.cpp
    test/util/exceptions_test.cpp
    test/util/instance_manager_test.cpp
    test/util/to_bytes_test.cpp
    test/util/wrap_void_test.cpp
    test/tox4j/ToxInstances_test.cpp
    test/tox/common_test.cpp
    test/main.cpp
    test/mock_jni.cpp
    test/mock_jni.h
  )

  target_include_directories(tox4j-test PUBLIC ${GTEST_INCLUDE_DIRS})

  target_link_libraries(tox4j-test ${GTEST_BOTH_LIBRARIES})
  target_link_libraries(tox4j-test tox4j-c)

  add_test(tox4j-test tox4j-test)
endif()
