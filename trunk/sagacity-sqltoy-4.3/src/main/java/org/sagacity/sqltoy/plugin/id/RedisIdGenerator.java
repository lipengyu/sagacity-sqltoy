/**
 * 
 */
package org.sagacity.sqltoy.plugin.id;

import java.util.Date;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagacity.sqltoy.SqlToyContext;
import org.sagacity.sqltoy.plugin.IdGenerator;
import org.sagacity.sqltoy.plugin.id.macro.MacroUtils;
import org.sagacity.sqltoy.utils.DateUtil;
import org.sagacity.sqltoy.utils.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;

/**
 * @project sagacity-sqltoy4.0
 * @description 基于redis的集中式主键生成策略
 * @author chenrenfei <a href="mailto:zhongxuchen@gmail.com">联系作者</a>
 * @version id:RedisIdGenerator.java,Revision:v1.0,Date:2018年1月30日
 * @Modification Date:2019-1-24 {key命名策略改为SQLTOY_GL_ID:tableName:xxx 便于redis检索}
 */
public class RedisIdGenerator implements IdGenerator {
	/**
	 * 定义全局日志
	 */
	protected final static Logger logger = LogManager.getLogger(RedisIdGenerator.class);
	private static RedisIdGenerator me = new RedisIdGenerator();

	/**
	 * 全局ID的前缀符号,用于避免在redis中跟其它业务场景发生冲突
	 */
	private final static String GLOBAL_ID_PREFIX = "SQLTOY_GL_ID:";

	/**
	 * @todo 获取对象单例
	 * @param sqlToyContext
	 * @return
	 */
	public static IdGenerator getInstance(SqlToyContext sqlToyContext) {
		return me;
	}

	/**
	 * 日期格式
	 */
	private String dateFormat;

	private RedisTemplate redisTemplate;

	/**
	 * @param redisTemplate
	 *            the redisTemplate to set
	 */
	@Autowired(required = false)
	@Qualifier(value = "redisTemplate")
	public void setRedisTemplate(RedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/**
	 * @return the redisTemplate
	 */
	public RedisTemplate getRedisTemplate() {
		return redisTemplate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.plugin.IdGenerator#getId(java.lang.String,
	 * java.lang.String, java.lang.Object[], int)
	 */
	@Override
	public Object getId(String tableName, String signature, String[] relatedColumns, Object[] relatedColValue,
			Date bizDate, int jdbcType, int length) {
		String key = (signature == null ? "" : signature);
		// 主键生成依赖业务的相关字段值
		HashMap<String, Object> keyValueMap = new HashMap<String, Object>();
		if (relatedColumns != null && relatedColumns.length > 0) {
			for (int i = 0; i < relatedColumns.length; i++) {
				keyValueMap.put(relatedColumns[i].toLowerCase(), relatedColValue[i]);
			}
		}
		// 替换signature中的@df() 和@case()等宏表达式
		String realKey = MacroUtils.replaceMacros(key, keyValueMap, true);
		// 没有宏
		if (realKey.equals(key)) {
			// 长度够放下6位日期
			if (length - realKey.length() > 6) {
				Date realBizDate = (bizDate == null ? new Date() : bizDate);
				realKey = realKey
						.concat(DateUtil.formatDate(realBizDate, (dateFormat == null) ? "yyMMdd" : dateFormat));
			}
		}
		// 参数替换
		if (!keyValueMap.isEmpty()) {
			realKey = MacroUtils.replaceParams(realKey, keyValueMap);
		}
		// 结合redis计数取末尾几位顺序数
		Long result;

		// update 2019-1-24 key命名策略改为SQLTOY_GL_ID:tableName:xxx 便于redis检索
		if (tableName != null)
			result = generateId(realKey.equals("") ? tableName : tableName.concat(":").concat(realKey));
		else
			result = generateId(realKey);
		return realKey + StringUtil.addLeftZero2Len("" + result, length - realKey.length());
	}

	/**
	 * @todo 根据key获取+1后的key值
	 * @param key
	 * @return
	 */
	public long generateId(String key) {
		return generateId(key, 1, null);
	}

	/**
	 * @todo 批量获取key值
	 * @param key
	 * @param increment
	 * @return
	 */
	public long generateId(String key, int increment) {
		return generateId(key, increment, null);
	}

	/**
	 * @todo 批量获取key值,并指定过期时间
	 * @param key
	 * @param increment
	 * @param expireTime
	 * @return
	 */
	public long generateId(String key, int increment, Date expireTime) {
		RedisAtomicLong counter = new RedisAtomicLong(GLOBAL_ID_PREFIX.concat(key),
				redisTemplate.getConnectionFactory());
		// 设置过期时间
		if (expireTime != null)
			counter.expireAt(expireTime);
		// 设置提取多个数量
		if (increment > 1)
			return counter.addAndGet(increment);
		else
			return counter.incrementAndGet();
	}

	/**
	 * @param dateFormat
	 *            the dateFormat to set
	 */
	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}
}
