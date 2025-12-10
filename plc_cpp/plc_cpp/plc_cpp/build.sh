#!/bin/bash

echo "=== PLC C++ 构建脚本 v3.1 ==="

# 检查依赖
echo "检查依赖..."
if ! command -v pkg-config &> /dev/null; then
    echo "❌ 缺少 pkg-config"
    exit 1
fi

if ! pkg-config --exists libgpiod; then
    echo "❌ 缺少 libgpiod 开发库"
    echo "请运行: sudo apt-get install libgpiod-dev"
    exit 1
fi

echo "✅ 依赖检查完成"

# 清理并重新构建
echo "清理构建目录..."
rm -rf build
mkdir -p build

echo "配置CMake..."
cd build
cmake .. -DCMAKE_BUILD_TYPE=Release

if [ $? -ne 0 ]; then
    echo "❌ CMake配置失败"
    exit 1
fi

echo "开始编译..."
make -j$(nproc)

if [ $? -eq 0 ]; then
    echo "🎉 编译成功！"
    echo "可执行文件位于: ./build/plc_core"
    ls -la ./plc_core
    echo ""
    echo "下一步:"
    echo "1. 运行配置脚本: ./setup_config.sh"
    echo "2. 运行PLC: sudo ./build/plc_core"
else
    echo "❌ 编译失败，请检查错误信息"
    exit 1
fi