#!/bin/bash

echo "=== PLC 配置文件设置脚本 v3.1 ==="

# 创建配置目录
echo "创建配置目录..."
mkdir -p /home/hyit/plc_core

# 复制配置文件
echo "复制配置文件..."
cp config/plc_config.json /home/hyit/plc_core/

# 设置权限
echo "设置文件权限..."
chmod 644 /home/hyit/plc_core/*.json
chown hyit:hyit /home/hyit/plc_core/*.json

# 验证文件
echo "验证配置文件..."
echo "系统配置:"
ls -la /home/hyit/plc_core/plc_config.json
echo "用户配置:"
ls -la /home/hyit/plc_core/user_config.json

echo "✅ 配置文件设置完成！"
echo ""
echo "现在可以运行PLC:"
echo "sudo ./build/plc_core"


