package com.dft.boetools.programs;

import static com.dft.boetools.AliasHelper.secEnterprise;
import static com.dft.boetools.AliasHelper.secLDAP;
import static com.dft.boetools.AliasHelper.secSAPR3;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.plugin.desktop.user.IUser;
import com.dft.boetools.AliasHelper;
import com.dft.boetools.BOEHelper;

/**
 * AliasAdder
 * 
 * This class adds additional aliases to existing users in a BOE Repository.
 * The purpose is to ensure that every user has an alias of a given type or types.
 * This can be useful when doing large system migrations, where it may be necessary 
 * to purge aliases of a particular type and then put them back again.   Since a user
 * with no aliases will be deleted and with them their documents it is sometimes 
 * necessary to add temporary aliases to accounts.  Thus the existence of this 
 * script.
 * 
 * To add this class to the CMC as a Program File, use the following as the 
 * class to run parameter.
 * 
 * com.dft.boetools.programs.AliasAdder
 * 
 * This class accepts the following arguments specified in the CMC per the standard
 * Program Job Object methods.  Remember to surround all Name=Value pairs in "". 
 * 
 * @see AbstractUserManipulatingProgram
 * 
 * 2) ALIAS_TYPE
 *    The ALIAS_TYPE parameter specifies what type of Business Objects Alias to add
 *    to each user.  This parameter can be set to one or more of the following values,
 *    secEnterprise, secLDAP, secSAPR3.  To specify more than one active alias type 
 *    provide the multiple values separated by comma.  If specifying multiple values
 *    and using the SINGLE_ALIAS=True parameter then specific the aliases in the order
 *    of priority.  I.E. if you want to attempt to add an LDAP Alias and then only
 *    add an Enterprise Alias if the LDAP Alias fails then set "ALIAS_TYPE=secLDAP,secEnterprise"
 *    Default value is secEnterprise.
 * 
 * 3) SINGLE_ALIAS
 *    The SINGLE_ALIAS parameter alters the behavior of the system when multiple aliases
 *    types are specified in ALIAS_TYPE.  By default one of each type will be added to 
 *    each user account with logging of any failures.  When set to true the program will
 *    attempt each alias type and as soon as it gets a single success will stop processing 
 *    any additional alias types for that user.
 *    
 * 4) PASSWORD
 *    If ALIAS_TYPE includes secEnterprise you must specify a password generation policy
 *    for the Enterprise Accounts.  Values can be RANDOM, or any other value which will
 *    be treated as a single hard coded value to be used as the password for all enterprise
 *    aliases.  The default value is RANDOM.
 * 
 * 5) SAP_SYSTEM
 *    IF ALIAS_TYPE includes secSAPR3 you must specify an SAP System to add users aliases
 *    from.  This should be in the format of SYSID~CLNT# for example BWD~100.  
 *    
 * @author roy.wells
 *
 */
public class AliasAdder extends AbstractUserManipulatingProgram {
	
	// Parameters
	private static final String ALIAS_TYPE = "ALIAS_TYPE";
	private static final String SINGLE_ALIAS = "SINGLE_ALIAS";
	private static final String PASSWORD = "PASSWORD";
	private static final String SAP_SYSTEM = "SAP_SYSTEM";
	
	// Parameter Values
	private static final String RANDOM = "RANDOM";
	
	@Override
	protected void addRequiredArguments(List<String> required) {
		super.addRequiredArguments(required);
	}
	
	@Override
	protected void addDefaultArguments(Properties defaults) {
		super.addDefaultArguments(defaults);
		defaults.setProperty(ALIAS_TYPE, secEnterprise);
		defaults.setProperty(SINGLE_ALIAS, "False");
		defaults.setProperty(PASSWORD, RANDOM);	
		defaults.setProperty(SAP_SYSTEM, "");
	}

	@Override
	protected void runInternal(BOEHelper boe) throws Exception {
	
		// get the ALIAS_TYPE list
		final List<String> aliasTypes = getListArgument(ALIAS_TYPE);
		
		// Get SINGLE_ALIAS Flag
		final boolean isSingleAlias = getBooleanArgument(SINGLE_ALIAS);
		
		// Get password settings for enterprise Aliases
		final String password = getArgument(PASSWORD);
		final boolean passwordIsRandom = password.equalsIgnoreCase(RANDOM);
		
		// Get SAP System ID
		final String sapSystem = getArgument(SAP_SYSTEM);
		
		// For each user ensure proper aliases are added.
		Set<Integer> userIDS = getUserIDS();
		for (Integer userId : userIDS) {
			IUser user = (IUser) Q().getObjectByID(userId);

			logger.info("Start Processing user " + user.getTitle());
			
			boolean aliasExists = false;
			boolean aliasAdded = false;
			
			for (String type : aliasTypes) {
				try{
					if (type.equals(secEnterprise)) {
						String pword = passwordIsRandom ? AliasHelper.generateRandomPassword() : password;
						if (! AliasHelper.userHasAlias(user, secEnterprise)) {
							AliasHelper.addEnterpriseAlias(user, user.getTitle(), pword);
							logger.info("	Added alias of type " + type + " to user " + user.getTitle());
							aliasAdded = true;
						} else {
							logger.info("	Alias of type " + type + " already exists");
							aliasExists = true;
						}
					} else if (type.equals(secLDAP)) {
						if (! AliasHelper.userHasAlias(user, secLDAP)) {
							AliasHelper.addLDAPAlias(user, user.getTitle());
							logger.info("	Added alias of type " + type + " to user " + user.getTitle());
							aliasAdded = true;
						} else {
							logger.info("	Alias of type " + type + " already exists");
							aliasExists = true;
						}
					} else if (type.equals(secSAPR3)) {
						String name = sapSystem + "/" + user.getTitle();
						if (! AliasHelper.userHasAlias(user, secSAPR3)) {
							AliasHelper.addSAPAlias(user, name);
							logger.info("	Added alias of type " + type + " to user " + user.getTitle());
							aliasAdded = true;
						} else {
							logger.info("	Alias of type " + type + " already exists");
							aliasExists = true;
						}
					} else {
						logger.warn("	Unknown Alias type " + type + " ignored");
					}
					
					if (isSingleAlias && (aliasAdded || aliasExists)) break;
					
				} catch (SDKException e) {
					logger.warn("	FAILED! to create alias of type " + type + " for user " + user.getTitle() + " : Reason for Failure was " + e.getMessage().trim());
				}
			}
			
			logger.info("Finished Processing user " + user.getTitle() + ((aliasAdded) ? "" : " : No Aliases Added for this user"));		
		}
		
	}
	

	public static void main(String[] args) {
		AliasAdder a = new AliasAdder();
		a.test(args);
	}
}
