cmake_minimum_required(VERSION 3.10)
project(plc_core CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 查找 libgpiod 依赖
find_package(PkgConfig REQUIRED)
pkg_check_modules(LIBGPIOD REQUIRED libgpiod)
pkg_check_modules(LIBMODBUS REQUIRED libmodbus)

# 添加头文件目录
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/include/nlohmann
    ${LIBGPIOD_INCLUDE_DIRS}
    ${LIBMODBUS_INCLUDE_DIRS}
)

# 添加所有源文件以生成可执行文件
add_executable(plc_core
    src/main.cpp
    src/gpio_driver.cpp
    src/shared_memory.cpp
    src/ladder_engine.cpp
    src/timer_counter.cpp
    src/watchdog.cpp
    src/json_parser.cpp
    src/plc_runtime.cpp
)

# 链接所需的库
target_link_libraries(plc_core
    PRIVATE
    pthread
)

if (TARGET PkgConfig::LIBGPIOD)
    target_link_libraries(plc_core PRIVATE PkgConfig::LIBGPIOD)
else()
    target_link_libraries(plc_core PRIVATE ${LIBGPIOD_LIBRARIES})
endif()

if (TARGET PkgConfig::LIBMODBUS)
    target_link_libraries(plc_core PRIVATE PkgConfig::LIBMODBUS)
else()
    target_link_libraries(plc_core PRIVATE ${LIBMODBUS_LIBRARIES})
endif()

target_compile_definitions(plc_core PRIVATE HAS_LIBMODBUS=1)
