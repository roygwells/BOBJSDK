package com.dft.boetools.programs;

import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.dft.boetools.BOEHelper;
import com.dft.boetools.QueryHelper;
import com.dft.boetools.QueryHelper.InfoObjectWorker;

public class KeyWordSetter extends AbstractProgram {

	private static final String qryStr = "query://{SELECT SI_NAME, SI_KIND, SI_KEYWORD FROM CI_INFOOBJECTS WHERE SI_KIND='FavoritesFolder' AND SI_KEYWORD IS NULL}";
	
	
	@Override
	protected void runInternal(BOEHelper boe) throws Exception {
		QueryHelper q = new QueryHelper(boe);
		q.forEachResult(qryStr, new InfoObjectWorker() {
			
			public void doWork(IInfoObject o) throws Exception {
				logger.debug("Setting Keyword = " + o.getTitle());
				o.setKeyword(o.getTitle());
				o.save();			
			}
		});
	}

	public static void main(String[] args) {
		KeyWordSetter prg = new KeyWordSetter();
		prg.test(args);
	}

}
