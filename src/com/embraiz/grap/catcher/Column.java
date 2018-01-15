package com.embraiz.grap.catcher;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.helper.StringUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 对应一个字段
 * @author Administrator
 *
 */
class Column {
	private String name;
	private List<CatchMatcher> regulars;
	private boolean formatNumber = false;
	private String filters;
	private String afterfilters;
	private boolean mutiple = false;
	
	public Column() {
		super();
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<CatchMatcher> getRegulars() {
		return regulars;
	}
	
	public void setRegulars(List<CatchMatcher> regulars) {
		this.regulars = regulars;
	}

	public boolean isFormatNumber() {
		return formatNumber;
	}

	public void setFormatNumber(boolean formatNumber) {
		this.formatNumber = formatNumber;
	}

	public String getFilters() {
		return filters;
	}

	public void setFilters(String filters) {
		this.filters = filters;
	}

	public String getAfterfilters() {
		return afterfilters;
	}

	public void setAfterfilters(String afterfilters) {
		this.afterfilters = afterfilters;
	}

	public boolean isMutiple() {
		return mutiple;
	}

	public void setMutiple(boolean mutiple) {
		this.mutiple = mutiple;
	}
	
	public static Column parse(Element element,String prefix){
		if(element == null){
			throw new NullPointerException(prefix+"-element不能为null。");
		}
		
		Column column = new Column();
		
		String name = element.getAttribute("name");
		String formatNumber = element.getAttribute("formatNumber");
		String filter = element.getAttribute("filter");
		String afterFilter = element.getAttribute("afterfilter");
		String mutiple = element.getAttribute("mutiple");
		
		if(StringUtil.isBlank(name)){
			throw new RuntimeException(prefix+"-属性name没有声明。");
		}
		
		column.setName(name);
		
		if(StringUtil.isBlank(formatNumber) == false){
			if(formatNumber.matches("true")){
				column.setFormatNumber(true);
			}else if(formatNumber.matches("false")){
				column.setFormatNumber(false);
			}else{
				throw new RuntimeException(prefix+"-属性formatNumber值"+formatNumber+"不合法，要求为true或false。");
			}
		}
		
		if(StringUtil.isBlank(filter) == false){
			String[] filters = Filter.split(filter);
			
			for (String filt : filters) {
				if(FilterFactory.contain(filt) == false){
					throw new RuntimeException(prefix+"-无法识别过滤器"+filt+",请检查是否存在拼写错误，或者检查filter.xml中是否有定义"+filt+"过滤器。");
				}
			}
			
			column.setFilters(filter);
 		}
		
		if(StringUtil.isBlank(afterFilter) == false){
			String[] filters = Filter.split(afterFilter);
			for (String filt : filters) {
				if(FilterFactory.contain(filt) == false){
					throw new RuntimeException(prefix+"-无法识别过滤器"+filt+",请检查是否存在拼写错误，或者检查filter.xml中是否有定义"+filt+"过滤器。");
				}
			}
			
			column.setAfterfilters(afterFilter);
		}
		
		if(StringUtil.isBlank(mutiple) == false){
			if(mutiple.matches("true")){
				column.setMutiple(true);
			}else if(mutiple.matches("false")){
				column.setMutiple(false);
			}else{
				throw new RuntimeException(prefix+"-属性mutiple值"+mutiple+"不合法，要求为true或false。");
			}
		}
		
		List<CatchMatcher> regulars = new ArrayList<>();
		
		NodeList list = element.getElementsByTagName("matcher");
		
		for(int i = 0; i < list.getLength() ; i++){
			Element matcherElement = (Element) list.item(i);
			CatchMatcher catchMatcher = CatchMatcher.parse(matcherElement,prefix+"-Matcher["+i+"]");
			regulars.add(catchMatcher);
		}
		
		column.setRegulars(regulars);
		
		return column;
	}
	
}
