package com.dft.boetools;


import org.apache.log4j.Logger;

import com.crystaldecisions.enterprise.ocaframework.IManagedService.ManagedExpiredException;
import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.exception.SDKServerException;
import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.framework.ISessionMgr;
import com.crystaldecisions.sdk.framework.ITrustedPrincipal;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.occa.security.IUserInfo;

/**
 * Simple wrapper for BOE Enterprise Sessions that provides some convenience methods. 
 * @author rwells
 *
 */
public class BOEHelper {
	
	public static final String ENTERPRISE_AUTH = "secEnterprise";
	public static final String LDAP_AUTH = "secLDAP";
	public static final String AD_AUTH = "secWinAD";
	
	
	private static final String INFOSTORE_SERVICE_KEY = "InfoStore";
	private IEnterpriseSession entSession;
	private IInfoStore store;
	private String logonToken;
	private static Logger logger = Logger.getLogger(BOEHelper.class);
	boolean logoffOnSessionEnd;
	
	public BOEHelper() {
		this(false);
	}
	
	public BOEHelper(boolean logoffOnSessionEnd) {
		this.logoffOnSessionEnd = logoffOnSessionEnd;
	}
	
	public BOEHelper(IEnterpriseSession session) throws SDKException{
		this();
		this.setSession(session);
	}
	

	
	public IEnterpriseSession getSession() {
		return entSession;
	}

	public void setSession(IEnterpriseSession session) throws SDKException{
		this.entSession = session;
		this.store = (IInfoStore) entSession.getService(INFOSTORE_SERVICE_KEY);
	}
	
	private interface LogonMethod {	IEnterpriseSession logon(String nextCMS) throws SDKException;	}

	/** 
	* Implements round robin authentication against a set of CMS's either storing an authenticated 
	* Enterprise session or throwing an SDKException as to why we could not authenticate. 
	* 
	* Logic will take the first cms in the list starting at cmsIndex, and attempt to log on to 
	* each cms until it finds one that works or has tried them all. Will return either a valid
	* IEnterpriseSession or the last logon exception.  
	* 
	* We support starting at a non-zero index so that client code can keep track of the last CMS
	* used and attempt to roughly balance load across multiple CMS's in a cluster.
	*/ 
	private int logon(String[] CMSArray, int cmsIndex, LogonMethod method) throws SDKException {
		int size=CMSArray.length;
		int currentIndex = 0;
		int start = cmsIndex;
		IEnterpriseSession session = null;
		SDKException lastException = null;
		logger.debug("Starting Logon Process with " + size + " cms's to try.");
		while (currentIndex < size && session == null){
			// Implementation of circular array indexing 
			cmsIndex = (start + currentIndex + size) % size;
			String nextCMS = CMSArray[cmsIndex].trim();
			logger.debug("Next CMS = " + nextCMS + " at index = " + cmsIndex);
			// Logon
			try{
				session = method.logon(nextCMS); 		
			} catch(SDKException e) {
				logger.debug("Execption caused by logon " + e.getMessage(), e);
				lastException = e;
				session = null;
			}
			
			currentIndex++;
		}
		if (session != null) { 
			this.setSession(session);
			return cmsIndex;
		} else {
			throw lastException;
		}
	}
	
	
	/** 
	* Logon using Trusted Authentication
	*/ 
	public int logonTrusted(final String username, final String authsecret, String[] CMSArray, int cmsIndex) throws SDKException{
		return logon(CMSArray, cmsIndex, new LogonMethod() {
			
			public IEnterpriseSession logon(String nextCMS) throws SDKException {
				ISessionMgr mgr = CrystalEnterprise.getSessionMgr();
				logger.debug("Creating Trusted Principal");
				ITrustedPrincipal princ = mgr.createTrustedPrincipal(username, nextCMS, authsecret);
				logger.debug("Logging On Trusted Principal");
				return mgr.logon(princ);					
			}
		});
	}
	
	/**
	 * Logon with a normal user name and password.
	 */
	public int logonNormal(final String username, final String password, final String authType, String[] CMSArray, int cmsIndex) throws SDKException {
		return logon(CMSArray, cmsIndex, new LogonMethod() {
			public IEnterpriseSession logon(String nextCMS) throws SDKException {
				ISessionMgr mgr = CrystalEnterprise.getSessionMgr();
				return mgr.logon(username, password, nextCMS, authType);		
			}
		});
	}
	
