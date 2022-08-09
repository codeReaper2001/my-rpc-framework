# 测试接口代码

Controller：

<img src="https://codereaper-image-bed.oss-cn-shenzhen.aliyuncs.com/img/image-20220722172738077.png" alt="image-20220722172738077" width="600" />

服务调用接口：

<img src="https://codereaper-image-bed.oss-cn-shenzhen.aliyuncs.com/img/image-20220722173108722.png" alt="image-20220722173108722" width="300" />

接口参数：

<img src="https://codereaper-image-bed.oss-cn-shenzhen.aliyuncs.com/img/image-20220722173456243.png" alt="image-20220722173456243" width="450" />

服务器端的实现类：

<img src="https://codereaper-image-bed.oss-cn-shenzhen.aliyuncs.com/img/image-20220722173639626.png" alt="image-20220722173639626" width="600" />

# 单机测试结果

## 汇总报告

<img src="https://codereaper-image-bed.oss-cn-shenzhen.aliyuncs.com/img/image-20220722173524844.png" alt="image-20220722173524844" width="1000" />

吞吐量约 7300/s

## 聚合报告

<img src="https://codereaper-image-bed.oss-cn-shenzhen.aliyuncs.com/img/image-20220722173701093.png" alt="image-20220722173701093" width="1000" />

- 90%：8ms
- 95%：9ms
- 99%：15ms

# 两台机器测试结果

辅助机器：一个机器使用JMeter发送请求

服务机器：

- 机器①：启动rpc服务提供端（RPC Server）和rpc服务消费端（"/index"接口）
- 机器②：只启动rpc服务提供端（RPC Server）

测试结果：

<img src="https://codereaper-image-bed.oss-cn-shenzhen.aliyuncs.com/img/image-20220810000226627.png" alt="image-20220810000226627" width="1000"/>

吞吐量可达到 10000/s