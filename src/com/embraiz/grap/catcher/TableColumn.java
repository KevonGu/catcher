package com.embraiz.grap.catcher;

import java.util.List;

import org.jsoup.helper.StringUtil;
import org.w3c.dom.Element;

class TableColumn {
	private String name;
	private String target;
	private boolean formatNumber = false;
	private List<CatchMatcher> headMatchers;
	private List<CatchMatcher> valueMatchers;
	private Double minNum,maxNum;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isFormatNumber() {
		return formatNumber;
	}

	public void setFormatNumber(boolean formatNumber) {
		this.formatNumber = formatNumber;
	}
	
	public List<CatchMatcher> getHeadMatchers() {
		return headMatchers;
	}
	
	public void setHeadMatchers(List<CatchMatcher> headMatchers) {
		this.headMatchers = headMatchers;
	}
	
	public List<CatchMatcher> getValueMatchers() {
		return valueMatchers;
	}
	
	public void setValueMatchers(List<CatchMatcher> valueMatchers) {
		this.valueMatchers = valueMatchers;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public Double getMinNum() {
		return minNum;
	}

	public void setMinNum(Double minNum) {
		this.minNum = minNum;
	}

	public Double getMaxNum() {
		return maxNum;
	}

	public void setMaxNum(Double maxNum) {
		this.maxNum = maxNum;
	}


	public static TableColumn parse(Element element,String prefix){
		
		if(element == null){
			throw new NullPointerException(prefix+"-element不能为null");
		}
		
		TableColumn column = new TableColumn();
		
		String name = element.getAttribute("name");
		String target = element.getAttribute("target");
		String formatNumber = element.getAttribute("formatNumber");
		String minNum = element.getAttribute("minNum");
		String maxNum = element.getAttribute("maxNum");
		
		if(StringUtil.isBlank(name)){
			throw new RuntimeException(prefix+"-没有声明属性name。");
		}
		
		column.setName(name);
		
		if(StringUtil.isBlank(target) == false){
			if(target.matches("row")){
				column.setTarget("row");
			}
		}
		
		if(StringUtil.isBlank(formatNumber) == false){
			if(formatNumber.matches("true")){
				column.setFormatNumber(true);
			}else if(formatNumber.matches("false")){
				column.setFormatNumber(false);
			}else{
				throw new RuntimeException(prefix+"-属性formatNumber值"+formatNumber+"不合法，要求为true或false。");
			}
		}
		
		if(StringUtil.isBlank(maxNum) == false){
			if(StringUtil.isNumeric(maxNum)){
				column.setMaxNum(Double.parseDouble(maxNum));
			}else{
				throw new RuntimeException(prefix+"-属性maxNum值"+maxNum+"不合法，要求为一个double类型的数。");
			}
		}
		
		if(StringUtil.isBlank(minNum) == false){
			if(StringUtil.isNumeric(minNum)){
				column.setMinNum(Double.parseDouble(minNum));
			}else{
				throw new RuntimeException(prefix+"-属性minNum值"+minNum+"不合法，要求为一个double类型的数。");
			}
		}
		
		List<CatchMatcher> headmatchers = TablePattern.listElement("headermatchers", "matcher", element, prefix+"-headermatchers");
		List<CatchMatcher> valuematchers = TablePattern.listElement("valuematchers", "matcher", element, prefix+"-valuematchers");
		
		
		column.setHeadMatchers(headmatchers);
		column.setValueMatchers(valuematchers);
		
		return column;
	}
	
}
