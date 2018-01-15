package com.embraiz.grap.catcher;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.helper.StringUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Administrator
 *
 */
class CatchMatcher {
	private String format;
	private Pattern reg;
	private List<Replace> replaces;
	private int max = Integer.MAX_VALUE;
	private int min = 0;
	private double scale = -1;
	private String filter;
	private String checkReg;
	private int limit = Integer.MAX_VALUE;
	private boolean mutiple = false;
	
	
	
	public CatchMatcher() {
		super();
	}
	
	

	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	public Pattern getReg() {
		return reg;
	}
	public void setReg(Pattern reg) {
		this.reg = reg;
	}
	
	public int getMax() {
		return max;
	}
	public void setMax(int max) {
		this.max = max;
	}
	public int getMin() {
		return min;
	}
	public void setMin(int min) {
		this.min = min;
	}

	public List<Replace> getReplaces() {
		return replaces;
	}

	public void setReplaces(List<Replace> replaces) {
		this.replaces = replaces;
	}



	public double getScale() {
		return scale;
	}


	public void setScale(double scale) {
		this.scale = scale;
	}

	
	



	public String getCheckReg() {
		return checkReg;
	}



	public void setCheckReg(String checkReg) {
		this.checkReg = checkReg;
	}

	


	public int getLimit() {
		return limit;
	}



	public void setLimit(int limit) {
		this.limit = limit;
	}



	public boolean isMutiple() {
		return mutiple;
	}



	public void setMutiple(boolean mutiple) {
		this.mutiple = mutiple;
	}



	/**
	 * 匹配一个字符串
	 * @param content 输入文本
	 * @return 匹配到的文本（已经经过Repalce处理）
	 */
	public String match(String content){
		Matcher matcher = reg.matcher(content);
		while (matcher.find()) {
			String v = matcher.group();
			v = replaceAll(v);
			if(v.length()>=min && v.length()<=max && checkFormat(v) == true){
				return v;
			}
		}
		
		return null;
	}
	
	/**
	 * 匹配一个字符串
	 * @param content 输入文本
	 * @param filter after过滤器组合
	 * @return
	 */
	public String match(String content,String filter){
		Matcher matcher = reg.matcher(content);
		while (matcher.find()) {
			String v = matcher.group();
			v = replaceAll(v);
			v = FilterFactory.filte(v, filter);
			if(v.length()>=min && v.length()<=max && checkFormat(v) == true){
				return v;
			}
		}
		
		return null;
	}
	
	/**
	 * 多值匹配 匹配文中 全部符合条件的文本
	 * @param content 输入文本
	 * @return 匹配到字符串的列表
	 */
	public List<String> mutipleMatch(String content){
		Matcher matcher = reg.matcher(content);
		List<String> res = new ArrayList<>();
		while (matcher.find()) {
			String v = matcher.group();
			v = replaceAll(v);
			if(v.length()>=min && v.length()<=max && checkFormat(v) == true){
				res.add(v);
			}
		}
		return res;
	}
	
	/**
	 * 多值匹配 匹配文中 全部符合条件的文本
	 * @param content 输入文本
	 * @param filter after过滤器
	 * @return 匹配到的字符串列表
	 */
	public List<String> mutipleMatch(String content,String filter){
		Matcher matcher = reg.matcher(content);
		List<String> res = new ArrayList<>();
		while(matcher.find()){
			String v = matcher.group();
			v = replaceAll(v);
			v = FilterFactory.filte(v, filter);
			if(v.length()>=min && v.length()<=max && checkFormat(v) == true){
				res.add(v);
			}
		}
		return res;
	}
	
	/**
	 * 使用Matcher的Repalce对文本进行替换
	 * @param input 输入文本
	 * @return 替换之后的文本
	 */
	public String replaceAll(String input){
		for (Replace replace : replaces) {
			if(replace.getRef() != null){
				input = FilterFactory.filte(input, replace.getRef());
			}else{
				input = input.replaceAll(replace.getFrom(), replace.getTo());
			}
			
		}
		return input;
	}
	
	private boolean checkFormat(String value){
		if(checkReg==null || checkReg.equals("")){
			return true;
		}
		
		if(value.matches(checkReg) == true){
			return true;
		}else{
			return false;
		}
	}

