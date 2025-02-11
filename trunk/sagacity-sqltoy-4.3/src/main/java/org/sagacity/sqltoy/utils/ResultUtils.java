/**
 * @Copyright 2009 版权归陈仁飞，不要肆意侵权抄袭，如引用请注明出处保留作者信息。
 */
package org.sagacity.sqltoy.utils;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagacity.sqltoy.SqlToyConstants;
import org.sagacity.sqltoy.SqlToyContext;
import org.sagacity.sqltoy.callback.RowCallbackHandler;
import org.sagacity.sqltoy.callback.UpdateRowHandler;
import org.sagacity.sqltoy.config.SqlConfigParseUtils;
import org.sagacity.sqltoy.config.model.FormatModel;
import org.sagacity.sqltoy.config.model.GroupMeta;
import org.sagacity.sqltoy.config.model.LinkModel;
import org.sagacity.sqltoy.config.model.PivotModel;
import org.sagacity.sqltoy.config.model.SecureMask;
import org.sagacity.sqltoy.config.model.SqlToyConfig;
import org.sagacity.sqltoy.config.model.SqlToyResult;
import org.sagacity.sqltoy.config.model.SqlTranslate;
import org.sagacity.sqltoy.config.model.SqlType;
import org.sagacity.sqltoy.config.model.SummaryModel;
import org.sagacity.sqltoy.config.model.UnpivotModel;
import org.sagacity.sqltoy.dialect.utils.DialectUtils;
import org.sagacity.sqltoy.executor.QueryExecutor;
import org.sagacity.sqltoy.model.DataSetResult;
import org.sagacity.sqltoy.model.QueryResult;

