package com.embraiz.grap.catcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.helper.StringUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class TablePattern {
	private int id;
	private Map<String,TableColumn> columns;
	private List<CatchMatcher> tableMatchers;
	private List<CatchMatcher> singleMatcher;
	private List<CatchMatcher> tableExpect;
	private List<CatchMatcher> singleExpect;
	private List<TableColumn> targetColumns;
	private int maxRow = Integer.MAX_VALUE;
	private int minRow = 0;
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public List<TableColumn> getTargetColumns() {
		return targetColumns;
	}

	public void setTargetColumns(List<TableColumn> targetColumns) {
		this.targetColumns = targetColumns;
	}

	public Map<String, TableColumn> getColumns() {
		return columns;
	}
	
	public void setColumns(Map<String, TableColumn> columns) {
		this.columns = columns;
	}
	
	public List<CatchMatcher> getTableMatchers() {
		return tableMatchers;
	}
	
	public void setTableMatchers(List<CatchMatcher> tableMatchers) {
		this.tableMatchers = tableMatchers;
	}
	
	public List<CatchMatcher> getSingleMatcher() {
		return singleMatcher;
	}

	public void setSingleMatcher(List<CatchMatcher> singleMatcher) {
		this.singleMatcher = singleMatcher;
	}
	
	

	public int getMaxRow() {
		return maxRow;
	}

	public void setMaxRow(int maxRow) {
		this.maxRow = maxRow;
	}

	public int getMinRow() {
		return minRow;
	}

	public void setMinRow(int minRow) {
		this.minRow = minRow;
	}

	public TableColumn getTableColumn(String name){
		return columns.get(name);
	}

	public List<CatchMatcher> getTableExpect() {
		return tableExpect;
	}

	public void setTableExpect(List<CatchMatcher> tableExpect) {
		this.tableExpect = tableExpect;
	}

	public List<CatchMatcher> getSingleExpect() {
		return singleExpect;
	}

	public void setSingleExpect(List<CatchMatcher> singleExpect) {
		this.singleExpect = singleExpect;
	}
	
	public static TablePattern parse(Element element,String prefix){
		if(element == null){
			throw new NullPointerException(prefix+"-element不能为null");
		}
		TablePattern pattern = new TablePattern();
		
		String id = element.getAttribute("id");
		String maxRow = element.getAttribute("maxrow");
		String minRow = element.getAttribute("minrow");
		
		if(StringUtil.isBlank(id)){
			throw new NullPointerException(prefix+"-没有声明属性id的值。");
		}
		
		if(id.matches("\\d+") == false){
			throw new RuntimeException(prefix+"-属性id值"+id+"不合法，要求为一个整数。");
		}
		
		pattern.setId(Integer.parseInt(id));
		
		if(StringUtil.isBlank(minRow) == false){
			if(maxRow.matches("\\d+")){
				pattern.setMinRow(Integer.parseInt(minRow));
			}else{
				throw new RuntimeException(prefix+"-属性minrow值"+minRow+"不合法，要求为一个整数。");
			}
		}
		
		if(StringUtil.isBlank(maxRow) == false){
			if(maxRow.matches("\\d+")){
				pattern.setMaxRow(Integer.parseInt(maxRow));
			}else{
				throw new RuntimeException(prefix+"-属性maxrow值"+minRow+"不合法，要求为一个整数。");
			}
		}
		
		NodeList list = element.getElementsByTagName("tablecolumn");
		Map<String,TableColumn> columns = new HashMap<String, TableColumn>();
		List<TableColumn> targetColumn = new ArrayList<>();
		
		for(int i = 0; i < list.getLength() ;i ++){
			Element columnElement = (Element) list.item(i);
			TableColumn column = TableColumn.parse(columnElement,prefix+"-TableColumn["+i+"]");
			if(column.getTarget() == null || !column.getTarget().equals("row")){
				columns.put(column.getName(), column);
			}else{
				targetColumn.add(column);
			}
			
		}
		
		pattern.setColumns(columns);
		pattern.setTargetColumns(targetColumn);

		List<CatchMatcher> tableMatchers = listElement("tablematchers", "matcher", element,prefix+"-tablematchers");
		List<CatchMatcher> singleMatchers = listElement("singlematchers", "matcher", element,prefix+"-singlematchers");
		List<CatchMatcher> tableexpect = listElement("tableexpect", "matcher", element,prefix+"-tableexpect");
		List<CatchMatcher> singleexpect = listElement("singleexpect", "matcher", element,prefix+"-singleexpect");
		
		pattern.setTableMatchers(tableMatchers);
		pattern.setTableExpect(tableexpect);
		pattern.setSingleMatcher(singleMatchers);
		pattern.setSingleExpect(singleexpect);
		
		return pattern;
	}
	
	public static List<CatchMatcher> listElement(String warpper,String tag,Element element,String prefix){
		List<CatchMatcher> matchers = new ArrayList<>();
		NodeList list = element.getElementsByTagName(warpper);
		
		for(int i = 0 ; i < list.getLength() ; i ++){
			Element warp = (Element) list.item(i);
			NodeList itemList = warp.getElementsByTagName(tag);
			for(int j = 0 ; j < itemList.getLength() ; j++){
				CatchMatcher catchMatcher = CatchMatcher.parse((Element)itemList.item(j),prefix+"-Matcher["+i+"]");
				matchers.add(catchMatcher);
			}
		}
		
		return matchers;
	}
	
}
