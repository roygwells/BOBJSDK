package com.dft.boetools.programs;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.occa.infostore.CePropertyID;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.ISchedulable;
import com.crystaldecisions.sdk.plugin.desktop.folder.IFolder;
import com.crystaldecisions.sdk.plugin.desktop.user.IUser;
import com.dft.boetools.BOEHelper;
import com.dft.boetools.StringHelper;

/**
 * The purpose of this class is to support migrations of user content between XIR2 
 * and later versions of the Business Objects Suite.  When users are copied to a 
 * new system, ideally their CUID's are preserved and their user content will migrate
 * correctly.
 * 
 * However if the migration of users is not managed carefully CUID's of user accounts
 * can get out of sync between environments and the user content will then not import 
 * correctly.  This class aims to solve this latter scenario.
 * 
 * The proposed solution is to copy user content from it's normal location in Personal 
 * Documents folders into a set of public document folders.  The public folders will all
 * reside under a specified top level public folder.  For each user who has content being
 * copied we will create a sub folder with the same name as the user account.  Migration 
 * can then proceed to move the new public folders into the target environment and the 
 * content can be moved back into personal documents folders based on finding the account
 * with the matching name.  
 * 
 * This Class Specifically handles the process of moving content back from the public 
 * folders into the target user personal folders after it has been migrated to thew
 * new 4.0 environment. 
 * 
 * Control over the copying process will be provided by a number of input parameters.
 * 
 * This class extends AbstractUserManipulatingProgram see that class for additional
 * Parameters controlling which users are processed.
 * @see AbstractUserManipulatingProgram
 * 
 * ROOT_FOLDER: This parameter defines the name of the root folder under "Public Folders" 
 * 				where each users content will be placed.  This should in theory be a folder
 * 				created by an Administrator with Security set so that only an Administrator
 * 				can access the content.  Otherwise personal document content might be made
 * 				available to other users.  If not specified this parameter has a default value
 * 				of "USER_CONTENT".  If the folder does not exist at run time an error will
 * 				be thrown and the program will exit having not copied any content.
 * 
 * 
 * DELETE_EXISTING:  This boolean parameter tells the program what to do if existing content is already in the
 * 				target folders as there would be if the program had already been run once before.  The 
 * 				default value is true, which means any existing content will be replaced with the source material
 * 				that is being copied.  Setting the parameter to false will cause the program to simply skip 
 * 				over content if there is already matching content in the target folder.
 * 
 * MOVE_BY_COPY: This boolean parameter tells the program how to move the content from the source folder
 * 				to the target folder.  The default value for this parameter is True.  This means the content
 * 				will by copied from the source to the target, thus leaving the source untouched.  Setting
 * 				the parameter to false will cause content to be "Moved" not "Copied".  There for no objects
 * 				will remain in the source after the operation is complete.  Generally you would use
 * 				the default parameter when running the program against users personal folders in the source
 * 				system and then toggle the parameter to false when running the program in the new environment.
 * 
 * @author roy.wells
 *
 */
public class UserContentCopierReverse extends AbstractUserManipulatingProgram {

	private static final String ROOT_FOLDER = "ROOT_FOLDER";
	private static final String DELETE_EXISTING = "DELETE_EXISTING";
	private static final String MOVE_BY_COPY = "MOVE_BY_COPY";
	
	private static final String DEFAULT_ROOT = "USER_CONTENT";
	
	
	@Override
	protected void addDefaultArguments(Properties defaults) {
		super.addDefaultArguments(defaults);
		defaults.setProperty(ROOT_FOLDER, DEFAULT_ROOT);
		defaults.setProperty(DELETE_EXISTING, Boolean.TRUE.toString());
		defaults.setProperty(MOVE_BY_COPY, Boolean.TRUE.toString());		
	
	}

	@Override
	protected void addRequiredArguments(List<String> required) {
		super.addRequiredArguments(required);
		required.add(ROOT_FOLDER);
	}

