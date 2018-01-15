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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;




public class TableProcess {
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
	private class MatcherResult{
		String content;
		CatchMatcher matcher;
	}
	private static Map<Integer,TablePattern> patterns ;
	private static DecimalFormat decimalFormat = new DecimalFormat(",###,###.00");
	public final static int PACKAGE = 0, ITEM_LIST = 1, OTHER = 2;
	private static Logger logger = Logger.getLogger(TableProcess.class);
	
	private static Pattern tableTagPattern = Pattern.compile("<\\s*?[Tt][Aa][Bb][Ll][Ee][\\s\\S]*?>");
	

	static {
		init();
	}
	
	/**
	 * 用于获取序号前的提示信息 
	 * 如  ...采购预算：A包 100元 B包100元
	 * getLastString("...采购预算:")
	 * @param text
	 * @param need
	 * @return
	 */
	public static String getLastString(String text,int start,int end,int need,String regx){
		int index = end - 1;
		StringBuilder builder = new StringBuilder();
		int needcounter = 0;
		while(index >= start){
			
			String c = text.charAt(index)+"";
			
			if(c.matches(regx)){
				builder.insert(0, c);
				needcounter++;
			}else{
				if(needcounter>=need){
					return builder.toString();
				}
			}
			index--;
		}
		return builder.toString();
	}

