package com.embraiz.grap.catcher;

import java.util.HashMap;
import java.util.Map;

import org.jsoup.helper.StringUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 对应每一一个grap_pattern id 的 抓取方案
 * @author Administrator
 *
 */
public class CatchPattern {
	private int id;
	private Map<String,Column> columns;
	private String filters;
	
	public CatchPattern() {
		super();
	}

	public CatchPattern(int id, Map<String, Column> columns) {
		super();
		this.id = id;
		this.columns = columns;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public Map<String, Column> getColumns() {
		return columns;
	}
	
	public void setColumns(Map<String, Column> columns) {
		this.columns = columns;
	}
	
	public Column getColumn(String name){
		return columns.get(name);
	}

	public String getFilters() {
		return filters;
	}

	public void setFilters(String filters) {
		this.filters = filters;
	}
	
	public static CatchPattern parse(Element element,String prefix){
		if(element == null){
			throw new NullPointerException(prefix+"-element不能为null。");
		}
		
		CatchPattern catchPattern = new CatchPattern();
		
		String id = element.getAttribute("id");
		String filters = element.getAttribute("filter");
		
		if(StringUtil.isBlank(id)){
			throw new RuntimeException(prefix+"-没有声明id属性值。");
		}
		
		if(id.matches("-?\\d+") == false){
			throw new RuntimeException(prefix+"-属性id值"+id+"不合法，要求为一个整数。");
		}
		
		catchPattern.setId(Integer.parseInt(id));
		
		if(StringUtil.isBlank(filters) == false){
			String[] filterSplit = Filter.split(filters);
			
			for (String filter : filterSplit) {
				if(FilterFactory.contain(filter) == false){
					throw new RuntimeException(prefix+"-无法识别过滤器"+filter+",请检查是否存在拼写错误，或者检查filter.xml中是否定义了"+filter+"过滤器。");
				}
			}
			
			catchPattern.setFilters(filters);
		}
		
		Map<String,Column> columns = new HashMap<String, Column>();
		
		NodeList list = element.getElementsByTagName("column");
		
		for(int i = 0; i < list.getLength() ; i ++){
			Element columnElement = (Element) list.item(i);
			Column column = Column.parse(columnElement,prefix+"-Column["+i+"]");
			columns.put(column.getName(), column);
		}
		
		catchPattern.setColumns(columns);
		
		return catchPattern;
	}
}
