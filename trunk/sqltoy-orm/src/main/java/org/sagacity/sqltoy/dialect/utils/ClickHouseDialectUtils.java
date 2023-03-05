package org.sagacity.sqltoy.dialect.utils;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sagacity.sqltoy.SqlExecuteStat;
import org.sagacity.sqltoy.SqlToyContext;
import org.sagacity.sqltoy.callback.PreparedStatementResultHandler;
import org.sagacity.sqltoy.callback.ReflectPropsHandler;
import org.sagacity.sqltoy.config.model.EntityMeta;
import org.sagacity.sqltoy.config.model.FieldMeta;
import org.sagacity.sqltoy.config.model.OperateType;
import org.sagacity.sqltoy.config.model.PKStrategy;
import org.sagacity.sqltoy.config.model.SqlToyResult;
import org.sagacity.sqltoy.config.model.SqlType;
import org.sagacity.sqltoy.model.ColumnMeta;
import org.sagacity.sqltoy.utils.BeanUtil;
import org.sagacity.sqltoy.utils.DataSourceUtils.DBType;
import org.sagacity.sqltoy.utils.ReservedWordsUtil;
import org.sagacity.sqltoy.utils.SqlUtil;
import org.sagacity.sqltoy.utils.SqlUtilsExt;
import org.sagacity.sqltoy.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @project sqltoy-orm
 * @description 提供clickhouse数据库通用的操作功能实现,为不同版本提供支持
 * @author zhongxuchen
 * @version v1.0,Date:2020年1月20日
 */
public class ClickHouseDialectUtils {
	/**
	 * 定义日志
	 */
	protected final static Logger logger = LoggerFactory.getLogger(ClickHouseDialectUtils.class);

