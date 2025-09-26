# Woody：Java应用性能诊断分析工具

Woody是一款专注于Java应用性能问题诊断的工具，旨在帮助开发者
1. 定位高GC频率问题，识别内存分配热点
2. 分析CPU使用率过高的代码路径
3. 追踪接口耗时瓶颈，定位内部操作耗时占比
4. 诊断锁竞争问题，支持精准优化
5. 针对特定业务接口/请求的性能问题（CPU、内存、耗时）进行深度分析

## 适用环境

- **JDK版本**：支持JDK 1.8及以上
- **操作系统**：
  - macOS
  - Linux x64/arm64
- 低版本JDK和其他操作系统暂不支持

## 核心特性

- 基于命令行交互，集成async-profiler生成采样样本和火焰图
- 实现业务请求与火焰图样本的精确关联，可自定义表达式取请求参数属性生成请求id
- 支持手动过滤无关业务入口，提高采样精准率
- 极低性能损耗，适合生产环境使用

## 支持中间件

1. SpringMVC
2. Dubbo
3. Grpc
4. Kafka
5. RocketMQ

> 后续将持续扩展支持更多中间件

## 快速开始

1. 从项目release页面下载最新版本的`woody-boot-xxx.jar`
2. 启动工具：
   ```bash
   java -jar woody-boot-1.0.0.jar
   ```
3. 选择目标Java进程编号，进入命令交互界面，输入stop结束退出

