package com.dft.boetools.programs;

import com.crystaldecisions.sdk.occa.infostore.CePropertyID;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.plugin.desktop.user.IUser;
import com.dft.boetools.BOEHelper;
import com.dft.boetools.QueryHelper;

public class CR4ESourceConverter extends AbstractProgram {

	@Override
	protected void runInternal(BOEHelper boe) throws Exception {
		QueryHelper q = new QueryHelper(boe);
		
		IInfoObjects newObjs = q.newInfoObjectsCollection();
		IUser newUser = (IUser) newObjs.add(IUser.KIND);
		newUser.setTitle("");
		newUser.setNewPassword("Blah");
		newUser.save();
		
		///newUser.properties()
		
		//newUser.getTitle("BLAH");
		
		newUser.properties().getString(CePropertyID.SI_NAME);
		
		newUser.properties().add("MYCUSTOMPROPERTY", "MYCUSTOMVALUE", 0);
		newUser.save();
	}

}
