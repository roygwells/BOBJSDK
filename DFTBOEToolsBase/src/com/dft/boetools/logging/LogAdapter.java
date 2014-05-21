package com.dft.boetools.logging;

/**
 * In some scenarios we may have to provide our own logging (Web Application like Launch), 
 * other times we can pick up logging from the BOE environment (Publication Extensions). 
 * This class will expose a logging interface but pass through the logging methods to an underlying object
 * @author rwells
 *
 */
public interface LogAdapter {
	  public abstract void debug(Object paramObject);

	  public abstract void debug(Object paramObject, Throwable paramThrowable);

	  public abstract void error(Object paramObject);

	  public abstract void error(Object paramObject, Throwable paramThrowable);

	  public abstract void fatal(Object paramObject);

	  public abstract void fatal(Object paramObject, Throwable paramThrowable);

	  public abstract void info(Object paramObject);

	  public abstract void info(Object paramObject, Throwable paramThrowable);

	  public abstract void warn(Object paramObject);

	  public abstract void warn(Object paramObject, Throwable paramThrowable);
}
