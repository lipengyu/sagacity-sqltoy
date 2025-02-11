/**
 * 
 */
package org.sagacity.sqltoy.dialect.utils;

import java.io.Serializable;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;

import org.sagacity.sqltoy.SqlToyConstants;
import org.sagacity.sqltoy.SqlToyContext;
import org.sagacity.sqltoy.callback.ReflectPropertyHandler;
import org.sagacity.sqltoy.config.model.EntityMeta;
import org.sagacity.sqltoy.config.model.PKStrategy;
import org.sagacity.sqltoy.config.model.SqlToyConfig;
import org.sagacity.sqltoy.config.model.SqlToyResult;
import org.sagacity.sqltoy.dialect.handler.GenerateSavePKStrategy;
import org.sagacity.sqltoy.dialect.handler.GenerateSqlHandler;
import org.sagacity.sqltoy.dialect.model.ReturnPkType;
import org.sagacity.sqltoy.dialect.model.SavePKStrategy;
import org.sagacity.sqltoy.executor.QueryExecutor;
import org.sagacity.sqltoy.model.QueryResult;
import org.sagacity.sqltoy.utils.DataSourceUtils.DBType;

/**
 * @project sqltoy-orm
 * @description 提供postgresql数据库共用的逻辑实现，便于今后postgresql不同版本之间共享共性部分的实现
 * @author chenrenfei <a href="mailto:zhongxuchen@gmail.com">联系作者</a>
 * @version id:PostgreSqlDialectUtils.java,Revision:v1.0,Date:2015年3月5日
 */
public class PostgreSqlDialectUtils {
	/**
	 * 判定为null的函数
	 */
	public static final String NVL_FUNCTION = "COALESCE";

