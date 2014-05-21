package com.dft.boetools.programs;

import java.util.Iterator;
import java.util.Set;

import com.crystaldecisions.sdk.plugin.desktop.user.IUser;
import com.dft.boetools.BOEHelper;

/**
 * There is no mechanism built into the CMC that allows for the mass setting of Web Intelligence Preferences
 * This class will attempt to resolve that by allowing users to specify preferences for user in batch.
 * 
 * This class extends AbstractUserManipulatingProgram see that class for additional
 * Parameters controlling which users are processed.
 * @see AbstractUserManipulatingProgram
 * 
 * Once we have selected a set of users to manipulate the preferences for, we will use the following
 * parameters to specify the desired parameter values for the users.  All of the following parameters 
 * are optional.  If not specified the particular preference will be left unaltered.
 * 
 * WEBI_VIEW_MODE:	This parameter specifies the type of viewer the user will use when viewing
 * 					Web Intelligence documents.  Allowed Values (WEB,RICH,DESKTOP,PDF)
 * 
 * WEBI_EDIT_MODE:	This parameter specifies the type of editor the user will use when modifying
 * 					Web Intelligence documents.  Allowed Values (WEB,RICH,DESKTOP)
 * 
 * WEBI_VIEW_LOCALE:This parameter specifies the method for determining the viewer locale at 
 * 					Runtime, either "Use the document locale to format the data" or "Use my preferred
 * 					viewing locale to format the Data".  Allowed Values (PREFERRED, DOCUMENT) 
 * 
 * WEBI_DRILL_PROMPT: This boolean parameter indicates the state of the "Prompt When drill requires additional
 * 					data" check box.  True indicates checked and false indicates unchecked.
 * 
 * WEBI_DRILL_SYNCH: This boolean parameter indicates the state of the "Synchronize drill on report blocks" check box.  
 * 					True indicates checked and false indicates unchecked.
 * 
 * WEBI_DRILL_HIDE: This boolean parameter indicates the state of the "Hide Drill Toolbar on startup" check box.  
 * 					True indicates checked and false indicates unchecked.
 * 
 * WEBI_DRILL_START:This parameter indicates the state of the "Start Drill Session" radio button.  
 * 					Allowed	Values (DUPLICATE, EXISTING).  Which equate to the respective radio button values
 * 
 * WEBI_EXCEL_PRIORITY:This parameter indicates the state of the "Select a priority for saving to MS Excel" radio button.  
 * 					Allowed	Values (FORMAT, DATA).  Which equate to the respective radio button values of emphasizing the data
 * 					formatting or emphasizing data processing.
 * 
 *   
 * @author roy.wells
 *
 */
public class WebiPreferenceSetter extends AbstractUserManipulatingProgram {

	
	private static final String WEBI_VIEW_MODE = "WEBI_VIEW_MODE";
	private static final String WEBI_EDIT_MODE = "WEBI_EDIT_MODE";
	private static final String WEBI_VIEW_LOCALE = "WEBI_VIEW_LOCALE";
	private static final String WEBI_DRILL_PROMPT = "WEBI_DRILL_PROMPT";
	private static final String WEBI_DRILL_SYNCH = "WEBI_DRILL_SYNCH";
	private static final String WEBI_DRILL_HIDE = "WEBI_DRILL_HIDE";
	private static final String WEBI_DRILL_START = "WEBI_DRILL_START";
	private static final String WEBI_EXCEL_PRIORITY = "WEBI_EXCEL_PRIORITY";	

