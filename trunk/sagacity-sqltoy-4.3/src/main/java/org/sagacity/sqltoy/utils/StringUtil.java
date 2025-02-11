/**
 * @Copyright 2007 版权归陈仁飞，不要肆意侵权抄袭，如引用请注明出处保留作者信息。
 */
package org.sagacity.sqltoy.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @project sagacity-sqltoy
 * @description 字符串处理常用功能
 * @author zhongxuchen <a href="mailto:zhongxuchen@gmail.com">联系作者</a>
 * @version id:StringUtil.java,Revision:v1.0,Date:Oct 19, 2007 10:09:42 AM
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class StringUtil {
	/**
	 * 字符串中包含中文的表达式
	 */
	private static Pattern chinaPattern = Pattern.compile("[\u4e00-\u9fa5]");

	/**
	 * private constructor,cann't be instantiated by other class 私有构造函数方法防止被实例化
	 */
	private StringUtil() {
	}

	@Deprecated
	public static boolean isNotNullAndBlank(Object str) {
		return !isBlank(str);
	}

	@Deprecated
	public static boolean isNullOrBlank(Object str) {
		return isBlank(str);
	}

	/**
	 * @todo 判断字符串是空或者空白
	 * @param str
	 * @return
	 */
	public static boolean isNotBlank(Object str) {
		return !isBlank(str);
	}

	public static boolean isBlank(Object str) {
		if (null == str)
			return true;
		if ((str instanceof CharSequence) && str.toString().trim().equals("")) {
			return true;
		} else if ((str instanceof Collection) && ((Collection) str).isEmpty()) {
			return true;
		} else if ((str instanceof Map) && ((Map) str).isEmpty()) {
			return true;
		}
		return false;
	}

	/**
	 * @todo 替换换行、回车、tab符号;\r 换行、\t tab符合、\n 回车
	 * @param source
	 * @param target
	 * @return
	 */
	public static String clearMistyChars(String source, String target) {
		if (source == null)
			return null;
		return source.replaceAll("\r", target).replaceAll("\t", target).replaceAll("\n", target);
	}

	/**
	 * @todo 返回第一个字符大写，其余保持不变的字符串
	 * @param sourceStr
	 * @return
	 */
	public static String firstToUpperCase(String sourceStr) {
		if (isBlank(sourceStr))
			return sourceStr;
		if (sourceStr.length() == 1)
			return sourceStr.toUpperCase();
		return sourceStr.substring(0, 1).toUpperCase().concat(sourceStr.substring(1));
	}

	/**
	 * @todo 返回第一个字符小写，其余保持不变的字符串
	 * @param sourceStr
	 * @return
	 */
	public static String firstToLowerCase(String sourceStr) {
		if (isBlank(sourceStr))
			return sourceStr;
		if (sourceStr.length() == 1)
			return sourceStr.toUpperCase();
		return sourceStr.substring(0, 1).toLowerCase().concat(sourceStr.substring(1));
	}

	/**
	 * @todo 返回第一个字符大写，其余保持不变的字符串
	 * @param sourceStr
	 * @return
	 */
	public static String firstToUpperOtherToLower(String sourceStr) {
		if (isBlank(sourceStr))
			return sourceStr;
		if (sourceStr.length() == 1)
			return sourceStr.toUpperCase();
		return sourceStr.substring(0, 1).toUpperCase().concat(sourceStr.substring(1).toLowerCase());
	}

	/**
	 * @todo 在不分大小写情况下字符所在位置
	 * @param source
	 * @param pattern
	 * @return
	 */
	public static int indexOfIgnoreCase(String source, String pattern) {
		if (source == null || pattern == null)
			return -1;
		return source.toLowerCase().indexOf(pattern.toLowerCase());
	}

	/**
	 * @todo 左补零
	 * @param source
	 * @param length
	 * @return
	 */
	public static String addLeftZero2Len(String source, int length) {
		return addSign2Len(source, length, 0, 0);
	}

	/**
	 * @todo 用空字符给字符串补足不足指定长度部分
	 * @param source
	 * @param length
	 * @return
	 */
	public static String addRightBlank2Len(String source, int length) {
		return addSign2Len(source, length, 1, 1);
	}

	/**
	 * @param source
	 * @param length
	 * @param flag
	 * @param leftOrRight
	 * @return
	 */
	private static String addSign2Len(String source, int length, int flag, int leftOrRight) {
		if (source == null || source.length() >= length)
			return source;
		int addSize = length - source.length();
		StringBuilder addStr = new StringBuilder();
		// 右边
		if (leftOrRight == 1)
			addStr.append(source);
		String sign = (flag == 1) ? " " : "0";
		for (int i = 0; i < addSize; i++)
			addStr.append(sign);
		// 左边
		if (leftOrRight == 0)
			addStr.append(source);
		return addStr.toString();
	}

	/**
	 * @todo <b>用特定符号循环拼接指定的字符串</b>
	 * @date 2012-7-12 下午10:17:30
	 * @param source
	 * @param sign
	 * @param loopSize
	 * @return
	 */
	public static String loopAppendWithSign(String source, String sign, int loopSize) {
		if (loopSize == 0)
			return "";
		if (loopSize == 1)
			return source;
		StringBuilder result = new StringBuilder(source);
		for (int i = 1; i < loopSize; i++)
			result.append(sign).append(source);
		return result.toString();
	}

	/**
	 * @todo 补字符(限单字符)
	 * @param source
	 * @param sign
	 * @param size
	 * @param isLeft
	 */
	public static String appendStr(String source, String sign, int size, boolean isLeft) {
		int length = 0;
		StringBuilder addStr = new StringBuilder("");
		String tmpStr = "";
		if (source != null) {
			length = source.length();
			tmpStr = source;
		}
		if (!isLeft)
			addStr.append(tmpStr);
		for (int i = 0; i < size - length; i++) {
			addStr.append(sign);
		}
		if (isLeft)
			addStr.append(tmpStr);
		return addStr.toString();
	}

	/**
	 * @todo 查询对称标记符号的位置，startIndex必须是<source.indexOf(beginMarkSign)
	 * @param beginMarkSign
	 * @param endMarkSign
	 * @param source
	 * @param startIndex
	 * @return
	 */
	public static int getSymMarkIndex(String beginMarkSign, String endMarkSign, String source, int startIndex) {
		// 判断对称符号是否相等
		boolean symMarkIsEqual = beginMarkSign.equals(endMarkSign) ? true : false;
		int beginSignIndex = source.indexOf(beginMarkSign, startIndex);
		int endIndex = -1;
		if (beginSignIndex == -1)
			return source.indexOf(endMarkSign, startIndex);
		else
			endIndex = source.indexOf(endMarkSign, beginSignIndex + 1);
		int tmpIndex = 0;
		while (endIndex > beginSignIndex) {
			// 寻找下一个开始符号
			beginSignIndex = source.indexOf(beginMarkSign, (symMarkIsEqual ? endIndex : beginSignIndex) + 1);
			// 找不到或则下一个开始符号位置大于截止符号则返回
			if (beginSignIndex == -1 || beginSignIndex > endIndex)
				return endIndex;
			tmpIndex = endIndex;
			// 开始符号在截止符号前则寻找下一个截止符号
			endIndex = source.indexOf(endMarkSign, (symMarkIsEqual ? beginSignIndex : endIndex) + 1);
			// 找不到则返回
			if (endIndex == -1)
				return tmpIndex;
		}
		return endIndex;
	}

	/**
	 * @todo 查询对称标记符号的位置
	 * @param beginMarkSign
	 * @param endMarkSign
	 * @param source
	 * @param startIndex
	 * @return
	 */
	public static int getSymMarkIndexIgnoreCase(String beginMarkSign, String endMarkSign, String source,
			int startIndex) {
		return getSymMarkIndex(beginMarkSign.toLowerCase(), endMarkSign.toLowerCase(), source.toLowerCase(),
				startIndex);
	}

	/**
	 * @todo 查询对称标记符号的位置
	 * @param beginMarkSign
	 * @param endMarkSign
	 * @param source
	 * @param startIndex
	 * @return
	 */
	public static int getSymMarkMatchIndex(String beginMarkSign, String endMarkSign, String source, int startIndex) {
		// 判断对称符号是否相等
		boolean symMarkIsEqual = beginMarkSign.equals(endMarkSign) ? true : false;
		Pattern startP = Pattern.compile(beginMarkSign);
		Pattern endP = Pattern.compile(endMarkSign);
		int[] beginSignIndex = StringUtil.matchIndex(source, startP, startIndex);
		int[] endIndex = { -1, -1 };
		if (beginSignIndex[0] == -1)
			return StringUtil.matchIndex(source, endP, startIndex)[0];
		else
			endIndex = StringUtil.matchIndex(source, endP, beginSignIndex[1] + 1);
		int[] tmpIndex = { 0, 0 };
		while (endIndex[0] > beginSignIndex[0]) {
			// 寻找下一个开始符号
			beginSignIndex = StringUtil.matchIndex(source, startP,
					(symMarkIsEqual ? endIndex[1] : beginSignIndex[1]) + 1);
			// 找不到或则下一个开始符号位置大于截止符号则返回
			if (beginSignIndex[0] == -1 || beginSignIndex[0] > endIndex[0])
				return endIndex[0];
			tmpIndex = endIndex;
			// 开始符号在截止符号前则寻找下一个截止符号
			endIndex = StringUtil.matchIndex(source, endP, (symMarkIsEqual ? beginSignIndex[1] : endIndex[1]) + 1);
			// 找不到则返回
			if (endIndex[0] == -1)
				return tmpIndex[0];
		}
		return endIndex[0];
	}

	/**
	 * @todo 通过正则表达式判断是否匹配
	 * @param source
	 * @param regex
	 * @return
	 */
	public static boolean matches(String source, String regex) {
		return matches(source, Pattern.compile(regex));
	}

	/**
	 * @todo 通过正则表达式判断是否匹配
	 * @param source
	 * @param p
	 * @return
	 */
	public static boolean matches(String source, Pattern p) {
		return p.matcher(source).find();
	}

	/**
	 * @todo 找到匹配的位置
	 * @param source
	 * @param regex
	 * @return
	 */
	public static int matchIndex(String source, String regex) {
		return matchIndex(source, Pattern.compile(regex));
	}

	public static int[] matchIndex(String source, String regex, int start) {
		return matchIndex(source, Pattern.compile(regex), start);
	}

	public static int matchIndex(String source, Pattern p) {
		Matcher m = p.matcher(source);
		if (m.find())
			return m.start();
		else
			return -1;
	}

	public static int[] matchIndex(String source, Pattern p, int start) {
		if (source.length() <= start)
			return new int[] { -1, -1 };
		Matcher m = p.matcher(source.substring(start));
		if (m.find())
			return new int[] { m.start() + start, m.end() + start };
		else
			return new int[] { -1, -1 };
	}

	public static int matchLastIndex(String source, String regex) {
		return matchLastIndex(source, Pattern.compile(regex));
	}

	public static int matchLastIndex(String source, Pattern p) {
		Matcher m = p.matcher(source);
		int matchIndex = -1;
		while (m.find()) {
			matchIndex = m.start();
		}
		return matchIndex;
	}

	/**
	 * @todo 获取匹配成功的个数
	 * @param source
	 * @param regex
	 * @return
	 */
	public static int matchCnt(String source, String regex) {
		return matchCnt(source, Pattern.compile(regex));
	}

	/**
	 * @todo 获取匹配成功的个数
	 * @param Pattern
	 * @param source
	 * @return
	 */
	public static int matchCnt(String source, Pattern p) {
		Matcher m = p.matcher(source);
		int count = 0;
		while (m.find()) {
			count++;
		}
		return count;
	}

	/**
	 * @todo 获取匹配成功的个数
	 * @param source
	 * @param regex
	 * @param beginIndex
	 * @param endIndex
	 * @return
	 */
	public static int matchCnt(String source, String regex, int beginIndex, int endIndex) {
		return matchCnt(source.substring(beginIndex, endIndex), Pattern.compile(regex));
	}

	/**
	 * @todo 获取字符指定次数的位置
	 * @param source
	 * @param regex
	 * @param order
	 * @return
	 */
	public static int indexOrder(String source, String regex, int order) {
		int begin = 0;
		int count = 0;
		int index = source.indexOf(regex, begin);
		while (index != -1) {
			if (count == order)
				return index;
			else {
				begin = index + 1;
				index = source.indexOf(regex, begin);
			}
			count++;
		}
		return -1;
	}

	/**
	 * @todo 字符串转ASCII
	 * @param str
	 * @return
	 */
	public static int[] str2ASCII(String str) {
		char[] chars = str.toCharArray(); // 把字符中转换为字符数组
		int[] result = new int[chars.length];
		for (int i = 0; i < chars.length; i++) {// 输出结果
			result[i] = (int) chars[i];
		}
		return result;
	}

	/**
	 * @todo 切割字符串，排除特殊字符对，如a,b,c,dd(a,c),dd(a,c)不能切割
	 * @param source
	 * @param splitSign
	 *            如逗号、分号、冒号或具体字符串,非正则表达式
	 * @param filter
	 * @return
	 */
	public static String[] splitExcludeSymMark(String source, String splitSign, HashMap filter) {
		if (source == null)
			return null;
		int splitIndex = source.indexOf(splitSign);
		if (splitIndex == -1)
			return new String[] { source };
		if (filter == null || filter.isEmpty())
			return source.split(splitSign);
		else {
			String[][] filters = new String[filter.size()][2];
			Iterator iter = filter.entrySet().iterator();
			int count = 0;
			String beginSign;
			String endSign;
			int beginSignIndex;
			Map.Entry entry;
			while (iter.hasNext()) {
				entry = (Map.Entry) iter.next();
				beginSign = (String) entry.getKey();
				endSign = (String) entry.getValue();
				beginSignIndex = source.indexOf(beginSign);
				if (beginSignIndex != -1 && source.indexOf(endSign, beginSignIndex + 1) != -1) {
					filters[count][0] = beginSign;
					filters[count][1] = endSign;
					count++;
				}
			}
			// 没有对称符合过滤则直接返回分割结果
			if (count == 0)
				return source.split(splitSign);

			ArrayList splitResults = new ArrayList();
			int preSplitIndex = 0;
			int splitSignLength = splitSign.length();
			int symBeginIndex = 0;
			int symEndIndex = 0;
			int skipIndex = 0;
			int minBegin = -1;
			int minEndIndex = -1;
			int meter = 0;
			while (splitIndex != -1) {
				// 寻找最前的对称符号
				minBegin = -1;
				minEndIndex = -1;
				meter = 0;
				for (int i = 0; i < count; i++) {
					symBeginIndex = source.indexOf(filters[i][0], skipIndex);
					symEndIndex = getSymMarkIndex(filters[i][0], filters[i][1], source, skipIndex);
					if (symBeginIndex != -1 && symEndIndex != -1 && (meter == 0 || (symBeginIndex < minBegin))) {
						minBegin = symBeginIndex;
						minEndIndex = symEndIndex;
						meter++;
					}

				}
				// 在中间
				if (minBegin < splitIndex && minEndIndex > splitIndex) {
					skipIndex = minEndIndex + 1;
					splitIndex = source.indexOf(splitSign, minEndIndex + 1);
				} else {
					// 对称开始符号在分割符号后面或分割符前面没有对称符号或找不到对称符号
					if (minBegin > splitIndex || minBegin == -1) {
						splitResults.add(source.substring(preSplitIndex + (preSplitIndex == 0 ? 0 : splitSignLength),
								splitIndex));
						preSplitIndex = splitIndex;
						skipIndex = preSplitIndex + 1;
						splitIndex = source.indexOf(splitSign, preSplitIndex + 1);
					} // 对称截止符号在分割符前面，向下继续寻找
					else {
						skipIndex = minEndIndex + 1;
					}
				}
				// 找不到下一个分隔符号
				if (splitIndex == -1) {
					splitResults.add(source.substring(preSplitIndex + (preSplitIndex == 0 ? 0 : splitSignLength)));
					break;
				}
			}
			String[] resultStr = new String[splitResults.size()];
			for (int j = 0; j < splitResults.size(); j++)
				resultStr[j] = (String) splitResults.get(j);
			return resultStr;
		}
	}

	/**
	 * @todo 将字符串转换成驼峰形式
	 * @param source
	 * @param firstIsUpperCase
	 * @return
	 */
	public static String toHumpStr(String source, boolean firstIsUpperCase) {
		if (StringUtil.isBlank(source))
			return source;
		// update 2018-3-22 将-复活统一成_
		String[] humpAry = source.trim().replace("-", "_").split("\\_");
		String cell;
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < humpAry.length; i++) {
			cell = humpAry[i];
			// 全大写或全小写
			if (cell.toUpperCase().equals(cell))
				result.append(firstToUpperOtherToLower(cell));
			else
				result.append(firstToUpperCase(cell));
		}
		// 首字母变大写
		if (firstIsUpperCase)
			return firstToUpperCase(result.toString());
		else
			return firstToLowerCase(result.toString());
	}

	/**
	 * @todo 通过特殊符号对字符进行安全模糊化处理
	 * @param value
	 * @param preSize
	 * @param tailSize
	 * @param maskStr
	 * @return
	 */
	public static String secureMask(Object value, int preLength, int tailLength, String maskStr) {
		if (value == null)
			return null;
		String tmp = value.toString();
		if (tmp.length() <= preLength + tailLength)
			return tmp;
		else
			return tmp.substring(0, preLength).concat(maskStr == null ? "***" : maskStr)
					.concat(tmp.substring(tmp.length() - tailLength));
	}

	/**
	 * @todo 判断字符串中是否包含中文
	 * @param str
	 * @return
	 */
	public static boolean hasChinese(String str) {
		if (chinaPattern.matcher(str).find()) {
			return true;
		}
		return false;
	}

	/**
	 * @todo 驼峰形式字符用分割符号链接,example:humpToSplitStr("organInfo","_") result:organ_Info
	 * @param source
	 * @param split
	 * @return
	 */
	public static String humpToSplitStr(String source, String split) {
		if (source == null)
			return null;
		char[] chars = source.trim().toCharArray();
		StringBuilder result = new StringBuilder();
		int charInt;
		int uperCaseCnt = 0;
		for (int i = 0; i < chars.length; i++) {
			charInt = chars[i];
			if (charInt >= 65 && charInt <= 90) {
				uperCaseCnt++;
			} else {
				uperCaseCnt = 0;
			}
			// 连续大写
			if (uperCaseCnt == 1 && i != 0)
				result.append(split);
			result.append(Character.toString(chars[i]));
		}
		return result.toString();
	}

	/**
	 * @todo 填充args参数
	 * @param template
	 * @param args
	 * @return
	 */
	public static String fillArgs(String template, Object... args) {
		if (template == null || (args == null || args.length == 0))
			return template;
		for (Object arg : args) {
			template = template.replaceFirst("\\$?\\{\\s*\\}", arg == null ? "null" : arg.toString());
		}
		return template;
	}
}
