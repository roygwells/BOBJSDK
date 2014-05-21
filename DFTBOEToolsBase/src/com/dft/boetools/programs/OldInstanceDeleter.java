package com.dft.boetools.programs;

import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.dft.boetools.BOEHelper;
import com.dft.boetools.QueryHelper;
import com.dft.boetools.QueryHelper.InfoObjectWorker;

public class OldInstanceDeleter extends AbstractProgram {

	@Override
	protected void runInternal(BOEHelper boe) throws Exception {
		final String oldInstQry="query://{Select SI_ID, SI_NAME, SI_CREATION_TIME, SI_INSTANCE From CI_INFOOBJECTS Where SI_CREATION_TIME <= '2012/01/01/01/00/00' AND SI_INSTANCE = 1}";
		final QueryHelper q = new QueryHelper(boe);
		
		q.forEachResult(oldInstQry, new InfoObjectWorker() {

			public void doWork(IInfoObject o) throws Exception {
				logger.debug("Deleteing Object: "	+ o.getTitle() + "  " + o.properties().getProperty("SI_CREATION_TIME"));
				o.deleteNow();
			}
		});

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		OldInstanceDeleter a = new OldInstanceDeleter();
		a.test(args);
	}

}
