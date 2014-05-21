package com.dft.boetools.programs;

import java.util.List;

import com.crystaldecisions.sdk.occa.infostore.CePropertyID;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.dft.boetools.BOEHelper;
import com.dft.boetools.QueryHelper;
import com.dft.boetools.QueryHelper.InfoObjectWorker;

/**
 * PARAMETERS
 * 
 * QUERY:	The query to pass to the SDK
 * 
 * @author roy.wells
 *
 */
public class QueryTester extends AbstractProgram {

	private static final String QUERY = "QUERY";
	
	
	
	@Override
	protected void addRequiredArguments(List<String> required) {
		super.addRequiredArguments(required);
		required.add(QUERY);
	}

	@Override
	protected void runInternal(BOEHelper boe) throws Exception {
		//String query = getArgument(QUERY);
		String query="path://InfoObjects/Inboxes/roy.wells/";
		QueryHelper q = new QueryHelper(boe);
		logger.debug("Executing Query: " + query);
		logger.debug("SI_ID, SI_CUID, SI_NAME");
		logger.debug("Other Properties");
		q.forEachResult(query, new InfoObjectWorker() {
			
			public void doWork(IInfoObject o) throws Exception {
				logger.debug(o.getID() + ", " + o.getCUID() + ", " + o.getTitle() + " : " + o.properties());
				logger.debug(o.properties().getDate(CePropertyID.SI_CREATION_TIME));
			}
		});
	}
	
	public static void main(String[] args) {
		QueryTester a = new QueryTester();
		a.test(args);
	}
}
