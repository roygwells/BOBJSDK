package com.dft.boetools.programs;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.occa.infostore.CePropertyID;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.plugin.desktop.user.IUser;
import com.crystaldecisions.sdk.properties.IProperties;
import com.dft.boetools.BOEHelper;
import com.dft.boetools.QueryHelper;
import com.dft.boetools.StringHelper;

/**
 * This is a generic program that will do Something with a set of Users, like manipulate
 * their favorites or set their preferences.
 * 
 * Parameters....
 * 
 USER_GROUPS:	
 This parameter allows the administrator running the program to Specify the users
 who will be manipulated by groups.  The parameter value can be a comma separated list
 of User Group names.  Users who are direct members of one or more of these 
 groups will be affected (Note users must be direct members of the group).  
 This parameter has a default value of "Everyone".
  
 USERS:		
 This parameter allows the administrator running the program to specify a specific
 set of users who will be manipulated.  The parameter value can be a comma 
 separated list of User Names.  Users who are explicitly listed in this parameter 
 will be affected.  This parameter can be used in conjunction with
 the USER_GROUPS parameter.  If both are specified the Super Set of users between the 
 parameters will be affected.  I.E. You can specify a USER_GROUP
 and get all the users in that group plus you can specify several additional Users
 out side that group that will also be affected.
  
 EXCLUDED_USERS: 
 This parameter allows the administrator running the program to specify a specific
 set of users to exclude from the operation regardless of the other parameters values.
 The parameter value can be a comma separated list of User Names.  
 For example you can specify a group of users to include with USER_GROUPS and 
 omit a subset of users from that group by specifying those user names in this parameter
  
 NEW_USERS_ONLY: 
 This optional boolean parameter defaults to false.  If set to true only users who have
 a creation date that is more recent than the last runtime for this program will be included
 in the manipulation performed by this program.  If set to true you must also provide the additional
 parameter JOB_NAME which should be set to the name given to this program object in the CMC so that
 we can locate the most recent successful instance of the program.
  
 JOB_NAME:	
 This optional parameter is necessary if you set NEW_USERS_ONLY to true.  As described
 above, it should be set to the name of the program in the CMC so that the last Run Time can
 be determined.  If not specified a default value for the last runtime will be used.  This will
 be JAN 1st 1970.  This parameter is built into the base AbstractProgram getLastRuntime Method
  
 @author roy.wells
 
 */
public abstract class AbstractUserManipulatingProgram extends AbstractProgram {
	
	protected static final String USER_GROUPS = "USER_GROUPS";
	protected static final String USERS = "USERS";
	protected static final String EXCLUDED_USERS = "EXCLUDED_USERS";
	protected static final String NEW_USERS_ONLY = "NEW_USERS_ONLY";
	
	//protected static final String DEFAULT_USER_GROUP = "Everyone";

	@Override
	protected void addDefaultArguments(Properties defaults) {
		super.addDefaultArguments(defaults);
		defaults.setProperty(USER_GROUPS, "");
		defaults.setProperty(NEW_USERS_ONLY, Boolean.FALSE.toString());
	}	

		 
	@Override
	protected void runInternal(BOEHelper boe) throws Exception {
		Set<Integer> userIds = getUserIDS();
		
		logger.debug("USERS to be Processed: " + userIds);
		for (Integer id : userIds) {
			IUser user = (IUser) Q().getObjectByID(id);
			manipulateUser(user);
		}
	}
	
	protected void manipulateUser(IUser user) throws SDKException {}
	
	/**
	 * This method will use the three parameters USER_GROUPS, USERS, EXCLUDED_USERS to
	 * generate a list of User ID's that can then be processed to do whatever work
	 * this class is designed to do.
	 * @return
	 * @throws SDKException
	 */
	protected Set<Integer> getUserIDS() throws SDKException {
		String countSelect = "SELECT COUNT(SI_ID) ";
		String normalSelect = "SELECT TOP 1000 SI_ID ";
		String fromPfx = "FROM CI_SYSTEMOBJECTS WHERE children(\"si_name = 'usergroup-user'\", \"si_name = '";
		String fromSfx = "'\") AND SI_KIND='User'";
		String orderBy = " ORDER BY SI_ID";
		
		String userSelect = "SELECT SI_ID FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'User' AND SI_NAME IN ";
		String users = getArgument(USERS);
		if (users != null) {
			String[] userList = users.split(StringHelper.COMMA_SEPARATED_VALUES);
			userSelect += StringHelper.inClause(userList);
		}
		
		
		boolean newUsersOnly = getBooleanArgument(NEW_USERS_ONLY);
		String newUsersCondition = "";
		if (newUsersOnly) {
			Date lrt = getLastRuntime();
			String lrtUTC = QueryHelper.formatDateForBOEQuery(lrt, TimeZone.getDefault());			
			newUsersCondition += " AND SI_CREATION_TIME >= '" +lrtUTC+ "' ";		
			userSelect += newUsersCondition;
			fromSfx += newUsersCondition;
		}
		
		// Get Excluded Users
		String excluded = getArgument(EXCLUDED_USERS);
		if (excluded != null) {
			String[] excludeList = excluded.split(StringHelper.COMMA_SEPARATED_VALUES);
			String excludeInClause = " AND SI_NAME NOT IN " + StringHelper.inClause(excludeList);
			fromSfx += excludeInClause;
			userSelect += excludeInClause;
		}

		// Start with the User Groups.
		String userGroups = getArgument(USER_GROUPS);
		String[] userGroupsList = userGroups.split(StringHelper.COMMA_SEPARATED_VALUES);
		

		Set<Integer> userIDS = new HashSet<Integer>();
		
		
		// Iterate through the list of Groups and query for the users that are members of that group
		for (int i = 0; i < userGroupsList.length; i++) {
			String countQry = countSelect + fromPfx + userGroupsList[i] + fromSfx;
			IInfoObjects countResult = Q().executeRawQuery(countQry);
			IProperties aggCountProperties = (IProperties) ((IInfoObject) countResult.get(0)).properties().getProperty(CePropertyID.SI_AGGREGATE_COUNT).getValue();
			int count = ((Integer) aggCountProperties.getProperty(CePropertyID.SI_ID).getValue()).intValue();
			int retrievedCount = 0;
			int maxID = 0;
			
			// Deal with teh possibility that the number of users in the group could be huge and should not be retrieved in a single select statement
			while (retrievedCount < count) {
				String selectQry = normalSelect + fromPfx + userGroupsList[i] + fromSfx + " AND SI_ID > " + maxID + orderBy;
				IInfoObjects results = Q().executeRawQuery(selectQry);
				for(Iterator iter = results.iterator(); iter.hasNext();) {
					IInfoObject o = (IInfoObject) iter.next();
					int id = o.getID();
					maxID = id;
					userIDS.add(new Integer(id));
					retrievedCount++;
				}				
			}	
		}
		
		// Get Explicit Users
		if (users != null) {
			IInfoObjects explicitUsers = Q().executeRawQuery(userSelect);
			for (Iterator itr = explicitUsers.iterator(); itr.hasNext();) {
				IInfoObject u = (IInfoObject) itr.next();
				userIDS.add(Integer.valueOf(u.getID()));
			}
		}
		
		return userIDS;
	}
}