	/**
	 * @todo 保存对象
	 * @param sqlToyContext
	 * @param entityMeta
	 * @param insertSql
	 * @param entity
	 * @param conn
	 * @param dbType
	 * @return
	 * @throws Exception
	 */
	public static Object save(SqlToyContext sqlToyContext, final EntityMeta entityMeta, final String insertSql,
			Serializable entity, final Connection conn, final Integer dbType) throws Exception {
		PKStrategy pkStrategy = entityMeta.getIdStrategy();
		final boolean isIdentity = (pkStrategy != null && pkStrategy.equals(PKStrategy.IDENTITY));
		final boolean isSequence = (pkStrategy != null && pkStrategy.equals(PKStrategy.SEQUENCE));
		String[] reflectColumns;
		boolean isAssignPK = isAssignPKValue(pkStrategy);
		if ((isIdentity && !isAssignPK) || (isSequence && !isAssignPK)) {
			reflectColumns = entityMeta.getRejectIdFieldArray();
		} else {
			reflectColumns = entityMeta.getFieldsArray();
		}
		// 构造全新的新增记录参数赋值反射(覆盖之前的)
		ReflectPropsHandler handler = DialectUtils.getAddReflectHandler(entityMeta, null,
				sqlToyContext.getUnifyFieldsHandler());
		handler = DialectUtils.getSecureReflectHandler(handler, sqlToyContext.getFieldsSecureProvider(),
				sqlToyContext.getDesensitizeProvider(), entityMeta.getSecureFields());
		Object[] fullParamValues = BeanUtil.reflectBeanToAry(entity, reflectColumns,
				SqlUtilsExt.getDefaultValues(entityMeta), handler);
		boolean needUpdatePk = false;
		int pkIndex = entityMeta.getIdIndex();
		// 是否存在业务ID
		boolean hasBizId = (entityMeta.getBusinessIdGenerator() == null) ? false : true;
		int bizIdColIndex = hasBizId ? entityMeta.getFieldIndex(entityMeta.getBusinessIdField()) : 0;
		// 标识符
		String signature = entityMeta.getBizIdSignature();
		Integer[] relatedColumn = entityMeta.getBizIdRelatedColIndex();
		String[] relatedColumnNames = entityMeta.getBizIdRelatedColumns();
		int relatedColumnSize = (relatedColumn == null) ? 0 : relatedColumn.length;
		// 主键采用assign方式赋予，则调用generator产生id并赋予其值
		if (entityMeta.getIdStrategy() != null && null != entityMeta.getIdGenerator()) {
			int bizIdLength = entityMeta.getBizIdLength();
			int idLength = entityMeta.getIdLength();
			Object[] relatedColValue = null;
			String businessIdType = hasBizId ? entityMeta.getColumnJavaType(entityMeta.getBusinessIdField()) : "";
			if (relatedColumn != null) {
				relatedColValue = new Object[relatedColumnSize];
				for (int meter = 0; meter < relatedColumnSize; meter++) {
					relatedColValue[meter] = fullParamValues[relatedColumn[meter]];
					if (StringUtil.isBlank(relatedColValue[meter])) {
						throw new IllegalArgumentException("对象:" + entityMeta.getEntityClass().getName()
								+ " 生成业务主键依赖的关联字段:" + relatedColumnNames[meter] + " 值为null!");
					}
				}
			}
			if (StringUtil.isBlank(fullParamValues[pkIndex])) {
				// id通过generator机制产生，设置generator产生的值
				fullParamValues[pkIndex] = entityMeta.getIdGenerator().getId(entityMeta.getTableName(), signature,
						entityMeta.getBizIdRelatedColumns(), relatedColValue, null, entityMeta.getIdType(), idLength,
						entityMeta.getBizIdSequenceSize());
				needUpdatePk = true;
			}
			if (hasBizId && StringUtil.isBlank(fullParamValues[bizIdColIndex])) {
				fullParamValues[bizIdColIndex] = entityMeta.getBusinessIdGenerator().getId(entityMeta.getTableName(),
						signature, entityMeta.getBizIdRelatedColumns(), relatedColValue, null, businessIdType,
						bizIdLength, entityMeta.getBizIdSequenceSize());
				// 回写业务主键值
				BeanUtil.setProperty(entity, entityMeta.getBusinessIdField(), fullParamValues[bizIdColIndex]);
			}
		}
		SqlToyResult sqlToyResult = new SqlToyResult(insertSql, fullParamValues);
		sqlToyResult = DialectUtils.doInterceptors(sqlToyContext, null, OperateType.insert, sqlToyResult,
				entity.getClass(), dbType);
		String realInsertSql = sqlToyResult.getSql();
		SqlExecuteStat.showSql("执行单记录插入", realInsertSql, null);
		final Object[] paramValues = sqlToyResult.getParamsValue();
		final Integer[] paramsType = entityMeta.getFieldsTypeArray();
		PreparedStatement pst = null;
		Object result = SqlUtil.preparedStatementProcess(null, pst, null, new PreparedStatementResultHandler() {
			@Override
			public void execute(Object obj, PreparedStatement pst, ResultSet rs) throws SQLException, IOException {
				if (isIdentity || isSequence) {
					pst = conn.prepareStatement(insertSql,
							new String[] { entityMeta.getColumnName(entityMeta.getIdArray()[0]) });
				} else {
					pst = conn.prepareStatement(insertSql);
				}
				SqlUtil.setParamsValue(sqlToyContext.getTypeHandler(), conn, dbType, pst, paramValues, paramsType, 0);
				pst.execute();
				if (isIdentity || isSequence) {
					ResultSet keyResult = pst.getGeneratedKeys();
					if (keyResult != null) {
						while (keyResult.next()) {
							this.setResult(keyResult.getObject(1));
						}
					}
				}
			}
		});
		// 无主键直接返回null
		if (entityMeta.getIdArray() == null) {
			return null;
		}
		if (result == null) {
			result = fullParamValues[pkIndex];
		}
		// 回置到entity 主键值
		if (needUpdatePk || isIdentity || isSequence) {
			BeanUtil.setProperty(entity, entityMeta.getIdArray()[0], result);
		}
		return result;
	}

