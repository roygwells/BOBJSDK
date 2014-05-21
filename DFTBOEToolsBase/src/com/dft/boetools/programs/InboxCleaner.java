package com.dft.boetools.programs;

import java.util.Properties;

import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.dft.boetools.BOEHelper;
import com.dft.boetools.QueryHelper;
import com.dft.boetools.QueryHelper.InfoObjectWorker;
import com.dft.boetools.StringHelper;

public class InboxCleaner extends AbstractProgram {
	
	protected static final String INBOX_LIMIT = "INBOX_LIMIT";
	
	@Override
	protected void addDefaultArguments(Properties defaults) {
		super.addDefaultArguments(defaults);
		defaults.setProperty(INBOX_LIMIT, "50");
	}	
	
	@Override
	protected void runInternal(final BOEHelper boe) throws Exception {
		
		final String inboxQry="path://InfoObjects/Inboxes/";
		final QueryHelper q = new QueryHelper(boe);
		final int inboxLimit = getIntArgument(INBOX_LIMIT);
		
		q.forEachResult(inboxQry, new InfoObjectWorker() {

			public void doWork(IInfoObject o) throws Exception {
				logger.debug("Processing Inbox "	+ o.getTitle());
				final IInfoObjects deletedObjs = q.newInfoObjectsCollection();
							
				// Get all the children of this inbox.
				q.forEachResult(inboxQry +   StringHelper.escQteBOE(o.getTitle()) + "/?OrderBy=SI_CREATION_TIME DESC", new InfoObjectWorker() {
					
					private int inboxItemCount = 0;
					
					public void doWork(IInfoObject o) throws Exception {
						if (inboxItemCount >= inboxLimit) {
							logger.debug("	Deleting item " + inboxItemCount + " named: " + o.getTitle());	
							// Collect Objects for later deletion so as not to messup paginated query and skip over objects
							deletedObjs.add(o);
						}
						inboxItemCount++;
					}
				});
				
				// Do the actual deletion
				for (Object next : deletedObjs) { ((IInfoObject) next).deleteNow(); }
			}
		});

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		InboxCleaner a = new InboxCleaner();
		a.test(args);
	}

}
