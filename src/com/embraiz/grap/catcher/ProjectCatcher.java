package com.embraiz.grap.catcher;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;
import org.jsoup.helper.StringUtil;



/**
 * 
 * @author Administrator
 *
 */
public class ProjectCatcher extends Catcher{
	protected static Map<Integer,CatchPattern> patterns ;
	private static Logger logger = Logger.getLogger(ProjectCatcher.class);
	static {
		init();
	}
	
	//初始化匹配方案
	private static void init(){
		StringBuilder console = new StringBuilder();
		reload();
		System.out.println(console.toString());
	}
	
	private static File[] getPatternList(){
		File dir = new File(Configuration.getPatternDir()+File.separator+"project");
		
		return dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if(!pathname.isFile()){
					return false;
				}
				
				if(!pathname.getName().endsWith(".project.pattern.xml")){
					return false;
				}
				
				return true;
			}
		});
		
	}
	
	//重新加载方案
	public static void reload(){
		
		//每个元素为一个匹配方案 索引为int类型的id
				logger.info("加载project.pattern.xml文件");
				HashMap<Integer,CatchPattern> patterns = new HashMap<>();
				try {
					
					File[] list = getPatternList();
					
					for (File file : list) {
						load(patterns, file);
					}
					
					ProjectCatcher.patterns = patterns;
				} catch (Exception e) {
					logger.fatal("加载project.pattern.xml文件出错-"+e);
					logger.debug("异常详情", e);
				}
	}
	
	/**
	 * 从文本中查找包组序号
	 * @param content 输入文本
	 * @param patternId 方案编号
	 * @return
	 */
	public static String findPackIndex(String content,int patternId){
		CatchPattern pattern = patterns.get(patternId);
		if (pattern == null) {
			logger.warn("Pattern["+patternId+"]不存在,无法查找包组序号");
			return "";
		}
		
		Column index = pattern.getColumn("Index");
		if(index == null){
			logger.warn("Pattern["+patternId+"]-Column[Index]-不存在,无法查找包组序号");
			return "";
		}
		
		String defaultFilter = index.getFilters();
		if(StringUtil.isBlank(defaultFilter)){
			defaultFilter = pattern.getFilters();
		}
		
		List<CatchMatcher> catchMatchers = index.getRegulars();
		String res = null;
		
		for (CatchMatcher catchMatcher : catchMatchers) {
			String filter = catchMatcher.getFilter();
			if(filter != null){
				res = catchMatcher.match(content, filter);
			}else if(defaultFilter != null){
				res = catchMatcher.match(content,defaultFilter);
			}else{
				res = catchMatcher.match(content);
			}
			
			if(StringUtil.isBlank(res) == false){
				return res;
			}
		}
		
		return "";
	}
	
	public Object catchColum(Object data,int patternId,String content){
		HashMap<String,String> cache = new HashMap<>();
		
		return catchColum(data, patternId, content, cache);
	}
	
	private Object catchColum(Object data,int patternId,String content,Map<String,String> cache){
		
		//获取匹配配方案
		CatchPattern pattern = patterns.get(patternId);
				
		//匹配方案不存在 直接返回
		if(pattern == null){
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

			// try to get column
			// 尝试获取传入对象 对应字段的值
			Method getMethod = null;
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
					catchContent = cache.get(filt);

					if (catchContent == null) {
						catchContent = FilterFactory.filte(content, filt);
						cache.put(filt, catchContent);
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
				boolean res = assignValue(data, value, method,getMethod, column, catchMatcher);
				
				if(res == true && column.isMutiple() == false){
					break;
				}
			}

		}
		return data;
	}
	
	public Object catchColum(Object data,int patternId[],String content){
		HashMap<String,String> cache = new HashMap<>();
		for (int i : patternId) {
			catchColum(data, i, content,cache);
		}
		return data;
	}
	
	/**
	 * 抓取包组
	 * @param clazz
	 * @param patternId
	 * @param content
	 * @return
	 */
	public int catchData(Class<?> clazz, int patternId, String content,Map<String,Object> data){
		
		CatchPattern pattern = patterns.get(patternId);
		
		if(pattern == null){
			return 0;
		}
		
		
		Column column = pattern.getColumn("Index");
		
		if(column == null){
			logger.error("Pattern["+patternId+"]不存在Column[Index]");
			return 0;
		}
		
		String filt = column.getFilters();
		if(filt == null) filt = pattern.getFilters();
		if(filt != null){
			content = FilterFactory.filte(content, filt);
		}
		
		int count = 0;
		
		
		
		//按照包组序号 对文本进行初步分割
		Map<String, String> splitContent = split(content, column.getRegulars());
		
		if(splitContent == null){
			return 0;
		}
		
		//对每个包组的文本进行匹配
		for (Map.Entry<String, String> ent : splitContent.entrySet()) {
			
			String packContent = ent.getValue();
			
			String afterfilter = column.getAfterfilters();
			if(afterfilter != null && !afterfilter.equals("")){
				
				packContent = FilterFactory.filte(packContent, afterfilter);
				
			}
			
			try {
				Object entity = null;
				entity = data.get(ent.getKey());
				
				if(entity == null){
					entity = clazz.newInstance();
					data.put(ent.getKey(), entity);
				}
				count++;
				catchColum(entity, patternId, packContent);
			} catch (Exception e) {
				e.printStackTrace();
				return count;
			}
			
		}
		
		return count;
	}
	
	public int catchData(Class<?> clazz, int[] patternId, String content,Map<String,Object> data){
		if(patternId.length == 0){
			return 0;
		}
		
		CatchPattern pattern = patterns.get(patternId[patternId.length-1]);
		
		if(pattern == null){
			return 0;
		}
		
		
		Column column = pattern.getColumn("Index");
		
		if(column == null){
			logger.error("Pattern["+patternId+"]不存在Column[Index]");
			return 0;
		}
		
		String filt = column.getFilters();
		if(filt == null) filt = pattern.getFilters();
		if(filt != null){
			content = FilterFactory.filte(content, filt);
		}
		
		int count = 0;
		
		
		
		//按照包组序号 对文本进行初步分割
		Map<String, String> splitContent = split(content, column.getRegulars());
		
		if(splitContent == null){
			return 0;
		}
		
		//对每个包组的文本进行匹配
		for (Map.Entry<String, String> ent : splitContent.entrySet()) {
			
			String packContent = ent.getValue();
			
			String afterfilter = column.getAfterfilters();
			if(afterfilter != null && !afterfilter.equals("")){
				
				packContent = FilterFactory.filte(packContent, afterfilter);
				
			}
			
			try {
				Object entity = null;
				entity = data.get(ent.getKey());
				
				if(entity == null){
					entity = clazz.newInstance();
					data.put(ent.getKey(), entity);
				}
				count++;
				catchColum(entity, patternId, packContent);
			} catch (Exception e) {
				e.printStackTrace();
				return count;
			}
			
		}
		
		return count;
	}
	
	/**
	 * 
	 * @param content
	 * @param pattern
	 * @return
	 */
	private Map<String,String> split(String content,List<CatchMatcher> matchers){
		//存储每个包组的文本
		Map<String, String> resMap = new HashMap<>();
		//用剔除重复出现的包组序号
		Map<String,String> data = new HashMap<>();
		//所有包含包组信息的文本块
		ArrayList<Index> indexList = new ArrayList<>();
		//包组序号列表
		ArrayList<String> indexs = new ArrayList<>();
		
		for (CatchMatcher matcher : matchers) {
			
			
			// 匹配出序号
			Matcher m = matcher.getReg().matcher(content);
			
			// 遍历多有匹配到的序号
			while (m.find()) {
				String key = null;
				key = m.group();

				// 对序号进行替换用于排序
				key = matcher.replaceAll(key);

				if (!data.containsKey(key)) {
					indexs.add(key);
					data.put(key, "");
				}

				// 存储 文本块
				Index in = new Index();
				in.index = key;
				in.start = m.start();
				in.end = m.end();
				indexList.add(in);

			}
		}
		
		if(indexList.size() == 0){
			return null;
		}

		
		Collections.sort(indexList, new Comparator<Index>() {

			@Override
			public int compare(Index o1, Index o2) {
				if(o1.start < o2.start)
					return -1;
				if(o1.start > o2.start)
					return 1;
				else
					return 0;
			}
			
		});
		
		//移除重复统计的序号
		for(int i=0;i<indexList.size()-1;i++){
			Index curr = indexList.get(i);
			Index next = indexList.get(i+1);
			if(curr.end >= next.start){
				if(curr.index.equals(next.index)){
					indexList.remove(i+1);
					i--;
				}else{
					next.start = curr.end+1;
				}
				
			}
		}

		boolean newrow = true;
		Index[] row = new Index[indexs.size()];
		int indexSort = 0;
		int allIndex = 0;
		String header = "";
		ArrayList<String> skipList = new ArrayList<>();
		
		//对序号进行排序
		Collections.sort(indexs, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				if(o1.length() < o2.length())
					return -1;
				else if(o1.length() > o2.length()){
					return 1;
				}else
					return o1.compareToIgnoreCase(o2);
			}

			
			
		});
		
		
		for (String string : indexs) {
			resMap.put(string, "");
		}
		
		//遍历全部包组序号
		for (Index ind : indexList) {
			
			//如果序号不递增 新建一行
			if(newrow == false && indexs.indexOf(row[indexSort-1].index) >= indexs.indexOf(ind.index)){
				newrow = true;
			}
			
			//新建一行
			if(newrow == true){
				
				indexSort = 0;
				
				if(allIndex == 0){//首行
					header = content.substring(0, ind.start);
				}else{//非首行
					header = content.substring(indexList.get(allIndex-1).end,ind.start);
				}
				
				header = getLastString(header, 4);
				newrow = false;
				
			}
			
			row[indexSort] = ind;
			
			boolean needSkip = false;
			String cont = "";
			if(allIndex == (indexList.size()-1) ){//尾行最后一个
				cont = content.substring(ind.end, content.length());
			}else{
				if(ind.end>indexList.get(allIndex+1).start){//与下一个序号重叠
					needSkip = true;
				}else{
					
					cont = content.substring(ind.end, indexList.get(allIndex+1).start);
					cont = cont.trim();
					
					//处理 包组一、二 这种情况 
					char firstChar = cont.charAt(0);
					while(firstChar == '、'){
						int len = findOrder(cont, 1, 1);
						if(len > 0){
							String order = cont.substring(1, 1+len);
							order = parseOrder(order);
							String text = resMap.get(order);
							if(text == null){
								resMap.put(order, "");
							}
							skipList.add(order);
							cont = cont.substring(1+len).trim();
						}else{
							break;
						}
					}
				}
				
			}
			
			cont = cont.trim();
			if(cont.length() < 2){
				needSkip=true;
			}
			
			if(needSkip){
				skipList.add(ind.index);
			}else{
				
				String text = resMap.get(ind.index);
				text += header+" "+cont+" ";
				resMap.put(ind.index, text);
				
				for (String iii : skipList) {
					text = resMap.get(iii);
					text += header+" "+cont+" ";
					resMap.put(iii, text);
				}
				
				skipList.clear();
			}
			
			indexSort++;
			allIndex++;
			
		}
		
		return resMap;
	}
	
	public Map<String,String> getPackContent(Class<?> clazz, int patternId, String content){
		
		CatchPattern pattern = patterns.get(patternId);
		
		if(pattern == null){
			return new HashMap<>();
		}
		
		Column column = pattern.getColumn("Index");
		
		String filt = column.getFilters();
		if(filt == null) filt = pattern.getFilters();
		if(filt != null){
			content = FilterFactory.filte(content, filt);
		}
		//按照包组序号 对文本进行初步分割
		Map<String, String> splitContent = split(content, column.getRegulars());
		
		if(splitContent == null){
			return new HashMap<>();
		}
		
		return splitContent;
	}
	
	/**
	 * 用于获取序号前的提示信息 
	 * 如  ...采购预算：A包 100元 B包100元
	 * getLastString("...采购预算:")
	 * @param text
	 * @param need
	 * @return
	 */
	public static String getLastString(String text,int need){
		int index = text.length() - 1;
		StringBuilder builder = new StringBuilder();
		int needcounter = 0;
		while(index >= 0){
			
			String c = text.charAt(index)+"";
			if(c.matches("[\u4e00-\u9fa5\\(\\)（）:：]"))
			builder.insert(0, c);
			
			if(c.matches("[\u4e00-\u9fa5\\(\\)（）]")){
				needcounter++;
			}else{
				if(needcounter>=4 && c.matches("\\s")){
					return builder.toString();
				}
			}
			index--;
		}
		return builder.toString();
	}
	
	/**
	 * 找出序号
	 * @param content 文本
	 * @param begin 开始查找
	 * @param or 查找方向 1 从左到右 -1 从右到左
	 * @return
	 */
	public int findOrder(String content,int begin, int or){
		if(or == 0){
			return -1;
		}
		int curr = begin;
		for(int i = 0 ; curr >= 0 && curr < content.length(); i += or){
			curr = begin + i;
			if(isOrderChar(content.charAt(curr))){
				continue;
			}else{
				return i;
			}
		}
		
		return -1;
	}
	
	/**
	 * 判断是否为序号字符
	 * @param c
	 * @return
	 */
	private boolean isOrderChar(char c){
		String cha = c+"";
		if(cha.matches("[\\da-zA-Z一二三四五六七八九十]")){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 
	 * @param order
	 * @return
	 */
	private String parseOrder(String order){
		if(order.matches("[一二三四五六七八九十]+")){
			return RegxUtil.numberToLower(order);
		}else{
			return order;
		}
	}
	
	
}

class Item implements Comparable<Item>{

	public String value;
	
	
	
	public Item(String value) {
		super();
		this.value = value;
	}

	@Override
	public int compareTo(Item o) {
		if(o == null){
			return 1;
		}
		
		if(o.value.length() > value.length()){
			return -1;
		}
		
		if(o.value.length() < value.length()){
			return 1;
		}
		
		return value.compareTo(o.value);
	}
	
}

class Index{
	String index;
	int start;
	int end;
	@Override
	public String toString() {
		return index+"<"+start+"-"+end+">";
	}
}