# elastic-job-lite-spring-boot-starter

### 使用步骤 【参考: <a href = "https://github.com/shujianhua/demo-integration/tree/master/demo-elastic-job">使用示例</a>】

#### 创建Spring Boot项目,并在 pom.xml 中依赖：
``` java
<dependency>
    <groupId>com.shu.elasticjob.spring.boot</groupId>
    <artifactId>elastic-job-lite-spring-boot-starter</artifactId>
    <version>${lasted.release.version}</version>
</dependency>
```
#### 创建作业任务服务类：

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
3. 配置文件 application.yml
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

