#!/bin/bash

echo "=== PLC C++ 快速构建脚本 (修复版) ==="

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
    echo "可执行文件:"
    ls -la bin/plc_core
    ls -la lib/libplc_interface.so
    echo ""
    echo "使用方法:"
    echo "1. 创建配置目录: mkdir -p /home/hyit/plc_core"
    echo "2. 复制配置文件: cp config/*.json /home/hyit/plc_core/"
    echo "3. 运行PLC: sudo ./build/bin/plc_core"
    echo ""
    echo "停止PLC: Ctrl+C 或 sudo pkill -f plc_core"
else
    echo "❌ 编译失败，请检查错误信息"
    exit 1
fi


