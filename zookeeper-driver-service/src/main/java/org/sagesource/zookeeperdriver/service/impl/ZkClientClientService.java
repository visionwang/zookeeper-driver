package org.sagesource.zookeeperdriver.service.impl;

import org.apache.curator.retry.ExponentialBackoffRetry;
import org.sagesource.zookeeperdriver.client.manager.ZkClientManager;
import org.sagesource.zookeeperdriver.client.property.ZkClientConnectProperty;
import org.sagesource.zookeeperdriver.client.wrapper.ZkClientWrapper;
import org.sagesource.zookeeperdriver.entity.ZkServerInfo;
import org.sagesource.zookeeperdriver.mapper.ZkServerInfoMapper;
import org.sagesource.zookeeperdriver.service.intf.IZkClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p></p>
 * <pre>
 *     author      Sage XueQi
 *     date        2016/12/1
 *     email       job.xueqi@gmail.com
 * </pre>
 */
@Service
public class ZkClientClientService implements IZkClientService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ZkClientClientService.class);

	@Autowired
	private ZkServerInfoMapper zkServerInfoMapper;

	@Override
	public ZkClientWrapper lineToZookeeper(int serverInfoId) {
		try {
			LOGGER.info("查询zk服务配置 zk_server_info_id=[{}]", serverInfoId);

			ZkServerInfo zkServerInfo = zkServerInfoMapper.selectByPrimaryKey(serverInfoId);
			if (zkServerInfo == null) throw new NullPointerException("ZK服务信息不存在");

			String connectionString    = zkServerInfo.getServers();
			int    retrySleepTime      = zkServerInfo.getRetrySleepTime();
			int    retryTimes          = zkServerInfo.getRetryTimes();
			int    connectionTimeoutMs = zkServerInfo.getConnTimeout();
			int    sessionTimeoutMs    = zkServerInfo.getSessionTimeout();
			LOGGER.info("创建zk连接 参数:connectionString=[{}],retrySleepTime=[{}],retryTimes=[{}],connectionTimeoutMs=[{}],sessionTimeoutMs=[{}]",
					connectionString, retrySleepTime, retryTimes, connectionTimeoutMs, sessionTimeoutMs);

			ZkClientConnectProperty zkClientConnectProperty = new ZkClientConnectProperty();
			zkClientConnectProperty.setConnectionString(connectionString);
			zkClientConnectProperty.setRetryPolicy(new ExponentialBackoffRetry(retrySleepTime, retryTimes));
			zkClientConnectProperty.setConnectionTimeoutMs(connectionTimeoutMs);
			zkClientConnectProperty.setSessionTimeoutMs(sessionTimeoutMs);

			ZkClientWrapper client = ZkClientManager.getZkClient(zkClientConnectProperty);
			LOGGER.info("创建zk连接客户端成功 clientKey=[{}]", client.getClientKey());
			return client;
		} catch (Exception e) {
			LOGGER.error("创建zk连接客户端失败! serverInfoId=[{}]", serverInfoId, e);
			throw e;
		}
	}

	@Override
	public void closeZkClient(ZkClientWrapper zkClient) {
		try {
			LOGGER.info("关闭zk连接 clientKey=[{}]", zkClient.getClientKey());
			ZkClientManager.closeZkClient(zkClient);

			LOGGER.info("关闭zk连接成功 clientKey=[{}]", zkClient.getClientKey());
		} catch (Exception e) {
			LOGGER.error("关闭zk连接客户端失败! serverInfoId=[{}]", zkClient.getClientKey(), e);
			throw e;
		}
	}
}