	/**
	 * @todo 保存批量对象数据
	 * @param sqlToyContext
	 * @param entityMeta
	 * @param insertSql
	 * @param entities
	 * @param batchSize
	 * @param reflectPropsHandler
	 * @param conn
	 * @param dbType
	 * @param autoCommit
	 * @return
	 * @throws Exception
	 */
	public static Long saveAll(SqlToyContext sqlToyContext, EntityMeta entityMeta, String insertSql, List<?> entities,
			final int batchSize, ReflectPropsHandler reflectPropsHandler, Connection conn, final Integer dbType,
			final Boolean autoCommit) throws Exception {
		DialectUtils.doInterceptors(sqlToyContext, null, OperateType.insertAll, new SqlToyResult(insertSql, null),
				entities.get(0).getClass(), dbType);
		PKStrategy pkStrategy = entityMeta.getIdStrategy();
		boolean isIdentity = pkStrategy != null && pkStrategy.equals(PKStrategy.IDENTITY);
		boolean isSequence = pkStrategy != null && pkStrategy.equals(PKStrategy.SEQUENCE);
		String[] reflectColumns;
		boolean isAssignPK = isAssignPKValue(pkStrategy);
		if ((isIdentity && !isAssignPK) || (isSequence && !isAssignPK)) {
			reflectColumns = entityMeta.getRejectIdFieldArray();
		} else {
			reflectColumns = entityMeta.getFieldsArray();
		}
		// 构造全新的新增记录参数赋值反射(覆盖之前的)
		ReflectPropsHandler handler = DialectUtils.getAddReflectHandler(entityMeta, reflectPropsHandler,
				sqlToyContext.getUnifyFieldsHandler());
		handler = DialectUtils.getSecureReflectHandler(handler, sqlToyContext.getFieldsSecureProvider(),
				sqlToyContext.getDesensitizeProvider(), entityMeta.getSecureFields());
		List<Object[]> paramValues = BeanUtil.reflectBeansToInnerAry(entities, reflectColumns,
				SqlUtilsExt.getDefaultValues(entityMeta), handler);
		int pkIndex = entityMeta.getIdIndex();
		// 是否存在业务ID
		boolean hasBizId = (entityMeta.getBusinessIdGenerator() == null) ? false : true;
		int bizIdColIndex = hasBizId ? entityMeta.getFieldIndex(entityMeta.getBusinessIdField()) : 0;
		// 标识符
		String signature = entityMeta.getBizIdSignature();
		Integer[] relatedColumn = entityMeta.getBizIdRelatedColIndex();
		String[] relatedColumnNames = entityMeta.getBizIdRelatedColumns();
		int relatedColumnSize = (relatedColumn == null) ? 0 : relatedColumn.length;
		// 无主键值以及多主键以及assign或通过generator方式产生主键策略
		if (pkStrategy != null && null != entityMeta.getIdGenerator()) {
			int bizIdLength = entityMeta.getBizIdLength();
			int idLength = entityMeta.getIdLength();
			Object[] rowData;
			boolean isAssigned = true;
			String idJdbcType = entityMeta.getIdType();
			Object[] relatedColValue = null;
			String businessIdType = hasBizId ? entityMeta.getColumnJavaType(entityMeta.getBusinessIdField()) : "";
			List<Object[]> idSet = new ArrayList<Object[]>();
			for (int i = 0, s = paramValues.size(); i < s; i++) {
				rowData = (Object[]) paramValues.get(i);
				// 判断主键策略关联的字段是否有值,合法性验证
				if (relatedColumn != null) {
					relatedColValue = new Object[relatedColumnSize];
					for (int meter = 0; meter < relatedColumnSize; meter++) {
						relatedColValue[meter] = rowData[relatedColumn[meter]];
						if (StringUtil.isBlank(relatedColValue[meter])) {
							throw new IllegalArgumentException("对象:" + entityMeta.getEntityClass().getName()
									+ " 生成业务主键依赖的关联字段:" + relatedColumnNames[meter] + " 值为null!");
						}
					}
				}
				// 主键值为null,调用主键生成策略并赋值
				if (StringUtil.isBlank(rowData[pkIndex])) {
					isAssigned = false;
					rowData[pkIndex] = entityMeta.getIdGenerator().getId(entityMeta.getTableName(), signature,
							entityMeta.getBizIdRelatedColumns(), relatedColValue, null, idJdbcType, idLength,
							entityMeta.getBizIdSequenceSize());
				}
				if (hasBizId && StringUtil.isBlank(rowData[bizIdColIndex])) {
					rowData[bizIdColIndex] = entityMeta.getBusinessIdGenerator().getId(entityMeta.getTableName(),
							signature, entityMeta.getBizIdRelatedColumns(), relatedColValue, null, businessIdType,
							bizIdLength, entityMeta.getBizIdSequenceSize());
					// 回写业务主键值
					BeanUtil.setProperty(entities.get(i), entityMeta.getBusinessIdField(), rowData[bizIdColIndex]);
				}
				idSet.add(new Object[] { rowData[pkIndex] });
			}
			// 批量反向设置最终得到的主键值
			if (!isAssigned) {
				BeanUtil.mappingSetProperties(entities, entityMeta.getIdArray(), idSet, new int[] { 0 }, true);
			}
		}
		SqlExecuteStat.showSql("批量保存[" + paramValues.size() + "]条记录", insertSql, null);
		return SqlUtilsExt.batchUpdateForPOJO(sqlToyContext.getTypeHandler(), insertSql, paramValues,
				entityMeta.getFieldsTypeArray(), entityMeta.getFieldsDefaultValue(), entityMeta.getFieldsNullable(),
				batchSize, autoCommit, conn, dbType);
	}

