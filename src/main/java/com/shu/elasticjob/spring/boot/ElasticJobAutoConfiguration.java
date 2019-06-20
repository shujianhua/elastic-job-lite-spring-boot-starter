package com.shu.elasticjob.spring.boot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.config.dataflow.DataflowJobConfiguration;
import com.dangdang.ddframe.job.config.script.ScriptJobConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.event.rdb.JobEventRdbConfiguration;
import com.dangdang.ddframe.job.executor.handler.JobProperties.JobPropertiesEnum;
import com.dangdang.ddframe.job.lite.api.listener.AbstractDistributeOnceElasticJobListener;
import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.shu.elasticjob.spring.boot.annotation.ElasticJobLite;

@Configuration
@ConditionalOnExpression("'${elaticjob.zookeeper.serverList}'.length() > 0")
public class ElasticJobAutoConfiguration {

	/**
	 * 如: host1:2181,host2:2181
	 */
	@Value("${spring.elasticjob.zookeeper.serverList}")
	private String serverList;
	/**
	 * Zookeeper的命名空间
	 */
	@Value("${spring.elasticjob.zookeeper.namespace}")
	private String namespace;

	/**
	 * 等待重试的间隔时间的初始值 单位：毫秒
	 */
	@Value("${spring.elasticjob.zookeeper.baseSleepTimeMilliseconds:1000}")
	private int baseSleepTimeMilliseconds = 1000;

	/**
	 * 等待重试的间隔时间的最大值 单位：毫秒
	 */
	@Value("${spring.elasticjob.zookeeper.maxSleepTimeMilliseconds:3000}")
	private int maxSleepTimeMilliseconds = 3000;

	/**
	 * 最大重试次数
	 */
	@Value("${spring.elasticjob.zookeeper.maxRetries:3}")
	private int maxRetries = 3;

	/**
	 * 连接超时时间 单位：毫秒
	 */
	@Value("${spring.elasticjob.zookeeper.connectionTimeoutMilliseconds:15000}")
	private int connectionTimeoutMilliseconds = 15000;

	/**
	 * 会话超时时间 单位：毫秒
	 */
	@Value("${spring.elasticjob.zookeeper.sessionTimeoutMilliseconds:60000}")
	private int sessionTimeoutMilliseconds = 60000;

	/**
	 * 连接Zookeeper的权限令牌 缺省为不需要权限验
	 */
	@Value("${spring.elasticjob.zookeeper.digest:}")
	private String digest;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private ZookeeperRegistryCenter regCenter;

	/**
	 * 初始化zookeeper注册中心
	 * @return ZookeeperRegistryCenter
	 */
	@Bean(initMethod = "init")
	public ZookeeperRegistryCenter regCenter() {
		ZookeeperConfiguration zookeeperConfiguration = new ZookeeperConfiguration(serverList, namespace);
		zookeeperConfiguration.setBaseSleepTimeMilliseconds(baseSleepTimeMilliseconds);
		zookeeperConfiguration.setConnectionTimeoutMilliseconds(connectionTimeoutMilliseconds);
		zookeeperConfiguration.setMaxSleepTimeMilliseconds(maxSleepTimeMilliseconds);
		zookeeperConfiguration.setSessionTimeoutMilliseconds(sessionTimeoutMilliseconds);
		zookeeperConfiguration.setMaxRetries(maxRetries);
		if (StringUtils.isNotBlank(digest)) {
			zookeeperConfiguration.setDigest(digest);
		}
		return new ZookeeperRegistryCenter(zookeeperConfiguration);
	}

	@PostConstruct
	public void initElasticJob() {

		Map<String, ElasticJob> map = applicationContext.getBeansOfType(ElasticJob.class);

		for (Map.Entry<String, ElasticJob> entry : map.entrySet()) {
			ElasticJob elasticJob = entry.getValue();
			ElasticJobLite elasticJobLiteAnnotation = elasticJob.getClass().getAnnotation(ElasticJobLite.class);

			initJob(elasticJobLiteAnnotation, elasticJob);
		}
	}

	/**
	 * 初始化任务
	 */
	protected void initJob(ElasticJobLite elasticJobLiteAnnotation, ElasticJob elasticJob) {
		// 构建核心配置
		JobCoreConfiguration jobCoreConfiguration = getJobCoreConfiguration(elasticJobLiteAnnotation);
		// 获取作业类型配置
		JobTypeConfiguration jobTypeConfiguration = getJobTypeConfiguration(elasticJobLiteAnnotation, jobCoreConfiguration, elasticJob);
		// 获取Lite作业配置
		LiteJobConfiguration liteJobConfiguration = getLiteJobConfiguration(jobTypeConfiguration, elasticJobLiteAnnotation);
		// 获取作业事件追踪的数据源配置
		JobEventRdbConfiguration jobEventRdbConfiguration = getJobEventRdbConfiguration(elasticJobLiteAnnotation.eventTraceRdbDataSource());
		// 获取作业监听器
		ElasticJobListener[] elasticJobListeners = creatElasticJobListeners(elasticJobLiteAnnotation);
		// 注册作业
		if (null == jobEventRdbConfiguration) {
			new SpringJobScheduler(elasticJob, regCenter, liteJobConfiguration, elasticJobListeners).init();
		} else {
			new SpringJobScheduler(elasticJob, regCenter, liteJobConfiguration, jobEventRdbConfiguration, elasticJobListeners).init();
		}
	}

