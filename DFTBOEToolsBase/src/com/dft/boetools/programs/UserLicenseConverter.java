package com.dft.boetools.programs;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.plugin.desktop.user.IUser;
import com.dft.boetools.BOEHelper;
import com.dft.boetools.QueryHelper;
import com.dft.boetools.QueryHelper.InfoObjectWorker;


/**
 * This class facilitates converting large populations of users between
 * Named and Concurrent license models.  There is no default User Interface 
 * that allows for this type of mass conversion.  It's important to note that
 * this process must open, modify and save each User object individually and 
 * therefore may run for a considerable amount of time.
 * 
 * This class supports the following parameters...
 * 
 * CONVERSION_TYPE:  This parameter allows you to specify the target license
 * type.  Allowed values are "N" or "C", where "N" represents converting all
 * users to Named user license and "C" represents converting all users to 
 * Concurrent user license. This parameter has a default value of "C" and is
 * therefore optional at runtime if Concurrent Users are your desired outcome.
 * 
 * EXCLUDE_USERS:  This parameter allows you to exclude a small subset of users
 * from the conversion process.  It accepts a comma separated list of user names.
 * As each user is inspected for conversion they will be compared against this list, 
 * and if found in the list they will be skipped during the conversion process.  This
 * parameter is optional and will be ignored if not specified.
 *  
 * @author roy.wells
 *
 */
public class UserLicenseConverter extends AbstractProgram {
	
	private static final String CONVERSION_TYPE = "CONVERSION_TYPE";
	private static final String EXCLUDE_USERS = "EXCLUDE_USERS";
	
	@Override
	protected Properties getDefaultArguments() {
		Properties arguments = new Properties();
		arguments.setProperty(CONVERSION_TYPE, "C");
		arguments.setProperty(EXCLUDE_USERS, "");
		return arguments;
	}
	
	@Override
	protected void runInternal(BOEHelper boe) throws Exception {
		QueryHelper q = new QueryHelper(boe);
		
		final boolean convertToConcurrent = getArgument(CONVERSION_TYPE).equalsIgnoreCase("C");
		
		final Set<String> excludedUsers = getCollectionArgument(new HashSet<String>(), EXCLUDE_USERS);
		
		String query = "path://SystemObjects/Users/@*";
		q.forEachResult(query, new InfoObjectWorker() {
			
			public void doWork(IInfoObject o) throws Exception {
				IUser u = (IUser) o;
				if (excludedUsers.contains(u.getTitle())) {
					logger.info("User: " + u.getTitle() + " is being excluded from conversion.  Moving On.");
					return;
				}
				logger.info("Setting License Type for User: " + u.getTitle() + " to " + ((convertToConcurrent) ? " Concurrent" : " Named"));
				u.setConnection((convertToConcurrent) ? IUser.CONCURRENT : IUser.NAMED);
				u.save();
			}
		});
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		UserLicenseConverter a = new UserLicenseConverter();
		a.test(args);
	}

}
