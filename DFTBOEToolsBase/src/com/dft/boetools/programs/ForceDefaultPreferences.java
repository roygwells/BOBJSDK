package com.dft.boetools.programs;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.plugin.desktop.user.IUser;
import com.crystaldecisions.sdk.properties.IProperties;

public class ForceDefaultPreferences extends AbstractUserManipulatingProgram {

	@Override
	protected void manipulateUser(IUser user) throws SDKException {
		IProperties props = user.properties();
		//logger.debug(props);
		
		IProperties data = props.getProperties("SI_DATA");
		//logger.debug(data);
		
		data.remove("SI_IV_PREFERENCES");
		
		//logger.debug(data);

		user.save();
		
		logger.info("Forced Preferences for User : " + user.getTitle());
	}

	public static void main(String[] args) {
		ForceDefaultPreferences prg = new ForceDefaultPreferences();
		prg.test(args);
	}

}
