/**
 * 
 */
package org.sagacity.sqltoy.dialect.utils;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

import org.sagacity.sqltoy.SqlToyContext;
import org.sagacity.sqltoy.callback.CallableStatementResultHandler;
import org.sagacity.sqltoy.config.model.EntityMeta;
import org.sagacity.sqltoy.config.model.SqlToyConfig;
import org.sagacity.sqltoy.config.model.SqlToyResult;
import org.sagacity.sqltoy.config.model.SqlType;
import org.sagacity.sqltoy.executor.QueryExecutor;
import org.sagacity.sqltoy.model.LockMode;
import org.sagacity.sqltoy.model.QueryResult;
import org.sagacity.sqltoy.model.StoreResult;
import org.sagacity.sqltoy.utils.ResultUtils;
import org.sagacity.sqltoy.utils.SqlUtil;

import oracle.jdbc.OracleTypes;

/**
 * @project sqltoy-orm
 * @description 提供基于oracle广泛应用的数据库的一些通用的逻辑处理,避免大量重复代码
 * @author chenrenfei <a href="mailto:zhongxuchen@gmail.com">联系作者</a>
 * @version id:OracleDialectUtils.java,Revision:v1.0,Date:2014年12月26日
 */
@SuppressWarnings("rawtypes")
public class OracleDialectUtils {
	/**
	 * 定义日志
	 */
	//private final static Logger logger = LogManager.getLogger(OracleDialectUtils.class);

