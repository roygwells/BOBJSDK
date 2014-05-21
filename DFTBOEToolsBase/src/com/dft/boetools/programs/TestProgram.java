package com.dft.boetools.programs;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.crystaldecisions.sdk.plugin.desktop.user.IUser;
import com.dft.boetools.BOEHelper;

/**
 * This class simply tests the AbstractProgram startup, shutdown, and logging code.
 * @author roy.wells
 */
public class TestProgram extends AbstractProgram{

	@Override
	protected void runInternal(BOEHelper boe) throws Exception {
		logger.debug("Executing TestProgram runInternal");

		Date now = new Date();
		
		TimeZone here = TimeZone.getDefault();
		int offset = here.getOffset(now.getTime());
		logger.debug("Offset = " + offset);
		
		Date utcDate = new Date(now.getTime() - offset);
		
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("yyyy.MM.dd.HH.mm.ss");
		
		
		logger.debug("Current Time in Local Time Zone. " + sdf.format(now));
		
		logger.debug("Current Time in UTC Time Zone " + sdf.format(utcDate));
		
		
		IUser roy = (IUser) Q().getObjectByID(57355, "SI_ID, SI_NAME, SI_DATA, SI_UPDATE_TS");
		
		Date royUpdate = roy.getUpdateTimeStamp();
		
		logger.debug("Roy Update Date Raw = " + sdf.format(royUpdate));
		
		logger.debug("Profile values for Roy");
		
		roy.setProfileString("DOCUMENT_WIViewTechno", "P");
		
		roy.save();
		
		logger.debug("DOCUMENT_WIViewTechno = " + roy.getProfileString("DOCUMENT_WIViewTechno"));
		
		
		Thread.sleep(1000*60*5);
		
	}

	public static void main(String[] args) {
		TestProgram a = new TestProgram();
		a.test(args);
	}
}
