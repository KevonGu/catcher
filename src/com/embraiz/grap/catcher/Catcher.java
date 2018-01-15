package com.embraiz.grap.catcher;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public abstract class Catcher {

	private static DecimalFormat decimalFormat = new DecimalFormat(",###,###.00");
	private static Logger logger = Logger.getLogger(Catcher.class);
	

	protected static void load(Map<Integer,CatchPattern> patterns, File file)
			throws Exception {
				logger.info("加载:"+file.getPath());
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				//加载xml文档
				DocumentBuilder builder = factory.newDocumentBuilder();
					
				Document doc = builder.parse( file );
					
				//获取所有<pattern></pattern>标签
				NodeList elementList = doc.getElementsByTagName("pattern");
				//循环遍历所有<pattern></pattern>标签
				for(int i=0; i < elementList.getLength(); i++){
				Element patternElement = (Element) elementList.item(i);
					CatchPattern pattern = CatchPattern.parse(patternElement,file.getPath()+"-Pattern["+i+"]");
					
					if(patterns.containsKey(pattern.getId())){
						logger.error("重复定义了TablePattern["+pattern.getId()+"]");
					}else{
						logger.info("解析:"+file.getPath()+"-Pattern["+pattern.getId()+"]");
						patterns.put(pattern.getId(), pattern);
					}
					
				}
					
	}
	
	
	/***
	 * 给实体赋值
	 * @param data 实体
	 * @param value 值
	 * @param method setter方法
	 * @param getter getter方法
	 * @param column Column
	 * @param catchMatcher 匹配的matcher
	 * @return
	 */
	protected boolean assignValue(Object data, String value, Method method,Method getter,
			Column column, CatchMatcher catchMatcher) {
				Class<?> type = method.getParameterTypes()[0];
				//根据参数类型将 字符串转换为对应的类型 并 调用set方法
				try {
					if( type == String.class){
						
						//格式化数字
						if(column.isFormatNumber()){
							Double num = Double.parseDouble(value);
							if(catchMatcher.getScale()>0){
								num = num*catchMatcher.getScale();
							}
							value = decimalFormat.format(num);
						}
						String old = (String) getter.invoke(data);
						if(old != null){
							if(column.isFormatNumber()){
								value = decimalFormat.format(Double.parseDouble(value.replace(",", ""))
										+Double.parseDouble(old.replace(",", "")));
							}else{
								value = old+","+value;
							}
						}
						
						method.invoke(data, value);
					} else if(type == Integer.class){
						int num = Integer.parseInt(value);
						if(catchMatcher.getScale()>0){
							num = (int)(num*catchMatcher.getScale());
						}
						Integer old = (Integer) getter.invoke(data);
						if(old != null){
							num += old;
						}
						method.invoke(data, num);
					} else if(type == BigInteger.class){
						BigInteger num = new BigInteger(value);
						
						if(catchMatcher.getScale()>0){
							BigDecimal bigdec = new BigDecimal(num);
							bigdec = bigdec.multiply(new BigDecimal(catchMatcher.getScale()));
							num = bigdec.toBigInteger();
						}
						
						BigInteger old = (BigInteger) getter.invoke(data);
						if(old != null){
							num = num.add(old);
						}
						
						method.invoke(data, num);
					} else if(type == Float.class){
						float num = Float.parseFloat(value);
						
						if(catchMatcher.getScale() > 0){
							num = (float)(num*catchMatcher.getScale());
						}
						
						Float old = (Float) getter.invoke(data);
						if(old != null){
							num += old;
						}
						
						method.invoke(data, num);
					} else if(type == Double.class){
						double num = Double.parseDouble(value);
						
						if(catchMatcher.getScale() > 0) {
							num = (double)(num * catchMatcher.getScale());
						}
						
						Double old = (Double) getter.invoke(data);
						if(old != null){
							num += old;
						}
						
						method.invoke(data, num);
					} else if(type == Long.class){
						long num = Long.parseLong(value);
						
						if(catchMatcher.getScale() > 0){
							num = (long)( num * catchMatcher.getScale() );
						}

						Long old = (Long) getter.invoke(data);
						if(old != null){
							num += old;
						}
						
						method.invoke(data, num);
						
					} else if(type == Date.class){
						String format = catchMatcher.getFormat();
						
						if(format == null){
							format = "yyyy-MM-dd";
						}
						
						DateFormat formatter = new SimpleDateFormat( format );
						
						method.invoke(data, formatter.parse(value));
						
					} else {
					
					}
					return true;
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}


}