	protected JobCoreConfiguration getJobCoreConfiguration(ElasticJobLite elasticJobAnnotation) {

		JobCoreConfiguration.Builder builder = JobCoreConfiguration
				.newBuilder(elasticJobAnnotation.jobName(), elasticJobAnnotation.cron(), elasticJobAnnotation.shardingTotalCount())
				.shardingItemParameters(elasticJobAnnotation.shardingItemParameters()).jobParameter(elasticJobAnnotation.jobParameter())
				.failover(elasticJobAnnotation.failover()).misfire(elasticJobAnnotation.misfire()).description(elasticJobAnnotation.description());
		if (StringUtils.isNotBlank(elasticJobAnnotation.jobExceptionHandler())) {
			builder.jobProperties(JobPropertiesEnum.JOB_EXCEPTION_HANDLER.getKey(), elasticJobAnnotation.jobExceptionHandler());
		}
		if (StringUtils.isNotBlank(elasticJobAnnotation.executorServiceHandler())) {
			builder.jobProperties(JobPropertiesEnum.EXECUTOR_SERVICE_HANDLER.getKey(), elasticJobAnnotation.executorServiceHandler());
		}
		return builder.build();
	}

	protected JobTypeConfiguration getJobTypeConfiguration(ElasticJobLite elasticJobLiteAnnotation, JobCoreConfiguration jobCoreConfiguration,
			ElasticJob elasticJob) {
		switch (elasticJobLiteAnnotation.jobType()) {
		case SIMPLE:
			return new SimpleJobConfiguration(jobCoreConfiguration, elasticJob.getClass().getCanonicalName());
		case DATAFLOW:
			return new DataflowJobConfiguration(jobCoreConfiguration, elasticJob.getClass().getCanonicalName(), elasticJobLiteAnnotation.streamingProcess());
		case SCRIPT:
			return new ScriptJobConfiguration(jobCoreConfiguration, elasticJobLiteAnnotation.scriptCommandLine());
		default:
			return null;
		}
	}

	/**
	 * 构建Lite作业
	 */
	private LiteJobConfiguration getLiteJobConfiguration(JobTypeConfiguration jobTypeConfiguration, ElasticJobLite elasticJobLiteAnnotation) {
		// 构建Lite作业
		return LiteJobConfiguration.newBuilder(Objects.requireNonNull(jobTypeConfiguration))
				.monitorExecution(elasticJobLiteAnnotation.monitorExecution()).monitorPort(elasticJobLiteAnnotation.monitorPort())
				.maxTimeDiffSeconds(elasticJobLiteAnnotation.maxTimeDiffSeconds())
				.jobShardingStrategyClass(elasticJobLiteAnnotation.jobShardingStrategyClass())
				.reconcileIntervalMinutes(elasticJobLiteAnnotation.reconcileIntervalMinutes()).disabled(elasticJobLiteAnnotation.disabled())
				.overwrite(elasticJobLiteAnnotation.overwrite()).build();
	}

	/**
	 * 获取作业事件追踪的数据源配置
	 *
	 * @param eventTraceRdbDataSource 作业事件追踪的数据源Bean引用
	 * @return JobEventRdbConfiguration
	 */
	private JobEventRdbConfiguration getJobEventRdbConfiguration(String eventTraceRdbDataSource) {
		if (StringUtils.isBlank(eventTraceRdbDataSource)) {
			return null;
		}
		if (!applicationContext.containsBean(eventTraceRdbDataSource)) {
			throw new RuntimeException("The datasource is not exist [" + eventTraceRdbDataSource + "] !");
		}
		DataSource dataSource = (DataSource) applicationContext.getBean(eventTraceRdbDataSource);
		return new JobEventRdbConfiguration(dataSource);
	}

	/**
	 * 获取监听器
	 *
	 */
	private ElasticJobListener[] creatElasticJobListeners(ElasticJobLite elasticJobLiteAnnotation) {

		List<ElasticJobListener> elasticJobListeners = new ArrayList<>(2);

		if (StringUtils.isNotBlank(elasticJobLiteAnnotation.listenerName())) {
			// 注册每台作业节点均执行的监听
			ElasticJobListener elasticJobListener = (ElasticJobListener) applicationContext.getBean(elasticJobLiteAnnotation.listenerName());
			if (null != elasticJobListener) {
				elasticJobListeners.add(elasticJobListener);
			}
		}

		if (StringUtils.isNotBlank(elasticJobLiteAnnotation.distributedListenerName())) {
			// 注册分布式监听者
			AbstractDistributeOnceElasticJobListener distributedListener = (AbstractDistributeOnceElasticJobListener) applicationContext
					.getBean(elasticJobLiteAnnotation.distributedListenerName());
			if (null != distributedListener) {
				elasticJobListeners.add(distributedListener);
			}
		}

		// 集合转数组
		ElasticJobListener[] elasticJobListenerArray = new ElasticJobListener[elasticJobListeners.size()];
		for (int i = 0; i < elasticJobListeners.size(); i++) {
			elasticJobListenerArray[i] = elasticJobListeners.get(i);
		}
		return elasticJobListenerArray;

	}

}