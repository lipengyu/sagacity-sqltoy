/**
 * 
 */
package org.sagacity.sqltoy.plugin.function;

import org.sagacity.sqltoy.plugin.IFunction;
import org.sagacity.sqltoy.utils.DataSourceUtils.DBType;

/**
 * @project sqltoy-orm
 * @description 针对mysql数据库字符连接函数concat在其它数据库中的函数转换
 * @author renfei.chen <a href="mailto:zhongxuchen@gmail.com">联系作者</a>
 * @version id:Concat.java,Revision:v1.0,Date:2013-3-21
 */
public class Concat extends IFunction {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.config.function.IFunction#dialects()
	 */
	@Override
	public String dialects() {
		return "mysql8";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.config.function.IFunction#regex()
	 */
	@Override
	public String regex() {
		return "(?i)\\Wconcat\\(";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.config.function.IFunction#wrap(int,
	 * java.lang.String[])
	 */
	@Override
	public String wrap(int dialect, String functionName, boolean hasArgs, String... args) {
		if (dialect == DBType.SQLSERVER || dialect == DBType.SQLSERVER2017 || dialect == DBType.SQLSERVER2014
				|| dialect == DBType.SQLSERVER2016 || dialect == DBType.SQLSERVER2019) {
			if (args != null) {
				StringBuilder result = new StringBuilder();
				for (int i = 0; i < args.length; i++) {
					if (i > 0)
						result.append("+");
					result.append(args[i]);
				}
				return result.toString();
			}
		} else if (dialect == DBType.ORACLE || dialect == DBType.ORACLE12) {
			if (args != null && args.length > 2) {
				StringBuilder result = new StringBuilder();
				for (int i = 0; i < args.length; i++) {
					if (i > 0)
						result.append("||");
					result.append(args[i]);
				}
				return result.toString();
			}
		}
		return null;
	}

}