	protected void runInternal(BOEHelper boe) throws Exception {
		// Get the root folder to copy content From.
		IInfoObject rootFolder = getRootFolder();
		IInfoObject favFolderRoot = getFavFolderRoot();
		
		// Get the list of Users to retrieve content for.
		Set userIds = getUserIDS();
		
		// get DELETE_EXISTING parameter
		boolean deleteExisting = getBooleanArgument(DELETE_EXISTING);
		boolean doCopy = getBooleanArgument(MOVE_BY_COPY);
		
		// For Every User locate the public folder holding their migrated documents, and copy all the content in it 
		// to their personal folder
		for (Iterator i = userIds.iterator(); i.hasNext();) {
			Integer userId = (Integer) i.next();
			IUser user = (IUser) Q().getObjectByID(userId.intValue());
			IInfoObject favFolderCopy = Q().getObjectByQuery("Select * from CI_INFOOBJECTS WHERE SI_KIND = 'Folder' AND SI_NAME = '" + StringHelper.escQteBOE(user.getTitle()) + "' AND SI_PARENTID = " + rootFolder.getID(),true);
			if (favFolderCopy == null) {
				logger.debug("Could not find migrated content for User " + user.getTitle());
				continue;
			}
			logger.debug("Moving content for user " + user.getTitle());
			copyFolderToTarget(favFolderCopy, favFolderRoot, deleteExisting, user, doCopy);				
		}
		
		
	}
	
	private IInfoObject getRootFolder() throws Exception{
		String rootFolderName = getArgument(ROOT_FOLDER);
		
		IInfoObject rootFolder = Q().getObjectByName(rootFolderName, IFolder.FOLDER_KIND);
		if (rootFolder == null) {
			throw new IllegalArgumentException("ROOT_FOLDER must specify an actual public folder that exists in the repository");
		}
		
		return rootFolder;
	}
	
	private IInfoObject getFavFolderRoot() throws Exception {
		return Q().getObjectByQuery("path://InfoObjects/User Folders", false);
	}

	
	
	
	
	private void copyFolderToTarget(IInfoObject folderToCopy, IInfoObject targetRoot, boolean deleteExisting, IUser user, boolean doCopy) throws SDKException{
		// Check for existence of target folder, create if not there.
		IInfoObject targetFolder = Q().getObjectByQuery("SELECT * FROM CI_INFOOBJECTS WHERE SI_NAME = '" + StringHelper.escQteBOE(folderToCopy.getTitle()) + "' AND SI_PARENTID = " + targetRoot.getID(),true);
		
		if (targetFolder == null) {
			//Create Target Folder
			IInfoObjects newFolders =Q().newInfoObjectsCollection();
			targetFolder = newFolders.add(IFolder.FOLDER_KIND);
			targetFolder.setTitle(folderToCopy.getTitle());
			targetFolder.setParentID(targetRoot.getID());
			targetFolder.properties().getProperty(CePropertyID.SI_OWNERID).setValue(Integer.valueOf(user.getID()));
			try {
				targetFolder.save();
			} catch (Exception e) {
				// if this save fails for some reason  we need to log it and bail out of this copy operation
				// but we don't want to stop the whole process by throwing the error up stream.
				logger.error("Problem copying folder " + targetFolder.getTitle() + " for user " + user.getTitle() + " : " + e.getMessage());
				return;
			}
		}
		
		IInfoObjects objectsToCopy = Q().executeRawQuery("SELECT * FROM CI_INFOOBJECTS WHERE SI_PARENTID = " + folderToCopy.getID());
		for (Iterator i = objectsToCopy.iterator(); i.hasNext();) {
			IInfoObject o = (IInfoObject) i.next();
			if (o.getKind().equals(IFolder.FOLDER_KIND)) {
				copyFolderToTarget(o, targetFolder, deleteExisting, user, doCopy);
			} else {
				
				// Check for existing.
				IInfoObject existing = Q().getObjectByQuery("SELECT SI_ID FROM CI_INFOOBJECTS WHERE SI_PARENTID = " + targetFolder.getID() +" and SI_NAME = '" + StringHelper.escQteBOE(o.getTitle()) +"'",true);
				if (existing != null && deleteExisting) {
					existing.deleteNow();
					if (doCopy) {
						doCopy(o, targetFolder, user);
					} else {
						doMove(o, targetFolder, user);
					}
				} else if (existing == null) {
					if (doCopy) {
						doCopy(o, targetFolder, user);	
					}else {
						doMove(o, targetFolder, user);
					}
					
				} else {
					// Do Nothing as object exists in target and we don't want to delete it.
					logger.debug("Skipping object " + o.getTitle() + " as object with same name already exists in target");
				}				
			}			
		}
	}