	public String getFilter() {
		return filter;
	}
	
	public String[] getFilters(){
		if(filter == null){
			return null;
		}
		return filter.split(",");
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}


	@Override
	public String toString() {
		return "CatchMatcher [format=" + format + ", reg=" + reg.pattern()
				+ ", replaces=" + replaces + ", max=" + max + ", min=" + min
				+ ", scale=" + scale + "]";
	}
	
	public static CatchMatcher parse(Element element,String prefix){
		CatchMatcher catchMatcher = new CatchMatcher();
		
		if(element == null){
			throw new NullPointerException("element不能为null。");
		}
		
		String regular = element.getAttribute("regular");
		String max = element.getAttribute("max");
		String min = element.getAttribute("min");
		String filter = element.getAttribute("filter");
		String format = element.getAttribute("format");
		String scale = element.getAttribute("scale");
		String limit = element.getAttribute("limit");
		String mutiple = element.getAttribute("mutiple");
		String checkReg = element.getAttribute("check");
		
		if(StringUtil.isBlank(regular)){
			throw new RuntimeException(prefix+"-没有声明regular属性。");
		}
		
		Pattern reg = Pattern.compile(regular);
		catchMatcher.setReg(reg);

		if(StringUtil.isBlank(max) == false){
			if(max.matches("\\d+")){
				catchMatcher.setMax(Integer.parseInt(max));
			}else{
				throw new RuntimeException(prefix+"-属性max值"+max+"格式不正确，要求非负整数。");
			}
		}
		
		if(StringUtil.isBlank(min) == false){
			if(min.matches("\\d+")){
				catchMatcher.setMin(Integer.parseInt(min));
			}else{
				throw new RuntimeException(prefix+"-属性min值"+min+"格式不正确，要求非负整数。");
			}
		}
		
		if(StringUtil.isBlank(filter) == false){
			String[] filters = Filter.split(filter);
			for (String filt : filters) {
				if(FilterFactory.contain(filt) == false){
					throw new RuntimeException(prefix+"-无法识别过滤器"+filt+",请检查是否存在拼写错误或检查filter.xml中是否定义了"+filt+"过滤器。");
				}
			}
			
			catchMatcher.setFilter(filter);
			
		}
		
		if(StringUtil.isBlank(format) == false){
			if(RegxUtil.isDateFormat(format)){
				catchMatcher.setFormat(format);
			}else{
				throw new RuntimeException(prefix+"-非法日期格式"+format);
			}
		}
		
		if(StringUtil.isBlank(scale) == false){
			if(scale.matches("\\d+\\.?\\d*")){
				catchMatcher.setScale(Double.parseDouble(scale));
			}else{
				throw new RuntimeException(prefix+"-属性sacle值"+scale+"不合法，要求为一个非负double类型。");
			}
		}
		
		if(StringUtil.isBlank(limit) == false){
			if(limit.matches("\\d+")){
				catchMatcher.setLimit(Integer.parseInt(limit));
			}else{
				throw new RuntimeException(prefix+"-属性limit值"+limit+"不合法，要求为一个非负整数。");
			}
		}
		
		if(StringUtil.isBlank(mutiple) == false){
			if(mutiple.matches("true")){
				catchMatcher.setMutiple(true);
			}else if(mutiple.matches("false")){
				catchMatcher.setMutiple(false);
			}else{
				throw new RuntimeException(prefix+"-属性mutiple值"+mutiple+"不合法，要求为true或false。");
			}
		}
		
		if(StringUtil.isBlank(checkReg) == false){
			if(RegxUtil.isValid(checkReg)){
				catchMatcher.setCheckReg(checkReg);
			}else{
				throw new RuntimeException(prefix+"-属性check值"+checkReg+"不合法，要求为一个合法的正则表达式。");
			}
		}
		
		NodeList list = element.getElementsByTagName("replace");
		
		List<Replace> replaces = new ArrayList<>();
		
		for(int i=0; i < list.getLength();i++){
			Element replaceElement = (Element) list.item(i);
			
			Replace replace = Replace.parse(replaceElement);
			replaces.add(replace);
			
		}
		
		catchMatcher.setReplaces(replaces);
		
		return catchMatcher;
	}
	
}
