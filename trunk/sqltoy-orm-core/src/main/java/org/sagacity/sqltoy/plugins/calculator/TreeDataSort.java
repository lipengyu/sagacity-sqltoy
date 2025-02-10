/**
 * 
 */
package org.sagacity.sqltoy.plugins.calculator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagacity.sqltoy.config.model.LabelIndexModel;
import org.sagacity.sqltoy.config.model.TreeSortModel;
import org.sagacity.sqltoy.plugins.utils.CalculateUtils;
import org.sagacity.sqltoy.utils.CollectionUtil;
import org.sagacity.sqltoy.utils.MacroIfLogic;
import org.sagacity.sqltoy.utils.StringUtil;

/**
 * @project sagacity-sqltoy
 * @description 对树型表结构数据进行排序
 * @author zhongxuchen
 * @version v1.0, Date:2022年10月28日
 * @modify 2022年10月28日,修改说明
 * @modify 2023年7月23日 增加level-order-column属性，支持同层级内数据排序
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class TreeDataSort {
	public static void process(TreeSortModel treeTableSortModel, LabelIndexModel labelIndexMap, List treeList) {
		if (treeList == null || treeList.isEmpty()) {
			return;
		}
		Integer idColIndex = labelIndexMap.get(treeTableSortModel.getIdColumn());
		Integer pidColIndex = labelIndexMap.get(treeTableSortModel.getPidColumn());
		if (idColIndex == null || pidColIndex == null) {
			throw new RuntimeException("对树形结构数据进行排序,未正确指定id-column和pid-column!");
		}
		int dataWidth = ((List) treeList.get(0)).size();
		// 汇总列
		List<Integer> sumColList = CalculateUtils.parseColumns(labelIndexMap, treeTableSortModel.getSumColumns(),
				dataWidth);
		// 组织树形结构
		sortTree(treeList, idColIndex, pidColIndex);
		// 树结构从底层往上级汇总
		if (!sumColList.isEmpty()) {
			Integer[] sumIndexes = new Integer[sumColList.size()];
			sumColList.toArray(sumIndexes);
			summaryTreeList(treeTableSortModel, labelIndexMap, treeList, sumIndexes, idColIndex, pidColIndex);
		}
		// 对每层的数据进行排序
		if (StringUtil.isNotBlank(treeTableSortModel.getLevelOrderColumn())) {
			Integer sortColIndex = labelIndexMap.get(treeTableSortModel.getLevelOrderColumn());
			if (sortColIndex == null) {
				throw new RuntimeException("对树形结构每层级内部进行排序，未正确指定层级排序依据的列:levelOrderColumn="
						+ treeTableSortModel.getLevelOrderColumn() + "!");
			}
			int dataType = CollectionUtil.getSortDataType(treeList, sortColIndex);
			boolean desc = treeTableSortModel.getOrderWay().equalsIgnoreCase("desc") ? true : false;
			// 整个集合按某列的值进行排序
			CollectionUtil.sortList(treeList, sortColIndex, dataType, 0, treeList.size() - 1, !desc);
			// 再重新进行树节点组织
			sortTree(treeList, idColIndex, pidColIndex);
		}
	}

	/**
	 * @TODO 按照树的父子关系组织顺序
	 * @param treeList
	 * @param idColIndex
	 * @param pidColIndex
	 */
	private static void sortTree(List treeList, Integer idColIndex, Integer pidColIndex) {
		// 获取根节点值
		Set topPids = getTopPids(treeList, idColIndex, pidColIndex);
		List result = new ArrayList();
		List row;
		// 提取第一层树节点
		for (int i = 0; i < treeList.size(); i++) {
			row = (List) treeList.get(i);
			if (topPids.contains(row.get(pidColIndex))) {
				result.add(row);
				treeList.remove(i);
				i--;
			}
		}
		int beginIndex = 0;
		int addCount = 0;
		Object idValue;
		Object pidValue;
		while (treeList.size() != 0) {
			addCount = 0;
			// id
			idValue = ((List) result.get(beginIndex)).get(idColIndex);
			for (int i = 0; i < treeList.size(); i++) {
				pidValue = ((List) treeList.get(i)).get(pidColIndex);
				if (idValue.equals(pidValue)) {
					result.add(beginIndex + addCount + 1, treeList.get(i));
					treeList.remove(i);
					addCount++;
					i--;
				}
			}
			// 下一个
			beginIndex++;
			// 防止因数据不符合规则造成的死循环
			if (beginIndex + 1 > result.size()) {
				break;
			}
		}
		treeList.clear();
		treeList.addAll(result);
	}

	/**
	 * @TODO 提取根节点
	 * @param treeList
	 * @param idIndex
	 * @param pidIndex
	 * @return
	 */
	public static Set getTopPids(List treeList, int idIndex, int pidIndex) {
		Set<Object> idSet = new HashSet<Object>();
		Set<Object> pidSet = new HashSet<Object>();
		List row;
		for (int i = 0, n = treeList.size(); i < n; i++) {
			row = (List) treeList.get(i);
			idSet.add(row.get(idIndex));
			pidSet.add(row.get(pidIndex));
		}
		Set topPids = new HashSet();
		for (Object pid : pidSet) {
			if (!idSet.contains(pid)) {
				topPids.add(pid);
			}
		}
		return topPids;
	}

	/**
	 * @TODO 对排序后的树结构数据进行汇总，将子级数据汇总到父级上
	 * @param treeTableSortModel
	 * @param labelIndexMap
	 * @param treeList
	 * @param sumIndexes
	 * @param idColIndex
	 * @param pidColIndex
	 */
	public static void summaryTreeList(TreeSortModel treeTableSortModel, LabelIndexModel labelIndexMap, List treeList,
			Integer[] sumIndexes, Integer idColIndex, Integer pidColIndex) {
		List idRow;
		Object pid;
		Object id;
		List pidRow;
		Object pidCellValue, idCellValue;
		boolean hasFilter = false;
		if (StringUtil.isNotBlank(treeTableSortModel.getFilterColumn())
				&& StringUtil.isNotBlank(treeTableSortModel.getCompareValues())
				&& StringUtil.isNotBlank(treeTableSortModel.getCompareType())) {
			hasFilter = true;
		}
		boolean doSum = true;
		// 从最后一行开始
		Object filterValue;
		for (int i = treeList.size() - 1; i > 0; i--) {
			idRow = (List) treeList.get(i);
			pid = idRow.get(pidColIndex);
			doSum = true;
			if (hasFilter) {
				filterValue = idRow.get(labelIndexMap.get(treeTableSortModel.getFilterColumn()));
				doSum = MacroIfLogic.compare(filterValue, treeTableSortModel.getCompareType(),
						treeTableSortModel.getCompareValues());
			}
			// 上一行开始寻找父节点
			if (doSum) {
				for (int j = i - 1; j >= 0; j--) {
					pidRow = (List) treeList.get(j);
					id = pidRow.get(idColIndex);
					if (id.equals(pid)) {
						// 汇总列
						for (int sumIndex : sumIndexes) {
							pidCellValue = pidRow.get(sumIndex);
							idCellValue = idRow.get(sumIndex);
							// 父节点汇总列的值为null,将子节点的值转BigDecimal赋上
							if (pidCellValue == null) {
								if (idCellValue == null) {
									pidRow.set(sumIndex, BigDecimal.ZERO);
								} else {
									pidRow.set(sumIndex, new BigDecimal(idCellValue.toString().replace(",", "")));
								}
							} else if (pidCellValue instanceof BigDecimal) {
								// 子节点值+ 父节点值
								if (idCellValue != null) {
									pidRow.set(sumIndex, ((BigDecimal) pidCellValue)
											.add(new BigDecimal(idCellValue.toString().replace(",", ""))));
								}
							} else if (idCellValue != null) {
								// 子节点值+ 父节点值
								pidRow.set(sumIndex, new BigDecimal(pidCellValue.toString().replace(",", ""))
										.add(new BigDecimal(idCellValue.toString().replace(",", ""))));
							} else {
								// 子节点值转BigDecimal
								pidRow.set(sumIndex, new BigDecimal(pidCellValue.toString().replace(",", "")));
							}
						}
						break;
					}
				}
			}
		}
	}
}
