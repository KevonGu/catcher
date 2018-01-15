package com.embraiz.grap.catcher;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;




public class FilterFactory {
	private static Map<String,Filter> filters;
	private static Logger logger = Logger.getLogger(FilterFactory.class);
	
	static{
		try {
			logger.info("加载:filter.xml");
			init();
		} catch (Exception e) {
			logger.error("加载:filter.xml出错-"+e);
			logger.debug("异常详情", e);
		}
	}
	
	private static void init() throws Exception{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//加载xml文档
			Map<String,Filter> filters = new HashMap<String, Filter>();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse( new File(Configuration.getPatternDir()+File.separator+"filter.xml") );
			NodeList nl = doc.getElementsByTagName("filter");

			FilterFactory.filters = filters;
			
			for(int k = 0; k < nl.getLength() ; k++){
					Element filterElement = (Element) nl.item(k);                                 ;
					
					Filter filter = Filter.parse(filterElement);
					
					filters.put(filter.getName(), filter);
			}
			
			
		
	}
	
	public static void reload(){
		try{
			logger.info("加载:filters.xml");
			init();
		}catch(Exception e){
			logger.error("加载:filters.xml出错-"+e);
			logger.debug("异常详情", e);
		}
		
	}
	
	
	
	public static Filter getFilter(String name){
		return filters.get(name);
	}

	/**
	 * 检查是否包含数组中的全部过滤器
	 * @param filterNames
	 * @return
	 */
	public static boolean containsAll(String[] filterNames){
		for (String name : filterNames) {
			if( !filters.containsKey(name) )
				return false;
		}
		return true;
	}
	
	/**
	 * 过滤文本
	 * @param input 待过滤的文本
	 * @param filter 过滤器组合 filter1,filter2,filter3...
	 * @return
	 */
	public static String filte(String input,String filter){
		return filte(input, Filter.split(filter));
	}
	
	public static String filte(String input,String[] filter){
		
		for (String name : filter) {
			input = filters.get(name).filte(input);
		}
		
		return input;
	}
	
	/**
	 * 检查是否存在过滤器 例如：contain("tag-filter")
	 * @param filter 过滤器名称
	 * @return
	 */
	public static boolean contain(String filter){
		return filters.containsKey(filter);
	}
}
