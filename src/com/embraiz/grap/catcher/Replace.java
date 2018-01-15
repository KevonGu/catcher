package com.embraiz.grap.catcher;

import org.w3c.dom.Element;

class Replace {
	private String from;
	private String to = "";
	private String ref;
	
	
	public String getRef() {
		return ref;
	}
	public void setRef(String ref) {
		this.ref = ref;
	}
	public Replace() {
		super();
	}
	public Replace(String from, String to) {
		super();
		this.from = from;
		this.to = to;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	
	public String replace(String input){
		if(ref == null){
			return input.replaceAll(from, to);
		}else{
			return FilterFactory.filte(input, ref);
		}
	}
	
	public static Replace parse(Element element){
		if(element == null){
			throw new NullPointerException("element不能为 null。");
		}
		
		Replace replace = new Replace();
		
		String from = element.getAttribute("from");
		String to = element.getAttribute("to");
		String ref = element.getAttribute("ref");
		
		if(isEmpty(from) && isEmpty(ref)){
			System.out.println(from);
			System.out.println(ref);
			throw new RuntimeException("至少需要声明from属性或ref属性。");
		}else if(isEmpty(ref) == false){
			if(FilterFactory.contain(ref) == false){
				throw new RuntimeException("无法识别translator "+ref+"，请检查是否存在拼写错误或者检查filter.xml中是否包含"+ref+"。");
			}else{
				replace.setRef(ref);
			}
			
		}else{
			if(RegxUtil.isValid(from)){
				replace.setFrom(from);
			}else{
				throw new RuntimeException("正则表达式"+from+"不合法。");
			}
			
		}
		
		if(isEmpty(to) == false){
			replace.setTo(to);
		}
		
		return replace;
	}
	
	private static boolean isEmpty(String s){
		if(s == null || s.equals(""))
			return true;
		else
			return false;
	}
	
}
