package com.dft.boetools.programs;

import static com.dft.boetools.AliasHelper.secSAPR3;
import static com.dft.boetools.QueryHelper.GROUP_QUERY_PREFIX;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.plugin.desktop.user.IUser;
import com.crystaldecisions.sdk.plugin.desktop.user.IUserAlias;
import com.crystaldecisions.sdk.plugin.desktop.user.IUserAliases;
import com.dft.boetools.BOEHelper;
import com.dft.boetools.QueryHelper;
import com.dft.boetools.QueryHelper.InfoObjectWorker;


/**
 * AliasRemover
 * 
 * This class performs essentially the opposite function of AliasAdder.  It will
 * remove all aliases of a given type from all users who are members of a specified
 * Group.
 * 
 * To add this class to the CMC as a Program File, use the following as the 
 * class to run parameter.
 * 
 * com.dft.boetools.programs.AliasRemover
 * 
 * This class accepts the following arguments specified in the CMC per the standard
 * Program Job Object methods.  Remember to surround all Name=Value pairs in "". 
 * 
 * 1) GROUP_NAME
 *    The GROUP_NAME parameter should provide the string name of a group that exists
 *    in the repository.  Aliases will only be added to users who are a direct member
 *    of this group. To add aliases to all users run the job with "GROUP_NAME=Everyone"
 *    
 * 2) ALIAS_TYPE
 *    The ALIAS_TYPE parameter specifies what type of Business Objects Alias to remove
 *    from each user.  This parameter can be set to one or more of the following values,
 *    secEnterprise, secLDAP, secSAPR3.  To specify more than one active alias type 
 *    provide the multiple values separated by comma.  All aliases of these types
 *    will be removed.  
 *    
 * 3) DELETE_USER
 *    The DELETE_USER parameter accepts either True or False as a value.  If set to true
 *    and the alias being removed from a user is the last one the program will proceed
 *    to remove the alias.  This will result in the user being deleted.  The default for
 *    this value is false so that no user deletions will occur.
 *    
 * @author roy.wells
 *
 */
public class AliasRemover extends AbstractProgram {

	// Parameters
	private static final String GROUP_NAME = "GROUP_NAME";
	private static final String ALIAS_TYPE = "ALIAS_TYPE";
	private static final String DELETE_USER = "DELETE_USER";
	
	@Override
	protected void addDefaultArguments(Properties defaults) {
		super.addDefaultArguments(defaults);
		defaults.setProperty(DELETE_USER, "False");
	}

	@Override
	protected void addRequiredArguments(List<String> required) {
		super.addRequiredArguments(required);
		Collections.addAll(required, new String[] {GROUP_NAME, ALIAS_TYPE});
	}
	
	@Override
	protected void runInternal(BOEHelper boe) throws Exception {
		// Setup a query helper
		QueryHelper q = new QueryHelper(boe);
		
		// Step one, get the list of Users we will process
		String usersInGroupQry = GROUP_QUERY_PREFIX + getArgument(GROUP_NAME) + "/members[SI_GROUP_MEMBERS]@SI_ALIASES, SI_NAME, SI_ID";
				
		// get the ALIAS_TYPE list
		final List<String> aliasTypes = getListArgument(ALIAS_TYPE);
		
		// For each user ensure proper aliases are added.
		q.forEachResult(usersInGroupQry, new InfoObjectWorker() {
						
			public void doWork(IInfoObject o) throws Exception {
				logger.info("Removing Aliases for user " + o.getTitle());
				
				IUser user = (IUser) o;
				IUserAliases aliases = user.getAliases();
				try {
					for (String type : aliasTypes) {
						for (Iterator i = aliases.iterator(); i.hasNext();) {
							IUserAlias a = (IUserAlias) i.next();
							if (a.getAuthentication().equals(type)) {
								if (aliases.size() > 1) {
									i.remove();
									logger.info("Removed alias of type " + type + " from user " + user.getTitle());
								} else {
									logger.warn("Could not remove alias of type " + type + " because it was the last alias associated with user " + user.getTitle());
								}
							}
						}
					}
					user.save();
				} catch (SDKException e) {
					logger.error("Failed to remove aliases for user " + user.getTitle(),e);
				}
			}
		});
				
	}
	
	public static void main(String[] args) {
		AliasRemover a = new AliasRemover();
		a.test(new String[] {GROUP_NAME+"="+"Group", ALIAS_TYPE+"="+secSAPR3});
	}
}