	public static List<String> getPreHeader(String html){
		List<String> list = new LinkedList<>();
		
		int start = 0;
		
		Matcher matcher = tableTagPattern.matcher(html);
		
		while(matcher.find()){
			String content = html.substring(start,matcher.start());
			
			content = FilterFactory.filte(content, "table-preheader-filter");
			String preHead = getLastString(content, 0, content.length(), 4,"[\u4e00-\u9fa5\\(\\)（）:：\\da-zA-Z、：:]");
			list.add(preHead);
		}
		
		return Collections.unmodifiableList(list);
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

			TableProcess.patterns = patterns;
			
					
		} catch (Exception e) {
			logger.fatal("加载.packages.tablepattern.xml文件出错-"+e);
			logger.debug("异常详情",e);
		}
	}

	private static File[] getPatternList(){
		File dir = new File(Configuration.getPatternDir()+File.separator+"table");
		
		return dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if(!pathname.isFile()){
					return false;
				}
				
				if(!pathname.getName().endsWith(".packages.tablepattern.xml")){
					return false;
				}
				
				return true;
			}
		});
		
	}
	
	
	//初始化匹配方案
	private static void init(){
		reload();
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
	
	private List<String> tablePreHeaders = null;
	
	public String getHeaderMatch(int patternId,Table table){
		TablePattern tablePattern = patterns.get(patternId);
		
		List<ColumnMatcher> headers = explodeHeader(table, tablePattern);
		if(headers == null) return "";
		
		String res = "";
		
		for (ColumnMatcher columnMatcher : headers) {
			String headerValue = "";
			String header = "";
			int index = columnMatcher.startCol;
			
			headerValue = table.get(0, index).content;
			header = columnMatcher.column;
			
			res += "["+index+"]"+headerValue+":"+header+"\r\n";
		}
		
		return res;
	}
	
	
	public String getHeaderMatchWithoutIndex(int patternId,Table table){
		TablePattern tablePattern = patterns.get(patternId);
		
		if(table.getSizeByRow(0) == 1){
			table.removeRow(0);
		}
		
		List<ColumnMatcher> headers = explodeHeaderWithoutIndex(table, tablePattern);
		
		String res = "";
		
		for (ColumnMatcher columnMatcher : headers) {
			String headerValue = "";
			String header = "";
			int index = columnMatcher.startCol;
			
			headerValue = table.get(0, index).content;
			header = columnMatcher.column;
			
			res += "["+index+"]"+headerValue+":"+header+"\r\n";
		}
		
		return res;
	}
	
	public String getPackageIndex(int patternId,Table table){
		
		TablePattern tablePattern = patterns.get(patternId);
		
		List<ColumnMatcher> headers = explodeHeader(table, tablePattern);
		if(headers == null || headers.size() == 0) return "";
		
		
		ColumnMatcher columnMatcher = headers.get(0);
		int col = columnMatcher.startCol;
		List<Table.Node> nodes = table.getNodes(col, 1, table.getRowCount()-1);
		
		TableColumn column = tablePattern.getTableColumn("Index");
		List<CatchMatcher> matchers = column.getValueMatchers();
		String order = "";
		for (Table.Node node : nodes) {
			for (int i = 0;i<matchers.size()&&i < columnMatcher.matcher.getLimit(); i++) {
				CatchMatcher catchMatcher = matchers.get(i);
				String res = catchMatcher.match(node.content);
				if(res != null && !res.equals("")){
					order += res+" ";
					break;
				}
			}
		}
		
		return order;
	}
	
	/**
	 * 鉴定表格类型
	 * @param patternId
	 * @param tableElement
	 * @return
	 */
	public int indentifyType(int patternId,Element tableElement){
		
		Table.replaceInnerTableToText(tableElement);
		
		int[] headerRow = getHeaderRowRange(tableElement);
		
		if(headerRow == null){
			return OTHER;
		}
		
		if(isPacketTable(headerRow[0], headerRow[1], tableElement, patternId) == true){
			return PACKAGE;
		}
		
		if(isItemTable(headerRow[0], headerRow[1], tableElement, patternId) == true){
			return ITEM_LIST;
		}

		return OTHER;
	}
	
	//判断方案是否存在
	public boolean isTablePatternExist(int patternId){
		if(patterns.get(patternId) != null){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 
	 * @param entity
	 * @param root
	 * @param patternId
	 * @return
	 * @throws Exception
	 */
	public Object processSingle(Object entity, Element root,int patternId){
		TablePattern tablePattern = patterns.get(patternId);
		if(tablePattern == null) return null;
		
		Class<?> clzz = entity.getClass();
		
		List<Element> elements = root.getElementsByTag("table");
		List<ColumnMatcher> headerReflect = null;
		Table table = null;
		
		for (Element tableElement : elements) {
			Table.replaceInnerTableToText(tableElement);
			//获取表头的行范围
			int[] headerRow = getHeaderRowRange(tableElement);
			
			//没有获取到 直接跳过
			if(headerRow == null) continue;
			
			//判定表格类型
			if(isItemTable(headerRow[0], headerRow[1], tableElement, patternId) == false){
				continue;
			}
			
			table = new Table();
			try {
				table.parseTable(tableElement);
			} catch (Exception e) {
				logger.warn("转化表格失败",e);
			}
			
			//移除表头行 之前的 行
			if(headerRow[0]>0){
				table.removeRows(0, headerRow[0]);
			}
			
			
			if(table.getRowCount()<tablePattern.getMinRow() || table.getRowCount()>tablePattern.getMaxRow()){
				continue;
			}
			
			//获取表头 与 字段之间得映射关系
			headerReflect = explodeHeaderWithoutIndex(table, tablePattern);
			
			//移除 行节点元素少于 表头行节点数得行 即 列之间 出现了公用单元格的行
			removeMergeRow(table, table.getSizeByRow(0));
			
			//移除表头行
			table.removeRows(0, headerRow[1]);
			
			//处理表格内容
			explodeValueWithoutIndex(clzz, table, headerReflect, tablePattern,entity);
			
		}
		
		return entity;
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
	public int processTable(Class<?> clzz,Element root,String html,int patternId,Map<String,Object> data){
		//找不到对应方案编号 直接
		TablePattern tablePattern = patterns.get(patternId);
		if(tablePattern == null) throw new RuntimeException("找不到对应方案编号 id="+patternId);
		
		//获取文档中的表格所有表格元素
		List<Element> elements = root.getElementsByTag("table");
		//映射关系
		List<ColumnMatcher> headerReflect = null;
		Table table = null;
		int countBefore = data.values().size();
		for (int i=0;i<elements.size();i++) {
			Element tableElement = elements.get(i);
			Table.replaceInnerTableToText(tableElement);
			
			
			//获取表头的行范围
			int[] headerRow = getHeaderRowRange(tableElement);
			
			//没有获取到 直接跳过
			if(headerRow == null) continue;
			
			
			//判定表格类型
			if(isPacketTable(headerRow[0], headerRow[1], tableElement, patternId) == true){
				
			}
			//采购表格且表格前面有包组序号
			else if(isItemTable(headerRow[0], headerRow[1], tableElement, patternId) == true){
				
				if(tablePreHeaders == null){
					tablePreHeaders = getPreHeader(html);
				}
				String index = ProjectCatcher.findPackIndex(tablePreHeaders.get(i), tablePattern.getId());
				
				if(StringUtil.isBlank(index) == false){
					Object entity = data.get(index);
					if(entity == null){
						try {
							entity = clzz.newInstance();
							data.put(index, entity);
						} catch (Exception e) {
							logger.error("无法创建实体"+clzz.getName(), e);
							continue;
						}
					}
					
					handleSinglePackTable(clzz, tablePattern, tableElement, headerRow, entity);
				}
				
				continue;
			}else{
				continue;
			}
			
			//转换表格
			table = new Table();
			try {
				table.parseTable(tableElement);
			} catch (Exception e) {
				logger.warn("转化表格失败");
			}
			
			//移除表头行 之前的 行
			if(headerRow[0]>0){
				table.removeRows(0, headerRow[0]);
			}
			
			//获取映射关系
			headerReflect = explodeHeader(table, tablePattern);
			
			//没有映射关系
			if(headerReflect == null){
				continue;
			}
			
			table.removeRows(0, headerRow[1]);
			
			//没有找到索引列
			if(headerReflect.get(0).column.equals("Index") == false){
				continue;
			}
			
			//移除只有一个单元格的行
			removeMergeRow(table, 2);
			int count = explodeValue(clzz, table, headerReflect, tablePattern,data);
			
			//没有从表格中提取到包组信息的情况
			if(count == 0){
				if(isItemTable(headerRow[0], headerRow[1], tableElement, patternId) == true){
					
					if(tablePreHeaders == null){
						tablePreHeaders = getPreHeader(html);
					}
					String index = ProjectCatcher.findPackIndex(tablePreHeaders.get(i), tablePattern.getId());
					
					if(StringUtil.isBlank(index) == false){
						Object entity = data.get(index);
						if(entity == null){
							try {
								entity = clzz.newInstance();
								data.put(index, entity);
							} catch (Exception e) {
								logger.error("无法创建实体"+clzz.getName(), e);
								continue;
							}
						}
						
						handleSinglePackTable(clzz, tablePattern, tableElement, headerRow, entity);
					}
				}
			}
			
		}
		
		int countAfter = data.values().size();
		return countAfter - countBefore;
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
	 * 提取表头信息 建立表格列 与 实体之间的映射关系
	 * @param table 表格的元素类型
	 * @param tablePattern	匹配方案
	 * @return List 如果有找到Index列 [0]为Index列
	 */
	private List<ColumnMatcher> explodeHeader(Table table,TablePattern tablePattern){
		
		if(table == null || table.getColumnCount() == 0) return null;
		
		ColumnMatcher headerReflect = null;

		
		//查找索引列
		TableColumn indexColumn = tablePattern.getTableColumn("Index");
		

		//获取所有Cather
		List<CatchMatcher> indexHeaderMatchers = indexColumn.getHeadMatchers();
		List<Table.Node> nodes = table.getNodes(0, 1, 0, table.getColumnCount());
		
		out:
		for(CatchMatcher catchMatcher : indexHeaderMatchers){

			int colspanIndex = 0;
			//遍历首行所有节点
			for (Table.Node node : nodes) {
				String content = node.content;
				String res = catchMatcher.match(content);
				
				//找到索引列
				if(res != null){
					ColumnMatcher columnMatcher = new ColumnMatcher(colspanIndex,colspanIndex+node.colspan,"Index",catchMatcher);
					headerReflect = columnMatcher;
					break out;
				}
				colspanIndex += node.colspan;
			}
			
			
		}
		

		if(headerReflect == null){
			return null;
		}
		
		List<ColumnMatcher> reflects = explodeHeaderWithoutIndex(table, tablePattern);
		reflects.add(0, headerReflect);
		
		return reflects;
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
	private int explodeValue(Class<?> clazz ,Table table,List<ColumnMatcher> headerReflect, TablePattern tablePattern,Map<String,Object> data) {
		
		//检查参数
		if(table == null || table.getRowCount() < 1 || headerReflect == null){
			return  -1;
		}
		
		int rowCount = table.getRowCount();
		int rowContentCount = 1;
		int count = 0;
		
		for (int i = 0; i <  rowCount; i=i+rowContentCount) {
			
			//获取索引列映射
			ColumnMatcher headerColumn = headerReflect.get(0);
			
			//获取当前行索引列的元素
			Table.Node nodeOrder = table.get(i,headerColumn.startCol);
			
			//获取一个子包内容的行数
			rowContentCount = nodeOrder.rowspan;
			
			//获取索引序号
			MatcherResult indexMatcherResult = matcher(nodeOrder.content, tablePattern.getTableColumn("Index").getValueMatchers(),headerColumn.matcher.getLimit());
			String order = null;
			
			if(indexMatcherResult != null) {
				order = indexMatcherResult.content;
			} else {
				continue;
			}
			
			
			
			//如果data中 没有对应的实体 新建实体
			Object entity = data.get(order);
			if(entity == null){
				try {
					entity = clazz.newInstance();
				} catch (Exception e) {
					logger.error("无法创建实体"+clazz.getName(), e);
					return 0;
				}
				data.put(order, entity);
			}
			
			count++;
			
			
			//循环便利一个子包中的所有行 一个子包可能包含多行表格
				//遍历
			for(int k = 1; k < headerReflect.size(); k++){
				
				//获取当前列的映射关系
				ColumnMatcher columnMatcher = headerReflect.get(k);

				//已经抓取到值了 不需要重复抓取
				if(getValue(entity, columnMatcher.column)!=null) continue;
				
				//获取节点
				List<Table.Node> nodes = table.getNodes(i, rowContentCount,columnMatcher.startCol,columnMatcher.colCount);
				
				for (Table.Node node : nodes) {
					//内容
					String content = node.content;
					//header匹配
					CatchMatcher headerMatcher = columnMatcher.matcher;
					//获取匹配列
					TableColumn column = tablePattern.getTableColumn(columnMatcher.column);
					//值匹配
					List<CatchMatcher> matchers = column.getValueMatchers();
					
					//匹配到值matcher
					CatchMatcher matchMatcher = null;
					//单元格内容
					String matchValue = content;
					
					
					MatcherResult mr = matcher(content, matchers,columnMatcher.matcher.getLimit());
					if(mr != null){
						matchValue = mr.content;
						matchMatcher = mr.matcher;
					}else if(matchers.size() != 0){
						//valueMatcher有Matcher 但是没有匹配到内容 直接跳过
						continue;
					}
					
					//给实体赋值
					try {
						assignValue(entity, column.getName(), matchValue, tablePattern, column, headerMatcher, matchMatcher);
					} catch (Exception e) {
						logger.error("无法给实体"+clazz.getName()+"赋值", e);
					}
					
				}
				
			}

			
			
			
			List<Table.Node> rowsNodes = table.getNodes(i, rowContentCount, 0, table.getColumnCount());
			
			//获取行文本
			String rowText = Table.nodesToText(rowsNodes);
			
			//获取需要匹配行的列
			List<TableColumn> targetColumns = tablePattern.getTargetColumns();
			
			for (TableColumn tableColumn : targetColumns) {
				List<CatchMatcher> valueMatchers = tableColumn.getValueMatchers();
				MatcherResult mr = matcher(rowText, valueMatchers,headerColumn.matcher.getLimit());
				
				if(mr != null){
					try {
						assignValue(entity, tableColumn.getName(), mr.content, tablePattern, tableColumn, new CatchMatcher(), mr.matcher);
					} catch (Exception e) {
						logger.error("无法给实体"+clazz.getName()+"赋值", e);
					}
				}else{
					continue;
				}
				
			}
			
			}
			return count;
	}
	
	/**
	 * 
	 * @param clazz
	 * @param table
	 * @param headerReflect
	 * @param tablePattern
	 * @param data
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws ParseException
	 */
	private int explodeValueWithoutIndex(Class<?> clazz ,Table table,List<ColumnMatcher> headerReflect, TablePattern tablePattern,Object entity){

		if(table == null || headerReflect == null){
			return  -1;
		}
		
		int rowCount = table.getRowCount();
		int count = 1;
		
		//循环便利一个子包中的所有行 一个子包可能包含多行表格
		//遍历一行中的所有列
		for(ColumnMatcher columnMatcher : headerReflect){
			//获取当前列的映射关系
			//不存在映射关系 直接跳过本列
			if(columnMatcher == null) continue;
			//已经抓取到值了 不需要重复抓取
			if(getValue(entity, columnMatcher.column)!=null) continue;
			List<Table.Node> nodes = table.getNodes(0,rowCount,columnMatcher.startCol,columnMatcher.colCount);
			for (Table.Node node : nodes) {
				//内容
				String content = node.content;
				//header匹配
				CatchMatcher headerMatcher = columnMatcher.matcher;
				//获取匹配列
				TableColumn column = tablePattern.getTableColumn(columnMatcher.column);
				//值匹配
				List<CatchMatcher> matchers = column.getValueMatchers();
				
				//匹配到值matcher
				CatchMatcher  matchMatcher = null;
				String matchValue = content;
					
				MatcherResult mr = matcher(content, matchers,columnMatcher.matcher.getLimit());
				if(mr != null){
					matchValue = mr.content;
					matchMatcher = mr.matcher;
				}else if(matchers.size() != 0){
					continue;
				}
				
				//给实体赋值
				try {
					assignValue(entity, column.getName(), matchValue, tablePattern, column, headerMatcher, matchMatcher);
				} catch (Exception e) {
					logger.error("无法给实体"+clazz.getName()+"赋值", e);
				}
					
			}
				
			}
			return count;
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
		
		//首行只有一个表格单元 且这个单元存在列合并
		if(cols.size() == 1 && Table.getColspan(cols.get(0)) != 1){
			//如果表格只有一行 直接返回空值
			if(rows.size() < 2){
				return null;
			}else{
				//取次行作为表头开始行
				row = rows.get(1);
				cols = row.getElementsByTag("td");
				headerRow[0] = 1;
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
	 * 获取setter方法
	 * @param clazz
	 * @param name
	 * @return
	 */
	private Method getSetter(Class<?> clazz , String name){
		Method[] methods = clazz.getMethods();
		
		for (Method m : methods) {
			if(m.getName().equals(name) && m.getParameterTypes().length == 1){
				return m;
			}
		}
		
		throw new RuntimeException(clazz.getCanonicalName()+"中找不到"+name+"方法");
	}
	
	private Object getValue(Object entity,String columnName){
		Method method = getGetter(entity.getClass(), "get"+columnName);
		try {
			return method.invoke(entity);
		} catch (Exception e) {
			return null;
		}
	}
	
	private int handleSinglePackTable(Class<?> clzz,TablePattern tablePattern, Element tableElement,int headerRow[],Object entity){
		Table table = new Table();
		List<ColumnMatcher> headerReflect = null;
		
		try {
			table.parseTable(tableElement);
		} catch (Exception e) {
			logger.warn("转化表格失败",e);
			return 0;
		}
		
		//移除表头行 之前的 行
		if(headerRow[0]>0){
			table.removeRows(0, headerRow[0]);
		}
		
		//获取表头 与 字段之间得映射关系
		headerReflect = explodeHeaderWithoutIndex(table, tablePattern);
		
		//移除 行节点元素少于 表头行节点数得行 即 列之间 出现了公用单元格的行
		removeMergeRow(table, table.getSizeByRow(0));
		
		//移除表头行
		table.removeRows(0, headerRow[1]);
		
		//处理表格内容
		explodeValueWithoutIndex(clzz, table, headerReflect, tablePattern,entity);
		
		return 1;
	}
	
	/**
	 * 判断是否为采购表格
	 * @param headerStart 表头开始行
	 * @param len 表头行数
	 * @param table 表格元素
	 * @param patternId 方案编号
	 * @return
	 */
	private boolean isItemTable(int headerStart,int len,Element table,int patternId){

		String tHeader = "";
		
		for(int i=0; i< len; i++){
			tHeader += table.getElementsByTag("tr").get(headerStart+i).html();
		}
		
		tHeader = FilterFactory.filte(tHeader, "table-header-filter");
		
		TablePattern tablePattern = patterns.get(patternId);
		boolean isMatch = false;
		boolean isExcept = false;

		List<CatchMatcher> tableMatchers = tablePattern.getSingleMatcher();
		for (CatchMatcher catchMatcher : tableMatchers) {
			Pattern p = catchMatcher.getReg();
			Matcher m = p.matcher(tHeader);
			if(m.find()){
				isMatch = true;
				break;
			}
		}
		
		List<CatchMatcher> matchers = tablePattern.getSingleExpect();
		for (CatchMatcher catchMatcher : matchers) {
			Pattern p = catchMatcher.getReg();
			Matcher m = p.matcher(tHeader);
			if(m.find()){
				isExcept = true;
				break;
			}
		}
		
		if(isMatch == true && isExcept == false){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 判断表格是否属于包组表格
	 * @param headerStart 表头开始行
	 * @param len 表头行数
	 * @param table 表格元素
	 * @param patternId 使用的方案
	 * @return
	 */
	private boolean isPacketTable(int headerStart,int len,Element table,int patternId){
		
		String tHeader = "";
		
		for(int i=0; i< len; i++){
			tHeader += table.getElementsByTag("tr").get(headerStart+i).html();
		}
		
		tHeader = FilterFactory.filte(tHeader, "table-header-filter");
		
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
	 * 匹配出字符串
	 * @param content
	 * @param matchers
	 * @return
	 */
	private MatcherResult matcher(String content,List<CatchMatcher> matchers,int limit){
		CatchMatcher m = null;
		String mm = null;
		
		//遍历所有匹配规则 对单元格里面的进行处理
		for ( int i = 0 ;i < matchers.size() && i<limit;i++) {
			CatchMatcher catchMatcher = matchers.get(i);
			mm = catchMatcher.match(content);
			
			if(mm != null){
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
	
	/**
	 * 移除存在列合并的行
	 * @param table
	 * @param min
	 */
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
		
		if(isFirst == false){
			table.removeRows(strat, table.getRowCount() - strat);
		}
	}
	
}
