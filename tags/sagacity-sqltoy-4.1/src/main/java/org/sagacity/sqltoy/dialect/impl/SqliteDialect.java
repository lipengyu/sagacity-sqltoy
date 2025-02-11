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
import org.sagacity.sqltoy.config.model.SqlType;
import org.sagacity.sqltoy.dialect.Dialect;
import org.sagacity.sqltoy.dialect.handler.GenerateSavePKStrategy;
import org.sagacity.sqltoy.dialect.handler.GenerateSqlHandler;
import org.sagacity.sqltoy.dialect.model.ReturnPkType;
import org.sagacity.sqltoy.dialect.model.SavePKStrategy;
import org.sagacity.sqltoy.dialect.utils.DialectUtils;
import org.sagacity.sqltoy.dialect.utils.SqliteDialectUtils;
import org.sagacity.sqltoy.executor.QueryExecutor;
import org.sagacity.sqltoy.lock.LockMode;
import org.sagacity.sqltoy.model.QueryResult;
import org.sagacity.sqltoy.model.StoreResult;
import org.sagacity.sqltoy.utils.DataSourceUtils.DBType;

/**
 * @project sqltoy-orm
 * @description 基于sqlite数据库方言的各类操作实现
 * @author renfei.chen <a href="mailto:zhongxuchen@hotmail.com">联系作者</a>
 * @version Revision:v1.0,Date:2013-8-29
 * @Modification Date:2013-8-29 {填写修改说明}
 */
@SuppressWarnings({ "rawtypes" })
public class SqliteDialect implements Dialect {
	/**
	 * 定义日志
	 */
	protected final Logger logger = LogManager.getLogger(getClass());

	/**
	 * 判定为null的函数
	 */
	public static final String NVL_FUNCTION = "ifnull";

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
		String innerSql = sqlToyConfig.isHasFast() ? sqlToyConfig.getFastSql() : sqlToyConfig.getSql();
		StringBuilder sql = new StringBuilder();
		if (sqlToyConfig.isHasFast())
			sql.append(sqlToyConfig.getFastPreSql()).append(" (");

