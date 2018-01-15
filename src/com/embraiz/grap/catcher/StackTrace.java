package com.embraiz.grap.catcher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
/*
 * 将异常转化为字符串返�?
 */
public class StackTrace {
	 public String getStackTrace(Throwable aThrowable) {
	        final Writer result = new StringWriter();
	        final PrintWriter printWriter = new PrintWriter(result);
	        aThrowable.printStackTrace(printWriter);
	        return result.toString();
	      }
}