	private static final String DOCUMENT_WIViewTechno = "DOCUMENT_WIViewTechno";
	private static final String DOCUMENT_WICreateTechno = "DOCUMENT_WICreateTechno";
	private static final String DOCUMENT_WIUCLUsage = "DOCUMENT_WIUCLUsage";
	private static final String DOCUMENT_WIPromptDrillOutScope = "DOCUMENT_WIPromptDrillOutScope";
	private static final String DOCUMENT_WISyncDrillBlocks = "DOCUMENT_WISyncDrillBlocks";
	private static final String DOCUMENT_WIDrillBar = "DOCUMENT_WIDrillBar"; 
	private static final String DOCUMENT_WIStartNewDrill = "DOCUMENT_WIStartNewDrill";
	private static final String DOCUMENT_WISaveAsXLSOptimized = "DOCUMENT_WISaveAsXLSOptimized";
	
	private static enum VIEW_MODE {WEB,RICH,DESKTOP,PDF};
	private static enum LOCALE_MODE {PREFERRED, DOCUMENT};
	private static enum DRILL_MODE {DUPLICATE, EXISTING};
	private static enum EXCEL_MODE {FORMAT, DATA};
	
	private static enum VIEW_MODE_INT {H,J,R,P};
	private static enum EDIT_MODE_INT {I,J,R};
	private static enum Y_N {Y, N};
	private static enum DRILL_MODE_INT {duplicate, existing};
	
	
	@Override
	protected void runInternal(BOEHelper boe) throws Exception {
		Set<Integer> userIds = getUserIDS();
		for (Iterator i = userIds.iterator(); i.hasNext();) {
			Integer userId = (Integer) i.next();
			IUser user = (IUser) Q().getObjectByID(userId.intValue());
			logger.debug("Setting Preferences for User :" + user.getTitle());
			setWebiViewMode(user);
			setWebiEditMode(user);
			setWebiLocale(user);
			setDrillPrompt(user);
			setDrillSync(user);
			setDrillHide(user);
			setDrillStart(user);
			setExcelMode(user);
			user.save();
		}
		
	}

	private void setExcelMode(IUser user) {
		String excelMode = getArgument(WEBI_EXCEL_PRIORITY);
		if (excelMode != null) {
			EXCEL_MODE em;
			try {
				em = EXCEL_MODE.valueOf(excelMode);
				switch(em) {
					case FORMAT:
						user.setProfileString(DOCUMENT_WISaveAsXLSOptimized, Y_N.N.toString());
						break;
					case DATA:
						user.setProfileString(DOCUMENT_WISaveAsXLSOptimized,  Y_N.Y.toString());					
						break;						
				}
			} catch (Exception e) {
				logger.debug("Unknown WEBI_EXCEL_PRIORITY specified.  Ignoring this value : " + excelMode);
			}				
		}
	}
	
	private void setDrillStart(IUser user) {
		String drillStart = getArgument(WEBI_DRILL_START);
		if (drillStart != null) {
			DRILL_MODE dm;
			try {
				dm = DRILL_MODE.valueOf(drillStart);
				switch(dm) {
					case DUPLICATE:
						user.setProfileString(DOCUMENT_WIStartNewDrill, DRILL_MODE_INT.duplicate.toString());
						break;
					case EXISTING:
						user.setProfileString(DOCUMENT_WIStartNewDrill,  DRILL_MODE_INT.existing.toString());					
						break;						
				}
			} catch (Exception e) {
				logger.debug("Unknown WEBI_DRILL_START specified.  Ignoring this value : " + drillStart);
			}				
		}
	}
	
	private void setDrillHide(IUser user) {
		String drillHide = getArgument(WEBI_DRILL_HIDE);
		if (drillHide != null) {			
			try {
				boolean drillHideVal = Boolean.valueOf(drillHide);
				user.setProfileString(DOCUMENT_WIDrillBar, (drillHideVal) ? Y_N.N.toString() : Y_N.Y.toString());
			} catch (Exception e) {
				logger.debug("Unknown WEBI_DRILL_HIDE specified.  Ignoring this value : " + drillHide);
			}				
		}
	}

	
	private void setDrillSync(IUser user) {
		String drillSynch = getArgument(WEBI_DRILL_SYNCH);
		if (drillSynch != null) {			
			try {
				boolean drillSyncVal = Boolean.valueOf(drillSynch);
				user.setProfileString(DOCUMENT_WISyncDrillBlocks, (drillSyncVal) ? Y_N.Y.toString() : Y_N.N.toString());
			} catch (Exception e) {
				logger.debug("Unknown WEBI_DRILL_SYNCH specified.  Ignoring this value : " + drillSynch);
			}				
		}
	}
	
