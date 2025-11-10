#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
I2C 总线诊断工具
用于检查I2C设备连接和识别设备类型
"""

import sys

# 尝试导入 smbus2
try:
    from smbus2 import SMBus
    SMBUS2_AVAILABLE = True
except ImportError:
    try:
        import smbus
        SMBus = smbus.SMBus
        SMBUS2_AVAILABLE = False
    except ImportError:
        print("错误: 未找到 smbus 库")
        print("请安装: pip install smbus2")
        sys.exit(1)


def read_device_id(bus, addr):
    """尝试读取设备的ID寄存器"""
    device_info = {
        "address": addr,
        "readable": False,
        "who_am_i": None,
        "device_type": "未知"
    }
    
    try:
        # 尝试读取常见的ID寄存器
        # MPU6050: 0x75 (WHO_AM_I) = 0x68
        try:
            who_am_i = bus.read_byte_data(addr, 0x75)
            device_info["readable"] = True
            device_info["who_am_i"] = who_am_i
            if who_am_i == 0x68:
                device_info["device_type"] = "MPU6050 (可能)"
        except:
            pass
        
        # 尝试读取其他常见寄存器
        # 很多I2C设备在地址0x00或0x01有ID
        try:
            id_reg = bus.read_byte_data(addr, 0x00)
            if not device_info["readable"]:
                device_info["readable"] = True
                device_info["who_am_i"] = id_reg
        except:
            pass
        
    except Exception as e:
        device_info["error"] = str(e)
    
    return device_info


def diagnose_bus(bus_num):
    """诊断I2C总线"""
    print(f"\n{'='*60}")
    print(f"诊断 I2C 总线 {bus_num}")
    print(f"{'='*60}")
    
    try:
        bus = SMBus(bus_num)
        print(f"✅ 总线 {bus_num} 可访问")
    except FileNotFoundError:
        print(f"❌ 总线 {bus_num} 不存在")
        return
    except PermissionError:
        print(f"❌ 总线 {bus_num} 权限不足，请使用 sudo")
        return
    except Exception as e:
        print(f"❌ 总线 {bus_num} 访问失败: {e}")
        return
    
    # 扫描设备
    print(f"\n扫描设备...")
    devices = []
    for addr in range(0x08, 0x78):
        try:
            bus.read_byte(addr)
            devices.append(addr)
        except:
            pass
    
    if not devices:
        print("⚠️  未发现任何设备")
        bus.close()
        return
    
    print(f"✅ 发现 {len(devices)} 个设备:")
    for addr in devices:
        print(f"  地址: 0x{addr:02X}")
    
    # 尝试识别设备
    print(f"\n尝试识别设备...")
    for addr in devices:
        info = read_device_id(bus, addr)
        print(f"\n设备 0x{addr:02X}:")
        print(f"  类型: {info['device_type']}")
        if info['readable']:
            if info['who_am_i'] is not None:
                print(f"  ID寄存器值: 0x{info['who_am_i']:02X}")
        else:
            print(f"  ⚠️  无法读取ID寄存器（可能是普通I2C设备）")
    
    # 特别检查MPU6050
    print(f"\n{'='*60}")
    print("MPU6050 检查")
    print(f"{'='*60}")
    
    mpu_found = False
    for addr in [0x68, 0x69]:
        try:
            who_am_i = bus.read_byte_data(addr, 0x75)
            if who_am_i == 0x68:
                print(f"✅ 找到 MPU6050！")
                print(f"  地址: 0x{addr:02X}")
                print(f"  WHO_AM_I: 0x{who_am_i:02X}")
                mpu_found = True
                break
        except:
            continue
    
    if not mpu_found:
        print("❌ 未找到 MPU6050")
        print("\n可能的原因:")
        print("  1. MPU6050 未正确连接到 I2C 总线")
        print("  2. 硬件连接问题:")
        print("     - SDA 引脚未连接到 GPIO 41")
        print("     - SCL 引脚未连接到 GPIO 40")
        print("     - VCC 未连接到 3.3V 或 5V")
        print("     - GND 未连接到地")
        print("  3. MPU6050 未上电")
        print("  4. I2C 总线编号不正确")
        print("  5. 需要上拉电阻（通常 4.7kΩ）")
    
    bus.close()


def main():
    print("="*60)
    print("I2C 总线诊断工具")
    print("="*60)
    
    # 诊断所有可用的总线
    for bus_num in [0, 1, 2, 3, 4, 5]:
        diagnose_bus(bus_num)
    
    print(f"\n{'='*60}")
    print("硬件检查建议")
    print(f"{'='*60}")
    print("""
1. 检查 MPU6050 连接:
   - VCC  → 3.3V 或 5V
   - GND  → GND
   - SCL  → GPIO 40 (I2C时钟)
   - SDA  → GPIO 41 (I2C数据)
   - AD0  → GND (地址0x68) 或 VCC (地址0x69)

2. 检查上拉电阻:
   - SCL 和 SDA 通常需要 4.7kΩ 上拉电阻到 VCC
   - 某些开发板可能已内置上拉电阻

3. 检查电源:
   - 确保 MPU6050 有电源供应
   - 检查电源电压是否正确

4. 检查 I2C 总线:
   - 确认 GPIO 40/41 对应的是哪个 I2C 总线
   - 某些开发板可能需要配置 GPIO 复用功能

5. 使用万用表检查:
   - SDA/SCL 应该有电压（约 3.3V 或 5V，取决于上拉）
   - 检查是否有短路或断路
    """)


if __name__ == "__main__":
    main()

