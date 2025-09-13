# -*- coding: utf-8 -*-
"""
GPIO控制模块
管理振动马达、LED、蜂鸣器等硬件设备
"""

import time
import threading
import logging
from gpiozero.pins.lgpio import LGPIOFactory
from gpiozero import Device, LED
from config import GPIO_CONFIG

class GPIOController:
    """GPIO控制器类"""
    
    def __init__(self):
        self.gpio_available = False
        self.vibrator = None
        self.led = None
        self.buzzer = None
        self._gpio_initialized = False
        # 延迟初始化，提高启动速度
        # self._init_gpio()
    
    def _init_gpio(self):
        """初始化GPIO设备"""
        if self._gpio_initialized:
            return
            
        try:
            Device.pin_factory = LGPIOFactory(chip=0)
            self.vibrator = LED(GPIO_CONFIG["vibrator"])
            self.led = LED(GPIO_CONFIG["led"])
            self.buzzer = LED(GPIO_CONFIG["buzzer"])
            
            # 确保所有GPIO引脚初始化为低电平
            self.vibrator.off()
            self.led.off()
            self.buzzer.off()
            
            self.gpio_available = True
            self._gpio_initialized = True
            logging.info("GPIO初始化成功：LED(GPIO70)、振动马达(GPIO71)、蜂鸣器(GPIO72)已配置并设置为低电平")
        except Exception as e:
            logging.warning(f"GPIO初始化失败: {e}，GPIO功能将被禁用")
            self.gpio_available = False
            self._gpio_initialized = True  # 标记为已尝试初始化
    
    def trigger_level1_alert(self):
        """触发一级分心警报（LED闪烁）"""
        if not self._gpio_initialized:
            self._init_gpio()
        
        if not self.gpio_available:
            return
        
        try:
            # 一级警报：LED闪烁3次
            for _ in range(3):
                self.led.on()
                time.sleep(0.5)
                self.led.off()
                time.sleep(0.5)
            logging.info(" 一级警报触发：LED闪烁（GPIO70）")
        except Exception as e:
            logging.error(f"GPIO70 触发失败: {e}")
    
    def trigger_level2_alert(self):
        """触发二级分心警报（LED闪烁+振动马达间歇性震动）"""
        if not self._gpio_initialized:
            self._init_gpio()
        
        if not self.gpio_available:
            return
        
        try:
            # 二级警报：LED闪烁 + 振动马达间歇性震动
            for _ in range(5):
                # LED闪烁
                self.led.on()
                time.sleep(0.3)
                self.led.off()
                time.sleep(0.2)
                
                # 振动马达震动
                self.vibrator.on()
                time.sleep(0.5)
                self.vibrator.off()
                time.sleep(0.5)
            logging.info(" 二级警报触发：LED闪烁+振动马达间歇性震动（GPIO70+71）")
        except Exception as e:
            logging.error(f"GPIO71 触发失败: {e}")
    
    def trigger_level3_alert(self):
        """触发三级分心警报（蜂鸣器常响+LED闪烁+振动马达间歇性震动）"""
        if not self._gpio_initialized:
            self._init_gpio()
        
        if not self.gpio_available:
            return
        
        try:
            # 三级警报：蜂鸣器常响 + LED闪烁 + 振动马达间歇性震动
            # 蜂鸣器常响5秒
            self.buzzer.on()
            
            # 同时进行LED闪烁和振动马达间歇性震动
            for _ in range(10):  # 5秒内执行10次循环
                # LED闪烁
                self.led.on()
                time.sleep(0.2)
                self.led.off()
                time.sleep(0.3)
                
                # 振动马达间歇性震动
                self.vibrator.on()
                time.sleep(0.3)
                self.vibrator.off()
                time.sleep(0.2)
            
            # 关闭蜂鸣器
            self.buzzer.off()
            logging.info(" 三级警报触发：蜂鸣器常响+LED闪烁+振动马达间歇性震动（GPIO70+71+72）")
        except Exception as e:
            logging.error(f"GPIO72 触发失败: {e}")
    
    def trigger_manual(self, gpio, duration):
        """手动触发指定GPIO"""
        if not self._gpio_initialized:
            self._init_gpio()
        
        if not self.gpio_available:
            return
        
        try:
            if gpio == GPIO_CONFIG["led"]:
                self.led.on()
                time.sleep(duration)
                self.led.off()
                logging.info(f"手动触发 GPIO{gpio}（LED）：{duration}秒高电平")
            elif gpio == GPIO_CONFIG["vibrator"]:
                self.vibrator.on()
                time.sleep(duration)
                self.vibrator.off()
                logging.info(f"手动触发 GPIO{gpio}（振动马达）：{duration}秒高电平")
            elif gpio == GPIO_CONFIG["buzzer"]:
                self.buzzer.on()
                time.sleep(duration)
                self.buzzer.off()
                logging.info(f"手动触发 GPIO{gpio}（蜂鸣器）：{duration}秒高电平")
            else:
                logging.error(f"无效的 GPIO 编号: {gpio}")
        except Exception as e:
            logging.error(f"手动触发 GPIO{gpio} 失败: {e}")
    
    def cleanup(self):
        """清理GPIO资源"""
        if self.gpio_available:
            try:
                self.vibrator.off()
                self.vibrator.close()
                self.led.off()
                self.led.close()
                self.buzzer.off()
                self.buzzer.close()
                logging.info("GPIO 已清理")
            except Exception as e:
                logging.error(f"GPIO 清理失败: {e}")
    
    def is_available(self):
        """检查GPIO是否可用"""
        return self.gpio_available
    
    def force_init_and_reset(self):
        """强制初始化GPIO并重置所有引脚为低电平"""
        try:
            if not self._gpio_initialized:
                self._init_gpio()
            
            if self.gpio_available:
                # 强制设置为低电平
                self.vibrator.off()
                self.led.off()
                self.buzzer.off()
                logging.info("GPIO强制重置：所有引脚(GPIO70-LED/71-振动马达/72-蜂鸣器)已设置为低电平")
                return True
            else:
                logging.warning("GPIO不可用，无法重置引脚状态")
                return False
        except Exception as e:
            logging.error(f"GPIO强制重置失败: {e}")
            return False