	/**
	 * @todo 删除单个对象以及其级联表数据
	 * @param sqlToyContext
	 * @param entity
	 * @param conn
	 * @param dbType
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	public static Long delete(SqlToyContext sqlToyContext, Serializable entity, Connection conn, final Integer dbType,
			final String tableName) throws Exception {
		if (entity == null) {
			return 0L;
		}
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entity.getClass());
		if (null == entityMeta.getIdArray()) {
			throw new IllegalArgumentException("delete table:" + entityMeta.getSchemaTable(tableName, dbType)
					+ " no primary key,please check table design!");
		}
		Object[] idValues = BeanUtil.reflectBeanToAry(entity, entityMeta.getIdArray());
		Integer[] parameterTypes = new Integer[idValues.length];
		boolean validator = true;
		// 判断主键值是否为空
		for (int i = 0, n = idValues.length; i < n; i++) {
			parameterTypes[i] = entityMeta.getColumnJdbcType(entityMeta.getIdArray()[i]);
			if (StringUtil.isBlank(idValues[i])) {
				validator = false;
				break;
			}
		}
		if (!validator) {
			throw new IllegalArgumentException(entityMeta.getSchemaTable(tableName, dbType)
					+ "delete operate is illegal,table must has primary key and all primaryKey's value must has value!");
		}
		String deleteSql = "alter table ".concat(entityMeta.getSchemaTable(tableName, dbType)).concat(" delete ")
				.concat(entityMeta.getIdArgWhereSql());
		SqlToyResult sqlToyResult = new SqlToyResult(deleteSql, idValues);
		// 增加sql执行拦截器 update 2022-9-10
		sqlToyResult = DialectUtils.doInterceptors(sqlToyContext, null, OperateType.delete, sqlToyResult,
				entity.getClass(), dbType);
		return SqlUtil.executeSql(sqlToyContext.getTypeHandler(), sqlToyResult.getSql(), sqlToyResult.getParamsValue(),
				parameterTypes, conn, dbType, null, true);
	}

	public static Long update(SqlToyContext sqlToyContext, Serializable entity, String nullFunction,
			String[] forceUpdateFields, Connection conn, final Integer dbType, String tableName) throws Exception {
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entity.getClass());
		String realTable = entityMeta.getSchemaTable(tableName, dbType);
		// 无主键
		if (entityMeta.getIdArray() == null) {
			throw new IllegalArgumentException("表:" + realTable + " 无主键,不符合update/updateAll规则,请检查表设计是否合理!");
		}
		// 全部是主键则无需update
		if (entityMeta.getRejectIdFieldArray() == null) {
			logger.warn("表:" + realTable + " 字段全部是主键不存在更新字段,无需执行更新操作!");
			return 0L;
		}
		// 构造全新的修改记录参数赋值反射(覆盖之前的)
		ReflectPropsHandler handler = DialectUtils.getUpdateReflectHandler(null, forceUpdateFields,
				sqlToyContext.getUnifyFieldsHandler());
		handler = DialectUtils.getSecureReflectHandler(handler, sqlToyContext.getFieldsSecureProvider(),
				sqlToyContext.getDesensitizeProvider(), entityMeta.getSecureFields());
		// 排除分区字段
		String[] fields = entityMeta.getFieldsNotPartitionKey();
		Object[] fieldsValues = BeanUtil.reflectBeanToAry(entity, fields, null, handler);
		// 判断主键是否为空
		int end = fields.length;
		int pkIndex = end - entityMeta.getIdArray().length;
		for (int i = pkIndex; i < end; i++) {
			if (StringUtil.isBlank(fieldsValues[i])) {
				throw new IllegalArgumentException("通过对象对表:" + realTable + " 进行update操作,主键字段必须要赋值!");
			}
		}
		// 构建update语句
		String updateSql = generateUpdateSql(dbType, entityMeta, nullFunction, forceUpdateFields, realTable);
		if (updateSql == null) {
			throw new IllegalArgumentException("update sql is null,引起问题的原因是没有设置需要修改的字段!");
		}
		SqlToyResult sqlToyResult = new SqlToyResult(updateSql, fieldsValues);
		// 增加sql执行拦截器 update 2022-9-10
		sqlToyResult = DialectUtils.doInterceptors(sqlToyContext, null, OperateType.update, sqlToyResult,
				entity.getClass(), dbType);
		Long updateCnt = SqlUtil.executeSql(sqlToyContext.getTypeHandler(), sqlToyResult.getSql(),
				sqlToyResult.getParamsValue(), getIgnorePartionFieldsTypes(entityMeta), conn, dbType, null, false);
		return updateCnt;
	}

	/**
	 * @TODO 批量更新
	 * @param sqlToyContext
	 * @param entities
	 * @param batchSize
	 * @param forceUpdateFields
	 * @param reflectPropsHandler
	 * @param nullFunction
	 * @param conn
	 * @param dbType
	 * @param autoCommit
	 * @param tableName
	 * @param skipNull
	 * @return
	 * @throws Exception
	 */
	public static Long updateAll(SqlToyContext sqlToyContext, List<?> entities, final int batchSize,
			final String[] forceUpdateFields, ReflectPropsHandler reflectPropsHandler, String nullFunction,
			Connection conn, final Integer dbType, final Boolean autoCommit, String tableName, boolean skipNull)
			throws Exception {
		if (entities == null || entities.isEmpty()) {
			return 0L;
		}
		EntityMeta entityMeta = sqlToyContext.getEntityMeta(entities.get(0).getClass());
		String realTable = entityMeta.getSchemaTable(tableName, dbType);
		// 无主键
		if (entityMeta.getIdArray() == null) {
			throw new IllegalArgumentException("表:" + realTable + " 无主键,不符合update/updateAll规则,请检查表设计是否合理!");
		}
		// 全部是主键则无需update
		if (entityMeta.getRejectIdFieldArray() == null) {
			logger.warn("表:" + realTable + " 字段全部是主键不存在更新字段,无需执行更新操作!");
			return 0L;
		}
		// 构造全新的修改记录参数赋值反射(覆盖之前的)
		ReflectPropsHandler handler = DialectUtils.getUpdateReflectHandler(reflectPropsHandler, forceUpdateFields,
				sqlToyContext.getUnifyFieldsHandler());
		handler = DialectUtils.getSecureReflectHandler(handler, sqlToyContext.getFieldsSecureProvider(),
				sqlToyContext.getDesensitizeProvider(), entityMeta.getSecureFields());
		String[] fields = entityMeta.getFieldsNotPartitionKey();
		List<Object[]> paramsValues = BeanUtil.reflectBeansToInnerAry(entities, fields, null, handler);
		// 判断主键是否为空
		int end = fields.length;
		int pkIndex = end - entityMeta.getIdArray().length;
		int index = 0;
		// 累计多少行为空
		int skipCount = 0;
		Iterator<Object[]> iter = paramsValues.iterator();
		Object[] rowValues;
		while (iter.hasNext()) {
			rowValues = iter.next();
			for (int i = pkIndex; i < end; i++) {
				// 判断主键值是否为空
				if (StringUtil.isBlank(rowValues[i])) {
					// 跳过主键值为空的
					if (skipNull) {
						skipCount++;
						iter.remove();
						break;
					} else {
						throw new IllegalArgumentException(
								"通过对象对表" + realTable + " 进行updateAll操作,主键字段必须要赋值!第:" + index + " 条记录主键为null!");
					}
				}
			}
			index++;
		}
		if (skipCount > 0) {
			logger.debug("共有:{}行记录因为主键值为空跳过修改操作!", skipCount);
		}
		// 构建update语句
		String updateSql = generateUpdateSql(dbType, entityMeta, nullFunction, forceUpdateFields, realTable);
		if (updateSql == null) {
			throw new IllegalArgumentException("updateAll sql is null,引起问题的原因是没有设置需要修改的字段!");
		}
		SqlToyResult sqlToyResult = new SqlToyResult(updateSql, null);
		// 增加sql执行拦截器 update 2022-9-10
		sqlToyResult = DialectUtils.doInterceptors(sqlToyContext, null, OperateType.updateAll, sqlToyResult,
				entities.get(0).getClass(), dbType);
		SqlExecuteStat.showSql("批量修改[" + paramsValues.size() + "]条记录", sqlToyResult.getSql(), null);
		return SqlUtilsExt.batchUpdateForPOJO(sqlToyContext.getTypeHandler(), sqlToyResult.getSql(), paramsValues,
				getIgnorePartionFieldsTypes(entityMeta), null, null, batchSize, autoCommit, conn, dbType);
	}