![Woody启动界面](https://github.com/user-attachments/assets/3f065671-762e-4b30-a5f5-1e070ee03715)

## 命令参考

单横杠`-`表示命令操作，双横杠`--`表示参数，后续要接参数值

### fn（Function）- 自定义函数命令

用于定义表达式，实现**业务上下文过滤**或**自定义traceId生成**，支持灵活适配不同业务场景的参数提取与筛选需求。

| 参数 | 说明 |
|------|------|
| -n | 新建自定义函数（需配合--exp参数） |
| -l | 列举已创建的所有自定义函数（展示函数类型、表达式、创建时间） |
| -c | 清空所有已创建的自定义函数 |
| -b | 标记函数类型为“布尔过滤型”<br>（未指定此参数时，函数默认为“取值生成型”，用于生成traceId） |
| --exp | 自定义表达式（必须以两个`##`开头），根据函数类型不同，表达式格式有差异：<br>1. 取值生成型（默认，用于生成traceId）：<br>   - 格式：`##[参数下标].属性/方法(参数1,参数2)`，支持多层递进<br>   - 示例1：`##[0].orderId`（提取第0个请求参数的orderId属性）<br>   - 示例2：`##[1].getHeader("serviceId")`（调用第1个参数的getHeader方法，传入"serviceId"参数）<br>   - 示例3：`##this.getService(null)`（调用当前入口对象的getService方法，传入null参数）<br>2. 布尔过滤型（需加`-b`）：<br>   - 格式：`##属性/参数对比逻辑`，暂不支持`&&`/`||`等逻辑连接符<br>   - 示例1：`##[0].status == 101`（筛选第0个参数status为101的请求）<br>   - 示例2：`##this.moduleName == 'payment'`（筛选当前入口对象moduleName为"payment"的请求）<br>   - 示例3：`##[0].userId == #target[1].creatorId`（对比第0个参数的userId与第1个参数的creatorId是否相等） |

> 注意：函数创建后立即生效，后续的性能采样会自动应用已定义的“过滤型函数”筛选请求，“取值型函数”可配合`ig`命令用于traceId生成。

<img width="1172" height="224" alt="image" src="https://github.com/user-attachments/assets/ee7f537a-4604-4f4c-9337-c631396a8996" />


### ig（ID Generator）- 自定义traceId生成器

用于配置**traceId的生成来源**，支持关联已通过`fn`命令创建的自定义函数，实现业务化的traceId生成规则（替代默认的随机数traceId）。

| 参数 | 说明 |
|------|------|
| -n | 新建traceId生成器（需配合--target和--fn参数） |
| -l | 列举已配置的所有traceId生成器（展示目标对象、关联函数、生效状态） |
| -c | 清空所有已配置的traceId生成器（清空后恢复默认随机数生成规则） |
| --target | 指定traceId生成的“数据来源目标”，支持两种取值：<br> - `this`：表示当前业务入口对象（如SpringMVC的Controller实例、Dubbo的Provider实例）<br> - `param[index]`：表示业务请求的第N个参数（index为参数下标，如`param[0]`表示第1个参数） |
| --fn | 指定关联的自定义函数序号（通过`fn -l`可查看已创建函数的序号）<br> - 仅支持关联“取值生成型”函数（即未加`-b`创建的fn函数）<br> - 生成traceId时，工具会自动调用该函数从`--target`指定的对象中提取值，作为最终的traceId |

> 示例：若已通过`fn -n --exp ##[0].orderId`创建序号为1的“取值函数”，执行`ig -n --target param[0] --fn 1`后，后续请求的traceId将自动提取第0个参数的orderId属性，实现“订单号=traceId”的业务关联。

<img width="1284" height="490" alt="image" src="https://github.com/user-attachments/assets/e6b95d1e-2b34-4dfa-bcba-f963f5b22664" />


### pr（profiling resource）- 选择分析的业务入口

用于指定需要分析的业务入口资源，可同时选择多种中间件的多个入口。

| 参数 | 说明 |
|------|------|
| -ls | 列举当前应用的所有业务入口资源 |
| -lt | 列举当前应用支持的业务资源类型 |
| -s | 选择业务入口资源 |
| -us | 移除已选中的业务入口资源 |
| -lst | 列举已选择的业务入口资源类型列表（未选择时为空） |
| -lss | 列举已选择的业务入口资源 |
| --type | 指定中间件类型（支持上述5种类型） |
| --order | 指定中间件业务入口的资源编号（多编号用英文逗号分隔）<br>不指定时表示选择该类型的所有入口资源 |
| --id | 指定使用请求id生成器需要，默认是0，即用时间戳(微妙)生成请求id |

<img width="800" height="600" alt="image" src="https://github.com/user-attachments/assets/0afed170-f959-448c-b374-c66427c30ffc" />




### pe（profiling event）- 选择采集事件类型

用于指定需要采集的性能事件类型，对应async-profiler的4种火焰图类型。

| 参数 | 说明 |
|------|------|
| -l | 列举当前应用支持的事件类型<br>（注：部分应用可能不支持alloc，取决于JDK版本和操作系统） |
| -s | 选择要采集的事件类型 |
| --cpu | CPU事件，参数为采样间隔（ms） |
| --alloc | 内存分配事件，参数为采样阈值（kb） |
| --wall | 耗时事件，参数为采样间隔（ms） |
| --lock | 锁竞争事件，参数为采样间隔（ms） |
| -c | 清除已选中的事件类型 |

> 支持同时选择多个事件类型，将生成对应类型的火焰图

<img width="600" height="270" alt="image" src="https://github.com/user-attachments/assets/1cdbc405-0eed-4457-b2e0-875dc1036b47" />


### pf（profiling）- 操作性能分析过程

用于控制async-profiler的启动、停止和状态查询。

| 参数 | 说明 |
|------|------|
| start | 启动性能分析<br>（启动后需在30秒内触发已选择的业务入口请求，否则启动失败） |
| stop | 结束性能分析 |
| status | 查询当前性能分析状态（未运行/已运行时长） |
| --duration | 设置分析持续时间（秒），时间到后自动结束<br>（非必须，可通过stop命令提前结束） |
| --file | 指定分析结束后生成的火焰图文件名<br>（默认生成在工具运行目录，多事件时会自动添加类型前缀）<br>（未指定时，采样结果将被缓存，供ts命令使用） |

<img width="600" height="314" alt="image" src="https://github.com/user-attachments/assets/b6c96bf8-5c17-4470-bbf3-d87aa86fccbc" />
<img width="600" height="206" alt="image" src="https://github.com/user-attachments/assets/d6669c88-dad0-4b5d-b7e3-e0fb3c4e3e87" />


### ts（trace sample）- 检索分析业务请求和样本，生成火焰图

用于检索性能分析样本，支持通过traceId定位特定请求，或查看资源消耗TopN的请求。

| 参数 | 说明 |
|------|------|
| -l | 列出采样样本（需配合--id或--top参数） |
| -f | 生成火焰图（需配合--id或--top参数） |
| -c | 清除缓存的前次分析样本数据 |
| --file | 指定生成的火焰图文件名（配合-f参数使用） |
| --event | 指定分析事件类型<br>（当pe命令选择多个事件时必须指定，单个事件时可省略） |
| --id | 指定traceId（业务请求唯一标识），检索对应请求的样本 |
| --top | 指定数量N，检索资源消耗最多的前N个请求ID<br>（将显示样本数量、起止时间等信息） |

> traceId默认生成规则：1~Long.MAX_VALUE间的随机数<br>
> 可通过修改`ParametricIdGenerator`实现自定义traceId生成逻辑（从业务上下文/参数/入口对象提取），下个版本可通过命令及表达式从业务请求生成

<img width="600" height="1066" alt="image" src="https://github.com/user-attachments/assets/89720447-380c-4499-b734-8a59fc707e56" />
<img width="600" height="154" alt="image" src="https://github.com/user-attachments/assets/96e5d097-18c0-4518-b5d3-c034d9f2b0cb" />
<img width="600" height="1822" alt="image" src="https://github.com/user-attachments/assets/c9eb3f90-e282-4a4e-84f4-369e16fa36e7" />





## 如何本地编译及调试
本地编译: clone工程，执行 `mvn clean package -DskipTests` ，boot模块生成的jar包就是工具包，直接运行即可

调试: 待分析应用添加远程debug参数和端口 `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -Xdebug` ，woody工程直接远程关联debug即可

## 火焰图查看

火焰图的具体查看方法请参考相关文档或通过AI工具学习。