		// sql中是否存在排序或union
		boolean hasOrderOrUnion = DialectUtils.hasOrderByOrUnion(innerSql);
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
		return findBySql(sqlToyContext, sqlToyConfig, queryParam.getSql(), queryParam.getParamsValue(),
				queryExecutor.getRowCallbackHandler(), conn, queryExecutor.getFetchSize(), queryExecutor.getMaxRows());
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
				sql.toString(), new Long(pageSize), (pageNo - 1) * pageSize);
		return findBySql(sqlToyContext, sqlToyConfig, queryParam.getSql(), queryParam.getParamsValue(),
				queryExecutor.getRowCallbackHandler(), conn, queryExecutor.getFetchSize(), queryExecutor.getMaxRows());
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
			sql.append(" (").append(sqlToyConfig.getFastSql());
		} else
			sql.append(sqlToyConfig.getSql());
		sql.append(" limit ");
		sql.append(new Double(topSize).intValue());

		if (sqlToyConfig.isHasFast()) {
			sql.append(") ").append(sqlToyConfig.getFastTailSql());
		}

		SqlToyResult queryParam = DialectUtils.wrapPageSqlParams(sqlToyContext, sqlToyConfig, queryExecutor,
				sql.toString(), null, null);
		return findBySql(sqlToyContext, sqlToyConfig, queryParam.getSql(), queryParam.getParamsValue(),
				queryExecutor.getRowCallbackHandler(), conn, queryExecutor.getFetchSize(), queryExecutor.getMaxRows());
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
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entities.get(0).getClass());
		return DialectUtils.saveOrUpdateAll(sqlToyContext, entities, batchSize, entityMeta, forceUpdateFields,
				new GenerateSqlHandler() {
					public String generateSql(EntityMeta entityMeta, String[] forceUpdateFields) {
						return SqliteDialectUtils.getSaveOrUpdateSql(DBType.SQLITE, entityMeta, forceUpdateFields,
								tableName);
					}
				}, reflectPropertyHandler, conn, autoCommit);*/
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
						return SqliteDialectUtils.getSaveIgnoreExistSql(DBType.SQLITE, entityMeta, tableName);
					}
				}, reflectPropertyHandler, conn, autoCommit);

	}

	/*
	 * sqlite 只支持表锁,将操作放于事务中即可实现锁操作
	 * 
	 * @see org.sagacity.sqltoy.dialect.Dialect#load(java.io.Serializable,
	 * java.util.List, java.sql.Connection)
	 */
	@Override
	public Serializable load(final SqlToyContext sqlToyContext, Serializable entity, List<Class> cascadeTypes,
			LockMode lockMode, Connection conn, final String tableName) throws Exception {
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entity.getClass());
		// 获取loadsql(loadsql 可以通过@loadSql进行改变，所以需要sqltoyContext重新获取)
		SqlToyConfig sqlToyConfig = sqlToyContext.getSqlToyConfig(entityMeta.getLoadSql(tableName), SqlType.search);
		String loadSql = sqlToyConfig.getSql();
		// switch (lockMode) {
		// case UPGRADE_NOWAIT:
		// case UPGRADE:
		// loadSql = loadSql + " for update";
		// break;
		// }
		return (Serializable) DialectUtils.load(sqlToyContext, sqlToyConfig, loadSql, entityMeta, entity, cascadeTypes,
				conn);
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
		if (null == entities || entities.isEmpty())
			return null;
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entities.get(0).getClass());
		// 判断是否存在主键
		if (null == entityMeta.getIdArray())
			throw new Exception(
					entities.get(0).getClass().getName() + "Entity Object hasn't primary key,cann't use load method!");
		StringBuilder loadSql = new StringBuilder();
		loadSql.append("select * from ");
		loadSql.append(entityMeta.getSchemaTable());
		loadSql.append(" where ");
		String field;
		for (int i = 0, n = entityMeta.getIdArray().length; i < n; i++) {
			field = entityMeta.getIdArray()[i];
			if (i > 0)
				loadSql.append(" and ");
			loadSql.append(entityMeta.getColumnName(field));
			loadSql.append(" in (:").append(field).append(") ");
		}
		// if (lockMode != null) {
		// switch (lockMode) {
		// case UPGRADE_NOWAIT:
		// case UPGRADE:
		// loadSql.append(" for update with rs ");
		// break;
		// }
		// }
		return DialectUtils.loadAll(sqlToyContext, loadSql.toString(), entities, cascadeTypes, conn);
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
		// sqlite 只提供autoincrement 机制，即identity模式，所以sequence可以忽略
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entity.getClass());
		boolean isAssignPk = isAssignPKValue(entityMeta.getIdStrategy());
		String insertSql = DialectUtils.generateInsertSql(DBType.SQLITE, entityMeta, entityMeta.getIdStrategy(),
				NVL_FUNCTION, "NEXTVAL FOR " + entityMeta.getSequence(), isAssignPk, tableName);
		return DialectUtils.save(sqlToyContext, entityMeta, entityMeta.getIdStrategy(), isAssignPk,
				ReturnPkType.PREPARD_ID, insertSql, entity, new GenerateSqlHandler() {
					public String generateSql(EntityMeta entityMeta, String[] forceUpdateField) {
						return DialectUtils.generateInsertSql(DBType.SQLITE, entityMeta, entityMeta.getIdStrategy(),
								NVL_FUNCTION, "NEXTVAL FOR " + entityMeta.getSequence(),
								isAssignPKValue(entityMeta.getIdStrategy()), null);
					}
				}, new GenerateSavePKStrategy() {
					public SavePKStrategy generate(EntityMeta entityMeta) {
						return new SavePKStrategy(entityMeta.getIdStrategy(),
								isAssignPKValue(entityMeta.getIdStrategy()));
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
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entities.get(0).getClass());
		boolean isAssignPk = isAssignPKValue(entityMeta.getIdStrategy());
		String insertSql = DialectUtils.generateInsertSql(DBType.SQLITE, entityMeta, entityMeta.getIdStrategy(),
				NVL_FUNCTION, "NEXTVAL FOR " + entityMeta.getSequence(), isAssignPk, tableName);
		return DialectUtils.saveAll(sqlToyContext, entityMeta, entityMeta.getIdStrategy(), isAssignPk, insertSql,
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
						return SqliteDialectUtils.getSaveOrUpdateSql(DBType.SQLITE, entityMeta, forceUpdateFields,
								null);
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
		return DialectUtils.updateFetchBySql(sqlToyContext, sqlToyConfig, sql, paramsValue, updateRowHandler, conn, 0);
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
		String realSql = sql + " limit " + topSize;
		return DialectUtils.updateFetchBySql(sqlToyContext, sqlToyConfig, realSql, paramsValue, updateRowHandler, conn,
				0);
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
		String realSql = sql + " order by random() limit " + random;
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
		// 不支持
		throw new Exception(SqlToyConstants.UN_SUPPORT_MESSAGE);
	}

	private boolean isAssignPKValue(PKStrategy pkStrategy) {
		if (pkStrategy == null)
			return true;
		// 目前不支持sequence模式
		if (pkStrategy.equals(PKStrategy.SEQUENCE))
			return false;
		if (pkStrategy.equals(PKStrategy.IDENTITY))
			return false;
		return true;
	}
}
