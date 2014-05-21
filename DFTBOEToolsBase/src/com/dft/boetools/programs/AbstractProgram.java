package com.dft.boetools.programs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.CePropertyID;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.occa.infostore.ISchedulingInfo;
import com.crystaldecisions.sdk.plugin.desktop.program.IProgramBase;
import com.dft.boetools.BOEHelper;
import com.dft.boetools.QueryHelper;
import com.dft.boetools.StringHelper;

/**
 * Base class for creating Business Objects Program Objects.  These can be loaded
 * into the Business Objects Repository and then run on demand or on a schedule.  Program
 * Objects have the advantage that they automatically run in the Context of the Business
 * Objects server so there is no need to worry about storing BO Credentials
 * @author rwells
 *
 */
public abstract class AbstractProgram implements IProgramBase{
	protected Logger logger = Logger.getLogger(this.getClass());
	private QueryHelper q;

	/**
	 * Arguments that were set in the CMC and provide configuration information 
	 * for the current Program
	 */
	private Properties arguments;
	
	/**
	 * Subclasses can override this method if they have default values for certain arguments.
	 * Their implementation should return a Property collection with their defaults set.
	 */
	protected Properties getDefaultArguments(){
		Properties defaults = new Properties();
		addDefaultArguments(defaults);
		return defaults;		
	}

	/**
	 * Classes that want to be sure to pick up any arguments their parents have added 
	 * can override this method instead of getDefaultArguments.  Implementers should
	 * invoke super.addDefaultArguments(defaults) before adding their own defaults.
	 */
	protected void addDefaultArguments(Properties defaults) {	}
	
	/**
	 * Subclasses that have required arguments that must be populated at runtime can return 
	 * a list of the argument keys in this method.  The abstract program logic will verify the
	 * presence of these keys prior to executing any subclass logic.
	 * @return an array of String keys that define the required arguments for this program
	 */
	private List<String> getRequiredArguments() { 
		List<String> required = new ArrayList<String>();
		addRequiredArguments(required);
		return required;		
	}
	
	/**
	 * Classes that want to be sure to pick up any arguments their parents have added
	 * can override this method instead of getRequiredArguments.  Implementers should
	 * invoke super.addRequiredArguments(required) before adding their own arguments
	 * @param required
	 */
	protected void addRequiredArguments(List<String> required) {	}
	

	protected final String getArgument(String key) {
		return arguments.getProperty(key);
	}
	
	protected final List<String> getListArgument(String key) {
		return StringHelper.parseTo(getArgument(key));
	}
	
	protected final <T extends Collection<String>> T getCollectionArgument(T output, String key) {
		return StringHelper.parseTo(output, arguments.getProperty(key));
	}
	
	protected final  boolean getBooleanArgument(String key) {
		String value = arguments.getProperty(key);
		if("T".equalsIgnoreCase(value)) return Boolean.TRUE.booleanValue();
		return Boolean.valueOf(value);
	}
	
	protected final int getIntArgument(String key){
		return Integer.parseInt(arguments.getProperty(key));
	}
	
	/**
	 * This method will use the list of keys returned by getRequiredArguments() to ensure
	 * that all such arguments exist in the arguments map.  If any are found to be missing
	 * an exception will be thrown.
	 * 
	 * @throws Exception if a missing argument is detected
	 */
	private void validateRequiredArguments() throws Exception {
		for (String key : getRequiredArguments()) {
			if (!arguments.containsKey(key)) throw new Exception("Missing argument detected: " + key);
		}
	}
	
	/**
	 * This method will deal with arguments that are common across all Program objects.  Primarily 
	 * this will involve the handling of log files and notification of job failures. 
	 * 
	 * Supported Arguments:
	 * 
	 * LOG4J_CONFIG_FILE=FILE_NAME_POINTING_TO_LOG4J_PROPERTIES
	 *   This argument will allow you to override any default log4j configuration that was provided
	 *   in the deployed jar file.  Often this would be used to add appenders set to catch 
	 *   error logs and send them somewhere outside the default console log that is captured in the
	 *   CMC.  This is useful because the handling of logging for failed program jobs is somewhat weak.
	 *   Its important to note that the file path given for this property is relative to the Job
	 *   server processing this program job and might need to exist on multiple servers in a cluster. 
	 */
	private void processKnownArguments() {
		if(arguments.containsKey(LOG4J_CONFIG_FILE)){
			PropertyConfigurator.configure(arguments.getProperty(LOG4J_CONFIG_FILE));
		}
		
		if (arguments.containsKey(LOG4J_LEVEL)){
			Logger.getRootLogger().setLevel(Level.toLevel(arguments.getProperty(LOG4J_LEVEL)));
		} else {
			Logger.getRootLogger().setLevel(Level.DEBUG);
		}
	}
	
	private static final String LOG4J_CONFIG_FILE = "LOG4J_CONFIG_FILE";
	private static final String LOG4J_LEVEL = "LOG4J_LEVEL";
	
