package com.embraiz.grap.catcher;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;

public class CatchUtil {
	
	//
	public static Object catchContent(Object entity,String content,int...id){
		ContentCatcher catcher = new ContentCatcher();
		
		for (int i : id) {
			catcher.catchColum(entity, i, content);
		}
		
		return entity;
	}
	
	//
	public static List<Object> catchProjectByHtml(Class<?> clazz,String html,int id){
		
		return new ArrayList<>(catchProject(clazz, html, id).values());
	}
	
	public static Map<String,Object> catchProject(Class<?> clazz,String html,int... id){
		Map<String,Object> data = new HashMap<>();
		
		TableProcess process = new TableProcess();
		ProjectCatcher catcher = new ProjectCatcher();
		process.processTable(clazz, Jsoup.parse(html),html, id[id.length-1], data);
		catcher.catchData(clazz, id, html, data);

		int size = data.values().size();
		
		//没有抓到分包信息 说明项目没有分包 从表格中抓取包组相关信息（只包含一个包组）
		//如果没有抓取到包组 尝试抓取采购表格
		if(size == 0){
			
			Object entity = null;
			try {
				entity = clazz.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			data.put("1", entity);
			
			process.processSingle(entity, Jsoup.parse(html), id[id.length-1]);
			catcher.catchColum(entity, id, html);
			
		}else if(size == 1){

			Object entity = data.values().iterator().next();
			process.processSingle(entity, Jsoup.parse(html), id[id.length-1]);
			catcher.catchColum(entity, id, html);
			
		}
		
		return data;
	}

	public static List<Object> catchProjectByHtml(Class<?> clazz,String html,int... id){
		Map<String,Object> data = new HashMap<>();
		
		TableProcess process = new TableProcess();
		ProjectCatcher catcher = new ProjectCatcher();
		process.processTable(clazz, Jsoup.parse(html),html, id[id.length-1], data);
		catcher.catchData(clazz, id, html, data);

		int size = data.values().size();
		
		//没有抓到分包信息 说明项目没有分包 从表格中抓取包组相关信息（只包含一个包组）
		//如果没有抓取到包组 尝试抓取采购表格
		if(size == 0){
			
			Object entity = null;
			try {
				entity = clazz.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			data.put("1", entity);
			
			process.processSingle(entity, Jsoup.parse(html), id[id.length-1]);
			catcher.catchColum(entity, id, html);
			
		}else if(size == 1){

			Object entity = data.values().iterator().next();
			process.processSingle(entity, Jsoup.parse(html), id[id.length-1]);
			catcher.catchColum(entity, id, html);
			
		}
		return new ArrayList<>(data.values());
	}
	
	public static Map<String,Object> execPython(Map<String,Object> data, int patternId){
		File file = new File(Configuration.getPatternDir()+File.separator+patternId+".py");
		if(file.isFile() == false){
			Logger logger = Logger.getLogger(JythonExecutor.class);
			logger.warn("Pattern["+patternId+"]没对应的Python处理脚本，找不到"+file.getPath());
			return data;
		}
		
		return JythonExecutor.exec(data, file.getPath());
	}
}
