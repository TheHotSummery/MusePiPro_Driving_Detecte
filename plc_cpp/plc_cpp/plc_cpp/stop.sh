#!/bin/bash

echo "=== PLC 停止脚本 ==="

# 查找PLC进程
PLC_PID=$(pgrep -f "plc_core" | grep -v grep)

if [ -z "$PLC_PID" ]; then
    echo "❌ 未找到运行中的PLC进程"
    exit 1
fi

echo "找到PLC进程 PID: $PLC_PID"

# 尝试优雅停止
echo "尝试优雅停止..."
sudo kill -TERM $PLC_PID

# 等待3秒
sleep 3

# 检查是否还在运行
if pgrep -f plc_core > /dev/null; then
    echo "优雅停止失败，强制终止..."
    sudo pkill -9 -f plc_core
    sleep 1
fi

# 最终检查
if pgrep -f plc_core > /dev/null; then
    echo "❌ 无法停止PLC进程"
    exit 1
else
    echo "✅ PLC已成功停止"
fi
