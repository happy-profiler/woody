# Woody: A Performance Diagnostic and Analysis Tool for Java Applications  

Woody is a specialized tool for diagnosing performance issues in Java applications, designed to help developers:  
1. Identify high-frequency GC problems and locate hotspots of memory allocation (including allocation count and byte size)  
2. Analyze code paths causing high CPU usage (including the proportion of CPU resources consumed)  
3. Trace bottlenecks in interface response time and identify time-consuming operations (including the proportion of time spent)  
4. Diagnose intense lock contention to enable precise optimization  
5. Conduct in-depth analysis of performance issues (CPU, memory, latency) for specific business interfaces or requests  


## Supported Environments  

- **JDK Version**: JDK 1.8 and above  
- **Operating Systems**:  
  - macOS  
  - Linux x64/arm64  
- Lower JDK versions and other operating systems are not currently supported  


## Core Features  

- Command-line based interaction, integrated with async-profiler to generate sampling data and flame graphs  
- Enables precise correlation between business requests and flame graph samples  
- Supports manual filtering of irrelevant business entry points to improve sampling accuracy  
- Extremely low performance overhead, suitable for production environments  
- Leverages minimal code from Arthas (agent and spy modules) to avoid redundant development  


## Supported Middleware  

1. SpringMVC  
2. Dubbo  
3. Grpc  
4. Kafka  
5. RocketMQ  

> Support for additional middleware will be continuously added in future updates  


## Quick Start  

1. Download the latest `woody-boot-xxx.jar` from the project’s release page  
2. Launch the tool:  
   ```bash
   java -jar woody-boot-1.0.0.jar
   ```  
3. Select the target Java process ID to enter the command interaction interface. Use the `stop` command to exit.  

![Woody Launch Interface](https://github.com/user-attachments/assets/3f065671-762e-4b30-a5f5-1e070ee03715)  


## Command Reference  

A single hyphen `-` denotes command operations, while a double hyphen `--` denotes parameters that require a subsequent value.  


### pr (profiling resource) - Select Business Entry Points for Analysis  

Used to specify business entry points for performance analysis; multiple entry points from different middleware can be selected simultaneously.  

| Parameter | Description |  
|-----------|-------------|  
| -ls       | List all business entry points in the current application |  
| -lt       | List all supported business resource types in the current application |  
| -s        | Select business entry points |  
| -us       | Remove selected business entry points |  
| -lst      | List the types of selected business entry points (empty if none selected) |  
| -lss      | List the selected business entry points |  
| --type    | Specify the middleware type (supports the 5 types listed above) |  
| --order   | Specify the resource IDs of middleware business entry points (separate multiple IDs with commas). If not specified, all entry points of the specified type are selected |  

<img width="600" height="1464" alt="image" src="https://github.com/user-attachments/assets/8ce2eb40-c15a-4beb-bdf1-3b40a779f34c" />  


### pe (profiling event) - Select Event Types for Sampling  

Used to specify performance event types for sampling, corresponding to 4 flame graph types in async-profiler.  

| Parameter   | Description |  
|-------------|-------------|  
| -l          | List event types supported by the current application (note: `alloc` may not be supported in some applications, depending on the JDK version and OS) |  
| -s          | Select event types for sampling |  
| --cpu       | CPU event, with the parameter representing the sampling interval (in milliseconds) |  
| --alloc     | Memory allocation event, with the parameter representing the sampling threshold (in kilobytes) |  
| --wall      | Wall-clock time event, with the parameter representing the sampling interval (in milliseconds) |  
| --lock      | Lock contention event, with the parameter representing the sampling interval (in milliseconds) |  
| -c          | Clear selected event types |  

> Multiple event types can be selected simultaneously, and corresponding flame graphs will be generated.  

<img width="600" height="270" alt="image" src="https://github.com/user-attachments/assets/1cdbc405-0eed-4457-b2e0-875dc1036b47" />  


### pf (profiling) - Control Performance Profiling  

Used to start, stop, and check the status of async-profiler.  

| Parameter     | Description |  
|---------------|-------------|  
| start         | Start performance profiling (after starting, the selected business entry points must be triggered within 30 seconds; otherwise, the startup will fail and can be retried) |  
| stop          | Stop performance profiling |  
| status        | Check the current status of profiling (not running / duration of runtime) |  
| --duration    | Set the profiling duration (in seconds); profiling will end automatically when the time elapses (optional, can be ended early with the `stop` command) |  
| --file        | Specify the filename for the flame graph generated after profiling ends (saved in the tool’s running directory by default; a type prefix is automatically added for multiple events). If not specified, sampling data is cached for use with the `ts` command |  

<img width="600" height="314" alt="image" src="https://github.com/user-attachments/assets/b6c96bf8-5c17-4470-bbf3-d87aa86fccbc" />  
<img width="600" height="206" alt="image" src="https://github.com/user-attachments/assets/d6669c88-dad0-4b5d-b7e3-e0fb3c4e3e87" />  


### ts (trace sample) - Retrieve Business Requests & Samples, Generate Flame Graphs  

Used to retrieve performance profiling samples. Supports locating specific requests via `traceId` or viewing the top-N requests with the highest resource consumption.  

| Parameter   | Description |  
|-------------|-------------|  
| -l          | List sampling samples (requires the `--id` or `--top` parameter) |  
| -f          | Generate a flame graph (requires the `--id` or `--top` parameter) |  
| -c          | Clear cached sampling data from the previous profiling session |  
| --file      | Specify the filename for the generated flame graph (used with the `-f` parameter) |  
| --event     | Specify the profiling event type (mandatory if multiple events are selected via the `pe` command; optional if only one event is selected) |  
| --id        | Specify a `traceId` (unique identifier for a business request) to retrieve samples for the corresponding request |  
| --top       | Specify a number N to retrieve the top-N request IDs with the highest resource consumption (displays sample count, start/end time, etc.) |  

> Default `traceId` generation rule: Random number between 1 and Long.MAX_VALUE  
> Custom `traceId` generation logic can be implemented by modifying `ParametricIdGenerator` (extract from business context/parameters/entry objects). Command-based and expression-based `traceId` generation from business requests will be supported in the next version.  

<img width="600" height="1066" alt="image" src="https://github.com/user-attachments/assets/89720447-380c-4499-b734-8a59fc707e56" />  
<img width="600" height="154" alt="image" src="https://github.com/user-attachments/assets/96e5d097-18c0-4518-b5d3-c034d9f2b0cb" />  
<img width="600" height="1822" alt="image" src="https://github.com/user-attachments/assets/c9eb3f90-e282-4a4e-84f4-369e16fa36e7" />  


## Local Compilation & Debugging  

### Local Compilation  
1. Clone the project  
2. Execute `mvn clean package -DskipTests`  
3. The JAR file generated in the `boot` module is the tool package, which can be run directly.  


### Debugging  
1. Add remote debug parameters and port to the target application for analysis:  
   ```bash
   -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -Xdebug
   ```  
2. Associate the Woody project with the remote debug port for debugging.  


## Flame Graph Viewing  

For detailed instructions on viewing flame graphs, refer to relevant documentation or use AI tools for guidance.
