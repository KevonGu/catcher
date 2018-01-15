package com.embraiz.grap.catcher;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.helper.StringUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Filter {
	private String name;
	
	private List<Replace> replaces = null;
	
	
	
	public String getName() {
		return name;
	}



	public void setName(String name) {
		this.name = name;
	}



	public List<Replace> getReplaces() {
		return replaces;
	}



	public void setReplaces(List<Replace> replaces) {
		this.replaces = replaces;
	}



	public String filte(String input){
		
		for (Replace replace : replaces) {
			input = replace.replace(input);
		}
		
		return input;
	}
	
	public static String[] split(String input){
		return input.split(",");
	}
	
	public static Filter parse(Element element){
		if(element == null){
			throw new NullPointerException("element不能为null");
		}
		Filter filter = new Filter();
		String name = element.getAttribute("name");
		if(StringUtil.isBlank(name)){
			throw new RuntimeException("没有声明filter的name属性。");
		}
		
		filter.setName(name);
		
		NodeList list = element.getElementsByTagName("replace");
		List<Replace> replaces = new ArrayList<>();
		
		for (int i = 0; i < list.getLength(); i++) {
			Element repalceElement = (Element) list.item(i);
			Replace replace = Replace.parse(repalceElement);
			replaces.add(replace);
		}
		filter.setReplaces(replaces);
		
		return filter;
	}
	
}