	private void doCopy(IInfoObject o, IInfoObject targetFolder, IUser user) throws SDKException {
		IInfoObject copy = Q().copyObject(o);
		copy.setParentID(targetFolder.getID());
		copy.properties().getProperty(CePropertyID.SI_OWNERID).setValue(Integer.valueOf(user.getID()));
		try {
			copy.save();
		} catch (Exception e) {
			// if this save fails for some reason  we need to log it and bail out of this copy operation
			// but we don't want to stop the whole process by throwing the error up stream.
			logger.error("Problem copying object ID:" + o.getID() + " " + copy.getKind() + ":\"" + copy.getTitle() + "\" for user " + user.getTitle() + " : " + e.getMessage());
			return;
		}
		
		// Deal with Instances
		if (o instanceof ISchedulable) {
			IInfoObjects instances = Q().executeRawQuery("SELECT * FROM CI_INFOOBJECTS WHERE SI_INSTANCE=1 AND SI_PARENTID=" + o.getID());
			for (Iterator i = instances.iterator(); i.hasNext();) {
				IInfoObject inst = (IInfoObject) i.next();
				// Don't Mess with Recurring Schedules
				if (inst.getSchedulingInfo().getStatus() != 9) {
					IInfoObject instCopy = Q().copyObject(inst);
					instCopy.setParentID(copy.getID());
					instCopy.properties().getProperty(CePropertyID.SI_OWNERID).setValue(Integer.valueOf(user.getID()));
					try {
						instCopy.save();
					} catch (Exception e) {
						// if this save fails for some reason  we need to log it and bail out of this copy operation
						// but we don't want to stop the whole process by throwing the error up stream.
						logger.error("Problem copying instance ID:" + inst.getID() + " " + inst.getKind() + ":\"" + inst.getTitle() + "\" for user " + user.getTitle() + " : " + e.getMessage());
					}				
				}
			}
		}
	}
	
	private void doMove(IInfoObject o, IInfoObject targetFolder, IUser user) throws SDKException {
		o.setParentID(targetFolder.getID());
		o.properties().getProperty(CePropertyID.SI_OWNERID).setValue(Integer.valueOf(user.getID()));
		try {
			o.save();
		} catch (Exception e) {
			// if this save fails for some reason  we need to log it and bail out of this copy operation
			// but we don't want to stop the whole process by throwing the error up stream.
			logger.error("Problem Moving object ID:" + o.getID() + " " + o.getKind() + ":\"" + o.getTitle() + "\" for user " + user.getTitle() + " : " + e.getMessage());
		}
		

		// Deal with setting owner on Instances
		if (o instanceof ISchedulable) {
			IInfoObjects instances = Q().executeRawQuery("SELECT * FROM CI_INFOOBJECTS WHERE SI_INSTANCE=1 AND SI_PARENTID=" + o.getID());
			for (Iterator i = instances.iterator(); i.hasNext();) {
				IInfoObject inst = (IInfoObject) i.next();
				// Don't Mess with Recurring Schedules
				if (inst.getSchedulingInfo().getStatus() != 9) {
					inst.properties().getProperty(CePropertyID.SI_OWNERID).setValue(Integer.valueOf(user.getID()));
					try {
						inst.save();
					} catch (Exception e) {
						// if this save fails for some reason  we need to log it and bail out of this copy operation
						// but we don't want to stop the whole process by throwing the error up stream.
						logger.error("Problem Moving instance ID:" + inst.getID() + " " + inst.getKind() + ":\"" + inst.getTitle() + "\" for user " + user.getTitle() + " : " + e.getMessage());
					}				
				} else {
					// we need to clean up recurring schedules on a move because we can't change their ownership with out breaking the
					// instance and if we leave them here they will just keep running but the user wont be able to see any of the results.
					try {
						inst.deleteNow();
					} catch (Exception e) {
						logger.error("Could not delete recurring schedule durring move of content.  ID:" + inst.getID() + " " + inst.getKind() + ":\"" + inst.getTitle() + "\" for user " + user.getTitle() + " : " + e.getMessage() );
					}
				}
			}
		}
	}
	
	public static void main(String[] args) {
		UserContentCopierReverse a = new UserContentCopierReverse();
		a.test(args);
	}


}
