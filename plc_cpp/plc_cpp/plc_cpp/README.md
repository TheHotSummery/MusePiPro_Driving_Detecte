# PLC C++ 控制系统

## 简介
基于C++的PLC控制系统，用于控制GPIO输出，实现定时闪烁功能。

## 功能特性
- 6个GPIO输出控制 (Q0-Q5)
- 定时器控制 (T1-T6)
- 系统配置和用户配置分离
- 共享内存通信
- 看门狗保护
- 热重载配置

## 快速开始

### 1. 编译
```bash
chmod +x build.sh
./build.sh
```

### 2. 设置配置
```bash
mkdir -p /home/hyit/plc_core
cp config/*.json /home/hyit/plc_core/
```

### 3. 运行
```bash
chmod +x run.sh
./run.sh
```

### 4. 停止
```bash
chmod +x stop.sh
./stop.sh
```

### 5. 检查状态
```bash
chmod +x status.sh
./status.sh
```

## 文件说明

- `build.sh` - 编译脚本
- `run.sh` - 运行脚本
- `stop.sh` - 停止脚本
- `status.sh` - 状态检查脚本
- `config/` - 配置文件模板
- `build/bin/plc_core` - 主程序
- `build/lib/libplc_interface.so` - 接口库

## 配置说明

### 系统配置 (system_config.json)
- 控制Q3, Q4, Q5输出
- 使用T1, T2, T3定时器
- 间隔: 2s, 1s, 0.1s

### 用户配置 (user_config.json)
- 控制Q0, Q1, Q2输出
- 使用T4, T5, T6定时器
- 间隔: 2s, 1s, 0.1s

## 故障排除

1. **编译失败**: 检查依赖 `sudo apt-get install libgpiod-dev`
2. **权限问题**: 使用sudo运行 `sudo ./build/bin/plc_core`
3. **进程无法停止**: 使用 `sudo pkill -9 -f plc_core`
4. **配置文件错误**: 检查JSON格式

## 注意事项

- 需要sudo权限运行
- 确保GPIO引脚未被占用
- 修改配置后需要重启PLC
- 使用Ctrl+C或stop.sh脚本停止