package com.dft.boetools.programs;

import java.util.Date;
import java.util.Properties;

import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.plugin.desktop.connection.IConnection;
import com.dft.boetools.BOEHelper;
import com.dft.boetools.QueryHelper;

/**
 * This class will automatically kill running enterprise sessions that are older
 * than a specified MAX_SESSION_HOURS variable.  This is intended as a stop gap measure
 * to deal with the fact that the BOE Platform has issues with session management and 
 * occasionally leaves sessions open even though the user has logged out a long time ago.
 * 
 * Parameters...
 * 
 * MAX_SESSION_HOURS: The length of time in hours used to determine that a session is no longer valid. 
 * Default value is 24.
 *
 * This class can also be used as a library assuming the client code configures it correctly.  
 * @author roy.wells
 *
 */
public class SessionKiller extends AbstractProgram {

	
	private static final String MAX_SESSION_HOURS = "MAX_SESSION_HOURS";
	private static final double MILS_PER_HOUR = 1000 * 60 * 60;
	
	@Override
	protected void addDefaultArguments(Properties defaults) {
		super.addDefaultArguments(defaults);
		defaults.setProperty(MAX_SESSION_HOURS, "24");
	}


	public IInfoObjects getListOfSessions(QueryHelper q) throws Exception {
		return q.executeRawQuery(
				"Select * From CI_SYSTEMOBJECTS " +
				"Where SI_KIND='Connection' And SI_PARENT_FOLDER=41 " +
				"And SI_AUTHEN_METHOD!='server-token' " +
				"order by SI_NAME");
	}
	
	public double getSessionDurration(IConnection session) throws Exception {
		Date lastLogon = session.getLastLogon();
		return (System.currentTimeMillis() - lastLogon.getTime()) / MILS_PER_HOUR;
	}
	
	@Override
	protected void runInternal(BOEHelper boe) throws Exception {
		QueryHelper q = new QueryHelper(boe);
		
		int maxHours = getIntArgument(MAX_SESSION_HOURS);
		
		IInfoObjects listOfSessions = getListOfSessions(q);

		for (Object o : listOfSessions) {
			IConnection session = (IConnection) o;			
		
			logger.info("Found Session for " + session.getCMSName() + " " + session.getAuthenticationMethod() + " " + session.getUserAlias() + " " + session.getLastLogon());
			
			double durration = getSessionDurration(session);
			
			logger.info("Session Duration in Hours = " + durration);
			
			if (durration > maxHours) {
				logger.info("Current Session exceeds the maximum allowed duration.  Therefore session is being terminated");
				session.deleteNow();
			}
		}
		
	}
	
	public static void main(String[] args) {
		SessionKiller a = new SessionKiller();
		a.test(args);
	}

}
