package com.embraiz.grap.catcher;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegxUtil {
	private static Map<Character,Character> numberMapping = new HashMap<>();
	static{
		numberMapping.put('一', '1');
		numberMapping.put('二', '2');
		numberMapping.put('三', '3');
		numberMapping.put('四', '4');
		numberMapping.put('五', '5');
		numberMapping.put('六', '6');
		numberMapping.put('七', '7');
		numberMapping.put('八', '8');
		numberMapping.put('九', '9');
	}
	
	public static boolean isValid(String regx){
		try{
			Pattern.compile(regx);
			return true;
		}catch(Exception e){
			return false;
		}
	}
	
	public static boolean isDateFormat(String format){
		try{
			new SimpleDateFormat(format);
			return true;
		}catch(Exception e){
			return false;
		}
	}
	
	public static String numberToLower(String upper){
		StringBuilder builder = new StringBuilder();
		for(int i = 0 ; i < upper.length() ;i ++){
			char c = upper.charAt(i);
			Character num = numberMapping.get(c);
			if(num != null){
				builder.append(num);
			}else{
				if(c == '十'){
					if(upper.length() == 1){
						builder.append('1');
						builder.append('0');
					}
					else if(i == 0){
						builder.append('1');
					}else if( i == (upper.length() - 1 )){
						builder.append('0');
					}else{
						
					}
					
				}
			}
		}
		
		return builder.toString();
	}
}
