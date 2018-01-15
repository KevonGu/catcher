package com.embraiz.grap.catcher;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * 
 * @author Administrator
 * 
 */
public class ContentCatcher extends Catcher {
	private static Map<Integer, CatchPattern> patterns;
	private HashMap<String, String> filteContent = new HashMap<>();
	private static Logger logger = Logger.getLogger(ContentCatcher.class);

	static {
		init();
	}

	// 初始化匹配方案
	private static void init() {
		reload();
	}
	

	private static File[] getPatternList() {
		File dir = new File(Configuration.getPatternDir()+File.separator+"content");

		return dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if (!pathname.isFile()) {
					return false;
				}

				if (!pathname.getName().endsWith(".content.pattern.xml")) {
					return false;
				}

				return true;
			}
		});

	}

	// 重新加载方案
	public static void reload() {
		// 每个元素为一个匹配方案 索引为int类型的id
		logger.info("加载.content.pattern.xml文件");
		HashMap<Integer, CatchPattern> patterns = new HashMap<>();
		try {

			File[] list = getPatternList();

			for (File file : list) {
				load(patterns, file);
			}

			ContentCatcher.patterns = patterns;

		} catch (Exception e) {
			logger.fatal("加载.content.pattern.xml文件出错-" + e);
			logger.debug("异常详情", e);
		}
	}

	public Object catchColum(Object data, int patternId, String content) {

		// 获取匹配配方案
		CatchPattern pattern = patterns.get(patternId);

		// 匹配方案不存在 直接返回
		if (pattern == null) {
			logger.error("Pattern["+patternId+"]不存在");
			return data;
		}

		// 获取传入对象的所有方法
		Class<?> clazz = data.getClass();
		Method[] methods = clazz.getMethods();

		// 循环遍历所有方法
		for (Method method : methods) {

			String methodName = method.getName();

			// 如果方法名称不是以set开头直接跳过
			if (!methodName.startsWith("set")) {
				continue;
			}

			// 根据方法名获取 对应的字段名 例如 方法名 setName 则 字段名为 Name
			String colnumName = methodName.substring(3, methodName.length());

			// 在方案中找到对应的字段
			Column column = pattern.getColumn(colnumName);

			// column not declare
			// 字段不存在 说明没有自定匹配规则 直接跳过这个字段
			if (column == null) {
				continue;
			}
			// 对应set方法的参数类型
			Class<?>[] paras = method.getParameterTypes();

			// set方法参数个数应该都是只有1个
			if (paras.length > 1) {
				continue;
			}

			Object v = null;
			Method getMethod = null;
			// try to get column
			// 尝试获取传入对象 对应字段的值
			try {
				getMethod = clazz.getMethod("get" + colnumName);
				v = getMethod.invoke(data);
			} catch (NoSuchMethodException e) {
				System.err.println("fail to invoke " + clazz.getName() + ".get"
						+ colnumName + ", not such method");
				System.err.println("尝试调用 " + clazz.getName() + ".get"
						+ colnumName + "失败, 类中没有这个方法");
				e.printStackTrace();
			} catch (SecurityException e) {
				System.err.println("fail to invoke " + clazz.getName() + ".get"
						+ colnumName + ", permission dney");
				System.err.println("尝试调用 " + clazz.getName() + ".get"
						+ colnumName + "失败, 权限不足 请检查该方法的权限");
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}

			// value is exist
			// 值不等于null 说明已经成功抓取 不需要再次抓取 直接跳过这个字段
			if (v != null && !v.equals("")) {
				continue;
			}

			// value is not exist,get pattern list
			// 字段的值不存在 获取自定义的正则表达式
			List<CatchMatcher> matchers = column.getRegulars();

			// match
			// 循环遍历 使用自定义正则表达式 去匹配出字段
			for (CatchMatcher catchMatcher : matchers) {

				// 匹配前过滤
				String filt = catchMatcher.getFilter();
				if (filt == null)
					filt = column.getFilters();
				if (filt == null)
					filt = pattern.getFilters();

				// 匹配后过滤器
				String afterFilter = column.getAfterfilters();

				// 待匹配的文本
				String catchContent = null;

				// 最终匹配到的值
				String value = null;

				// 对文本进行过滤
				if (filt != null) {
					catchContent = filteContent.get(filt);

					if (catchContent == null) {
						catchContent = FilterFactory.filte(content, filt);
						filteContent.put(filt, catchContent);
					}

				} else {
					catchContent = content;
				}

				// 根据是否需要进行匹配后过滤 选择不同参数的方法
				if (afterFilter != null) {
					value = catchMatcher.match(catchContent, afterFilter);
				} else {
					value = catchMatcher.match(catchContent);
				}

				// 没有匹配到合适的内容
				if (value == null)
					continue;

				// 给实体赋值
				boolean res = assignValue(data, value, method, getMethod,
						column, catchMatcher);

				if (res == true && column.isMutiple() == false) {
					break;
				}
			}

		}
		return data;
	}

}
