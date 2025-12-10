#!/bin/bash

echo "=== PLC 状态检查 ==="

# 检查进程
echo "1. 检查进程状态:"
if pgrep -f plc_core > /dev/null; then
    echo "✅ PLC进程正在运行"
    echo "进程信息:"
    ps aux | grep plc_core | grep -v grep
else
    echo "❌ PLC进程未运行"
fi

echo ""

# 检查配置文件
echo "2. 检查配置文件:"
if [ -f "/home/hyit/plc_core/system_config.json" ]; then
    echo "✅ 系统配置文件存在"
else
    echo "❌ 系统配置文件不存在"
fi

if [ -f "/home/hyit/plc_core/user_config.json" ]; then
    echo "✅ 用户配置文件存在"
else
    echo "❌ 用户配置文件不存在"
fi

echo ""

# 检查可执行文件
echo "3. 检查可执行文件:"
if [ -f "build/bin/plc_core" ]; then
    echo "✅ PLC可执行文件存在"
    ls -la build/bin/plc_core
else
    echo "❌ PLC可执行文件不存在"
fi

echo ""

# 检查GPIO权限
echo "4. 检查GPIO权限:"
if [ -r "/dev/gpiochip0" ]; then
    echo "✅ GPIO设备可读"
else
    echo "❌ GPIO设备不可读，可能需要sudo权限"
fi

echo ""

# 检查共享内存
echo "5. 检查共享内存:"
if [ -f "/dev/shm/plc_core" ]; then
    echo "✅ 共享内存存在"
    ls -la /dev/shm/plc_core
else
    echo "❌ 共享内存不存在"
fi