	/**
	 * @TODO 获取排除分区字段的所有字段对应的类型
	 * @param entityMeta
	 * @return
	 */
	private static Integer[] getIgnorePartionFieldsTypes(EntityMeta entityMeta) {
		List<Integer> fieldTypes = new ArrayList<Integer>();
		FieldMeta fieldMeta;
		String[] fields = entityMeta.getFieldsArray();
		Integer[] fieldTypesArray = entityMeta.getFieldsTypeArray();
		for (int i = 0; i < fields.length; i++) {
			fieldMeta = entityMeta.getFieldMeta(fields[i]);
			if (!fieldMeta.isPartitionKey()) {
				fieldTypes.add(fieldTypesArray[i]);
			}
		}
		Integer[] result = new Integer[fieldTypes.size()];
		fieldTypes.toArray(result);
		return result;
	}

	private static String generateUpdateSql(Integer dbType, EntityMeta entityMeta, String nullFunction,
			String[] forceUpdateFields, String tableName) {
		if (entityMeta.getIdArray() == null) {
			return null;
		}
		StringBuilder sql = new StringBuilder(entityMeta.getFieldsArray().length * 30 + 30);
		sql.append(" alter table  ");
		sql.append(tableName);
		sql.append(" update ");
		String columnName;
		// 需要被强制修改的字段
		HashSet<String> fupc = new HashSet<String>();
		if (forceUpdateFields != null) {
			for (String field : forceUpdateFields) {
				fupc.add(ReservedWordsUtil.convertWord(entityMeta.getColumnName(field), dbType));
			}
		}
		FieldMeta fieldMeta;
		int meter = 0;
		for (int i = 0, n = entityMeta.getRejectIdFieldArray().length; i < n; i++) {
			fieldMeta = entityMeta.getFieldMeta(entityMeta.getRejectIdFieldArray()[i]);
			// 排除分区字段
			if (!fieldMeta.isPartitionKey()) {
				columnName = ReservedWordsUtil.convertWord(fieldMeta.getColumnName(), dbType);
				if (meter > 0) {
					sql.append(",");
				}
				sql.append(columnName);
				sql.append("=");
				if (fupc.contains(columnName)) {
					sql.append("?");
				} else {
					sql.append(nullFunction);
					sql.append("(?,").append(columnName).append(")");
				}
				meter++;
			}
		}
		sql.append(" where ");
		for (int i = 0, n = entityMeta.getIdArray().length; i < n; i++) {
			columnName = entityMeta.getColumnName(entityMeta.getIdArray()[i]);
			columnName = ReservedWordsUtil.convertWord(columnName, dbType);
			if (i > 0) {
				sql.append(" and ");
			}
			sql.append(columnName);
			sql.append("=?");
		}
		return sql.toString();
	}