/**
 * @project sagacity-sqltoy
 * @description 对SqlUtil类的扩展，提供查询结果的缓存key-value提取以及结果分组link功能
 * @author renfei.chen <a href="mailto:zhongxuchen@hotmail.com">联系作者</a>
 * @version Revision:v1.0,Date:2013-4-18
 * @modify Date:2016-12-13 {对行转列分类参照集合进行了排序}
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ResultUtils {
	/**
	 * 定义日志
	 */
	private final static Logger logger = LogManager.getLogger(ResultUtils.class);

	/**
	 * @todo 处理sql查询时的结果集,当没有反调或voClass反射处理时以数组方式返回resultSet的数据
	 * @param sqlToyContext
	 * @param sqlToyConfig
	 * @param conn
	 * @param rs
	 * @param rowCallbackHandler
	 * @param updateRowHandler
	 * @param startColIndex
	 * @return
	 * @throws Exception
	 */
	public static QueryResult processResultSet(final SqlToyContext sqlToyContext, final SqlToyConfig sqlToyConfig,
			Connection conn, ResultSet rs, RowCallbackHandler rowCallbackHandler, UpdateRowHandler updateRowHandler,
			int startColIndex) throws Exception {
		QueryResult result = new QueryResult();
		// 记录行记数器
		int index = 0;
		if (rowCallbackHandler != null) {
			while (rs.next()) {
				rowCallbackHandler.processRow(rs, index);
				index++;
			}
			result.setRows(rowCallbackHandler.getResult());
		} else {
			// 取得字段列数,在没有rowCallbackHandler時用数组返回
			int rowCnt = rs.getMetaData().getColumnCount();
			String[] labelNames = new String[rowCnt - startColIndex];
			String[] labelTypes = new String[rowCnt - startColIndex];
			HashMap<String, Integer> labelIndexMap = new HashMap<String, Integer>();
			for (int i = startColIndex; i < rowCnt; i++) {
				labelNames[index] = rs.getMetaData().getColumnLabel(i + 1);
				labelIndexMap.put(labelNames[index].toLowerCase(), index);
				labelTypes[index] = rs.getMetaData().getColumnTypeName(i + 1);
				index++;
			}
			result.setLabelNames(labelNames);
			result.setLabelTypes(labelTypes);
			// 返回结果为非VO class时才可以应用旋转和汇总合计功能
			result.setRows(getResultSet(sqlToyConfig, sqlToyContext, conn, rs, updateRowHandler, rowCnt, labelIndexMap,
					labelNames, startColIndex));
			// 字段脱敏
			if (sqlToyConfig.getSecureMasks() != null && result.getRows() != null) {
				secureMask(result, sqlToyConfig, labelIndexMap);
			}

			// 自动格式化
			if (sqlToyConfig.getFormatModels() != null && result.getRows() != null) {
				formatColumn(result, sqlToyConfig, labelIndexMap);
			}

		}
		// 填充记录数
		if (result.getRows() != null)
			result.setRecordCount(Long.valueOf(result.getRows().size()));
		return result;
	}

	/**
	 * @todo 对字段进行安全脱敏
	 * @param result
	 * @param sqlToyConfig
	 * @param labelIndexMap
	 */
	private static void secureMask(QueryResult result, SqlToyConfig sqlToyConfig,
			HashMap<String, Integer> labelIndexMap) {
		List<List> rows = result.getRows();
		SecureMask[] masks = sqlToyConfig.getSecureMasks();
		Integer index;
		Object value;
		for (SecureMask mask : masks) {
			index = labelIndexMap.get(mask.getColumn());
			if (index != null) {
				int column = index.intValue();
				for (List row : rows) {
					value = row.get(column);
					if (value != null)
						row.set(column, maskStr(mask, value));
				}
			}
		}
	}

	/**
	 * @todo 对字段进行格式化
	 * @param result
	 * @param sqlToyConfig
	 * @param labelIndexMap
	 */
	private static void formatColumn(QueryResult result, SqlToyConfig sqlToyConfig,
			HashMap<String, Integer> labelIndexMap) {
		List<List> rows = result.getRows();
		FormatModel[] formats = sqlToyConfig.getFormatModels();
		Integer index;
		Object value;
		for (FormatModel fmt : formats) {
			index = labelIndexMap.get(fmt.getColumn());
			if (index != null) {
				int column = index.intValue();
				for (List row : rows) {
					value = row.get(column);
					if (value != null) {
						// 日期格式
						if (fmt.getType() == 1)
							row.set(column, DateUtil.formatDate(value, fmt.getFormat()));
						// 数字格式化
						else
							row.set(column, NumberUtil.format(value, fmt.getFormat()));
					}
				}
			}
		}
	}

	/**
	 * @todo 对字符串脱敏
	 * @param mask
	 * @param value
	 * @return
	 */
	private static String maskStr(SecureMask mask, Object value) {
		String type = mask.getType();
		String realStr = value.toString();
		int size = realStr.length();
		// 单字符无需脱敏
		if (size == 1)
			return realStr;
		String maskCode = mask.getMaskCode();
		int headSize = mask.getHeadSize();
		int tailSize = mask.getTailSize();
		// 自定义剪切长度
		if (headSize > 0 || tailSize > 0) {
			return StringUtil.secureMask(realStr, (headSize > 0) ? headSize : 0, (tailSize > 0) ? tailSize : 0,
					maskCode);
		}
		// 按类别处理
		// 电话
		if ("tel".equals(type)) {
			if (size >= 11)
				return StringUtil.secureMask(realStr, 3, 4, maskCode);
			else
				return StringUtil.secureMask(realStr, 4, 0, maskCode);
		} // 邮件
		else if ("email".equals(type)) {
			return realStr.substring(0, 1).concat(maskCode).concat(realStr.substring(realStr.indexOf("@")));
		} // 身份证
		else if ("id-card".equals(type)) {
			return StringUtil.secureMask(realStr, 0, 4, maskCode);
		} // 银行卡
		else if ("bank-card".equals(type)) {
			return StringUtil.secureMask(realStr, 6, 4, maskCode);
		} // 姓名
		else if ("name".equals(type)) {
			if (size >= 4)
				return StringUtil.secureMask(realStr, 2, 0, maskCode);
			else
				return StringUtil.secureMask(realStr, 1, 0, maskCode);
		} // 地址
		else if ("address".equals(type)) {
			if (size >= 12)
				return StringUtil.secureMask(realStr, 6, 0, maskCode);
			else if (size >= 8)
				return StringUtil.secureMask(realStr, 4, 0, maskCode);
			else
				return StringUtil.secureMask(realStr, 2, 0, maskCode);
		} // 对公银行账号
		else if ("public-account".equals(type)) {
			return StringUtil.secureMask(realStr, 2, 0, maskCode);
		}

		// 按比例模糊(百分比)
		if (mask.getMaskRate() > 0) {
			int maskSize = Double.valueOf(size * mask.getMaskRate() * 1.00 / 100).intValue();
			if (maskSize < 1)
				maskSize = 1;
			else if (maskSize >= size)
				maskSize = size - 1;
			tailSize = (size - maskSize) / 2;
			headSize = size - maskSize - tailSize;
			if (maskCode == null) {
				maskCode = "*";
				if (maskSize > 3)
					maskCode = "***";
				else if (maskSize == 2)
					maskCode = "**";
			}
		}
		return StringUtil.secureMask(realStr, headSize, tailSize, maskCode);
	}

	private static List getResultSet(SqlToyConfig sqlToyConfig, SqlToyContext sqlToyContext, Connection conn,
			ResultSet rs, UpdateRowHandler updateRowHandler, int rowCnt, HashMap<String, Integer> labelIndexMap,
			String[] labelNames, int startColIndex) throws Exception {
		// 字段连接(多行数据拼接成一个数据,以一行显示)
		LinkModel linkModel = sqlToyConfig.getLinkModel();
		List<List> items = new ArrayList();
		boolean isDebug = logger.isDebugEnabled();
		// 判断是否有缓存翻译器定义
		Boolean hasTranslate = (sqlToyConfig.getTranslateMap() == null) ? false : true;
		HashMap<String, SqlTranslate> translateMap = hasTranslate ? sqlToyConfig.getTranslateMap() : null;
		HashMap<String, HashMap<String, Object[]>> translateCache = null;
		if (hasTranslate) {
			translateCache = sqlToyContext.getTranslateManager().getTranslates(sqlToyContext, conn, translateMap);
			if (translateCache == null || translateCache.isEmpty()) {
				hasTranslate = false;
				logger.debug("请正确配置TranslateManager!");
			}
		}

		// link 目前只支持单个字段运算
		int columnSize = labelNames.length;
		int index = 0;

		// 警告阀值
		int warnThresholds = SqlToyConstants.getWarnThresholds();
		boolean warnLimit = false;
		// 最大阀值
		long maxThresholds = SqlToyConstants.getMaxThresholds();
		boolean maxLimit = false;
		// 是否判断全部为null的行记录
		boolean ignoreAllEmpty = sqlToyConfig.isIgnoreEmpty();
		// 最大值要大于等于警告阀值
		if (maxThresholds > 1 && maxThresholds <= warnThresholds)
			maxThresholds = warnThresholds;
		List rowTemp;
		if (linkModel != null) {
			Object identity = null;
			int linkIndex = labelIndexMap.get(linkModel.getColumn().toLowerCase());
			StringBuilder linkBuffer = new StringBuilder();
			boolean hasDecorate = (linkModel.getDecorateAppendChar() == null) ? false : true;
			boolean isLeft = true;
			if (hasDecorate) {
				isLeft = linkModel.getDecorateAlign().equals("left") ? true : false;
			}
			Object preIdentity = null;
			Object linkValue;
			Object linkStr;
			boolean translateLink = hasTranslate ? translateMap.containsKey(linkModel.getColumn().toLowerCase())
					: false;
			HashMap<String, Object[]> linkTranslateMap = null;
			int linkTranslateIndex = 1;
			SqlTranslate translateModel = null;
			if (translateLink) {
				translateModel = translateMap.get(linkModel.getColumn().toLowerCase());
				linkTranslateIndex = translateModel.getIndex();
				linkTranslateMap = translateCache.get(translateModel.getColumn());
			}
			Object[] cacheValues;
			// 判断link拼接是否重新开始
			boolean isLastProcess = false;
			while (rs.next()) {
				isLastProcess = false;
				linkValue = rs.getObject(linkModel.getColumn());
				if (linkValue == null) {
					linkStr = "";
				} else {
					if (translateLink) {
						cacheValues = linkTranslateMap.get(linkValue.toString());
						if (cacheValues == null) {
							linkStr = "";
							if (isDebug)
								logger.debug("translate cache:{} 对应的key:{} 没有设置相应的value!", translateModel.getCache(),
										linkValue);
						} else
							linkStr = cacheValues[linkTranslateIndex];
					} else
						linkStr = linkValue.toString();
				}
				identity = (linkModel.getIdColumn() == null) ? "default" : rs.getObject(linkModel.getIdColumn());
				// 不相等
				if (!identity.equals(preIdentity)) {
					if (index != 0) {
						items.get(items.size() - 1).set(linkIndex, linkBuffer.toString());
						linkBuffer.delete(0, linkBuffer.length());
					}
					linkBuffer.append(linkStr);
					if (hasTranslate) {
						rowTemp = processResultRowWithTranslate(translateMap, translateCache, labelNames, rs,
								columnSize, ignoreAllEmpty);
					} else {
						rowTemp = processResultRow(rs, startColIndex, rowCnt, ignoreAllEmpty);
					}
					if (rowTemp != null)
						items.add(rowTemp);
					preIdentity = identity;
				} else {
					if (linkBuffer.length() > 0)
						linkBuffer.append(linkModel.getSign());
					linkBuffer.append(hasDecorate ? StringUtil.appendStr(linkStr.toString(),
							linkModel.getDecorateAppendChar(), linkModel.getDecorateSize(), isLeft) : linkStr);
					isLastProcess = true;
				}
				index++;
				// 存在超出25000条数据的查询
				if (index == warnThresholds) {
					warnLimit = true;
				}
				// 提取数据超过上限(-1表示不限制)
				if (index == maxThresholds) {
					maxLimit = true;
					break;
				}
			}
			if (isLastProcess) {
				items.get(items.size() - 1).set(linkIndex, linkBuffer.toString());
			}
		} else {
			// 修改操作不支持link操作
			boolean isUpdate = false;
			if (updateRowHandler != null)
				isUpdate = true;

			// 循环通过java reflection将rs中的值映射到VO中
			if (hasTranslate) {
				while (rs.next()) {
					// 先修改后再获取最终值
					if (isUpdate) {
						updateRowHandler.updateRow(rs, index);
						rs.updateRow();
					}
					rowTemp = processResultRowWithTranslate(translateMap, translateCache, labelNames, rs, columnSize,
							ignoreAllEmpty);
					if (rowTemp != null)
						items.add(rowTemp);
					index++;
					// 存在超出25000条数据的查询(具体数据规模可以通过参数进行定义)
					if (index == warnThresholds) {
						warnLimit = true;
					}
					// 超出最大提取数据阀值,直接终止数据提取
					if (index == maxThresholds) {
						maxLimit = true;
						break;
					}
				}
			} else {
				while (rs.next()) {
					if (isUpdate) {
						updateRowHandler.updateRow(rs, index);
						rs.updateRow();
					}
					rowTemp = processResultRow(rs, startColIndex, rowCnt, ignoreAllEmpty);
					if (rowTemp != null)
						items.add(rowTemp);
					index++;
					// 存在超出警告规模级的数据查询
					if (index == warnThresholds) {
						warnLimit = true;
					}
					// 提取数据超过上限(-1表示不限制)
					if (index == maxThresholds) {
						maxLimit = true;
						break;
					}
				}
			}
		}
		// 超出警告阀值
		if (warnLimit)
			warnLog(sqlToyConfig, index);
		// 超过最大提取数据阀值
		if (maxLimit)
			logger.error("Max Large Result:执行sql提取数据超出最大阀值限制{},sqlId={},具体语句={}", index, sqlToyConfig.getId(),
					sqlToyConfig.getSql());
		return items;
	}

	/**
	 * @todo 对结果进行数据旋转
	 * @param pivotModel
	 * @param labelIndexMap
	 * @param result
	 * @param pivotCategorySet
	 * @param debug
	 * @return
	 * @throws Exception
	 */
	private static List pivotResult(PivotModel pivotModel, HashMap<String, Integer> labelIndexMap, List result,
			List pivotCategorySet, boolean debug) {
		if (result == null || result.isEmpty())
			return result;
		// 行列转换
		if (pivotModel.getGroupCols() == null || pivotModel.getCategoryCols().length == 0) {
			return CollectionUtil.convertColToRow(result, null);
		}
		// 参照列，如按年份进行旋转
		Integer[] categoryCols = mappingLabelIndex(pivotModel.getCategoryCols(), labelIndexMap);

		// 旋转列，如按年份进行旋转，则旋转列为：年份下面的合格数量、不合格数量等子分类数据
		Integer[] pivotCols = mappingLabelIndex(pivotModel.getPivotCols(), labelIndexMap);
		// 分组主键列（以哪几列为基准）
		Integer[] groupCols = mappingLabelIndex(pivotModel.getGroupCols(), labelIndexMap);
		// update 2016-12-13 提取category后进行了排序
		List categoryList = (pivotCategorySet == null) ? extractCategory(result, categoryCols) : pivotCategorySet;
		if (debug) {
			logger.info("---------pivot category info--------------------");
			DebugUtil.printAry(categoryList, " , ", true);
		}
		return CollectionUtil.pivotList(result, categoryList, null, groupCols, categoryCols, pivotCols[0],
				pivotCols[pivotCols.length - 1], pivotModel.getDefaultValue());
	}

	/**
	 * @todo 列转行
	 * @param unpivotModel
	 * @param resultModel
	 * @param labelIndexMap
	 * @param result
	 * @return
	 * @throws Exception
	 */
	private static List unPivotResult(UnpivotModel unpivotModel, DataSetResult resultModel,
			HashMap<String, Integer> labelIndexMap, List result) {
		if (result == null || result.isEmpty())
			return result;
		int cols = unpivotModel.getColumns().length;
		List newResult = new ArrayList();
		Integer[] unpivotCols = new Integer[cols];
		Integer[] sortUnpivotCols = new Integer[cols];
		for (int i = 0; i < cols; i++) {
			unpivotCols[i] = labelIndexMap.get(unpivotModel.getColumns()[i]);
			sortUnpivotCols[i] = unpivotCols[i];
		}
		boolean hasLabelsColumn = (unpivotModel.getLabelsColumn() == null) ? false : true;

		// 判断select 中是否已经预留了旋转列label和value对应的字段，如果有则update相关列的数据
		// 如:select bizDate,'' as unpivot_label,null as
		// unpivot_value,unpivot_col1,unpivot_col2
		// 结果:bizDate unpivot_label unpivot_value
		// ==== 2015-12-1--- 个人 -------1000
		// ==== 2015-12-1--- 企业 -------4000
		int labelColIndex = -1;
		int valueColIndex = -1;
		if (labelIndexMap.get(unpivotModel.getLabelsColumn().toLowerCase()) != null)
			labelColIndex = labelIndexMap.get(unpivotModel.getLabelsColumn().toLowerCase()).intValue();

		if (labelIndexMap.get(unpivotModel.getAsColumn().toLowerCase()) != null)
			valueColIndex = labelIndexMap.get(unpivotModel.getAsColumn().toLowerCase()).intValue();

		// 排序,从大到小排
		CollectionUtil.sortArray(sortUnpivotCols, true);
		// 插在最开始被旋转的列位置前
		int addIndex = sortUnpivotCols[sortUnpivotCols.length - 1].intValue();
		if (addIndex < 0)
			addIndex = 0;
		// 将多列转成行，记录数量是列的倍数
		int size = result.size() * cols;
		int removeAdd = 0;
		for (int i = 0; i < size; i++) {
			removeAdd = 0;
			List row = (List) ((ArrayList) result.get(i / cols)).clone();
			// 标题列
			if (hasLabelsColumn) {
				if (labelColIndex != -1)
					row.set(labelColIndex, unpivotModel.getColsAlias()[i % cols]);
				else {
					row.add(addIndex, unpivotModel.getColsAlias()[i % cols]);
					removeAdd = 1;
				}
			}
			if (valueColIndex != -1)
				row.set(valueColIndex, row.get(unpivotCols[i % cols]));
			else {
				row.add(addIndex + removeAdd, row.get(unpivotCols[i % cols] + removeAdd));
				removeAdd = removeAdd + 1;
			}
			// 从最大列进行删除被旋转的列
			for (int j = 0; j < cols; j++)
				row.remove(sortUnpivotCols[j].intValue() + removeAdd);
			newResult.add(row);
		}

		// 标题的名称
		String[] labelNames = resultModel.getLabelNames();
		String[] labelTypes = resultModel.getLabelTypes();
		List<String> labelList = new ArrayList<String>();
		List<String> labelTypeList = new ArrayList<String>();
		for (int i = 0; i < labelNames.length; i++) {
			labelList.add(labelNames[i]);
			labelTypeList.add(labelTypes[i]);
		}
		if (removeAdd > 0) {
			// 变成行的列标题是否作为一列
			if (hasLabelsColumn) {
				labelList.add(addIndex, unpivotModel.getLabelsColumn());
				labelTypeList.add(addIndex, "string");
			}
			labelList.add(addIndex + removeAdd - 1, unpivotModel.getAsColumn());
			labelTypeList.add(addIndex + removeAdd - 1, "object");
		}
		// 移除转变成行的列
		for (int j = 0; j < cols; j++) {
			labelList.remove(sortUnpivotCols[j].intValue() + removeAdd);
			labelTypeList.remove(sortUnpivotCols[j].intValue() + removeAdd);
		}
		String[] newLabelNames = new String[labelList.size()];
		String[] newLabelTypes = new String[labelList.size()];
		labelList.toArray(newLabelNames);
		labelTypeList.toArray(newLabelTypes);
		resultModel.setLabelNames(newLabelNames);
		resultModel.setLabelTypes(newLabelTypes);
		return newResult;
	}

	/**
	 * @todo 将label别名换成对应的列编号(select name,sex from xxxTable，name别名对应的列则为0)
	 * @param columnLabels
	 * @param labelIndexMap
	 * @return
	 */
	private static Integer[] mappingLabelIndex(String[] columnLabels, HashMap<String, Integer> labelIndexMap) {
		Integer[] result = new Integer[columnLabels.length];
		for (int i = 0; i < result.length; i++) {
			if (CommonUtils.isInteger(columnLabels[i]))
				result[i] = Integer.parseInt(columnLabels[i]);
			else
				result[i] = labelIndexMap.get(columnLabels[i].toLowerCase());
		}
		return result;
	}

	/**
	 * @todo 提取出选择的横向分类信息
	 * @param items
	 * @param categoryCols
	 * @return
	 */
	private static List extractCategory(List items, Integer[] categoryCols) {
		List categoryList = new ArrayList();
		HashMap identityMap = new HashMap();
		String tmpStr;
		int categorySize = categoryCols.length;
		Object obj;
		List categoryRow;
		List row;
		for (int i = 0, size = items.size(); i < size; i++) {
			row = (List) items.get(i);
			tmpStr = "";
			categoryRow = new ArrayList();
			for (int j = 0; j < categorySize; j++) {
				obj = row.get(categoryCols[j]);
				categoryRow.add(obj);
				tmpStr = tmpStr.concat(obj == null ? "null" : obj.toString());
			}
			// 不存在
			if (!identityMap.containsKey(tmpStr)) {
				categoryList.add(categoryRow);
				identityMap.put(tmpStr, "");
			}
		}
		// 分组排序输出
		if (categoryCols.length > 1) {
			categoryList = sortList(categoryList, 0, 0, categoryList.size() - 1, true);
			for (int i = 1; i < categoryCols.length; i++) {
				categoryList = sortGroupList(categoryList, i - 1, i, true);
			}
		}
		return CollectionUtil.convertColToRow(categoryList, null);
	}

	/**
	 * @todo 分组排序
	 * @param sortList
	 * @param groupCol
	 * @param orderCol
	 * @param ascend
	 * @return
	 */
	private static List sortGroupList(List<List> sortList, int groupCol, int orderCol, boolean ascend) {
		int length = sortList.size();
		// 1:string,2:数字;3:日期
		int start = 0;
		int end;
		Object compareValue = null;
		Object tempObj;
		for (int i = 0; i < length; i++) {
			tempObj = sortList.get(i).get(groupCol);
			if (!tempObj.equals(compareValue)) {
				end = i - 1;
				sortList(sortList, orderCol, start, end, ascend);
				start = i;
				compareValue = tempObj;
			}
			if (i == length - 1) {
				sortList(sortList, orderCol, start, i, ascend);
			}
		}
		return sortList;
	}

	/**
	 * @todo 对二维数据进行排序
	 * @param sortList
	 * @param orderCol
	 * @param dataType
	 * @param ascend
	 * @return
	 */
	private static List sortList(List<List> sortList, int orderCol, int start, int end, boolean ascend) {
		if (end <= start)
			return sortList;
		Object iData;
		Object jData;
		// 1:string,2:数字;3:日期
		boolean lessThen = false;
		for (int i = start; i < end; i++) {
			for (int j = i + 1; j < end + 1; j++) {
				iData = sortList.get(i).get(orderCol);
				jData = sortList.get(j).get(orderCol);
				if ((iData == null && jData == null) || (iData != null && jData == null))
					lessThen = false;
				else if (iData == null && jData != null)
					lessThen = true;
				else {
					lessThen = (iData.toString()).compareTo(jData.toString()) < 0;
				}

				if ((ascend && !lessThen) || (!ascend && lessThen)) {
					List tempList = sortList.get(i);
					sortList.set(i, sortList.get(j));
					sortList.set(j, tempList);
				}
			}
		}
		return sortList;
	}

	/**
	 * @todo 对数据进行汇总合计
	 * @param summaryModel
	 * @param labelIndexMap
	 * @param result
	 * @return
	 */
	private static void groupSummary(SummaryModel summaryModel, HashMap<String, Integer> labelIndexMap, List result) {
		if (result == null || result.isEmpty())
			return;
		List<Integer> sumColList = new ArrayList<Integer>();
		// 参照列，如按年份进行旋转(columns="1..result.width()-1")
		String cols = summaryModel.getSummaryCols().replaceAll("result\\.width\\(\\)",
				Integer.toString(((List) result.get(0)).size()));
		cols = cols.replaceAll("\\$\\{dataWidth\\}", Integer.toString(((List) result.get(0)).size()));
		String[] columns = cols.split(",");
		String column;
		String endColumnStr;
		int step;
		int stepIndex;
		for (int i = 0; i < columns.length; i++) {
			column = columns[i].toLowerCase();
			// like {1..20?2} ?step 用于数据间隔性汇总
			if (column.indexOf("..") != -1) {
				step = 1;
				String[] beginToEnd = column.split("\\.\\.");
				int begin = 0;
				int end = 0;
				if (CommonUtils.isInteger(beginToEnd[0]))
					begin = Integer.parseInt(beginToEnd[0]);
				else
					begin = (new BigDecimal(ExpressionUtil.calculate(beginToEnd[0]).toString())).intValue();
				endColumnStr = beginToEnd[1];
				if (CommonUtils.isInteger(endColumnStr))
					end = Integer.parseInt(endColumnStr);
				else {
					stepIndex = endColumnStr.indexOf("?");
					if (stepIndex != -1) {
						step = Integer.parseInt(endColumnStr.substring(stepIndex + 1).trim());
						endColumnStr = endColumnStr.substring(0, stepIndex);
					}
					end = (new BigDecimal(ExpressionUtil.calculate(endColumnStr).toString())).intValue();
				}
				for (int j = begin; j <= end; j += step) {
					if (!sumColList.contains(j))
						sumColList.add(j);
				}
			} else if (CommonUtils.isInteger(column)) {
				if (!sumColList.contains(Integer.parseInt(column)))
					sumColList.add(Integer.parseInt(column));
			} else {
				Integer colIndex;
				if (labelIndexMap.containsKey(column))
					colIndex = labelIndexMap.get(column);
				else
					colIndex = (new BigDecimal(ExpressionUtil.calculate(column).toString())).intValue();
				if (!sumColList.contains(colIndex))
					sumColList.add(colIndex);
			}
		}
		Integer[] summaryCols = new Integer[sumColList.size()];
		sumColList.toArray(summaryCols);
		boolean hasAverage = false;
		if (summaryModel.getGlobalAverageTitle() != null || summaryModel.getSumSite().equals("left")
				|| summaryModel.getSumSite().equals("right"))
			hasAverage = true;
		Object[][] groupIndexs = null;
		if (summaryModel.getGroupMeta() != null) {
			groupIndexs = new Object[summaryModel.getGroupMeta().length][5];
			GroupMeta groupMeta;
			// {{汇总列，汇总标题，平均标题，汇总相对平均的位置(left/right/top/bottom)}}
			for (int i = 0; i < groupIndexs.length; i++) {
				Object[] group = new Object[5];
				groupMeta = summaryModel.getGroupMeta()[i];
				group[0] = CommonUtils.isInteger(groupMeta.getGroupColumn())
						? Integer.parseInt(groupMeta.getGroupColumn())
						: labelIndexMap.get(groupMeta.getGroupColumn().toLowerCase());
				group[1] = groupMeta.getSumTitle();
				group[2] = groupMeta.getAverageTitle();
				group[3] = summaryModel.getSumSite();
				if (groupMeta.getLabelColumn() != null)
					group[4] = CommonUtils.isInteger(groupMeta.getLabelColumn())
							? Integer.parseInt(groupMeta.getLabelColumn())
							: labelIndexMap.get(groupMeta.getLabelColumn().toLowerCase());
				groupIndexs[i] = group;
			}
		}
		int globalLabelIndex = -1;
		if (summaryModel.getGlobalLabelColumn() != null) {
			if (CommonUtils.isInteger(summaryModel.getGlobalLabelColumn()))
				globalLabelIndex = Integer.parseInt(summaryModel.getGlobalLabelColumn());
			else
				globalLabelIndex = labelIndexMap.get(summaryModel.getGlobalLabelColumn().toLowerCase());
		}
		// 逆向汇总
		if (summaryModel.isReverse()) {
			CollectionUtil.groupReverseSummary(result, groupIndexs, summaryCols, globalLabelIndex,
					summaryModel.getGlobalSumTitle(), hasAverage, summaryModel.getGlobalAverageTitle(),
					summaryModel.getRadixSize(), summaryModel.getSumSite());
		} else {
			CollectionUtil.groupSummary(result, groupIndexs, summaryCols, globalLabelIndex,
					summaryModel.getGlobalSumTitle(), hasAverage, summaryModel.getGlobalAverageTitle(),
					summaryModel.getRadixSize(), summaryModel.getSumSite(), summaryModel.isGlobalReverse());
		}
	}

	/**
	 * @todo 处理ResultSet的单行数据
	 * @param rs
	 * @param startColIndex
	 * @param rowCnt
	 * @return
	 * @throws Exception
	 */
	private static List processResultRow(ResultSet rs, int startColIndex, int rowCnt, boolean ignoreAllEmptySet)
			throws Exception {
		List rowData = new ArrayList();
		Object fieldValue;
		// 单行所有字段结果为null
		boolean allNull = true;
		for (int i = startColIndex; i < rowCnt; i++) {
			fieldValue = rs.getObject(i + 1);
			if (null != fieldValue) {
				if (fieldValue instanceof java.sql.Clob)
					fieldValue = SqlUtil.clobToString((java.sql.Clob) fieldValue);
				// 有一个非null
				allNull = false;
			}
			rowData.add(fieldValue);
		}
		// 全null返回null结果，外围判断结果为null则不加入结果集合
		if (allNull && ignoreAllEmptySet)
			return null;
		return rowData;
	}

	/**
	 * @todo 存在缓存翻译的结果处理
	 * @param translateMap
	 * @param translateCaches
	 * @param labelNames
	 * @param rs
	 * @param size
	 * @return
	 * @throws Exception
	 */
	private static List processResultRowWithTranslate(HashMap<String, SqlTranslate> translateMap,
			HashMap<String, HashMap<String, Object[]>> translateCaches, String[] labelNames, ResultSet rs, int size,
			boolean ignoreAllEmptySet) throws Exception {
		List rowData = new ArrayList();
		Object fieldValue;
		SqlTranslate translate;
		String label;
		String keyIndex;
		boolean allNull = true;
		for (int i = 0; i < size; i++) {
			label = labelNames[i];
			fieldValue = rs.getObject(label);
			label = label.toLowerCase();
			keyIndex = Integer.toString(i);
			if (null != fieldValue) {
				allNull = false;
				if (fieldValue instanceof java.sql.Clob)
					fieldValue = SqlUtil.clobToString((java.sql.Clob) fieldValue);
				if (translateMap.containsKey(label) || translateMap.containsKey(keyIndex)) {
					translate = translateMap.get(label);
					if (translate == null)
						translate = translateMap.get(keyIndex);
					fieldValue = translateKey(translate, translateCaches.get(translate.getColumn()), fieldValue);
				}
			}
			rowData.add(fieldValue);
		}
		if (allNull && ignoreAllEmptySet)
			return null;
		return rowData;
	}

	/**
	 * @date 2018-5-26 优化缓存翻译，提供keyCode1,keyCode2,keyCode3 形式的多代码翻译
	 * @todo 统一对key进行缓存翻译
	 * @param translate
	 * @param translateKeyMap
	 * @param fieldValue
	 * @return
	 */
	private static Object translateKey(SqlTranslate translate, HashMap<String, Object[]> translateKeyMap,
			Object fieldValue) {
		String fieldStr = fieldValue.toString();
		if (translate.getSplitRegex() == null) {
			Object[] cacheValues = translateKeyMap.get(fieldStr);
			if (cacheValues == null) {
				if (translate.getUncached() != null)
					fieldValue = translate.getUncached().replace("${value}", fieldStr);
				else
					fieldValue = fieldValue.toString();
				logger.debug("translate cache:{} 对应的key:{}没有设置相应的value!", translate.getCache(), fieldValue);
			} else
				fieldValue = cacheValues[translate.getIndex()];
			return fieldValue;
		} else {
			String[] keys = fieldStr.split(translate.getSplitRegex());
			String linkSign = translate.getLinkSign();
			StringBuilder result = new StringBuilder();
			int index = 0;
			for (String key : keys) {
				if (index > 0)
					result.append(linkSign);
				Object[] cacheValues = translateKeyMap.get(key.trim());
				if (cacheValues == null) {
					if (translate.getUncached() != null)
						result.append(translate.getUncached().replace("${value}", key));
					else
						result.append(key);
					logger.debug("translate cache:{} 对应的key:{}没有设置相应的value!", translate.getCache(), key);
				} else
					result.append(cacheValues[translate.getIndex()]);
				index++;
			}
			return result.toString();
		}
	}

	/**
	 * @todo 提取数据旋转对应的sql查询结果
	 * @param sqlToyContext
	 * @param sqlToyConfig
	 * @param queryExecutor
	 * @param conn
	 * @param dialect
	 * @return
	 * @throws Exception
	 */
	public static List getPivotCategory(SqlToyContext sqlToyContext, SqlToyConfig sqlToyConfig,
			QueryExecutor queryExecutor, Connection conn, String dialect) throws Exception {
		if (sqlToyConfig.getResultProcessor() != null && !sqlToyConfig.getResultProcessor().isEmpty()) {
			List resultProcessors = sqlToyConfig.getResultProcessor();
			Object processor;
			for (int i = 0; i < resultProcessors.size(); i++) {
				processor = resultProcessors.get(i);
				// 数据旋转只能存在一个
				if (processor instanceof PivotModel) {
					PivotModel pivotModel = (PivotModel) processor;
					if (pivotModel.getCategorySql() != null) {
						SqlToyConfig pivotSqlConfig = DialectUtils.getUnifyParamsNamedConfig(sqlToyContext,
								sqlToyContext.getSqlToyConfig(pivotModel.getCategorySql(), SqlType.search),
								queryExecutor, dialect, false);
						SqlToyResult pivotSqlToyResult = SqlConfigParseUtils.processSql(pivotSqlConfig.getSql(),
								queryExecutor.getParamsName(pivotSqlConfig),
								queryExecutor.getParamsValue(sqlToyContext, pivotSqlConfig));
						List pivotCategory = SqlUtil.findByJdbcQuery(pivotSqlToyResult.getSql(),
								pivotSqlToyResult.getParamsValue(), null, null, conn, sqlToyConfig.isIgnoreEmpty());
						// 行转列返回
						return CollectionUtil.convertColToRow(pivotCategory, null);
					}
				}
			}
		}
		return null;
	}

	/**
	 * @todo 非存储过程模式调用结果计算处理器
	 * @param sqlToyConfig
	 * @param dataSetResult
	 * @param pivotCategorySet
	 * @param debug
	 * @throws Exception
	 */
	public static void calculate(SqlToyConfig sqlToyConfig, DataSetResult dataSetResult, List pivotCategorySet,
			boolean debug) {
		if (sqlToyConfig.getResultProcessor() != null) {
			List items = dataSetResult.getRows();
			List resultProcessors = sqlToyConfig.getResultProcessor();
			Object processor;
			HashMap<String, Integer> labelIndexMap = new HashMap<String, Integer>();
			String realLabelName;
			String[] fields = dataSetResult.getLabelNames();
			for (int i = 0, n = fields.length; i < n; i++) {
				realLabelName = fields[i].toLowerCase();
				if (realLabelName.indexOf(":") != -1)
					realLabelName = realLabelName.substring(0, realLabelName.indexOf(":")).trim();
				labelIndexMap.put(realLabelName, i);
			}

			for (int i = 0; i < resultProcessors.size(); i++) {
				processor = resultProcessors.get(i);
				// 数据旋转
				if (processor instanceof PivotModel) {
					items = pivotResult((PivotModel) processor, labelIndexMap, items, pivotCategorySet, debug);
				} else if (processor instanceof UnpivotModel) {
					items = unPivotResult((UnpivotModel) processor, dataSetResult, labelIndexMap, items);
				} else {
					// 数据汇总合计
					groupSummary((SummaryModel) processor, labelIndexMap, items);
				}
			}
			dataSetResult.setRows(items);
		}
	}

	/**
	 * @todo 根据查询结果的类型，构造相应对象集合(增加map形式的结果返回机制)
	 * @param queryResultRows
	 * @param labelNames
	 * @param resultType
	 * @return
	 * @throws Exception
	 */
	public static List wrapQueryResult(List queryResultRows, String[] labelNames, Class resultType) throws Exception {
		if (resultType == null)
			return queryResultRows;
		if (queryResultRows == null)
			return null;
		// 如果结果类型是hashMap
		if (resultType.equals(HashMap.class) || resultType.equals(Map.class)
				|| resultType.equals(LinkedHashMap.class)) {
			int width = labelNames.length;
			boolean isLinked = (resultType.equals(LinkedHashMap.class)) ? true : false;
			List result = new ArrayList();
			List rowList;
			for (int i = 0, n = queryResultRows.size(); i < n; i++) {
				rowList = (List) queryResultRows.get(i);
				Map rowMap = isLinked ? new LinkedHashMap() : new HashMap();
				for (int j = 0; j < width; j++)
					rowMap.put(labelNames[j], rowList.get(j));
				result.add(rowMap);
			}
			return result;
		} else
			return BeanUtil.reflectListToBean(queryResultRows, labelNames, resultType);
	}

	/**
	 * @todo 加工字段名称，将数据库sql查询的columnName转成对应对象的属性名称(去除下划线)
	 * @param labelNames
	 * @return
	 */
	public static String[] humpFieldNames(String[] labelNames) {
		if (labelNames == null)
			return null;
		String[] result = new String[labelNames.length];
		for (int i = 0, n = labelNames.length; i < n; i++)
			result[i] = StringUtil.toHumpStr(labelNames[i], false);
		return result;
	}

	public static String[] humpFieldNames(QueryExecutor queryExecutor, String[] labelNames) {
		Type resultType = queryExecutor.getResultType();
		boolean hump = true;
		if (null != resultType && (resultType.equals(HashMap.class) || resultType.equals(Map.class)
				|| resultType.equals(LinkedHashMap.class)) && !queryExecutor.isHumpMapLabel()) {
			hump = false;
		}
		if (hump) {
			return humpFieldNames(labelNames);
		}
		return labelNames;
	}

	/**
	 * @todo 警告性日志记录,凡是单次获取超过一定规模数据的操作记录日志
	 * @param sqlToyConfig
	 * @param totalCount
	 */
	private static void warnLog(SqlToyConfig sqlToyConfig, int totalCount) {
		logger.warn("Large Result:totalCount={},sqlId={},sql={}", totalCount, sqlToyConfig.getId(),
				sqlToyConfig.getSql());
	}
}
