## BlueSerial

Android Bluetooth Serial App

### 使用手册

* Version 1.0.0  (20131115) *

BlueSerial是一个可以运行在Android智能手机上的一个应用程序。

机器人遥控器通过蓝牙和具有蓝牙功能的机器人控制器（例如：中鸣的E2-RCU控制器）通信，从而实现在手机端就可以无线控制小车、机器人等设备。

运行系统：Android 2.3 或以上版本的智能手机

蓝牙版本：蓝牙2.0或以上

注：E2-RCU控制器的蓝牙要设定为“被动模式”

#### 通信数据包格式：
一次发送6个字节，最前两个字节是数据包头，一个字节是命令编码，三个字节是命令参数。

170 + 85 + cmd + param1 + param2 + param3

170和85是数据包头

cmd是机器人遥控器程序上的功能命令编码

param1，param2，param3是命令对应的参数


#### 功能：

按钮：（从左往右）

按钮1： cmd = 1，param1 =（开 = 1，关 = 0），param2 = 0 ，param3 = 0

按钮2： cmd = 2，param1 =（开 = 1，关 = 0），param2 = 0 ，param3 = 0

按钮3： cmd = 3，param1 = 1 ，param2 = 0 ，param3 = 0

按钮4： cmd = 4，param1 = 1 ，param2 = 0 ，param3 = 0

滑块：   cmd = 5，param1 =  0～100，param2 = 0，param3 = 0


#### 360度摇杆：

cmd  = 15 

param1 = X轴， -100~100 （最左是-100，中间是0，最右是100）

param2 = Y轴， -100~100 （最下是-100，中间是0，最上是100）

param3 = 0


#### 加速度传感器：

cmd = 21

param1 = X轴， -100～100 （屏幕往左翻动是负数，中间是0，屏幕往右翻动正数）

param2 = Y轴， -100～100 （屏幕往前翻动正数，中间是0，屏幕往自己方向翻动负数）

param3 = Z轴， -100～100 （屏幕往上是正数，屏幕往下是负数）

注：手机必须具有加速度传感器才有这个功能


#### 如何使用BlueSerial程序：
1. 打开E2-RCU控制器，把E2-RCU控制器的蓝牙要设定为“被动模式”，记住“蓝牙名称”

2. 点击进入机器人遥控器程序后，点击手机的“菜单”按钮，选择“连接”，在“已匹配列表”里有想要控制的E2-RCU控制器就选择，没有的话就点击“扫描”，扫描想连接到的E2-RCU控制器

3. 在左上角显示连接到E2-RCU控制器后，就可以在手机上实施控制了

4. 在菜单项目中可以打开传感器，打开传感器后就可以用手机的传感器控制。不需要传感器时请关闭传感器。

#### 演示程序：

1. 小车的两个轮子在前是前进方向，在E2-RCU控制器的内置测试中把马达设置为两个轮子的转动方向向前，左边的马达接E2-RCU控制器的M1端口，右边的马达接E2-RCU控制器的M2端口。

2. 程序运行按“A”可以进入控制小车的演示程序，按“B”可以测试数据包的接收状况。

3. 滑块可以控制小车的速度，360度摇杆控制小车的运动。



#### 备注：

已发现有部分HTC手机无法连接到E2-RCU控制器。

想获取程序的最新版本请关注以下网址：https://github.com/jeffreylau7/BlueSerial/

