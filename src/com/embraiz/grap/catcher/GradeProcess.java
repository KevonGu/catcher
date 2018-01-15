package com.embraiz.grap.catcher;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;





public class GradeProcess {
	private static Map<Integer,TablePattern> patterns ;
	private static DecimalFormat decimalFormat = new DecimalFormat(",###,###.00");
	public final static int PACKAGE = 0, ITEM_LIST = 1, OTHER = 2;
	private Map<String,Method> metthodCache = new HashMap<>();
	private static Logger logger = Logger.getLogger(GradeProcess.class);
	
	static {
		init();
	}
	

	private static File[] getPatternList(){
		File dir = new File(Configuration.getPatternDir()+File.separator+"grade");
		
		return dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if(!pathname.isFile()){
					return false;
				}
				
				if(!pathname.getName().endsWith(".grade.tablepattern.xml")){
					return false;
				}
				
				return true;
			}
		});
		
	}
	
	//判断方案是否存在
	public boolean isTablePatternExist(int patternId){
		if(patterns.get(patternId) != null){
			return true;
		} else {
			return false;
		}
	}

	private static void load(Map<Integer,TablePattern> patterns,File file) throws Exception{
		logger.info("加载:"+file.getPath());
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		
		//加载xml文档
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse( file );
		
		//获取所有<pattern></pattern>标签
		NodeList elementList = doc.getElementsByTagName("tablepattern");
		
		//循环遍历所有<pattern></pattern>标签
		for(int i=0; i < elementList.getLength(); i++){
			org.w3c.dom.Element tablePatternElement = (org.w3c.dom.Element) elementList.item(i);

			TablePattern pattern = TablePattern.parse(tablePatternElement,file.getPath()+"-Tablepattern["+i+"]");
			
			if(patterns.containsKey(pattern.getId())){
				logger.error("重复定义了TablePattern["+pattern+"]");
			}else{
				logger.info("解析:"+file.getPath()+"-"+"TablePattern["+pattern.getId()+"]");
				patterns.put(pattern.getId(), pattern);
			}
			
		}
	}
	
	
	public static void reload(){
		//每个元素为一个匹配方案 索引为int类型的id
		logger.info("加载.packages.tablepattern.xml文件");
		HashMap<Integer,TablePattern> patterns = new HashMap<Integer,TablePattern>();	
		try {
				
			File[] files = getPatternList();
					
			for (File file : files) {
				load(patterns, file);
			}

			GradeProcess.patterns = patterns;
					
		} catch (Exception e) {
			logger.fatal("加载.packages.tablepattern.xml文件出错-"+e);
			logger.debug("异常详情",e);
		}
	}

	//初始化匹配方案
	private static void init(){
		
		reload();
		
	}
	
	
	/**
	 * 
	 * @param clzz
	 * @param root
	 * @param patternId
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public List<Object> processTable(Class<?> clzz,Element root,int patternId) throws Exception{
		//找不到对应方案编号 直接
		TablePattern tablePattern = patterns.get(patternId);
		if(tablePattern == null) throw new RuntimeException("找不到对应方案编号 id="+patternId);
		
		//获取文档中的表格所有表格元素
		List<Element> elements = root.getElementsByTag("table");
		//映射关系
		List<ColumnMatcher> headerReflect = null;
		
		List<Object> data = new ArrayList<>();
		Table table = null;
		
		for (Element tableElement : elements) {
			
			Table.replaceInnerTableToText(tableElement);
			
			//获取表头的行范围
			int[] headerRow = getHeaderRowRange(tableElement);
			
			//没有获取到 直接跳过
			if(headerRow == null) continue;
			
			//判定表格类型
			if(isGradeTable(headerRow[0], headerRow[1], tableElement, patternId) == false){
				continue;
			}
			
			//转换表格
			table = new Table();
			table.parseTable(tableElement);
			
			//移除表头行 之前的 行
			if(headerRow[0]>0){
				table.removeRows(0, headerRow[0]);
			}
			
			//获取映射关系
			headerReflect = explodeHeaderWithoutIndex(table, tablePattern);
			
			//没有映射关系
			if(headerReflect == null){
				continue;
			}
			
			//移除表头
			table.removeRows(0, headerRow[1]);
			
			//移除只有一个单元格的行
			removeMergeRow(table, 2);
			
			int count = explodeValue(clzz, table, headerReflect, tablePattern,data);
			
			if(count > 0 ) break;
		}

		return data;
	}
	
	/**
	 * 计算表头行范围
	 * @param table
	 * @return int[2] [0]表头开始的行号 表头的行数
	 */
	private int[] getHeaderRowRange(Element table){
		int[] headerRow = new int[]{0,0};
		
		Elements rows = table.getElementsByTag("tr");
		
		//表格不存在行
		if(rows.size()==0){
			return null;
		}
		
		
		Element row = rows.get(0);
		Elements cols = row.getElementsByTag("td");
		
		//首行不存在td元素
		if(cols.size() == 0){
			return null;
		}
		
		for(int i=0;i<rows.size();i++){
			Elements currcols = rows.get(i).getElementsByTag("td");
			if(currcols.size() == 0 || (currcols.size() == 1 && Table.getColspan(currcols.get(0)) != 1)){
				continue;
			}else{
				headerRow[0] = i;
				cols = rows.get(i).getElementsByTag("td");
				break;
			}
			
		}
		
		
		if(cols.size() == 0){
			return null;
		}
		
		//计算最大行合并值 即表头的行数
		int max = 0;
		for(int i = 0; i < cols.size() ;i++){
			int rowSpan = Table.getRowspan(cols.get(i));
			if(max < rowSpan){
				max = rowSpan;
			}
		}
		
		headerRow[1] = max;
		
		return headerRow;
	}
	
	/**
	 * 判断表格是否属于得分表格
	 * @param headerStart 表头开始行
	 * @param len 表头行数
	 * @param table 表格元素
	 * @param patternId 使用的方案
	 * @return
	 */
	private boolean isGradeTable(int headerStart,int len,Element table,int patternId){
		
		String tHeader = "";
		
		for(int i=0; i< len; i++){
			tHeader += table.getElementsByTag("tr").get(headerStart+i).html();
		}
		
		tHeader = tHeader.replaceAll("<.*?>", " ");
		tHeader = tHeader.replaceAll("\\s+", " ");
		
		TablePattern tablePattern = patterns.get(patternId);
		boolean isMatch = false;
		boolean isExcept = false;
		//判断表格是否符合条件
		List<CatchMatcher> tableMatchers = tablePattern.getTableMatchers();
		for (CatchMatcher catchMatcher : tableMatchers) {
			Pattern p = catchMatcher.getReg();
			Matcher m = p.matcher(tHeader);
			if(m.find()){
				isMatch = true;
				break;
			}
		}
		
		//判断表格是否符合剔除条件
		List<CatchMatcher> tableExpect = tablePattern.getTableExpect();
		for (CatchMatcher catchMatcher : tableExpect) {
			Pattern p = catchMatcher.getReg();
			Matcher m = p.matcher(tHeader);
			if(m.find()){
				isExcept = true;
				break;
			}
		}
		
		
		if(isMatch == true && isExcept == false)
			return true;
		else
			return false;
	}
	
	
	/**
	 * 获取字段与列直接的映射关系
	 * @param table
	 * @param tablePattern
	 * @return
	 */
	private List<ColumnMatcher> explodeHeaderWithoutIndex(Table table,TablePattern tablePattern){
		
		if(table == null || table.getColumnCount() == 0) return null;
		
		List<ColumnMatcher> headerReflect = new ArrayList<>();
		
		
		//建立实体字段与列之间的映射
		List<Table.Node> nodes = table
				.getNodes(0, 1, 0, table.getColumnCount());

		Map<String, TableColumn> columns = tablePattern.getColumns();

		Collection<TableColumn> tableColumns = columns.values();

		// 获取所有字段的匹配规则
		for (TableColumn tableColumn : tableColumns) {
			// 如果是索引列 直接跳过
			if (tableColumn.getName().equals("Index"))
				continue;

			// 获取本列的匹配的规则
			List<CatchMatcher> headerMatchers = tableColumn.getHeadMatchers();
			// 遍历
			outer: for (CatchMatcher catchMatcher : headerMatchers) {
				int colspanIndex = 0;
				for (Table.Node node : nodes) {

					String content = node.content;
					String res = catchMatcher.match(content);
					// 匹配到结果
					if (res != null) {
						ColumnMatcher columnMatcher = new ColumnMatcher(
								colspanIndex, node.colspan,
								tableColumn.getName(), catchMatcher);
						headerReflect.add(columnMatcher);
						break outer;
					}

					colspanIndex += node.colspan;
				}

			}

		}
		
		return headerReflect;
	}
	
	
	private void removeMergeRow(Table table,int min){
		int strat = 0; 
		boolean isFirst = true;
		for(int i=0;i<table.getRowCount();i++){
			int rowSize = table.getSizeByRow(i);
			
			//
			if(isFirst == true && rowSize<min){
				strat = i;
				isFirst = false;
			}
			//
			else if(isFirst == false && rowSize>=min){
				int removeCount = i-1 - strat;
				table.removeRows(strat, removeCount);
				i -= removeCount;
				isFirst = true;
			}
			
			
		}
	}
	
	/**
	 * 
	 * @param clazz
	 * @param table
	 * @param headerReflect
	 * @param tablePattern
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws ParseException
	 */
	private int explodeValue(Class<?> clazz ,Table table,List<ColumnMatcher> headerReflect, TablePattern tablePattern,List<Object> data) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ParseException{
		
		//检查参数
		if(table == null || table.getRowCount() < 2 || headerReflect == null){
			return  -1;
		}
		
		int count = 0;
		
		for(int i = 0 ; i < table.getRowCount() ;i++){
			Object entity = clazz.newInstance();
			
			for(ColumnMatcher columnMatcher : headerReflect){
				String columnName = columnMatcher.column;
				String value = "";
				value = table.get(i, columnMatcher.startCol).content;
				
				TableColumn tableColumn = tablePattern.getTableColumn(columnName);
				
				List<CatchMatcher> valueMatchers = tableColumn.getValueMatchers();
				
				CatchMatcher  matchMatcher = null;
				String matchValue = value;
					
				MatcherResult mr = matcher(value, valueMatchers);
				if(mr != null){
					matchValue = mr.content;
					matchMatcher = mr.matcher;
				}else if(valueMatchers.size() != 0){
					continue;
				}
				//给实体赋值
				assignValue(entity, columnName, matchValue, tablePattern, tableColumn, columnMatcher.matcher, matchMatcher);
			}
			
			data.add(entity);
			count++;
		}
		
		
		return count;
	}
	
	/**
	 * 获取setter方法
	 * @param clazz
	 * @param name
	 * @return
	 */
	private Method getSetter(Class<?> clazz , String name){
		Method method = null;
		method = metthodCache.get(name);
		
		if(method != null){
			return method;
		}
		
		
		Method[] methods = clazz.getMethods();
		for (Method m : methods) {
			if(m.getName().equals(name) && m.getParameterTypes().length == 1){
				metthodCache.put(name, m);
				return m;
			}
		}
		
		throw new RuntimeException(clazz.getCanonicalName()+"中找不到"+name+"方法");
	}
	
	/**
	 * 获取getter方法
	 * @param clazz
	 * @param name
	 * @return
	 */
	private Method getGetter(Class<?> clazz , String name){
		try {
			return clazz.getMethod(name);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(clazz.getCanonicalName()+"中找不到"+name+"方法");
		}
	}
	
	/**
	 * 给实体赋值
	 * @param entity
	 * @param column
	 * @param value
	 * @param pattern
	 * @param tableColumn
	 * @param headerMatcher
	 * @param valueMatcher
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws ParseException
	 */
	private void assignValue(Object entity,String column,String value,TablePattern pattern,TableColumn tableColumn,CatchMatcher headerMatcher, CatchMatcher valueMatcher) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ParseException{

		//set方法
		Method setMethod = getSetter(entity.getClass(), "set"+column);
		Method getMethod = getGetter(entity.getClass(), "get"+column);
		
		//方法参数类型
		Class<?> parameter = setMethod.getParameterTypes()[0];
		
		//根据参数类型 将匹配出来的字符串转化成对应类型 并赋值给实体
		if(parameter == String.class){
			//格式化数字处理
			if(tableColumn.isFormatNumber()){
				Double num = Double.parseDouble(value);
				if(headerMatcher.getScale()>0){
					num = num*headerMatcher.getScale();
				}
				else if(valueMatcher != null && valueMatcher.getScale() >= 0){
					num = num*valueMatcher.getScale();
				}
				value = decimalFormat.format(num);
			}
			String old = (String) getMethod.invoke(entity);
			
			//多值叠加处理
			if(old!=null && !old.equals("")){
				if(tableColumn.isFormatNumber()){
					old = old.replace(",", "");
					double numold = Double.parseDouble(old);
					value = value.replace(",", "");
					double numnew = Double.parseDouble(value);
					
					value = decimalFormat.format(numold+numnew);
				}else{
					value = old+","+value;
				}
			}
			
			if(tableColumn.isFormatNumber() == true){
				double v = Double.parseDouble(value.replace(",", ""));
				
				if( tableColumn.getMinNum()!= null){
					if(v < tableColumn.getMinNum()){
						return;
					}
				}
				
				if(tableColumn.getMaxNum()!= null){
					if(v > tableColumn.getMaxNum()){
						return;
					}
				}
			}

			setMethod.invoke(entity, value);
			
		}else if(parameter == Integer.class){
			
			int num = Integer.parseInt(value);
			
			if(headerMatcher.getScale()>0){
				num = (int)(num*(headerMatcher.getScale()));
			}
			
			else if(valueMatcher != null && valueMatcher.getScale()>0){
				num = (int)(num*(valueMatcher.getScale()));
			}
			
			
			
			Integer old = (Integer) getMethod.invoke(entity);
			
			if(old == null){
				old = 0;
			}
			
			if( tableColumn.getMinNum()!= null){
				if(num < tableColumn.getMinNum()){
					return;
				}
			}
			
			if(tableColumn.getMaxNum()!= null){
				if(num> tableColumn.getMaxNum()){
					return;
				}
			}
			
			setMethod.invoke(entity, num+old);
			
		}else if(parameter == Long.class){

			long num = Long.parseLong(value);
			
			if(headerMatcher.getScale()>0){
				num = (long)(num*(headerMatcher.getScale()));
			}
			
			else if(valueMatcher != null && valueMatcher.getScale()>0){
				num = (long)(num*(valueMatcher.getScale()));
			}
			
			Long old = (Long) getMethod.invoke(entity);
			
			if(old == null){
				old = 0L;
			}

			if( tableColumn.getMinNum()!= null){
				if(num < tableColumn.getMinNum()){
					return;
				}
			}
			
			if(tableColumn.getMaxNum()!= null){
				if(num> tableColumn.getMaxNum()){
					return;
				}
			}
			
			setMethod.invoke(entity, num+old);
			
		}else if(parameter == Short.class){

			short num = Short.parseShort(value);
			
			if(headerMatcher.getScale()>0){
				num = (short)(num*(headerMatcher.getScale()));
			}
			
			else if(valueMatcher != null && valueMatcher.getScale()>0){
				num = (short)(num*(valueMatcher.getScale()));
			}
			

			if( tableColumn.getMinNum()!= null){
				if(num < tableColumn.getMinNum()){
					return;
				}
			}
			
			if(tableColumn.getMaxNum()!= null){
				if(num> tableColumn.getMaxNum()){
					return;
				}
			}
			
			Short old = (Short) getMethod.invoke(entity);
			if(old == null){
				old = 0;
			}
			
			setMethod.invoke(entity, num+old);
			
		}else if(parameter == Character.class){
			
			setMethod.invoke(entity, value.charAt(0));
			
		}else if(parameter == Float.class){
			
			float num = Float.parseFloat(value);
			
			if(headerMatcher.getScale()>0){
				num = (float)(num*(headerMatcher.getScale()));
			}
			
			else if(valueMatcher != null && valueMatcher.getScale()>0){
				num = (float)(num*(valueMatcher.getScale()));
			}
			

			if( tableColumn.getMinNum()!= null){
				if(num < tableColumn.getMinNum()){
					return;
				}
			}
			
			if(tableColumn.getMaxNum()!= null){
				if(num> tableColumn.getMaxNum()){
					return;
				}
			}
			
			Float old = (Float) getMethod.invoke(entity);
			
			if(old == null){
				old = 0F;
			}
			
			setMethod.invoke(entity, num+old);
			
			
		}else if(parameter == Double.class){
			
			double num = Double.parseDouble(value);
			if(headerMatcher.getScale()>0){
				num = (double)(num*(headerMatcher.getScale()));
			}
			else if(valueMatcher != null && valueMatcher.getScale()>0){
				num = (double)(num*(valueMatcher.getScale()));
			}
			

			if( tableColumn.getMinNum()!= null){
				if(num < tableColumn.getMinNum()){
					return;
				}
			}
			
			if(tableColumn.getMaxNum()!= null){
				if(num> tableColumn.getMaxNum()){
					return;
				}
			}
			
			Double old = (Double) getMethod.invoke(entity);
			
			if(old == null){
				old = 0d;
			}
			
			setMethod.invoke(entity, num+old);
			
			setMethod.invoke(entity, num);
			
		}else if(parameter == Boolean.class){
			
			if(value.equals("true")){
				setMethod.invoke(entity, true);
			}else if(value.equals("false")){
				setMethod.invoke(entity, false);
			}
			
		}else if(parameter == Date.class){
			String format = valueMatcher.getFormat();
			if(format == null){
				format = "yyyy-MM-dd";
			}
			DateFormat formatter = new SimpleDateFormat( format );
			setMethod.invoke(entity, formatter.parse(value));
		}else{
			
		}
	}
	
	/**
	 * 匹配出字符串
	 * @param content
	 * @param matchers
	 * @return
	 */
	private MatcherResult matcher(String content,List<CatchMatcher> matchers){
		CatchMatcher m = null;
		String mm = null;
		
		//遍历所有匹配规则 对单元格里面的进行处理
		for (CatchMatcher catchMatcher : matchers) {
			
			Matcher matcher = catchMatcher.getReg().matcher(content);
			
			//尝试匹配
			if(matcher.find()){
				mm = matcher.group();
			} else {
				continue;
			}
			
			List<Replace> replaces = catchMatcher.getReplaces();
			
			//遍历替换规则
			for (Replace replace : replaces) {
				mm = mm.replaceAll(replace.getFrom(), replace.getTo());
			}
			
			//检查有效性
			if(mm.length()>=catchMatcher.getMin() && mm.length() <= catchMatcher.getMax()){
				m = catchMatcher;
				break;
			}
		}
		
		if(m != null){
			MatcherResult mr = new MatcherResult();
			mr.content = mm;
			mr.matcher = m;
			return mr;
		}
		
		return null;
	}
	
	
	private class MatcherResult{
		String content;
		CatchMatcher matcher;
	}
	
	private class ColumnMatcher{
		//开始的列号 列数
		int startCol,colCount;
		//对应的字段名称
		String column;
		//匹配的Matcher
		
		CatchMatcher matcher;
		public ColumnMatcher(int startCol, int colCount, String column,
				CatchMatcher matcher) {
			super();
			this.startCol = startCol;
			this.colCount = colCount;
			this.column = column;
			this.matcher = matcher;
		}
		
		

		@Override
		public String toString() {
			return "ColumnMatcher:\nstartCol=" + startCol + "\ncolCount="
					+ colCount + "\ncolumn=" + column;
		}
		
		
	}
	
}
