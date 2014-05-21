package com.dft.boetools.programs;

import java.util.List;
import java.util.Properties;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.plugin.desktop.user.IMappedAttributes;
import com.crystaldecisions.sdk.plugin.desktop.user.IUser;

/**
 * Sets a user name and fullname based on the value stored in a custom mapped attribute.
 * @author roy.wells
 *
 */
public class UserNameCopier extends AbstractUserManipulatingProgram {

	private static final String CUSTOM_ATTRIBUTE = "CUSTOM_ATTRIBUTE";
	private static final String SET_FULLNAME = "SET_FULLNAME";
	private static final String SET_USERNAME = "SET_USERNAME";
		

	@Override
	protected void addDefaultArguments(Properties defaults) {
		super.addDefaultArguments(defaults);
		defaults.setProperty(CUSTOM_ATTRIBUTE, "SI_FULLNAME");
		defaults.setProperty(SET_FULLNAME, "true");
		defaults.setProperty(SET_USERNAME, "false");		
	}

	@Override
	protected void manipulateUser(IUser user) throws SDKException {
		String oldName = user.getTitle();
		String attributeKey = getArgument(CUSTOM_ATTRIBUTE);
		
		IMappedAttributes attributes = user.getCustomMappedAttributes();
		String newName = attributes.getAttribute(attributeKey);
		boolean setFullName = getBooleanArgument(SET_FULLNAME);
		boolean setUserName = getBooleanArgument(SET_USERNAME);
		
		if (newName != null && newName.length() > 0) {
			try {
				if (setUserName) {
					user.setTitle(newName);
					logger.info("Set User Name to " + newName + " from " + oldName);
				}
				
				if (setFullName) {
					user.setFullName(newName);
					logger.info("Set Full Name to " + newName + " from " + oldName);
				}
												
				user.save();
			} catch (Exception e){
				logger.warn("Could not set user name based on " + attributeKey + "for user " + oldName +  " due to error.");
				logger.warn(e);
			}
		} else {
			logger.warn("Could not set user name based on " + attributeKey + " for user " + oldName +  " due to empty or null Full Name.");
		}
	}
	
	public static void main(String[] args) {
		UserNameCopier prg = new UserNameCopier();
		prg.test(args);
	}

}
