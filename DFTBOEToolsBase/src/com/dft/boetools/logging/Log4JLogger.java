package com.dft.boetools.logging;

import org.apache.log4j.Logger;

public class Log4JLogger implements LogAdapter {

	private Logger logger;
	
	public Log4JLogger(Logger log) {
		this.logger = log;
	}
	
	public void debug(Object paramObject) {
		logger.debug(paramObject);
	}

	public void debug(Object paramObject, Throwable paramThrowable) {
		logger.debug(paramObject, paramThrowable);
	}

	public void error(Object paramObject) {
		logger.error(paramObject);
	}

	public void error(Object paramObject, Throwable paramThrowable) {
		logger.error(paramObject, paramThrowable);
	}

	public void fatal(Object paramObject) {
		logger.fatal(paramObject);
	}

	public void fatal(Object paramObject, Throwable paramThrowable) {
		logger.fatal(paramObject, paramThrowable);
	}

	public void info(Object paramObject) {
		logger.info(paramObject);
	}

	public void info(Object paramObject, Throwable paramThrowable) {
		logger.info(paramObject, paramThrowable);
	}

	public void warn(Object paramObject) {
		logger.warn(paramObject);
	}

	public void warn(Object paramObject, Throwable paramThrowable) {
		logger.warn(paramObject, paramThrowable);
	}

}
