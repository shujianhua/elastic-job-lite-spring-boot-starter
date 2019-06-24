package com.shu.elasticjob.spring.boot.annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.JobType;

/**
 * Elastic Job Lite 注解类
 * @author shujianhua
 */
@Inherited
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface ElasticJobLite {

	public abstract JobType jobType() default JobType.SIMPLE;

	/**
	 * cron表达式，用于控制作业触发时间
	 */
	public abstract String cron() default "";

	/**
	 * 任务名称
	 */
	public abstract String jobName() default "";

	/**
	 * 作业分片总数
	 */
	public abstract int shardingTotalCount() default 1;

	/**
	 * 分片序列号和参数用等号分隔，多个键值对用逗号分隔 分片序列号从0开始，不可大于或等于作业分片总数 如：0=a,1=b,2=c
	 */
	public abstract String shardingItemParameters() default "";

	/**
	 * 作业自定义参数 作业自定义参数，可通过传递该参数为作业调度的业务方法传参，用于实现带参数的作业 
	 * 例：每次获取的数据量、作业实例从数据库读取的主键等
	 */
	public abstract String jobParameter() default "";

	/**
	 * 是否开启失效转移
	 */
	public abstract boolean failover() default false;

	/**
	 * 是否开启错过任务重新执行
	 */
	public abstract boolean misfire() default true;

	/**
	 * 作业描述信息
	 */
	public abstract String description() default "";

	/**
	 * 扩展异常处理类
	 */
	public abstract String jobExceptionHandler() default "";

	/**
	 * 扩展作业处理线程池类
	 */
	public abstract String executorServiceHandler() default "";

	/**
	 * 作业是否禁止启动 可用于部署作业时，先禁止启动，部署结束后统一启动
	 */
	public abstract boolean disabled() default false;

	/**
	 * 本地配置是否可覆盖注册中心配置 如果可覆盖，每次启动作业都以本地配置为准
	 */
	public abstract boolean overwrite() default true;

	/**
	 * 监控作业运行时状态 每次作业执行时间和间隔时间均非常短的情况，建议不监控作业运行时状态以提升效率。 
	 * 因为是瞬时状态，所以无必要监控。请用户自行增加数据堆积监控。并且不能保证数据重复选取，应在作业中实现幂等性。
	 * 每次作业执行时间和间隔时间均较长的情况，建议监控作业运行时状态，可保证数据不会重复选取。
	 */
	public abstract boolean monitorExecution() default true;

	/**
	 * 作业监控端口 建议配置作业监控端口, 方便开发者dump作业信息。 使用方法: echo “dump” | nc 127.0.0.1 9888
	 */
	public abstract int monitorPort() default -1;

	/**
	 * 最大允许的本机与注册中心的时间误差秒数 如果时间误差超过配置秒数则作业启动时将抛异常 配置为-1表示不校验时间误差
	 */
	public abstract int maxTimeDiffSeconds() default -1;

	/**
	 * 作业分片策略实现类全路径 默认使用平均分配策略 详情参见：作业分片策略 http://elasticjob.io/docs/elastic-job-lite/02-guide/job-sharding-strategy
	 */
	public abstract String jobShardingStrategyClass() default "";

	/**
	 * 作业事件追踪的数据源Bean引用
	 */
	public abstract String eventTraceRdbDataSource() default "";

	/**
	 * 修复作业服务器不一致状态服务调度间隔时间，配置为小于1的任意值表示不执行修复 单位：分钟
	 */
	public abstract int reconcileIntervalMinutes() default 10;

	/**
	 * 前置后置任务监听实现类Bean引用
	 * 需实现ElasticJobListener接口
	 */
	public abstract String listenerName() default "";

	/**
	 * 前置后置任务分布式监听实现类Bean引用
	 * 需继承AbstractDistributeOnceElasticJobListener类
	 */
	public abstract String distributedListenerName() default "";
	
	/**
	 * 是否流式处理数据 (DataflowJob类型的作业专有属性)
	 */
	public abstract boolean streamingProcess() default false;
	//
	/**
	 * 脚本命令行 (ScriptJob 类型的作业专有属性)
	 */
	public abstract String scriptCommandLine() default "";
}
