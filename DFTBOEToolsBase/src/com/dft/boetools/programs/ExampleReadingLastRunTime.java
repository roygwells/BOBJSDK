package com.dft.boetools.programs;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.crystaldecisions.sdk.occa.infostore.CePropertyID;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.dft.boetools.BOEHelper;
import com.dft.boetools.StringHelper;

public class ExampleReadingLastRunTime extends AbstractProgram{


	private static final String JOB_NAME = "JOB_NAME";
	private static final String CHECK_LAST_RUN = "CHECK_LAST_RUN";
	
	@Override
	protected void addRequiredArguments(List<String> required) {
		super.addRequiredArguments(required);
		required.add(JOB_NAME);
	}

	
	@Override
	protected void addDefaultArguments(Properties defaults) {
		super.addDefaultArguments(defaults);
		defaults.setProperty(CHECK_LAST_RUN, Boolean.TRUE.toString());
	}

	
	//	Determine Last Run Time
	@Override
	protected void runInternal(BOEHelper boe) throws Exception {
				
		//	Read startup options from job configuration
		final boolean checkLastRun = getBooleanArgument(CHECK_LAST_RUN);
		final String jobName = getArgument(JOB_NAME);
		
		Date lastRunTime = null;
		if (checkLastRun) {
			String jobHistoryQuery = "SELECT TOP 1 SI_ID, SI_NAME, SI_STARTTIME FROM CI_INFOOBJECTS WHERE SI_NAME = '" + StringHelper.escQteBOE(jobName) + "' AND SI_INSTANCE = 1 ORDER BY SI_STARTTIME DESC";
			IInfoObjects historyList = Q().executeRawQuery(jobHistoryQuery);
			
			if (historyList.size() > 0) {
				IInfoObject instance = (IInfoObject) historyList.get(0);
				lastRunTime = instance.properties().getDate(CePropertyID.SI_STARTTIME);
				
				logger.debug("Last Run Time = " + lastRunTime);
				logger.debug("Current Date = " + new Date());
			}
		}		
	}
	


}
