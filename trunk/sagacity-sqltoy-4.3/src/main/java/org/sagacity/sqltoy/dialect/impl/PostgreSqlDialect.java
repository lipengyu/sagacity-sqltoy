/**
 * 
 */
package org.sagacity.sqltoy.dialect.impl;

import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagacity.sqltoy.SqlToyContext;
import org.sagacity.sqltoy.callback.ReflectPropertyHandler;
import org.sagacity.sqltoy.callback.RowCallbackHandler;
import org.sagacity.sqltoy.callback.UpdateRowHandler;
import org.sagacity.sqltoy.config.model.EntityMeta;
import org.sagacity.sqltoy.config.model.PKStrategy;
import org.sagacity.sqltoy.config.model.SqlToyConfig;
import org.sagacity.sqltoy.config.model.SqlType;
import org.sagacity.sqltoy.dialect.Dialect;
import org.sagacity.sqltoy.dialect.handler.GenerateSqlHandler;
import org.sagacity.sqltoy.dialect.utils.DialectUtils;
import org.sagacity.sqltoy.dialect.utils.PostgreSqlDialectUtils;
import org.sagacity.sqltoy.executor.QueryExecutor;
import org.sagacity.sqltoy.model.LockMode;
import org.sagacity.sqltoy.model.QueryResult;
import org.sagacity.sqltoy.model.StoreResult;
import org.sagacity.sqltoy.utils.DataSourceUtils.DBType;

/**
 * @project sqltoy-orm
 * @description 基于postgresql9.5+版本的方言实现,9.5开始insert into [ON CONFLICT DO
 *              NOTHING/UPDATE]功能生效
 * @author zhongxuchen <a href="mailto:zhongxuchen@gmail.com">联系作者</a>
 * @version id:PostgreSqlDialect.java,Revision:v1.0,Date:2015年8月10日
 */
@SuppressWarnings({ "rawtypes" })
public class PostgreSqlDialect implements Dialect {
	/**
	 * 定义日志
	 */
	protected final Logger logger = LogManager.getLogger(PostgreSqlDialect.class);

