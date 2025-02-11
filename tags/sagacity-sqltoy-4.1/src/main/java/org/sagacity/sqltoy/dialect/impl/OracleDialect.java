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
import org.sagacity.sqltoy.SqlToyConstants;
import org.sagacity.sqltoy.SqlToyContext;
import org.sagacity.sqltoy.callback.ReflectPropertyHandler;
import org.sagacity.sqltoy.callback.RowCallbackHandler;
import org.sagacity.sqltoy.callback.UpdateRowHandler;
import org.sagacity.sqltoy.config.model.EntityMeta;
import org.sagacity.sqltoy.config.model.PKStrategy;
import org.sagacity.sqltoy.config.model.SqlToyConfig;
import org.sagacity.sqltoy.config.model.SqlToyResult;
import org.sagacity.sqltoy.dialect.Dialect;
import org.sagacity.sqltoy.dialect.handler.GenerateSavePKStrategy;
import org.sagacity.sqltoy.dialect.handler.GenerateSqlHandler;
import org.sagacity.sqltoy.dialect.model.ReturnPkType;
import org.sagacity.sqltoy.dialect.model.SavePKStrategy;
import org.sagacity.sqltoy.dialect.utils.DialectUtils;
import org.sagacity.sqltoy.dialect.utils.OracleDialectUtils;
import org.sagacity.sqltoy.executor.QueryExecutor;
import org.sagacity.sqltoy.lock.LockMode;
import org.sagacity.sqltoy.model.QueryResult;
import org.sagacity.sqltoy.model.StoreResult;
import org.sagacity.sqltoy.utils.DataSourceUtils.DBType;
import org.sagacity.sqltoy.utils.SqlUtil;

/**
 * @project sqltoy-orm
 * @description oracle11g以及以下版本数据库的各类分页、取随机数、saveOrUpdate,lock机制实现
 * @author zhongxu <a href="mailto:zhongxuchen@gmail.com">联系作者</a>
 * @version id:OracleDialect.java,Revision:v1.0,Date:2013-3-21
 */
@SuppressWarnings({ "rawtypes" })
public class OracleDialect implements Dialect {
	/**
	 * 定义日志
	 */
	protected final Logger logger = LogManager.getLogger(getClass());

	/**
	 * 判定为null的函数
	 */
	public static final String NVL_FUNCTION = "nvl";

	public static final String NEXTVAL = ".nextval";