	/**
	 * Logon using a toke that was previously generated.
	 * @param token
	 * @throws SDKException
	 */
	public void logonWithToken(String token) throws SDKException {
		ISessionMgr mgr = CrystalEnterprise.getSessionMgr();
		this.setSession(mgr.logonWithToken(token));
	}
	
	/**
	 * Restore a Session from a serialized object.  This is not creating a new Enterprise Session 
	 * @param serializedSession
	 * @throws SDKException
	 */
	public void restoreSerializedSession (String serializedSession) throws SDKException {
		ISessionMgr mgr = CrystalEnterprise.getSessionMgr();
		this.setSession(mgr.getSession(serializedSession));
	}
	
	/**
	 * Logoff but don't release any tokens thus allowing for re-logon later by token.
	 */
	public void logoff() {
		logoff(false);
	}
	
	/**
	 * Logoff and release token if parameter is true.
	 * @param releaseToken
	 */
	public void logoff(boolean releaseToken) {
		if (isSessionValid()){
			if(releaseToken && this.logonToken != null) {
				try {
					entSession.getLogonTokenMgr().releaseToken(this.logonToken);
				} catch (SDKException e) {
					// We are logging off and about to terminate the session anyway, so 
					// if this fails just move on with life. It probably means the session
					// was already dead so it doesn't matter.
				}
			}
			entSession.logoff();
			entSession = null;
			store = null;			
		}		
	}
	
	/**
	 * Are we connected to a valid Enterprise Session and can we successfully query the CMS Repository.
	 * @return boolean
	 */
	public boolean isSessionValid() {
		boolean existingSessionValid = false;
		if (entSession != null && store != null) {            
			try {
                // query for empty string is the quickest round trip to the CMS. This should return a CMS generated
                // exception only if the enterprise session is valid
                store.query(""); //$NON-NLS-1$
            } catch (SDKServerException se) {
                // Sever exception expected; existingEntSession is valid.                    
                existingSessionValid = true;
            } catch (SDKException e) {
                // Sever exception not thrown. existingEntSession is not valid. Just pass on.
                existingSessionValid = false;
            } catch (ManagedExpiredException e) {
                existingSessionValid = false;
            }
		} 
			
		return existingSessionValid;			
	}
	
	/**
	 * Creates and returns a logon token with provided usage restrictions
	 * @param validMinutes number of minutes the token is valid for.
	 * @param validUses number of times the token can be used to establish a new sessions
	 * @return The token
	 * @throws SDKException 
	 */
	public String getLogonToken(int validMinutes, int validUses) throws SDKException{
		this.entSession.getSerializedSession();
		
		return this.entSession.getLogonTokenMgr().createLogonToken("", validMinutes, validUses);
	}
	
	/**
	 * Create and return a new logon token using default options
	 * @return
	 * @throws SDKException
	 */
	public String getDefaultLogonToken() throws SDKException{
		return this.getDefaultLogonToken(false);
	}
	
	/**
	 * Gets a Logon Token with default properties valid for 1440 minutes and 10 
	 * logons.  If useExisting is true and this method has already been called 
	 * once on this object then the same token created earlier will be reused 
	 * with out having to create another token.  
	 * @param useExisting
	 * @return
	 * @throws SDKException
	 */
	public String getDefaultLogonToken(boolean useExisting) throws SDKException{
		
		// Check for an existing token if reuse is true		
		if (useExisting && this.logonToken != null) {
			return this.logonToken;
		} 
		
		// reuse is false or we don't have a token, either way need to create one.
		String token = this.entSession.getLogonTokenMgr().createLogonToken("", 1440, 10);
		
		// If reuse is true store the token for future requests.
		if (useExisting) this.logonToken = token;
		
		return token;		
	}
	
	/**
	 * Let go of a token on the server.  Frees up some resources and prevents future use of that token.  
	 * @param token
	 * @throws SDKException
	 */
	public void releaseToken(String token) throws SDKException{		
		if (token.equals(this.logonToken)) {
			this.logonToken = null;
		}
		
		this.entSession.getLogonTokenMgr().releaseToken(token);
	}
	
	/**
	 * @return Information about the currently logged on user as an IUserInfo object.
	 * @throws SDKException
	 */
	public IUserInfo getUserInfo() throws SDKException{
		return entSession.getUserInfo();
	}
	
	
	/**
	 * @return an unwrapped InfoStore
	 */
	public IInfoStore getInfoStore() {
		return store;
	}
	
}
