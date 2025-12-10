#!/bin/bash

echo "=== PLC 运行脚本 ==="

# 检查可执行文件
if [ ! -f "build/bin/plc_core" ]; then
    echo "❌ 可执行文件不存在，请先编译"
    echo "运行: ./build.sh"
    exit 1
fi

# 检查配置文件
if [ ! -f "/home/hyit/plc_core/system_config.json" ]; then
    echo "❌ 系统配置文件不存在"
    echo "运行: mkdir -p /home/hyit/plc_core && cp config/*.json /home/hyit/plc_core/"
    exit 1
fi

if [ ! -f "/home/hyit/plc_core/user_config.json" ]; then
    echo "❌ 用户配置文件不存在"
    echo "运行: mkdir -p /home/hyit/plc_core && cp config/*.json /home/hyit/plc_core/"
    exit 1
fi

# 检查是否有PLC进程在运行
if pgrep -f plc_core > /dev/null; then
    echo "⚠️  检测到PLC进程正在运行"
    echo "是否要停止现有进程? (y/n)"
    read -r response
    if [ "$response" = "y" ] || [ "$response" = "Y" ]; then
        echo "停止现有进程..."
        sudo pkill -f plc_core
        sleep 2
    else
        echo "退出"
        exit 0
    fi
fi

echo "启动PLC..."
sudo ./build/bin/plc_core