	/**
	 * 判定为null的函数
	 */
	public static final String NVL_FUNCTION = "COALESCE";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#getRandomResult(org.sagacity.sqltoy.
	 * SqlToyContext, org.sagacity.sqltoy.config.model.SqlToyConfig,
	 * org.sagacity.sqltoy.executor.QueryExecutor, java.lang.Long, java.lang.Long,
	 * java.sql.Connection)
	 */
	@Override
	public QueryResult getRandomResult(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig,
			QueryExecutor queryExecutor, Long totalCount, Long randomCount, Connection conn) throws Exception {
		return PostgreSqlDialectUtils.getRandomResult(sqlToyContext, sqlToyConfig, queryExecutor, totalCount,
				randomCount, conn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#findPageBySql(org.sagacity.sqltoy.
	 * SqlToyContext, org.sagacity.sqltoy.config.model.SqlToyConfig,
	 * org.sagacity.sqltoy.executor.QueryExecutor, java.lang.Long,
	 * java.lang.Integer, java.sql.Connection)
	 */
	@Override
	public QueryResult findPageBySql(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig,
			QueryExecutor queryExecutor, Long pageNo, Integer pageSize, Connection conn) throws Exception {
		return PostgreSqlDialectUtils.findPageBySql(sqlToyContext, sqlToyConfig, queryExecutor, pageNo, pageSize, conn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#findTopBySql(org.sagacity.sqltoy.
	 * SqlToyContext, org.sagacity.sqltoy.config.model.SqlToyConfig,
	 * org.sagacity.sqltoy.executor.QueryExecutor, double, java.sql.Connection)
	 */
	@Override
	public QueryResult findTopBySql(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig, QueryExecutor queryExecutor,
			double topSize, Connection conn) throws Exception {
		return PostgreSqlDialectUtils.findTopBySql(sqlToyContext, sqlToyConfig, queryExecutor, topSize, conn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#findBySql(org.sagacity.sqltoy.
	 * SqlToyContext, org.sagacity.sqltoy.config.model.SqlToyConfig,
	 * java.lang.String, java.lang.Object[],
	 * org.sagacity.core.database.callback.RowCallbackHandler, java.sql.Connection)
	 */
	@Override
	public QueryResult findBySql(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig, String sql,
			Object[] paramsValue, RowCallbackHandler rowCallbackHandler, final Connection conn, final int fetchSize,
			final int maxRows) throws Exception {
		return DialectUtils.findBySql(sqlToyContext, sqlToyConfig, sql, paramsValue, rowCallbackHandler, conn, 0,
				fetchSize, maxRows);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#getCountBySql(org.sagacity.sqltoy.
	 * SqlToyContext, java.lang.String, java.lang.Object[], boolean,
	 * java.sql.Connection)
	 */
	@Override
	public Long getCountBySql(SqlToyContext sqlToyContext, String sql, Object[] paramsValue, boolean isLastSql,
			Connection conn) throws Exception {
		return DialectUtils.getCountBySql(sqlToyContext, sql, paramsValue, isLastSql, conn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#load(org.sagacity.sqltoy.
	 * SqlToyContext, java.io.Serializable, java.util.List,
	 * org.sagacity.sqltoy.lock.LockMode, java.sql.Connection)
	 */
	@Override
	public Serializable load(SqlToyContext sqlToyContext, Serializable entity, List<Class> cascadeTypes,
			LockMode lockMode, Connection conn, final Integer dbType, final String tableName) throws Exception {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#loadAll(org.sagacity.sqltoy.
	 * SqlToyContext, java.util.List, java.util.List,
	 * org.sagacity.sqltoy.lock.LockMode, java.sql.Connection)
	 */
	@Override
	public List<?> loadAll(SqlToyContext sqlToyContext, List<?> entities, List<Class> cascadeTypes, LockMode lockMode,
			Connection conn, final Integer dbType, final String tableName) throws Exception {
		if (null == entities || entities.isEmpty())
			return null;
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entities.get(0).getClass());
		// 判断是否存在主键
		if (null == entityMeta.getIdArray())
			throw new IllegalArgumentException(
					entities.get(0).getClass().getName() + " Entity Object hasn't primary key,cann't use load method!");
		StringBuilder loadSql = new StringBuilder();
		loadSql.append("select * from ");
		loadSql.append(tableName == null ? entityMeta.getSchemaTable() : tableName);
		loadSql.append(" where ");
		String field;
		for (int i = 0, n = entityMeta.getIdArray().length; i < n; i++) {
			field = entityMeta.getIdArray()[i];
			if (i > 0)
				loadSql.append(" and ");
			loadSql.append(entityMeta.getColumnName(field));
			loadSql.append(" in (:").append(field).append(") ");
		}
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#save(org.sagacity.sqltoy.
	 * SqlToyContext, java.io.Serializable, java.sql.Connection)
	 */
	@Override
	public Object save(SqlToyContext sqlToyContext, Serializable entity, Connection conn, final String tableName)
			throws Exception {
		return PostgreSqlDialectUtils.save(sqlToyContext, entity, conn, tableName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#saveAll(org.sagacity.sqltoy.
	 * SqlToyContext, java.util.List,
	 * org.sagacity.core.utils.callback.ReflectPropertyHandler, java.sql.Connection)
	 */
	@Override
	public Long saveAll(SqlToyContext sqlToyContext, List<?> entities, final int batchSize,
			ReflectPropertyHandler reflectPropertyHandler, Connection conn, final Boolean autoCommit,
			final String tableName) throws Exception {
		return PostgreSqlDialectUtils.saveAll(sqlToyContext, entities, batchSize, reflectPropertyHandler, conn,
				autoCommit, tableName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#update(org.sagacity.sqltoy.
	 * SqlToyContext, java.io.Serializable, java.lang.String[], boolean,
	 * java.lang.Class[], java.util.HashMap, java.sql.Connection)
	 */
	@Override
	public Long update(SqlToyContext sqlToyContext, Serializable entity, String[] forceUpdateFields, boolean cascade,
			Class[] emptyCascadeClasses, HashMap<Class, String[]> subTableForceUpdateProps, Connection conn,
			final String tableName) throws Exception {
		return DialectUtils.update(sqlToyContext, entity, NVL_FUNCTION, forceUpdateFields, cascade,
				(cascade == false) ? null : new GenerateSqlHandler() {
					public String generateSql(EntityMeta entityMeta, String[] forceUpdateFields) {
						PKStrategy pkStrategy = entityMeta.getIdStrategy();
						String sequence = "nextval('" + entityMeta.getSequence() + "')";
						if (pkStrategy != null && pkStrategy.equals(PKStrategy.IDENTITY)) {
							pkStrategy = PKStrategy.SEQUENCE;
							sequence = "nextval("
									+ entityMeta.getFieldsMeta().get(entityMeta.getIdArray()[0]).getDefaultValue()
									+ ")";
						}
						return PostgreSqlDialectUtils.getSaveOrUpdateSql(DBType.POSTGRESQL, entityMeta, pkStrategy,
								sequence, forceUpdateFields, null);
					}
				}, emptyCascadeClasses, subTableForceUpdateProps, conn, tableName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#updateAll(org.sagacity.sqltoy.
	 * SqlToyContext, java.util.List, java.lang.String[],
	 * org.sagacity.core.utils.callback.ReflectPropertyHandler, java.sql.Connection)
	 */
	@Override
	public Long updateAll(SqlToyContext sqlToyContext, List<?> entities, final int batchSize,
			String[] forceUpdateFields, ReflectPropertyHandler reflectPropertyHandler, Connection conn,
			final Boolean autoCommit, final String tableName) throws Exception {
		return DialectUtils.updateAll(sqlToyContext, entities, batchSize, forceUpdateFields, reflectPropertyHandler,
				NVL_FUNCTION, conn, autoCommit, tableName, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#saveOrUpdate(org.sagacity.sqltoy.
	 * SqlToyContext, java.io.Serializable, java.lang.String[], java.sql.Connection,
	 * java.lang.Boolean)
	 */
	@Override
	public Long saveOrUpdate(SqlToyContext sqlToyContext, Serializable entity, String[] forceUpdateFields,
			Connection conn, final Boolean autoCommit, final String tableName) throws Exception {
		List entities = new ArrayList();
		entities.add(entity);
		return saveOrUpdateAll(sqlToyContext, entities, sqlToyContext.getBatchSize(), null, forceUpdateFields, conn,
				autoCommit, tableName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#saveOrUpdateAll(org.sagacity.sqltoy.
	 * SqlToyContext, java.util.List,
	 * org.sagacity.core.utils.callback.ReflectPropertyHandler, java.lang.String[],
	 * java.sql.Connection, java.lang.Boolean)
	 */
	@Override
	public Long saveOrUpdateAll(SqlToyContext sqlToyContext, List<?> entities, final int batchSize,
			ReflectPropertyHandler reflectPropertyHandler, String[] forceUpdateFields, Connection conn,
			final Boolean autoCommit, final String tableName) throws Exception {
		Long updateCnt = DialectUtils.updateAll(sqlToyContext, entities, batchSize, forceUpdateFields,
				reflectPropertyHandler, NVL_FUNCTION, conn, autoCommit, tableName, true);
		logger.debug("修改记录数为:{}", updateCnt);
		// 如果修改的记录数量跟总记录数量一致,表示全部是修改
		if (updateCnt >= entities.size())
			return updateCnt;
		Long saveCnt = this.saveAllIgnoreExist(sqlToyContext, entities, batchSize, reflectPropertyHandler, conn,
				autoCommit, tableName);
		logger.debug("新建记录数为:{}", saveCnt);
		return updateCnt + saveCnt;
		/*
		 * EntityMeta entityMeta =
		 * sqlToyContext.getEntityMeta(entities.get(0).getClass()); return
		 * DialectUtils.saveOrUpdateAll(sqlToyContext, entities, batchSize, entityMeta,
		 * forceUpdateFields, new GenerateSqlHandler() { public String
		 * generateSql(EntityMeta entityMeta, String[] forceUpdateFields) { PKStrategy
		 * pkStrategy = entityMeta.getIdStrategy(); String sequence = "nextval('" +
		 * entityMeta.getSequence() + "')"; if (pkStrategy != null &&
		 * pkStrategy.equals(PKStrategy.IDENTITY)) { pkStrategy = PKStrategy.SEQUENCE;
		 * sequence = "nextval(" +
		 * entityMeta.getFieldsMeta().get(entityMeta.getIdArray()[0]).getDefaultValue()
		 * + ")"; } return PostgreSqlDialectUtils.getSaveOrUpdateSql(DBType.POSTGRESQL,
		 * entityMeta, pkStrategy, sequence, forceUpdateFields, tableName); } },
		 * reflectPropertyHandler, conn, autoCommit);
		 */
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#saveAllNotExist(org.sagacity.sqltoy.
	 * SqlToyContext, java.util.List,
	 * org.sagacity.sqltoy.callback.ReflectPropertyHandler, java.sql.Connection,
	 * java.lang.Boolean)
	 */
	@Override
	public Long saveAllIgnoreExist(SqlToyContext sqlToyContext, List<?> entities, final int batchSize,
			ReflectPropertyHandler reflectPropertyHandler, Connection conn, final Boolean autoCommit,
			final String tableName) throws Exception {
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entities.get(0).getClass());
		return DialectUtils.saveAllIgnoreExist(sqlToyContext, entities, batchSize, entityMeta,
				new GenerateSqlHandler() {
					public String generateSql(EntityMeta entityMeta, String[] forceUpdateFields) {
						PKStrategy pkStrategy = entityMeta.getIdStrategy();
						String sequence = "nextval('" + entityMeta.getSequence() + "')";
						if (pkStrategy != null && pkStrategy.equals(PKStrategy.IDENTITY)) {
							pkStrategy = PKStrategy.SEQUENCE;
							sequence = "nextval("
									+ entityMeta.getFieldsMeta().get(entityMeta.getIdArray()[0]).getDefaultValue()
									+ ")";
						}
						return PostgreSqlDialectUtils.getSaveIgnoreExist(DBType.POSTGRESQL, entityMeta, pkStrategy,
								sequence, tableName);
					}
				}, reflectPropertyHandler, conn, autoCommit);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#delete(org.sagacity.sqltoy.
	 * SqlToyContext, java.io.Serializable, java.sql.Connection)
	 */
	@Override
	public Long delete(SqlToyContext sqlToyContext, Serializable entity, Connection conn, final String tableName)
			throws Exception {
		return DialectUtils.delete(sqlToyContext, entity, conn, tableName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#deleteAll(org.sagacity.sqltoy.
	 * SqlToyContext, java.util.List, java.sql.Connection)
	 */
	@Override
	public Long deleteAll(SqlToyContext sqlToyContext, List<?> entities, final int batchSize, Connection conn,
			final Boolean autoCommit, final String tableName) throws Exception {
		return DialectUtils.deleteAll(sqlToyContext, entities, batchSize, conn, autoCommit, tableName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#updateFetch(org.sagacity.sqltoy.
	 * SqlToyContext, org.sagacity.sqltoy.config.model.SqlToyConfig,
	 * java.lang.String, java.lang.Object[],
	 * org.sagacity.core.database.callback.UpdateRowHandler, java.sql.Connection)
	 */
	@Override
	public QueryResult updateFetch(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig, String sql,
			Object[] paramValues, UpdateRowHandler updateRowHandler, Connection conn, final Integer dbType)
			throws Exception {
		String realSql = sql + " for update nowait";
		return DialectUtils.updateFetchBySql(sqlToyContext, sqlToyConfig, realSql, paramValues, updateRowHandler, conn,
				0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#updateFetchTop(org.sagacity.sqltoy.
	 * SqlToyContext, org.sagacity.sqltoy.config.model.SqlToyConfig,
	 * java.lang.String, java.lang.Object[], java.lang.Integer,
	 * org.sagacity.core.database.callback.UpdateRowHandler, java.sql.Connection)
	 */
	@Override
	public QueryResult updateFetchTop(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig, String sql,
			Object[] paramsValue, Integer topSize, UpdateRowHandler updateRowHandler, Connection conn,
			final Integer dbType) throws Exception {
		String realSql = sql + " limit " + topSize + " for update nowait";
		return DialectUtils.updateFetchBySql(sqlToyContext, sqlToyConfig, realSql, paramsValue, updateRowHandler, conn,
				0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagacity.sqltoy.dialect.Dialect#updateFetchRandom(org.sagacity.sqltoy
	 * .SqlToyContext, org.sagacity.sqltoy.config.model.SqlToyConfig,
	 * java.lang.String, java.lang.Object[], java.lang.Integer,
	 * org.sagacity.core.database.callback.UpdateRowHandler, java.sql.Connection)
	 */
	@Override
	public QueryResult updateFetchRandom(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig, String sql,
			Object[] paramsValue, Integer random, UpdateRowHandler updateRowHandler, Connection conn,
			final Integer dbType) throws Exception {
		String realSql = sql + " order by random() limit " + random + " for update nowait";
		return DialectUtils.updateFetchBySql(sqlToyContext, sqlToyConfig, realSql, paramsValue, updateRowHandler, conn,
				0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#findByStore(org.sagacity.sqltoy.
	 * SqlToyContext, org.sagacity.sqltoy.executor.StoreExecutor)
	 */
	@Override
	public StoreResult executeStore(SqlToyContext sqlToyContext, final SqlToyConfig sqlToyConfig, final String sql,
			final Object[] inParamsValue, final Integer[] outParamsType, final Connection conn) throws Exception {
		return DialectUtils.executeStore(sqlToyConfig, sqlToyContext, sql, inParamsValue, outParamsType, conn);
	}
}
