## Woody
Woody项目是一个专注于Java应用性能问题分析诊断的工具，目前在不断完善功能中。

目前碰到以下情形可以通过Woody来诊断分析:

  1. 应用GC频率较高，需要定位哪些业务请求，哪些代码分配内存较多，以及分配次数和分配字节数
  2. 应用CPU使用率较高，需要定位是哪些业务请求，哪些代码使用了大量CPU资源，以及cpu资源消耗比例
  3. 应用接口耗时较长，定位接口内部的哪些操作，哪些代码消耗时间，以及相应的时间消耗比例
  4. 应用锁竞争激烈，需要精准优化
  5. 某个业务接口，某个特定请求有性能问题(cpu,内存，耗时),需要精确定位问题

Woody目前只支持jdk1.8+，支持mac，linux x64/arm64，低版本的jdk和其他操作系统暂时不支持。通过命令行交互，配合async-profiler, 输出采样样本和火焰图来分析定位问题。火焰图查看方式请自行AI学习。

Woody能将业务请求和火焰图样本精确关联，可手动过滤不需要分析的业务入口，提高采样精准率，能将消耗降的很低。

工程少量代码借鉴自arthas, 主要是agent,spy模块,不重复造轮子。

### 支持中间件列表:
  1. SpringMVC
  2. Dubbo
  3. Grpc
  4. Kafka
  5. RocketMQ

目前只支持上述5个中间件，后续会不断丰富。

### 快速开始

从项目的release界面下载最新`woody-boot-xxx.jar`版本，然后用`java -jar` 形式启动
```bash
  java -jar woody-boot-1.0.0.jar
```
<img width="1000" height="406" alt="image" src="https://github.com/user-attachments/assets/3f065671-762e-4b30-a5f5-1e070ee03715" />

然后选择对应的java进程编号，之后会进入命令交互界面。

### 命令列表

#### pr(profiling resource)
可选参数:
* -ls: list resource, 列举出当前应用的业务入口资源
<img width="400" height="400" alt="image" src="https://github.com/user-attachments/assets/3dad9c98-ba42-4551-8208-0dd96730915c" />

* -lt: list resource type, 列举出当前应用的所有业务资源类型
<img width="400" height="132" alt="image" src="https://github.com/user-attachments/assets/cf12ca39-750b-46df-8f0f-51ee450ca013" />

  
* -lst: list selected resource types, 列举出已选择业务入口资源类型列表，没选时为[]
<img width="400" height="138" alt="image" src="https://github.com/user-attachments/assets/2982c61e-7882-440a-906b-bc8020589450" />


* -lss: list selected resource, 列举出已选择的业务入口资源
<img width="400" height="260" alt="image" src="https://github.com/user-attachments/assets/d3d67881-683e-48c7-9c0a-f62b3904af74" />


* -us: unselect, 移除已选中的业务入口资源
* -s : select, 选择业务入口资源
<img width="600" height="130" alt="image" src="https://github.com/user-attachments/assets/cf876f61-32e7-443f-9c6a-4469be4b0aa6" />

* --type
* --order


