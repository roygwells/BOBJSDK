package com.dft.boetools;

import java.util.Iterator;
import java.util.UUID;


import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.plugin.desktop.user.IUser;
import com.crystaldecisions.sdk.plugin.desktop.user.IUserAlias;
import com.crystaldecisions.sdk.plugin.desktop.user.IUserAliases;

public class AliasHelper {
	public static final String secEnterprise = "secEnterprise";
	public static final String secLDAP = "secLDAP";
	public static final String secSAPR3 = "secSAPR3";
	
	/**
	 * Determines if a user has a pre-existing Alias
	 * @param user the user to check
	 * @param aliasType the type of alias to look for
	 * @return true if alias of the specified type found, false otherwise
	 * @throws SDKException
	 */
	public static boolean userHasAlias(IUser user, String aliasType) throws SDKException{
		IUserAliases aliases = user.getAliases();
		for (Iterator i = aliases.iterator(); i.hasNext();) {
			IUserAlias a = (IUserAlias) i.next();
			// There is a strange slim chance that getAuthentication will throw a null pointer here.
			// if it does, it most likely doesn't match our desired alias type so treat it as false.
			try {
				if (a.getAuthentication().equals(aliasType)) {
					return true;
				}
			} catch (NullPointerException ne) {
				// do nothing, and act as if not equal.
			}
		} 
		return false;
	}
	
	/**
	 * Attempts to add an LDAP Alias to a user.  Invokes Save immediately as it's the only way to generate
	 * exceptions for unmapped groups or pre-existing aliases.
	 * @param user The user to add alias to.
	 * @param ldapUserId User ID of LDAP alias
	 * @throws SDKException if not a member of a mapped group or pre-existing alias exists.
	 */
	public static void addLDAPAlias(IUser user, String ldapUserId) throws SDKException {
		String userName = secLDAP + ":" + ldapUserId;
		addAlias(user, userName);
	}	
	
	/**
	 * Attempts to add an SAP Alias to a user.  Invokes Save immediately as it's the only way to generate
	 * exceptions for unmapped groups or pre-existing aliases.
	 * @param user The user to add alias to.
	 * @param sapId User ID of sap alias
	 * @throws SDKException if not a member of a mapped group or pre-existing alias exists.
	 */
	public static void addSAPAlias(IUser user, String sapId) throws SDKException {
		String userName = secSAPR3 + ":" + sapId;
		addAlias(user, userName);
	}	
	
	/**
	 * Adds an Enterprise Alias to a user.  Invokes save immediately.
	 * @param user to add alias to
	 * @param enterpriseUserId user id for new alias
	 * @param password password to set on user object to go with enterprise alias
	 * @throws SDKException
	 */
	public static void addEnterpriseAlias(IUser user, String enterpriseUserId, String password) throws SDKException {
		String userName = secEnterprise + ":" + enterpriseUserId;
		addAlias(user, userName);
		user.setNewPassword(password);		
		user.save();
	}

	/**
	 * Generic Alias add method used by other add Methods.
	 * @param user user to add to
	 * @param userName prepared name with authtype prefix applied.
	 * @throws SDKException
	 */
	private static void addAlias(IUser user, String userName)
			throws SDKException {
		IUserAlias newAlias = user.getAliases().addNew(userName, false);
		try {
			user.save();
		} catch (SDKException e) {
			// If the save fails it most likely indicates a problem with the alias
			// Because we are going to add other aliases to this object later, we cant
			// leave the bad alias dangling around.
			user.getAliases().remove(newAlias);
			throw e;
		}
	}
	
	public static String generateRandomPassword() {
		UUID nextId = UUID.randomUUID();
		return nextId.toString();
	}
}
