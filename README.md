# Elastic Job Lite Spring Boot Starter
### 前言
 本项目将 <a href="http://elasticjob.io/docs/elastic-job-lite/00-overview/"> Elastic Job Lite </a>封装成了基于注解形式的使用【@ElasticJobLite（）】，去掉了 xml 配置文件。

### 创建Spring Boot项目,并在 pom.xml 中依赖：
``` java
<dependency>
    <groupId>com.shu.elasticjob.spring.boot</groupId>
    <artifactId>elastic-job-lite-spring-boot-starter</artifactId>
    <version>${lasted.release.version}</version>
</dependency>
```
### 创建作业任务服务类：

1. Simple类型的任务 （监听器可选）
``` java
@ElasticJobLite(cron = "0/5 * * * * ?", jobName = "MySimpleJob", shardingTotalCount = 3, 
  shardingItemParameters = "0=B,1=S,2=G", listenerName = "mySimpleJobListener")
public class MySimpleJob implements SimpleJob {
  @Autowired
	private MyJobService myJobService;
	@Override
	public void execute(ShardingContext shardingContext) {
		//调用业务逻辑实现
		myJobService.doSomething();
	}
}
```
``` java 
@Component("mySimpleJobListener")
public class MySimpleJobListener implements ElasticJobListener {
	private static final Logger log = LoggerFactory.getLogger(MySimpleJobListener.class);
	@Override
	public void afterJobExecuted(ShardingContexts arg0) {
		//任务执行后处理
		//...
	}
	@Override
	public void beforeJobExecuted(ShardingContexts arg0) {
		//任务执行前处理
		//...
	}
}
```
2. Dataflow类型的任务 （监听器可选）
``` java
@ElasticJobLite(jobType = JobType.DATAFLOW, cron = "0/10 * * * * ?", jobName = "myDataflowJob", 
  shardingTotalCount = 2, shardingItemParameters = "0=A,1=B", listenerName = "myDataflowJobListener")
public class MyDataflowJob implements DataflowJob<Student> {
	private static final Logger log = LoggerFactory.getLogger(MyDataflowJob.class);
	@Override
	public List<Student> fetchData(ShardingContext paramShardingContext) {
		List<Student> list = new ArrayList<Student>();
		Student s = new Student();
		s.setUserName("zhangshan");
		list.add(s);
		return list;
	}
	@Override
	public void processData(ShardingContext paramShardingContext, List<Student> paramList) {
		for (Student s: paramList) {
			log.info(s.getUserName());
		}
	}
}

@Component("myDataflowJobListener")
public class MyDataflowJobListener implements ElasticJobListener {
	private static final Logger log = LoggerFactory.getLogger(MyDataflowJobListener.class);
	@Override
	public void afterJobExecuted(ShardingContexts arg0) {
		//任务执行后处理
		//...
	}
	@Override
	public void beforeJobExecuted(ShardingContexts arg0) {
		//任务执行前处理
		//...
	}
}
```
### 配置文件 application.yml
``` yml
spring:
  application:
     name: demoElasticJob
  elasticjob:
    #注册中心配置
    zookeeper:
      serverList: 192.168.20.81:2181
      namespace: namespace-demoElasticJob
#  datasource:
#    url: jdbc:mysql://192.168.20.81:3306/test?useUnicode=true&characterEncoding=utf8&useSSL=false
#    username: root
#    password: root@echinacoop123
#    druid:
#      initial-size: 5 #连接池初始化大小
#      min-idle: 10 #最小空闲连接数
#      max-active: 20 #最大连接数
```

### 注解属性说明 @ElasticJobLite()
属性 | 类型 | 必须 | 默认值 | 描述
----|----|----|----|----
jobType | String | 否 | JobType.SIMPLE | JobType.SIMPLE 或 JobType.DATAFLOW
cron | String | 是 |  | cron表达式，用于控制作业触发时间
jobName | String | 是 |  | 任务名称
shardingTotalCount | int | 否 | 1 | 作业分片总数
shardingItemParameters | String | 否 |  | 分片序列号和参数用等号分隔，多个键值对用逗号分隔 分片序列号从0开始，不可大于或等于作业分片总数 如：0=a,1=b,2=c
jobParameter | String | 否 |  | 作业自定义参数 作业自定义参数，可通过传递该参数为作业调度的业务方法传参，用于实现带参数的作业。例：每次获取的数据量、作业实例从数据库读取的主键等
failover | boolean | 否 | false | 是否开启失效转移
misfire | boolean | 否 | true | 是否开启错过任务重新执行
description | String | 否 |  | 作业描述信息
jobExceptionHandler | String | 否 |  | 扩展异常处理类
executorServiceHandler | String | 否 |  | 扩展作业处理线程池类
disabled | boolean | 否 | false | 作业是否禁止启动 可用于部署作业时，先禁止启动，部署结束后统一启动
overwrite | boolean | 否 | true | 本地配置是否可覆盖注册中心配置 如果可覆盖，每次启动作业都以本地配置为准
monitorExecution | boolean | 否 | true | 监控作业运行时状态 每次作业执行时间和间隔时间均非常短的情况，建议不监控作业运行时状态以提升效率。因为是瞬时状态，所以无必要监控。请用户自行增加数据堆积监控。并且不能保证数据重复选取，应在作业中实现幂等性。 每次作业执行时间和间隔时间均较长的情况，建议监控作业运行时状态，可保证数据不会重复选取。
monitorPort | int | 否 | -1 | 作业监控端口 建议配置作业监控端口, 方便开发者dump作业信息。
maxTimeDiffSeconds | int | 否 | -1 | 最大允许的本机与注册中心的时间误差秒数 如果时间误差超过配置秒数则作业启动时将抛异常 配置为-1表示不校验时间误差
jobShardingStrategyClass | String | 否 |  | 作业分片策略实现类全路径 默认使用平均分配策略 详情参见：作业分片策略 http://elasticjob.io/docs/elastic-job-lite/02-guide/job-sharding-strategy
eventTraceRdbDataSource | String | 否 |  | 作业事件追踪的数据源Bean引用
reconcileIntervalMinutes | int | 否 | 10 | 修复作业服务器不一致状态服务调度间隔时间，配置为小于1的任意值表示不执行修复 单位：分钟
listenerName | String | 否 |  | 前置后置任务监听实现类Bean引用，需实现ElasticJobListener接口
distributedListenerName | String | 否 |  | 前置后置任务分布式监听实现类Bean引用，需继承AbstractDistributeOnceElasticJobListener类
streamingProcess | boolean | 否 | false | 是否流式处理数据 (DataflowJob类型的作业专有属性)

	
- 详细配置请参考：<a href="http://elasticjob.io/docs/elastic-job-lite/02-guide/config-manual">Elastic Job 官网</a>

- 使用代码示例: <a href = "https://github.com/shujianhua/demo-integration/tree/master/demo-elastic-job">使用Demo示例</a>