	/**
	 * @todo 提供随机记录查询
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
		String innerSql = sqlToyConfig.isHasFast() ? sqlToyConfig.getFastSql() : sqlToyConfig.getSql();
		// sql中是否存在排序或union
		boolean hasOrderOrUnion = DialectUtils.hasOrderByOrUnion(innerSql);
		StringBuilder sql = new StringBuilder();

		if (sqlToyConfig.isHasFast())
			sql.append(sqlToyConfig.getFastPreSql()).append(" (");
		// 存在order 或union 则在sql外包裹一层
		if (hasOrderOrUnion)
			sql.append("select sag_random_table.* from (");
		sql.append(innerSql);
		if (hasOrderOrUnion)
			sql.append(") sag_random_table ");
		sql.append(" order by random() limit ");
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
	 * @todo 分頁查詢
	 * @param sqlToyContext
	 * @param sqlToyConfig
	 * @param queryExecutor
	 * @param pageNo
	 * @param pageSize
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public static QueryResult findPageBySql(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig,
			QueryExecutor queryExecutor, Long pageNo, Integer pageSize, Connection conn) throws Exception {
		StringBuilder sql = new StringBuilder();
		boolean isNamed = sqlToyConfig.isNamedParam();
		if (sqlToyConfig.isHasFast()) {
			sql.append(sqlToyConfig.getFastPreSql());
			sql.append(" (").append(sqlToyConfig.getFastSql());
		} else
			sql.append(sqlToyConfig.getSql());
		sql.append(" limit ");
		sql.append(isNamed ? ":" + SqlToyConstants.PAGE_FIRST_PARAM_NAME : "?");
		sql.append(" offset ");
		sql.append(isNamed ? ":" + SqlToyConstants.PAGE_LAST_PARAM_NAME : "?");
		if (sqlToyConfig.isHasFast()) {
			sql.append(") ").append(sqlToyConfig.getFastTailSql());
		}

		SqlToyResult queryParam = DialectUtils.wrapPageSqlParams(sqlToyContext, sqlToyConfig, queryExecutor,
				sql.toString(), Long.valueOf(pageSize), (pageNo - 1) * pageSize);
		return DialectUtils.findBySql(sqlToyContext, sqlToyConfig, queryParam.getSql(), queryParam.getParamsValue(),
				queryExecutor.getRowCallbackHandler(), conn, 0, queryExecutor.getFetchSize(),
				queryExecutor.getMaxRows());
	}

	/**
	 * @todo 实现top记录查询
	 * @param sqlToyContext
	 * @param sqlToyConfig
	 * @param queryExecutor
	 * @param topSize
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public static QueryResult findTopBySql(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig,
			QueryExecutor queryExecutor, double topSize, Connection conn) throws Exception {
		StringBuilder sql = new StringBuilder();
		if (sqlToyConfig.isHasFast()) {
			sql.append(sqlToyConfig.getFastPreSql());
			sql.append(" (").append(sqlToyConfig.getFastSql());
		} else
			sql.append(sqlToyConfig.getSql());
		sql.append(" limit ");
		sql.append(Double.valueOf(topSize).intValue());

		if (sqlToyConfig.isHasFast())
			sql.append(") ").append(sqlToyConfig.getFastTailSql());

		SqlToyResult queryParam = DialectUtils.wrapPageSqlParams(sqlToyContext, sqlToyConfig, queryExecutor,
				sql.toString(), null, null);
		return DialectUtils.findBySql(sqlToyContext, sqlToyConfig, queryParam.getSql(), queryParam.getParamsValue(),
				queryExecutor.getRowCallbackHandler(), conn, 0, queryExecutor.getFetchSize(),
				queryExecutor.getMaxRows());
	}

	/**
	 * @todo 保存单条对象记录
	 * @param sqlToyContext
	 * @param entity
	 * @param conn
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	public static Object save(SqlToyContext sqlToyContext, Serializable entity, Connection conn, String tableName)
			throws Exception {
		// 只支持sequence模式
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entity.getClass());
		PKStrategy pkStrategy = entityMeta.getIdStrategy();
		String sequence = "nextval('" + entityMeta.getSequence() + "')";
		if (pkStrategy != null && pkStrategy.equals(PKStrategy.IDENTITY)) {
			pkStrategy = PKStrategy.SEQUENCE;
			sequence = "nextval(" + entityMeta.getFieldsMeta().get(entityMeta.getIdArray()[0]).getDefaultValue() + ")";
		}

		boolean isAssignPK = isAssignPKValue(pkStrategy);
		String insertSql = DialectUtils.generateInsertSql(DBType.POSTGRESQL, entityMeta, pkStrategy, NVL_FUNCTION,
				sequence, isAssignPK, tableName);
		return DialectUtils.save(sqlToyContext, entityMeta, pkStrategy, isAssignPK, ReturnPkType.GENERATED_KEYS,
				insertSql, entity, new GenerateSqlHandler() {
					public String generateSql(EntityMeta entityMeta, String[] forceUpdateField) {
						PKStrategy pkStrategy = entityMeta.getIdStrategy();
						String sequence = "nextval('" + entityMeta.getSequence() + "')";
						if (pkStrategy != null && pkStrategy.equals(PKStrategy.IDENTITY)) {
							pkStrategy = PKStrategy.SEQUENCE;
							sequence = "nextval("
									+ entityMeta.getFieldsMeta().get(entityMeta.getIdArray()[0]).getDefaultValue()
									+ ")";
						}
						return DialectUtils.generateInsertSql(DBType.POSTGRESQL, entityMeta, pkStrategy, NVL_FUNCTION,
								sequence, isAssignPKValue(pkStrategy), null);
					}
				}, new GenerateSavePKStrategy() {
					public SavePKStrategy generate(EntityMeta entityMeta) {
						return new SavePKStrategy(entityMeta.getIdStrategy(),
								isAssignPKValue(entityMeta.getIdStrategy()));
					}
				}, conn);
	}

	/**
	 * @todo 批量保存对象入数据库
	 * @param sqlToyContext
	 * @param entities
	 * @param batchSize
	 * @param reflectPropertyHandler
	 * @param conn
	 * @param autoCommit
	 * @param tableName
	 * @throws Exception
	 */
	public static Long saveAll(SqlToyContext sqlToyContext, List<?> entities, final int batchSize,
			ReflectPropertyHandler reflectPropertyHandler, Connection conn, final Boolean autoCommit, String tableName)
			throws Exception {
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entities.get(0).getClass());
		PKStrategy pkStrategy = entityMeta.getIdStrategy();
		String sequence = "nextval('" + entityMeta.getSequence() + "')";
		if (pkStrategy != null && pkStrategy.equals(PKStrategy.IDENTITY)) {
			pkStrategy = PKStrategy.SEQUENCE;
			sequence = "nextval(" + entityMeta.getFieldsMeta().get(entityMeta.getIdArray()[0]).getDefaultValue() + ")";
		}

