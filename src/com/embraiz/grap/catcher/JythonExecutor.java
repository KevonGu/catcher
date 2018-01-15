package com.embraiz.grap.catcher;

import java.io.File;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.python.util.PythonInterpreter;

public class JythonExecutor {
	private static PythonInterpreter interpreter = null;
	private static ReentrantLock lock = new ReentrantLock();
	private static Map<String,Object> data = null;
	private static Logger logger = Logger.getLogger(JythonExecutor.class);
	
	
	public static void shoutDown(){
		lock.lock();
		if(interpreter!=null){
			logger.info("python执行环境关闭");
			interpreter.close();
			interpreter = null;
		}
		lock.unlock();
	}
	
	private static void init() {
		if(interpreter == null){
			logger.info("python执行环境初始化开始");
			interpreter = new PythonInterpreter();
			logger.info("python执行环境初始化完成");
		}
	}
	
	
	public static Map<String,Object> getData() {
		return data;
	}

	public static Map<String,Object> exec(Map<String, Object> data,String scriptPath){
		File scriptFile = new File(scriptPath);
		if(scriptFile.isFile() == false){
			return data;
		}
		
		lock.lock();
		if(interpreter == null){
			init();
		}
		logger.info("执行python脚本-"+scriptPath);
		JythonExecutor.data = data;
		interpreter.execfile(scriptPath);
		JythonExecutor.data = null;
		logger.info("执行完成-"+scriptPath);
		lock.unlock();
		return data;
	}
}