	public static final String DUAL = "dual";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.DialectSqlWrapper#getRandomResult(org.
	 * sagacity .sqltoy.SqlToyContext,
	 * org.sagacity.sqltoy.config.model.SqlToyConfig,
	 * org.sagacity.sqltoy.executor.QueryExecutor, java.lang.Long, java.lang.Long,
	 * java.sql.Connection)
	 */
	@Override
	public QueryResult getRandomResult(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig,
			QueryExecutor queryExecutor, Long totalCount, Long randomCount, Connection conn) throws Exception {
		return OracleDialectUtils.getRandomResult(sqlToyContext, sqlToyConfig, queryExecutor, totalCount, randomCount,
				conn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.DialectSqlWrapper#findPageBySql(org.sagacity
	 * .sqltoy.SqlToyContext, org.sagacity.sqltoy.config.model.SqlToyConfig,
	 * org.sagacity.sqltoy.executor.QueryExecutor,
	 * org.sagacity.core.database.callback.RowCallbackHandler, java.lang.Long,
	 * java.lang.Integer, java.sql.Connection)
	 */
	@Override
	public QueryResult findPageBySql(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig,
			QueryExecutor queryExecutor, Long pageNo, Integer pageSize, Connection conn) throws Exception {
		StringBuilder sql = new StringBuilder();
		boolean isNamed = sqlToyConfig.isNamedParam();
		int startIndex = 1;
		if (sqlToyConfig.isHasFast()) {
			sql.append(sqlToyConfig.getFastPreSql());
			sql.append(" (");
			startIndex = 0;
		}
		sql.append("SELECT * FROM (SELECT ROWNUM page_row_id,SAG_Paginationtable.* FROM ( ");
		sql.append(sqlToyConfig.isHasFast() ? sqlToyConfig.getFastSql() : sqlToyConfig.getSql());
		sql.append(") SAG_Paginationtable ");
		/*
		 * 判断sql中是否存在排序，因为oracle排序查询的机制通过ROWNUM<=?每次查出的结果可能不一样 ， 请参见ROWNUM机制以及oracle
		 * SORT ORDER BY STOPKEY
		 */
		if (SqlToyConstants.oraclePageIgnoreOrder() || !SqlUtil
				.hasOrderBy(sqlToyConfig.isHasFast() ? sqlToyConfig.getFastSql() : sqlToyConfig.getSql(), true)) {
			sql.append(" where ROWNUM <=");
			sql.append(isNamed ? ":" + SqlToyConstants.PAGE_FIRST_PARAM_NAME : "?");
			sql.append(" ) WHERE page_row_id>");
			sql.append(isNamed ? ":" + SqlToyConstants.PAGE_LAST_PARAM_NAME : "?");
		} else {
			sql.append(" ) WHERE page_row_id<=");
			sql.append(isNamed ? ":" + SqlToyConstants.PAGE_FIRST_PARAM_NAME : "?");
			sql.append(" and page_row_id >");
			sql.append(isNamed ? ":" + SqlToyConstants.PAGE_LAST_PARAM_NAME : "?");
		}

		if (sqlToyConfig.isHasFast()) {
			sql.append(") ").append(sqlToyConfig.getFastTailSql());
		}

		SqlToyResult queryParam = DialectUtils.wrapPageSqlParams(sqlToyContext, sqlToyConfig, queryExecutor,
				sql.toString(), pageNo * pageSize, (pageNo - 1) * pageSize);
		return DialectUtils.findBySql(sqlToyContext, sqlToyConfig, queryParam.getSql(), queryParam.getParamsValue(),
				queryExecutor.getRowCallbackHandler(), conn, startIndex, queryExecutor.getFetchSize(),
				queryExecutor.getMaxRows());
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
		StringBuilder sql = new StringBuilder();
		if (sqlToyConfig.isHasFast()) {
			sql.append(sqlToyConfig.getFastPreSql());
			sql.append(" (");
		}
		sql.append("SELECT SAG_Paginationtable.* FROM ( ");
		sql.append(sqlToyConfig.isHasFast() ? sqlToyConfig.getFastSql() : sqlToyConfig.getSql());
		sql.append(") SAG_Paginationtable where ROWNUM <=");
		sql.append(new Double(topSize).intValue());

		if (sqlToyConfig.isHasFast())
			sql.append(") ").append(sqlToyConfig.getFastTailSql());

		SqlToyResult queryParam = DialectUtils.wrapPageSqlParams(sqlToyContext, sqlToyConfig, queryExecutor,
				sql.toString(), null, null);
		return DialectUtils.findBySql(sqlToyContext, sqlToyConfig, queryParam.getSql(), queryParam.getParamsValue(),
				queryExecutor.getRowCallbackHandler(), conn, 0, queryExecutor.getFetchSize(),
				queryExecutor.getMaxRows());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.DialectSqlWrapper#findBySql(org.sagacity.
	 * sqltoy.config.model.SqlToyConfig, java.lang.String[], java.lang.Object[],
	 * java.lang.reflect.Type,
	 * org.sagacity.core.database.callback.RowCallbackHandler, java.sql.Connection)
	 */
	public QueryResult findBySql(final SqlToyContext sqlToyContext, final SqlToyConfig sqlToyConfig, final String sql,
			final Object[] paramsValue, final RowCallbackHandler rowCallbackHandler, final Connection conn,
			final int fetchSize, final int maxRows) throws Exception {
		return DialectUtils.findBySql(sqlToyContext, sqlToyConfig, sql, paramsValue, rowCallbackHandler, conn, 0,
				fetchSize, maxRows);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.DialectSqlWrapper#getCountBySql(java.lang
	 * .String, java.lang.String[], java.lang.Object[], java.sql.Connection)
	 */
	@Override
	public Long getCountBySql(final SqlToyContext sqlToyContext, final String sql, final Object[] paramsValue,
			final boolean isLastSql, final Connection conn) throws Exception {
		return DialectUtils.getCountBySql(sqlToyContext, sql, paramsValue, isLastSql, conn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#saveOrUpdate(org.sagacity.sqltoy.
	 * SqlToyContext, java.io.Serializable, java.sql.Connection)
	 */
	@Override
	public Long saveOrUpdate(SqlToyContext sqlToyContext, Serializable entity, final String[] forceUpdateFields,
			Connection conn, final Boolean autoCommit, final String tableName) throws Exception {
		List entities = new ArrayList();
		entities.add(entity);
		return saveOrUpdateAll(sqlToyContext, entities, sqlToyContext.getBatchSize(), null, forceUpdateFields, conn,
				autoCommit, tableName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#saveOrUpdateAll(org.sagacity.sqltoy
	 * .SqlToyContext, java.util.List, java.sql.Connection)
	 */
	@Override
	public Long saveOrUpdateAll(SqlToyContext sqlToyContext, List<?> entities, final int batchSize,
			ReflectPropertyHandler reflectPropertyHandler, final String[] forceUpdateFields, Connection conn,
			final Boolean autoCommit, final String tableName) throws Exception {
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entities.get(0).getClass());
		return DialectUtils.saveOrUpdateAll(sqlToyContext, entities, batchSize, entityMeta, forceUpdateFields,
				new GenerateSqlHandler() {
					public String generateSql(EntityMeta entityMeta, String[] forceUpdateFields) {
						PKStrategy pkStrategy = entityMeta.getIdStrategy();
						String sequence = entityMeta.getSequence() + NEXTVAL;
						if (pkStrategy != null && pkStrategy.equals(PKStrategy.IDENTITY)) {
							pkStrategy = PKStrategy.SEQUENCE;
							sequence = entityMeta.getFieldsMeta().get(entityMeta.getIdArray()[0]).getDefaultValue();
						}
						return DialectUtils.getSaveOrUpdateSql(DBType.ORACLE, entityMeta, pkStrategy, forceUpdateFields,
								DUAL, NVL_FUNCTION, sequence, isAssignPKValue(pkStrategy), tableName);
					}
				}, reflectPropertyHandler, conn, autoCommit);
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
						String sequence = entityMeta.getSequence() + NEXTVAL;
						if (pkStrategy != null && pkStrategy.equals(PKStrategy.IDENTITY)) {
							pkStrategy = PKStrategy.SEQUENCE;
							sequence = entityMeta.getFieldsMeta().get(entityMeta.getIdArray()[0]).getDefaultValue();
						}
						return DialectUtils.getSaveIgnoreExistSql(DBType.ORACLE12, entityMeta, pkStrategy, DUAL,
								NVL_FUNCTION, sequence, isAssignPKValue(pkStrategy), tableName);
					}
				}, reflectPropertyHandler, conn, autoCommit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#load(java.io.Serializable,
	 * java.util.List, java.sql.Connection)
	 */
	@Override
	public Serializable load(final SqlToyContext sqlToyContext, Serializable entity, List<Class> cascadeTypes,
			LockMode lockMode, Connection conn, final String tableName) throws Exception {
		return OracleDialectUtils.load(sqlToyContext, entity, cascadeTypes, lockMode, conn, tableName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#loadAll(java.util.List,
	 * java.util.List, java.sql.Connection)
	 */
	@Override
	public List<?> loadAll(final SqlToyContext sqlToyContext, List<?> entities, List<Class> cascadeTypes,
			LockMode lockMode, Connection conn, final String tableName) throws Exception {
		return OracleDialectUtils.loadAll(sqlToyContext, entities, cascadeTypes, lockMode, conn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#save(org.sagacity.sqltoy.
	 * SqlToyContext , java.io.Serializable, java.util.List, java.sql.Connection)
	 */
	@Override
	public Object save(SqlToyContext sqlToyContext, Serializable entity, Connection conn, final String tableName)
			throws Exception {
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entity.getClass());
		PKStrategy pkStrategy = entityMeta.getIdStrategy();
		String sequence = entityMeta.getSequence() + NEXTVAL;
		if (pkStrategy != null && pkStrategy.equals(PKStrategy.IDENTITY)) {
			pkStrategy = PKStrategy.SEQUENCE;
			sequence = entityMeta.getFieldsMeta().get(entityMeta.getIdArray()[0]).getDefaultValue();
		}
		String insertSql = DialectUtils.generateInsertSql(DBType.ORACLE, entityMeta, pkStrategy, NVL_FUNCTION, sequence,
				isAssignPKValue(pkStrategy), tableName);
		return DialectUtils.save(sqlToyContext, entityMeta, pkStrategy, isAssignPKValue(pkStrategy),
				ReturnPkType.PREPARD_ID, insertSql, entity, new GenerateSqlHandler() {
					// 通过反调方式提供oracle insert语句
					public String generateSql(EntityMeta entityMeta, String[] forceUpdateField) {
						PKStrategy pkStrategy = entityMeta.getIdStrategy();
						String sequence = entityMeta.getSequence() + NEXTVAL;
						// oracle sequence主键策略
						if (pkStrategy != null && pkStrategy.equals(PKStrategy.IDENTITY)) {
							pkStrategy = PKStrategy.SEQUENCE;
							sequence = entityMeta.getFieldsMeta().get(entityMeta.getIdArray()[0]).getDefaultValue();
						}
						return DialectUtils.generateInsertSql(DBType.ORACLE, entityMeta, pkStrategy, NVL_FUNCTION,
								sequence, isAssignPKValue(pkStrategy), null);
					}
				}, new GenerateSavePKStrategy() {
					public SavePKStrategy generate(EntityMeta entityMeta) {
						PKStrategy pkStrategy = entityMeta.getIdStrategy();
						if (pkStrategy != null && pkStrategy.equals(PKStrategy.IDENTITY)) {
							pkStrategy = PKStrategy.SEQUENCE;
						}
						return new SavePKStrategy(pkStrategy, isAssignPKValue(pkStrategy));
					}
				}, conn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#saveAll(org.sagacity.sqltoy.
	 * SqlToyContext , java.util.List,
	 * org.sagacity.core.utils.callback.ReflectPropertyHandler, java.sql.Connection)
	 */
	@Override
	public Long saveAll(SqlToyContext sqlToyContext, List<?> entities, final int batchSize,
			ReflectPropertyHandler reflectPropertyHandler, Connection conn, final Boolean autoCommit,
			final String tableName) throws Exception {
		// oracle12c 开始支持identity机制
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entities.get(0).getClass());
		PKStrategy pkStrategy = entityMeta.getIdStrategy();
		String sequence = entityMeta.getSequence() + NEXTVAL;
		if (pkStrategy != null && pkStrategy.equals(PKStrategy.IDENTITY)) {
			pkStrategy = PKStrategy.SEQUENCE;
			sequence = entityMeta.getFieldsMeta().get(entityMeta.getIdArray()[0]).getDefaultValue();
		}
		String insertSql = DialectUtils.generateInsertSql(DBType.ORACLE, entityMeta, pkStrategy, NVL_FUNCTION, sequence,
				isAssignPKValue(pkStrategy), tableName);
		return DialectUtils.saveAll(sqlToyContext, entityMeta, pkStrategy, isAssignPKValue(pkStrategy), insertSql,
				entities, batchSize, reflectPropertyHandler, conn, autoCommit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#update(org.sagacity.sqltoy.
	 * SqlToyContext , java.io.Serializable, java.lang.String[],
	 * java.sql.Connection)
	 */
	@Override
	public Long update(SqlToyContext sqlToyContext, Serializable entity, String[] forceUpdateFields,
			final boolean cascade, final Class[] emptyCascadeClasses,
			final HashMap<Class, String[]> subTableForceUpdateProps, Connection conn, final String tableName)
			throws Exception {
		return DialectUtils.update(sqlToyContext, entity, NVL_FUNCTION, forceUpdateFields, cascade,
				(cascade == false) ? null : new GenerateSqlHandler() {
					public String generateSql(EntityMeta entityMeta, String[] forceUpdateFields) {
						PKStrategy pkStrategy = entityMeta.getIdStrategy();
						String sequence = entityMeta.getSequence() + NEXTVAL;
						if (pkStrategy != null && pkStrategy.equals(PKStrategy.IDENTITY)) {
							pkStrategy = PKStrategy.SEQUENCE;
							sequence = entityMeta.getFieldsMeta().get(entityMeta.getIdArray()[0]).getDefaultValue();
						}
						return DialectUtils.getSaveOrUpdateSql(DBType.ORACLE, entityMeta, pkStrategy, forceUpdateFields,
								DUAL, NVL_FUNCTION, sequence, isAssignPKValue(pkStrategy), null);
					}
				}, emptyCascadeClasses, subTableForceUpdateProps, conn, tableName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#updateAll(org.sagacity.sqltoy.
	 * SqlToyContext, java.util.List,
	 * org.sagacity.core.utils.callback.ReflectPropertyHandler, java.sql.Connection)
	 */
	@Override
	public Long updateAll(SqlToyContext sqlToyContext, List<?> entities, final int batchSize,
			final String[] forceUpdateFields, ReflectPropertyHandler reflectPropertyHandler, Connection conn,
			final Boolean autoCommit, final String tableName) throws Exception {
		return DialectUtils.updateAll(sqlToyContext, entities, batchSize, forceUpdateFields, reflectPropertyHandler,
				NVL_FUNCTION, conn, autoCommit, tableName, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#delete(org.sagacity.sqltoy.
	 * SqlToyContext , java.io.Serializable, java.sql.Connection)
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
	 * org.sagacity.sqltoy.executor.QueryExecutor,
	 * org.sagacity.core.database.callback.UpdateRowHandler, java.sql.Connection)
	 */
	@Override
	public QueryResult updateFetch(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig, String sql,
			Object[] paramsValue, UpdateRowHandler updateRowHandler, Connection conn) throws Exception {
		String realSql = sql + " for update nowait";
		return DialectUtils.updateFetchBySql(sqlToyContext, sqlToyConfig, realSql, paramsValue, updateRowHandler, conn,
				0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#updateFetchTop(org.sagacity.sqltoy
	 * .SqlToyContext, org.sagacity.sqltoy.config.model.SqlToyConfig,
	 * org.sagacity.sqltoy.executor.QueryExecutor, java.lang.Integer,
	 * org.sagacity.core.database.callback.UpdateRowHandler, java.sql.Connection)
	 */
	@Override
	public QueryResult updateFetchTop(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig, String sql,
			Object[] paramsValue, Integer topSize, UpdateRowHandler updateRowHandler, Connection conn)
			throws Exception {
		throw new Exception(SqlToyConstants.UN_SUPPORT_MESSAGE);
		// StringBuilder query = new StringBuilder();
		// query.append("select SAG_Paginationtable.* FROM ( ");
		// query.append(sql);
		// query.append(") SAG_Paginationtable where ROWNUM<=" + topSize);
		// query.append(" for update nowait");
		// return DialectUtils.updateFetchBySql(sqlToyContext, sqlToyConfig,
		// query.toString(), paramsValue, updateRowHandler, conn, 0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagacity.sqltoy.dialect.Dialect#updateFetchRandom(org.sagacity.sqltoy
	 * .SqlToyContext, org.sagacity.sqltoy.config.model.SqlToyConfig,
	 * org.sagacity.sqltoy.executor.QueryExecutor, java.lang.Integer,
	 * org.sagacity.core.database.callback.UpdateRowHandler, java.sql.Connection)
	 */
	@Override
	public QueryResult updateFetchRandom(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig, String sql,
			Object[] paramsValue, Integer random, UpdateRowHandler updateRowHandler, Connection conn) throws Exception {
		throw new Exception(SqlToyConstants.UN_SUPPORT_MESSAGE);
		// String realSql = sql + " order by dbms_random.random fetch first "
		// + random + " rows only for update nowait";
		// return DialectUtils.updateFetchBySql(sqlToyContext, sqlToyConfig,
		// realSql, paramsValue, updateRowHandler, conn, 0);
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
		return OracleDialectUtils.executeStore(sqlToyConfig, sqlToyContext, sql, inParamsValue, outParamsType, conn);
	}

	private boolean isAssignPKValue(PKStrategy pkStrategy) {
		return true;
	}
}