	/**
	 * @todo 加载单个对象
	 * @param sqlToyContext
	 * @param entity
	 * @param cascadeTypes
	 * @param lockMode
	 * @param conn
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	public static Serializable load(final SqlToyContext sqlToyContext, Serializable entity, List<Class> cascadeTypes,
			LockMode lockMode, Connection conn, String tableName) throws Exception {
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entity.getClass());
		// 获取loadsql(loadsql 可以通过@loadSql进行改变，所以需要sqltoyContext重新获取)
		SqlToyConfig sqlToyConfig = sqlToyContext.getSqlToyConfig(entityMeta.getLoadSql(tableName), SqlType.search);
		String loadSql = sqlToyConfig.getSql();
		if (lockMode != null) {
			switch (lockMode) {
			case UPGRADE_NOWAIT:
				loadSql = loadSql + " for update nowait";
				break;
			case UPGRADE:
				loadSql = loadSql + " for update";
				break;
			}
		}
		return (Serializable) DialectUtils.load(sqlToyContext, sqlToyConfig, loadSql, entityMeta, entity, cascadeTypes,
				conn);
	}

	/**
	 * @todo oracle loadAll 实现
	 * @param sqlToyContext
	 * @param entities
	 * @param cascadeTypes
	 * @param lockMode
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public static List<?> loadAll(final SqlToyContext sqlToyContext, List<?> entities, List<Class> cascadeTypes,
			LockMode lockMode, Connection conn) throws Exception {
		if (null == entities || entities.isEmpty())
			return null;
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entities.get(0).getClass());
		// 判断是否存在主键
		if (null == entityMeta.getIdArray())
			throw new IllegalArgumentException(
					entities.get(0).getClass().getName() + " Entity Object hasn't primary key,cann't use load method!");
		StringBuilder loadSql = new StringBuilder();
		loadSql.append("select * from ");
		loadSql.append(entityMeta.getSchemaTable());
		loadSql.append(" where ");
		String field;
		// 用in 的方式加载全量数据(在实际应用过程中应该注意in () 形式有1000个参数的限制)
		for (int i = 0, n = entityMeta.getIdArray().length; i < n; i++) {
			field = entityMeta.getIdArray()[i];
			if (i > 0)
				loadSql.append(" and ");
			loadSql.append(entityMeta.getColumnName(field));
			loadSql.append(" in (:").append(field).append(") ");
		}
		// 是否锁记录
		if (lockMode != null) {
			switch (lockMode) {
			case UPGRADE_NOWAIT:
				loadSql.append(" for update nowait ");
				break;
			case UPGRADE:
				loadSql.append(" for update ");
				break;
			}
		}
		return DialectUtils.loadAll(sqlToyContext, loadSql.toString(), entities, cascadeTypes, conn);
	}

	/**
	 * @todo 取随机记录
	 * @param sqlToyContext
	 * @param sqlToyConfig
	 * @param queryExecutor
	 * @param totalCount
	 * @param randomCount
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public static QueryResult getRandomResult(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig,
			QueryExecutor queryExecutor, Long totalCount, Long randomCount, Connection conn) throws Exception {
		// 注：dbms_random包需要手工安装，位于$ORACLE_HOME/rdbms/admin/dbmsrand.sql
		String innerSql = sqlToyConfig.isHasFast() ? sqlToyConfig.getFastSql() : sqlToyConfig.getSql();
		// sql中是否存在排序或union
		boolean hasOrderOrUnion = DialectUtils.hasOrderByOrUnion(innerSql);
		StringBuilder sql = new StringBuilder();
		if (sqlToyConfig.isHasFast())
			sql.append(sqlToyConfig.getFastPreSql()).append(" (");
		// 存在order 或union 则在sql外包裹一层
		if (hasOrderOrUnion) {
			sql.append("select * from (");
			sql.append(" select sag_random_table.* from ( ");
			sql.append(innerSql);
			sql.append(") sag_random_table ");
			sql.append(" order by dbms_random.random )");
		} else {
			sql.append("select sag_random_table.* from ( ");
			sql.append(innerSql);
			sql.append(" order by dbms_random.random) sag_random_table ");
		}
		sql.append(" where rownum<=");
		sql.append(randomCount);

		if (sqlToyConfig.isHasFast())
			sql.append(") ").append(sqlToyConfig.getFastTailSql());

		SqlToyResult queryParam = DialectUtils.wrapPageSqlParams(sqlToyContext, sqlToyConfig, queryExecutor,
				sql.toString(), null, null);
		return DialectUtils.findBySql(sqlToyContext, sqlToyConfig, queryParam.getSql(), queryParam.getParamsValue(),
				queryExecutor.getRowCallbackHandler(), conn, 0, queryExecutor.getFetchSize(),
				queryExecutor.getMaxRows());
	}

	/**
	 * @todo <b>oracle 存储过程调用，inParam需放在outParam前面(oracle存储过程返回结果必须用out
	 *       参数返回，返回结果集则out 参数类型必须是OracleTypes.CURSOR,相对其他数据库比较特殊 )</b>
	 * @param sqlToyConfig
	 * @param sqlToyContext
	 * @param storeSql
	 * @param inParamValues
	 * @param outParamTypes
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public static StoreResult executeStore(final SqlToyConfig sqlToyConfig, final SqlToyContext sqlToyContext,
			final String storeSql, final Object[] inParamValues, final Integer[] outParamTypes, final Connection conn)
			throws Exception {
		CallableStatement callStat = null;
		ResultSet rs = null;
		return (StoreResult) SqlUtil.callableStatementProcess(null, callStat, rs, new CallableStatementResultHandler() {
			public void execute(Object obj, CallableStatement callStat, ResultSet rs) throws Exception {
				callStat = conn.prepareCall(storeSql);
				SqlUtil.setParamsValue(conn, callStat, inParamValues, null, 0);
				int cursorIndex = -1;
				int cursorCnt = 0;
				int inCount = (inParamValues == null) ? 0 : inParamValues.length;
				int outCount = (outParamTypes == null) ? 0 : outParamTypes.length;
				// 注册输出参数
				if (outCount != 0) {
					for (int i = 0; i < outCount; i++) {
						callStat.registerOutParameter(i + inCount + 1, outParamTypes[i]);
						if (OracleTypes.CURSOR == outParamTypes[i].intValue()) {
							cursorCnt++;
							cursorIndex = i;
						}
					}
				}
				callStat.execute();
				StoreResult storeResult = new StoreResult();
				// 只返回最后一个CURSOR 类型的数据集
				if (cursorIndex != -1) {
					rs = (ResultSet) callStat.getObject(inCount + cursorIndex + 1);
					QueryResult tempResult = ResultUtils.processResultSet(sqlToyContext, sqlToyConfig, conn, rs, null,
							null, 0);
					storeResult.setLabelNames(tempResult.getLabelNames());
					storeResult.setLabelTypes(tempResult.getLabelTypes());
					storeResult.setRows(tempResult.getRows());
				}

				// 有返回参数(CURSOR 的类型不包含在内)
				if (outCount != 0) {
					Object[] outParams = new Object[outCount - cursorCnt];
					int index = 0;
					for (int i = 0; i < outCount; i++) {
						if (OracleTypes.CURSOR != outParamTypes[i].intValue()) {
							// 存储过程自动分页第一个返回参数是总记录数
							outParams[index] = callStat.getObject(i + inCount + 1);
							index++;
						}
					}
					storeResult.setOutResult(outParams);
				}
				this.setResult(storeResult);
			}
		});
	}
}