		boolean isAssignPK = isAssignPKValue(pkStrategy);
		String insertSql = DialectUtils.generateInsertSql(DBType.POSTGRESQL, entityMeta, pkStrategy, NVL_FUNCTION,
				sequence, isAssignPK, tableName);
		return DialectUtils.saveAll(sqlToyContext, entityMeta, pkStrategy, isAssignPK, insertSql, entities, batchSize,
				reflectPropertyHandler, conn, autoCommit);
	}

	/**
	 * @todo postgresql9.5以及以上版本的saveOrUpdate语句
	 * @param dbType
	 * @param entityMeta
	 * @param pkStrategy
	 * @param sequence
	 * @param forceUpdateFields
	 * @param tableName
	 * @return
	 */
	public static String getSaveOrUpdateSql(Integer dbType, EntityMeta entityMeta, PKStrategy pkStrategy,
			String sequence, String[] forceUpdateFields, String tableName) {
		String realTable = (tableName == null) ? entityMeta.getSchemaTable() : tableName;
		if (entityMeta.getIdArray() == null)
			return DialectUtils.generateInsertSql(dbType, entityMeta, entityMeta.getIdStrategy(), NVL_FUNCTION, null,
					false, realTable);
		else {
			// 是否全部是ID
			boolean allIds = (entityMeta.getRejectIdFieldArray() == null);
			// 全部是主键采用replace into 策略进行保存或修改,不考虑只有一个字段且是主键的表情况
			StringBuilder sql = new StringBuilder("insert into ");
			StringBuilder values = new StringBuilder();
			sql.append(realTable);
			sql.append(" AS t1 (");
			for (int i = 0, n = entityMeta.getFieldsArray().length; i < n; i++) {
				if (i > 0) {
					sql.append(",");
					values.append(",");
				}
				sql.append(entityMeta.getColumnName(entityMeta.getFieldsArray()[i]));
				values.append("?");
			}
			sql.append(") values (");
			sql.append(values);
			sql.append(") ");
			// 非全部是主键
			if (!allIds) {
				sql.append(" ON CONFLICT ON ");
				if (entityMeta.getPkConstraint() != null) {
					sql.append(" CONSTRAINT ").append(entityMeta.getPkConstraint());
				} else {
					sql.append(" (");
					for (int i = 0, n = entityMeta.getIdArray().length; i < n; i++) {
						if (i > 0) {
							sql.append(",");
						}
						sql.append(entityMeta.getColumnName(entityMeta.getIdArray()[i]));
					}
					sql.append(" ) ");
				}

				sql.append(" DO UPDATE SET ");
				// 需要被强制修改的字段
				HashMap<String, String> forceUpdateColumnMap = new HashMap<String, String>();
				if (forceUpdateFields != null) {
					for (String forceUpdatefield : forceUpdateFields)
						forceUpdateColumnMap.put(entityMeta.getColumnName(forceUpdatefield), "1");
				}
				String columnName;
				for (int i = 0, n = entityMeta.getRejectIdFieldArray().length; i < n; i++) {
					columnName = entityMeta.getColumnName(entityMeta.getRejectIdFieldArray()[i]);
					if (i > 0)
						sql.append(",");
					sql.append(columnName).append("=");
					// 强制修改
					if (forceUpdateColumnMap.containsKey(columnName)) {
						sql.append("excluded.").append(columnName);
					} else {
						sql.append("COALESCE(excluded.");
						sql.append(columnName).append(",t1.");
						sql.append(columnName).append(")");
					}
				}
			}
			return sql.toString();
		}
	}

	/**
	 * @todo postgresql9.5以及以上版本的saveOrUpdate语句
	 * @param dbType
	 * @param entityMeta
	 * @param pkStrategy
	 * @param sequence
	 * @param tableName
	 * @return
	 */
	public static String getSaveIgnoreExist(Integer dbType, EntityMeta entityMeta, PKStrategy pkStrategy,
			String sequence, String tableName) {
		String realTable = (tableName == null) ? entityMeta.getSchemaTable() : tableName;
		if (entityMeta.getIdArray() == null)
			return DialectUtils.generateInsertSql(dbType, entityMeta, entityMeta.getIdStrategy(), NVL_FUNCTION, null,
					false, realTable);
		else {
			// 是否全部是ID
			boolean allIds = (entityMeta.getRejectIdFieldArray() == null);
			// 全部是主键采用replace into 策略进行保存或修改,不考虑只有一个字段且是主键的表情况
			StringBuilder sql = new StringBuilder("insert into ");
			StringBuilder values = new StringBuilder();
			sql.append(realTable);
			sql.append(" AS t1 (");
			for (int i = 0, n = entityMeta.getFieldsArray().length; i < n; i++) {
				if (i > 0) {
					sql.append(",");
					values.append(",");
				}
				sql.append(entityMeta.getColumnName(entityMeta.getFieldsArray()[i]));
				values.append("?");
			}
			sql.append(") values (");
			sql.append(values);
			sql.append(") ");
			// 非全部是主键
			if (!allIds) {
				sql.append(" ON CONFLICT ON ");
				if (entityMeta.getPkConstraint() != null) {
					sql.append(" CONSTRAINT ").append(entityMeta.getPkConstraint());
				} else {
					sql.append(" (");
					for (int i = 0, n = entityMeta.getIdArray().length; i < n; i++) {
						if (i > 0) {
							sql.append(",");
						}
						sql.append(entityMeta.getColumnName(entityMeta.getIdArray()[i]));
					}
					sql.append(" ) ");
				}
				sql.append(" DO NOTHING ");
			}
			return sql.toString();
		}
	}

	private static boolean isAssignPKValue(PKStrategy pkStrategy) {
		if (pkStrategy == null)
			return true;
		// 目前不支持sequence模式
		if (pkStrategy.equals(PKStrategy.SEQUENCE))
			return true;
		if (pkStrategy.equals(PKStrategy.IDENTITY))
			return true;
		return true;
	}
}