	/**
	 * This method is invoked by the BOE Platform.  The enterprise session passed in is 
	 * logged in to the platform as the user who is running the schedule for the program job object.
	 * Typically this is an Administrator account.  
	 * 
	 * We are also passed an array of string Arguments.  This array comes form the options configuration
	 * in the CMC GUI.  Much like the arguments passed to a command line program, the values in the 
	 * CMC are turned into a String array by splitting on any white space.  
	 * 
	 * This abstract program will parse the incoming arguments and attempt to turn them into a map 
	 * of keys and values for use by any sub classes.  The rules for parsing the values are as follows
	 * 
	 * 1) If an argument contains an '=' char then assume it is a name value pair.  Use the substring 
	 *    to the left of the '=' as the key and the substring to the right as the value.  Since the 
	 *    original string is parsed on white space ensure that the entire parameter including the 
	 *    name and value are wrapped in quotes. 
	 *    
	 *    Example: "This is a Key with Spaces=This is a Value with Spaces"
	 *      
	 * 2) If the argument does not contain '=' we will treat it like a boolean flag and simply
	 *    include the key in the map with the key also as the value.  It is assumed that subclasses
	 *    will simply look for the presence or absence of the key to signify some control flag.
	 *    
	 * After parsing arguments we will validate that all required arguments are present and then
	 * pass control of the program down to the sub class.
	 */
	public final void run(IEnterpriseSession session, IInfoStore store, String[] args) throws SDKException {
		//ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%-4r [%t] %-5p %c %x - %m%n"));
		//Logger.getRootLogger().addAppender(appender);
		//Logger.getRootLogger().setLevel(Level.DEBUG);
		
		try {
			// Initialize State
			arguments = getDefaultArguments();
			if (arguments == null) arguments = new Properties(); // Just in case some idiot returns null from override
						
			// Process our startup arguments, and replace defaults with those from command line.
			for (int i = 0; i < args.length; i++) {
				String arg = args[i];
				if (arg.indexOf("=") > 0) {
					String key = arg.substring(0, arg.indexOf("="));
					String value = arg.substring(arg.indexOf("=") + 1);
					arguments.put(key.trim(), value.trim());
				} else {
					arguments.put(arg, arg);
				}
			}
			
			validateRequiredArguments();
			processKnownArguments();
			
			//System.out.println("Test Print Statement");
			
			logger.info("Executing Program Object with following arguments:");
			logger.info(arguments);
						
			// Now do the actual work that we've come to do.			
			BOEHelper boe = new BOEHelper(session);	
			q= new QueryHelper(boe);
			runInternal(boe);
			
			logger.info("Completed execution of Program");
			
		
		} catch (Exception e) {
			logger.error(e.getMessage(), e);		
			
			// Literal output required by Business Objects to support Job Failure.  
			System.out.println();
			System.out.println("PROCPROGRAM:PROGRAM_ERROR");
			System.out.println("62009");
			System.out.println("Error: " + e.getMessage());
			
			// Only the first two lines of the stack trace will be stored in the CMS Repository under the 
			// SI_STATUSINFO property.
			e.printStackTrace(System.out); 
			
		} 
	}
	
	private static final String JOB_NAME = "JOB_NAME";
	
	protected final Date getLastRuntime() throws SDKException{
		String jobName = getArgument(JOB_NAME);
		if (jobName == null) {
			logger.warn("No JOB_NAME Parameter specified in startup arguments.  We can only determine the last runtime for the job with that parameter set.  Using beggining of Java epoch as default value (Jan 1st 1970)");
			return new Date(0L);
		} else {
			String jobHistoryQuery = "SELECT TOP 1 SI_ID, SI_NAME, SI_STARTTIME FROM CI_INFOOBJECTS WHERE SI_NAME = '" + StringHelper.escQteBOE(jobName) + "' AND SI_INSTANCE = 1 AND SI_SCHEDULE_STATUS=" + ISchedulingInfo.ScheduleStatus.COMPLETE+" ORDER BY SI_STARTTIME DESC";
			IInfoObjects historyList = Q().executeRawQuery(jobHistoryQuery);
			
			if (historyList.size() > 0) {
				IInfoObject instance = (IInfoObject) historyList.get(0);				
				return instance.properties().getDate(CePropertyID.SI_STARTTIME);
			} else {
				// This must be the first time this program has ever run
				logger.debug("No Job Found for getLastRunTime, assuming first time job has run, using Java Epoc as start time (Jan 1st 1970).");
				return new Date(0L);
			}
		}
	}
	
	protected final QueryHelper Q() {
		if (q == null) {
			throw new IllegalStateException();
		} else {
			return q;
		}
	}
	
	/**
	 * Only Used for development 	testing
	 * @param args
	 */
	protected final void test(String[] args) {
		if(args.length < 4){
			logger.error("Missing required authentication arguments, specify arguments as follows:");
			logger.error("username password CMSHostName AuthenticationType <Any Other Arguments>");
			logger.error("Example for Enterprise Authentication");
			logger.error("Administrator adminpassword MYSERVERNAME secEnterprise");
			return;
		}
		String user=args[0];
		String pass = args[1];
		String[] cms = {args[2]};		
		String auth = args[3];
		BOEHelper boe = new BOEHelper();
		String[] args2;
		if (args.length > 4) {
			int newSize = args.length - 4;
			args2 = new String[newSize];
			System.arraycopy(args, 4, args2, 0, newSize);
		} else {
			args2 = new String[0];
		}
		try {
			boe.logonNormal(user, pass, auth, cms, 0);
			this.run(boe.getSession(), boe.getInfoStore(), args2);			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			boe.logoff();
		}
	}
	
	protected abstract void runInternal(BOEHelper boe) throws Exception;
	
}
