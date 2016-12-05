package org.sagesource.zookeeperdriver.service.impl;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.sagesource.zookeeperdriver.client.dto.ZkData;
import org.sagesource.zookeeperdriver.client.dto.ZkNode;
import org.sagesource.zookeeperdriver.client.pool.ClientPoolInvoke;
import org.sagesource.zookeeperdriver.client.wrapper.ZkClientWrapper;
import org.sagesource.zookeeperdriver.helper.Constants;
import org.sagesource.zookeeperdriver.helper.exception.ZkDriverBusinessException;
import org.sagesource.zookeeperdriver.service.dto.ZkDataDto;
import org.sagesource.zookeeperdriver.service.dto.ZkNodeDto;
import org.sagesource.zookeeperdriver.service.intf.IZkNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p></p>
 * <pre>
 *     author      Sage XueQi
 *     date        2016/12/1
 *     email       job.xueqi@gmail.com
 * </pre>
 */
@Service
public class ZkNodeService implements IZkNodeService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ZkNodeService.class);

	@Override
	public boolean checkNodeExist(String clientKey, String path) throws Exception {
		LOGGER.info("判断节点是否存在 client_key=[{}],parentPath=[{}]", clientKey, path);
		try {
			if (StringUtils.isEmpty(clientKey)) throw new ZkDriverBusinessException("client key is null");

			return ClientPoolInvoke.invoke(clientKey, client -> client.exist(path));
		} catch (ZkDriverBusinessException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error("判断节点是否存在失败 client_key=[{}],path=[{}]", clientKey, path, e);
			throw e;
		}
	}

	@Override
	public List<ZkNodeDto> findChildrenNode(String clientKey, String parentPath) throws Exception {
		LOGGER.info("获得节点的子节点列表 client_key=[{}],parentPath=[{}]", clientKey, parentPath);
		List<ZkNodeDto> result = new ArrayList<>();
		try {
			if (StringUtils.isEmpty(clientKey)) throw new ZkDriverBusinessException("client key is null");

			List<ZkNode> nodeList = ClientPoolInvoke.invoke(clientKey, client -> client.getChildren(parentPath));
			nodeList.forEach((node) -> {
				ZkNodeDto dto = new ZkNodeDto();
				BeanUtils.copyProperties(node, dto);
				result.add(dto);
			});

			return result;
		} catch (ZkDriverBusinessException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error("获得节点的子节点列表失败 client_key=[{}],parentPath=[{}]", clientKey, parentPath, e);
			throw e;
		}
	}

	@Override
	public ZkDataDto readNodeData(String clientKey, String path) throws Exception {
		LOGGER.info("获取节点的数据 client_key=[{}],path=[{}]", clientKey, path);
		try {
			if (StringUtils.isEmpty(clientKey)) throw new ZkDriverBusinessException("client key is null");

			ZkData    data = ClientPoolInvoke.invoke(clientKey, client -> client.readData(path));
			ZkDataDto dto  = new ZkDataDto();
			dto.setData(new String(data.getData(), "UTF-8"));
			dto.setStat(data.getStat());
			dto.setVersion(data.getStat().getVersion());

			return dto;
		} catch (ZkDriverBusinessException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error("获取节点的数据失败 client_key=[{}],path=[{}]", clientKey, path, e);
			throw e;
		}
	}

	@Override
	public void createNode(String clientKey, String path, String data) throws Exception {
		LOGGER.info("创建节点 client_key=[{}],path=[{}],data=[{}]", clientKey, path, data);
		try {
			if (StringUtils.isEmpty(clientKey)) throw new ZkDriverBusinessException("client key is null");
			if (StringUtils.isEmpty(data)) throw new ZkDriverBusinessException("创建数据为空");
			ClientPoolInvoke.invoke(clientKey, client -> {
				client.create(path, data.getBytes(Constants.CHARSET_UTF_8));
				return null;
			});

		} catch (ZkDriverBusinessException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error("创建节点失败 client_key=[{}],path=[{}],data=[{}]", clientKey, path, data, e);
			throw e;
		}
	}

	@Override
	public void editNodeData(String clientKey, String path, String data) throws Exception {
		LOGGER.info("更新节点数据 client_key=[{}],path=[{}],data=[{}]", clientKey, path, data);
		try {
			if (StringUtils.isEmpty(clientKey)) throw new ZkDriverBusinessException("client key is null");
			if (StringUtils.isEmpty(data)) throw new ZkDriverBusinessException("更新数据为空");

			//1.查询旧的数据版本号
			ZkDataDto oldDataDto = readNodeData(clientKey, path);
			int       oldVersion = oldDataDto.getVersion();

			LOGGER.info("更新节点数据 client_key=[{}],old_data=[{}],old_version=[{}]", clientKey, oldDataDto.getData(), oldVersion);
			ClientPoolInvoke.invoke(clientKey, client -> {
				client.editData(path, data.getBytes(Constants.CHARSET_UTF_8), oldVersion);
				return null;
			});
		} catch (ZkDriverBusinessException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error("更新节点数据失败 client_key=[{}],path=[{}],data=[{}]", clientKey, path, data, e);
			throw e;
		}

	}

	@Override
	public void deleteNode(String clientKey, String path) throws Exception {
		LOGGER.info("删除节点 client_key=[{}],path=[{}]", clientKey, path);
		try {
			//查询旧节点的状态
			ZkDataDto oldZnode = readNodeData(clientKey, path);
			LOGGER.info("删除节点 client_key=[{}],path=[{}],old_znode=[{}]", clientKey, path, ReflectionToStringBuilder.toString(oldZnode));

			ClientPoolInvoke.invoke(clientKey, client -> {
				client.delete(path, true);
				return null;
			});
		} catch (Exception e) {
			LOGGER.error("删除节点失败 client_key=[{}],path=[{}]", clientKey, path);
			throw e;
		}
	}


}
