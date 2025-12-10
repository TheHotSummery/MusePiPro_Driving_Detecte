@echo off
REM PLC C++ Windows 构建脚本

echo === PLC C++ 构建脚本 (Windows) ===

REM 检查CMake
cmake --version >nul 2>&1
if errorlevel 1 (
    echo 错误: CMake 未安装
    echo 请从 https://cmake.org/download/ 下载并安装CMake
    pause
    exit /b 1
)

REM 检查编译器
g++ --version >nul 2>&1
if errorlevel 1 (
    echo 错误: g++ 未安装
    echo 请安装MinGW-w64或Visual Studio Build Tools
    pause
    exit /b 1
)

echo 依赖检查完成

REM 创建构建目录
echo 创建构建目录...
if not exist build mkdir build
cd build

REM 配置CMake
echo 配置CMake...
cmake .. -G "MinGW Makefiles" -DCMAKE_BUILD_TYPE=Release
if errorlevel 1 (
    echo CMake配置失败
    pause
    exit /b 1
)

REM 编译
echo 开始编译...
cmake --build . --config Release
if errorlevel 1 (
    echo 编译失败
    pause
    exit /b 1
)

echo.
echo === 构建完成 ===
echo 可执行文件: build\bin\plc_core.exe
echo 接口库: build\lib\libplc_interface.dll
echo.
echo 注意: 在Windows环境下，需要安装libgpiod的Windows版本
echo 或者使用WSL2进行Linux环境下的编译
echo.
pause


