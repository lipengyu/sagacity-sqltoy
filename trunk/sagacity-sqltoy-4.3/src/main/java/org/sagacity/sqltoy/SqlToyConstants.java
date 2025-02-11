/**
 * 
 */
package org.sagacity.sqltoy;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagacity.sqltoy.utils.CommonUtils;
import org.sagacity.sqltoy.utils.DataSourceUtils;
import org.sagacity.sqltoy.utils.StringUtil;

/**
 * @project sagacity-sqltoy4.0
 * @description sqlToy的基础常量参数定义
 * @author chenrenfei <a href="mailto:zhongxuchen@gmail.com">联系作者</a>
 * @version id:SqlToyConstants.java,Revision:v1.0,Date:2014年12月26日
 */
public class SqlToyConstants {
	/**
	 * 符号对,用来提取字符串中对称字符的过滤,如:{ name(){} }，第一个{对称的符合}是最后一位
	 */
	public static HashMap<String, String> filters = new HashMap<String, String>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1636155921862321269L;
		{
			put("(", ")");
			put("'", "'");
			put("\"", "\"");
			put("[", "]");
			put("{", "}");
		}
	};

	// 目前还不支持此功能的提醒
	public static String UN_SUPPORT_MESSAGE = "It is not support this function!";

	/**
	 * 判断sql中是否存在union all的表达式
	 */
	public static String UNION_ALL_REGEX = "\\W+union\\s+\\all\\W+";

	/**
	 * 判断sql中是否存在union的表达式
	 */
	public static String UNION_REGEX = "\\W+union\\W+";

	/**
	 * 当sql中是参数条件是?时转换后对应的别名模式:sagParamIndexName+index,如sagParamIndexName0、sagParamIndexName1
	 * 统一成别名模式的好处在于解决诸如分页、取随机记录等封装处理的统一性问题
	 */
	public static String DEFAULT_PARAM_NAME = "sagParamIndexName";

	/**
	 * 随机记录数量参数名称
	 */
	public final static String RANDOM_NAMED = "sagRandomSize";

	/**
	 * 分页开始记录参数Named
	 */
	public final static String PAGE_FIRST_PARAM_NAME = "pageFirstParamName";

	/**
	 * 分页截止记录参数Named
	 */
	public final static String PAGE_LAST_PARAM_NAME = "pageLastParamName";

	/**
	 * 临时表占位符号
	 */
	public final static String TEMPLATE_TABLE_HOLDER = "@templateTable";

	/**
	 * 缓存翻译时在缓存中未匹配上key的返回信息
	 */
	public static String UNCACHED_KEY_RESULT = "[${value}]未定义";

	/**
	 * 存放sqltoy的系统参数
	 */
	private static Properties PROP_ELEMENTS = new Properties();

	/**
	 * sqltoy 默认的配置文件
	 */
	private final static String DEFAULT_CONFIG = "org/sagacity/sqltoy/sqltoy-default.properties";

	public final static String XML_FETURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

	/**
	 * 服务器节点ID
	 */
	public static int WORKER_ID = 0;

	/**
	 * 数据中心ID
	 */
	public static int DATA_CENTER_ID = 0;

	/**
	 * 为22位或26位主键提供的主机Id
	 */
	public static String SERVER_ID;

	public static String keywordSign = "'";

	/**
	 * 字符串中内嵌参数的匹配模式
	 */
	private final static Pattern paramPattern = Pattern
			.compile("\\$\\{\\s*\\_?[0-9a-zA-Z]+((\\.|\\_)[0-9a-zA-Z]+)*(\\[\\d*(\\,)?\\d*\\])?\\s*\\}");

	/**
	 * 不输出sql的表达式
	 */
	public final static Pattern NOT_PRINT_REGEX = Pattern.compile("(?i)\\#not\\_(print|debug)\\#");

	/**
	 * 忽视空记录
	 */
	public final static Pattern IGNORE_EMPTY_REGEX = Pattern.compile("(?i)\\#ignore_all_null_set\\#");

	/**
	 * @todo 解析模板中的参数
	 * @param template
	 * @return
	 */
	private static LinkedHashMap<String, String> parseParams(String template) {
		LinkedHashMap<String, String> paramsMap = new LinkedHashMap<String, String>();
		Matcher m = paramPattern.matcher(template);
		String group;
		while (m.find()) {
			group = m.group();
			// key as ${name} value:name
			paramsMap.put(group, group.substring(2, group.length() - 1));
		}
		return paramsMap;
	}

	/**
	 * @todo 获取常量值
	 * @param key
	 * @return
	 */
	public static String getKeyValue(String key) {
		String result = PROP_ELEMENTS.getProperty(key);
		if (result == null)
			result = System.getProperty(key);
		return result;
	}

	/**
	 * @todo 获取常量值
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static String getKeyValue(String key, String defaultValue) {
		String result = PROP_ELEMENTS.getProperty(key);
		if (result == null)
			result = System.getProperty(key);
		if (StringUtil.isNotBlank(result))
			return result;
		return defaultValue;
	}

	/**
	 * @todo 获取缓存翻译默认过期时长(秒)
	 * @return
	 */
	public static int getCacheExpireSeconds() {
		return Integer.parseInt(getKeyValue("translate.cache.expire.seconds", "3600"));
	}

	/**
	 * @todo db2 是否为查询语句自动补充with ur进行脏读
	 * @return
	 */
	public static boolean db2WithUR() {
		return Boolean.parseBoolean(getKeyValue("db2.search.with.ur", "false"));
	}

	/**
	 * @todo 获取记录提取的警告阀值
	 * @return
	 */
	public static int getWarnThresholds() {
		// 默认值为25000
		return Integer.parseInt(getKeyValue("fetch.result.warn.thresholds.value", "25000"));
	}

	/**
	 * @todo 获取记录提取的最大阀值
	 * @return
	 */
	public static Long getMaxThresholds() {
		// 无限大
		return Long.parseLong(getKeyValue("fetch.result.max.thresholds.value", "999999999999"));
	}

	/**
	 * sybase iq 主键采用identity模式时是否需要在前后开启 SET TEMPORARY OPTION
	 * IDENTITY_INSERT=tableName
	 * 
	 * @return
	 */
	public static boolean sybaseIQIdentityOpen() {
		return Boolean.parseBoolean(getKeyValue("sybase.iq.identity.open", "false"));
	}

	/**
	 * @todo sqlserver identity主键是否要开启 SET IDENTITY_INSERT=tableName ON
	 *       ，2012版本之后无需执行此语句
	 * @return
	 */
	public static boolean sqlServerIdentityOpen() {
		return Boolean.parseBoolean(getKeyValue("sqlserver.identity.open", "false"));
	}

	/**
	 * @todo oracle分页是否忽视排序导致错乱的问题
	 * @return
	 */
	public static boolean oraclePageIgnoreOrder() {
		return Boolean.parseBoolean(getKeyValue("oracle.page.ignore.order", "false"));
	}

	/**
	 * @todo 取随机记录是否采用数据库自带的方言机制
	 * @return
	 */
	public static boolean randomWithDialect(Integer dbType) {
		// 目前是不支持的
		if (dbType == DataSourceUtils.DBType.SYBASE_IQ)
			return false;
		return Boolean.parseBoolean(getKeyValue("random.with.dialect", "true"));
	}

	/**
	 * @todo 获取文件中的常量元素
	 * @param propertiesFile
	 */
	private static void loadPropertyFile(String propertiesFile) {
		// 加载指定的额外参数，或提供开发者修改默认参数
		if (StringUtil.isBlank(propertiesFile))
			return;
		InputStream fis = null;
		try {
			Properties sagacityProps = new Properties();
			fis = CommonUtils.getFileInputStream(propertiesFile);
			sagacityProps.load(fis);
			PROP_ELEMENTS.putAll(sagacityProps);
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @todo 加载数据库方言的参数
	 * @param propertyFile
	 */
	public static void loadProperties(String propertyFile) {
		// 加载默认参数
		loadPropertyFile(DEFAULT_CONFIG);
		// 加载额外的配置文件
		loadPropertyFile(propertyFile);
	}

	/**
	 * @todo 用户可以根据实际数据库方言，通过常量参数转换默认值(如db2 当前时间戳，可以是current
	 *       timestamp,而在oracle中必须是current_timestamp)
	 * @param dbType
	 * @param defaultValue
	 * @return
	 */
	public static String getDefaultValue(Integer dbType, String defaultValue) {
		String realDefault = getKeyValue(defaultValue);
		if (realDefault == null) {
			if (defaultValue.toUpperCase().equals("CURRENT TIMESTAMP"))
				return "CURRENT_TIMESTAMP";
			return defaultValue;
		} else
			return realDefault;
	}

	/**
	 * @param uncachedKeyResult
	 *            the uncachedKeyResult to set
	 */
	public static void setUncachedKeyResult(String uncachedKeyResult) {
		UNCACHED_KEY_RESULT = uncachedKeyResult;
	}

	/**
	 * @todo 替换变量参数
	 * @param template
	 * @return
	 */
	public static String replaceParams(String template) {
		if (StringUtil.isBlank(template))
			return template;
		LinkedHashMap<String, String> paramsMap = parseParams(template);
		String result = template;
		if (paramsMap.size() > 0) {
			Map.Entry<String, String> entry;
			String value;
			for (Iterator<Map.Entry<String, String>> iter = paramsMap.entrySet().iterator(); iter.hasNext();) {
				entry = iter.next();
				value = getKeyValue(entry.getValue());
				if (value != null)
					result = result.replace(entry.getKey(), value);
			}
		}
		return result;
	}

	// /**
	// * @return the keywordSign
	// */
	// public static String getKeywordSign() {
	// return keywordSign;
	// }
	//
	// /**
	// * @param keywordSign the keywordSign to set
	// */
	// public static void setKeywordSign(String keywordSign) {
	// SqlToyConstants.keywordSign = keywordSign;
	// }

}
