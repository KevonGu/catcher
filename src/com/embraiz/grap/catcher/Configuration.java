package com.embraiz.grap.catcher;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class Configuration {
	private static String patternDir = "D:\\pattern\\";

	private static Logger logger = Logger.getLogger(Configuration.class);
	
	static {
		InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream("catcher.properties");
		try {
			Properties properties = new Properties();
			properties.load(inputStream);
			String dir = properties.getProperty("patternDir");

			if(dir != null){
				File file = new File(dir);
				if(file.isDirectory() == true){
					patternDir = dir;
					logger.info("配置pattern目录为"+dir);
				}
				
			}
			
		} catch (Exception e) {
			logger.warn("没有配置pattern目录，默认为"+patternDir);
		}
		
	}
	
	public static String getPatternDir(){
		return patternDir;
	}
}