	@SuppressWarnings("unchecked")
	public static List<ColumnMeta> getTableColumns(String catalog, String schema, String tableName, Connection conn,
			Integer dbType, String dialect) throws Exception {
		List<ColumnMeta> tableColumns = DefaultDialectUtils.getTableColumns(catalog, schema, tableName, conn, dbType,
				dialect);
		String sql = "SELECT name COLUMN_NAME,comment COMMENTS,is_in_primary_key PRIMARY_KEY,is_in_partition_key PARTITION_KEY from system.columns t where t.table=?";
		PreparedStatement pst = conn.prepareStatement(sql);
		ResultSet rs = null;
		// 通过preparedStatementProcess反调，第二个参数是pst
		Map<String, ColumnMeta> colMap = (Map<String, ColumnMeta>) SqlUtil.preparedStatementProcess(null, pst, rs,
				new PreparedStatementResultHandler() {
					@Override
					public void execute(Object rowData, PreparedStatement pst, ResultSet rs) throws Exception {
						pst.setString(1, tableName);
						rs = pst.executeQuery();
						Map<String, ColumnMeta> colComments = new HashMap<String, ColumnMeta>();
						while (rs.next()) {
							ColumnMeta colMeta = new ColumnMeta();
							colMeta.setColName(rs.getString("COLUMN_NAME"));
							colMeta.setComments(rs.getString("COMMENTS"));
							colMeta.setPK("1".equals(rs.getString("PRIMARY_KEY")) ? true : false);
							colMeta.setPartitionKey("1".equals(rs.getString("PARTITION_KEY")) ? true : false);
							colComments.put(colMeta.getColName(), colMeta);
						}
						this.setResult(colComments);
					}
				});
		ColumnMeta mapColMeta;
		for (ColumnMeta col : tableColumns) {
			mapColMeta = colMap.get(col.getColName());
			if (mapColMeta != null) {
				col.setComments(mapColMeta.getComments());
				col.setPK(mapColMeta.isPK());
				col.setPartitionKey(mapColMeta.isPartitionKey());
			}
		}
		return tableColumns;
	}