	private void setDrillPrompt(IUser user) {
		String drillPrompt = getArgument(WEBI_DRILL_PROMPT);
		if (drillPrompt != null) {			
			try {
				boolean drillPromptVal = Boolean.valueOf(drillPrompt);
				user.setProfileString(DOCUMENT_WIPromptDrillOutScope, (drillPromptVal) ? Y_N.Y.toString() : Y_N.N.toString());
			} catch (Exception e) {
				logger.debug("Unknown WEBI_DRILL_PROMPT specified.  Ignoring this value : " + drillPrompt);
			}				
		}
	}
	
	private void setWebiLocale(IUser user) {
		String localMode = getArgument(WEBI_VIEW_LOCALE);
		if (localMode != null) {
			LOCALE_MODE lm;
			try {
				lm = LOCALE_MODE.valueOf(localMode);
				switch(lm) {
					case PREFERRED:
						user.setProfileString(DOCUMENT_WIUCLUsage, Boolean.TRUE.toString());
						break;
					case DOCUMENT:
						user.setProfileString(DOCUMENT_WIUCLUsage, Boolean.FALSE.toString());					
						break;						
				}
			} catch (Exception e) {
				logger.debug("Unknown WEBI_VIEW_LOCALE specified.  Ignoring this value : " + localMode);
			}				
		}
	}

	private void setWebiEditMode(IUser user) {
		String editMode = getArgument(WEBI_EDIT_MODE);
		if (editMode != null) {
			VIEW_MODE vm;
			try {
				vm = VIEW_MODE.valueOf(editMode);
				switch(vm) {
					case WEB:
						user.setProfileString(DOCUMENT_WICreateTechno, EDIT_MODE_INT.I.toString());
						break;
					case RICH:
						user.setProfileString(DOCUMENT_WICreateTechno, EDIT_MODE_INT.J.toString());					
						break;
					case DESKTOP:
						user.setProfileString(DOCUMENT_WICreateTechno, EDIT_MODE_INT.R.toString());
						break;
					case PDF:
						throw new Exception("Illegal Webi Edit Mode: Value Entered was PDF.");	
				}
			} catch (Exception e) {
				logger.debug("Unknown WEBI_EDIT_MODE specified.  Ignoring this value : " + editMode);
			}				
		}
	}

	private void setWebiViewMode(IUser user) {
		String webiViewMode = getArgument(WEBI_VIEW_MODE);
		if (webiViewMode != null) {
			VIEW_MODE vm;
			try {
				vm = VIEW_MODE.valueOf(webiViewMode);
				switch(vm) {
					case WEB:
						user.setProfileString(DOCUMENT_WIViewTechno, VIEW_MODE_INT.H.toString());
						break;
					case RICH:
						user.setProfileString(DOCUMENT_WIViewTechno, VIEW_MODE_INT.J.toString());					
						break;
					case DESKTOP:
						user.setProfileString(DOCUMENT_WIViewTechno, VIEW_MODE_INT.R.toString());
						break;
					case PDF:
						user.setProfileString(DOCUMENT_WIViewTechno, VIEW_MODE_INT.P.toString());
						break;								
				}
			} catch (Exception e) {
				logger.debug("Unknown WEBI_VIEW_MODE specified.  Ignoring this value : " + webiViewMode);
			}				
		}
	}

	public static void main(String[] args) {
		WebiPreferenceSetter a = new WebiPreferenceSetter();
		a.test(args);
	}
	
}