	public static boolean isAssignPKValue(PKStrategy pkStrategy) {
		if (pkStrategy == null) {
			return true;
		}
		if (pkStrategy.equals(PKStrategy.SEQUENCE)) {
			return true;
		}
		if (pkStrategy.equals(PKStrategy.IDENTITY)) {
			return false;
		}
		return true;
	}

	/**
	 * @TODO 构造clickhouse的删除或修改语句
	 * @param entityMeta
	 * @param sql
	 * @param sqlType
	 * @return
	 */
	public static String wrapDelOrUpdate(EntityMeta entityMeta, String sql, SqlType sqlType) {
		String startSql = "alter table ".concat(entityMeta.getSchemaTable(null, DBType.CLICKHOUSE));
		// 删除操作
		if (sqlType == SqlType.delete) {
			// 截取where开始部分构造成:alter table tableName delete where
			// delete from table where
			sql = startSql.concat(" delete ").concat(sql.substring(StringUtil.matchIndex(sql, "(?i)\\swhere\\s")));
		} else if (sqlType == SqlType.update) {
			// 截取set后面语句,构造成:alter table tableName update field1=:value1,field2=:value2
			// update table set field1=:value1,field2=:value2
			sql = startSql.concat(" update ").concat(sql.substring(StringUtil.matchIndex(sql, "(?i)\\sset\\s") + 4));
		}
		return sql;
	}
}
