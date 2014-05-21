package com.dft.boetools;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.businessobjects.sdk.plugin.desktop.common.ISystemPrincipal;
import com.businessobjects.sdk.plugin.desktop.profile.IProfileValue;
import com.businessobjects.sdk.plugin.desktop.profile.IProfileValues;
import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.occa.infostore.IDestinationPlugin;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.plugin.desktop.user.IUser;
import com.crystaldecisions.sdk.plugin.desktop.usergroup.IUserGroup;
import com.crystaldecisions.sdk.uri.IPageResult;
import com.crystaldecisions.sdk.uri.IStatelessPageInfo;
import com.crystaldecisions.sdk.uri.PagingQueryOptions;
import com.dft.boetools.logging.Log4JLogger;
import com.dft.boetools.logging.LogAdapter;

/**
 * Commonly used methods for querying the Business Objects Repository.
 * @author rwells
 *
 */
public class QueryHelper {
	
	public static final String GROUP_QUERY_PREFIX = "path://SystemObjects/User Groups/";
	public static final String USER_QUERY_PREFIX = "path://SystemObjects/Users/";
	public static final String INFO_OBJECT_QUERY_PREFIX = "path://InfoObjects/Root Folder/";
	
	public static final String ALL_CHILDREN_SUFFIX = "/**/*";
	
	public static final String MAX_BATCH = "MAX_BATCH";
	
	
	/**
	 * A set of columns to be used in queries that fetches essential fields only.
	 */
	public static final String MINIMAL = "SI_ID, SI_NAME, SI_KIND, SI_CUID, SI_PARENTID";
	
	private IInfoStore store;
	
	// Default logger.  Can be overridden with setLogger(LogAdapter)
	protected LogAdapter logger;
	
	private int maxBatchSize;
	
	public QueryHelper(BOEHelper boe) {
		this(boe,new Log4JLogger(Logger.getLogger(QueryHelper.class)));
	}
	
	public QueryHelper(BOEHelper boe, LogAdapter newLogAdapter) {
		this(boe.getInfoStore(), newLogAdapter);
	}
	
	public QueryHelper(IInfoStore store, LogAdapter newLogAdapter) {
		
		this.store = store;
		this.logger = newLogAdapter;
		
		try {
			Properties config = new Properties();
			config.load(getClass().getResourceAsStream("/QueryHelper.properties"));
			maxBatchSize = Integer.parseInt(config.getProperty(MAX_BATCH));
		} catch (Exception e) {
			logger.error("Could not retrieve MAX_BATCH from Properties file, assign default value of 1000");
			maxBatchSize = 1000;
		}

	}
	

	/** 
	 * Create and return a new Empty IInfoObjects Collection.  Can be used to create new objects in bulk.
	 * @return
	 */
	public IInfoObjects newInfoObjectsCollection() {
		return store.newInfoObjectCollection();
	}
	
	/**
	 * Commits a modified collection of InfoObjects to the Repository.  Good for bulk operations as opposed
	 * to calling save on individual info objects.
	 * @param objs
	 * @throws SDKException
	 */
	public void commitObjects(IInfoObjects objs) throws SDKException{
		store.commit(objs);
	}

	/**
	 * Create a copy of an object.  You must set a unique title on the 
	 * copied object and or set a new parentId before saving the object for 
	 * the first time. 
	 * @param obj
	 * @return Copied object ready to take a parent id and be saved.
	 * @throws SDKException
	 */
	public IInfoObject copyObject(IInfoObject obj) throws SDKException {
		IInfoObjects newObjs = store.newInfoObjectCollection();
		newObjs.copy(obj, IInfoObjects.CopyModes.COPY_NEW_OBJECT_NEW_FILES);
		// store.commit(newObjs);
		return (IInfoObject) newObjs.get(0);
	}

	/**
	 * An even simpler version of getObject that will fetch a single object by CUID
	 * @param cuid
	 * @return
	 * @throws SDKException
	 */
	public IInfoObject getObjectByCUID(String cuid) throws SDKException{
		return getObjectByCUID(cuid, null);
	}
	
	/**
	 * A version of getObjectByCUID that allows you to specify exactly which columns to load during 
	 * the query.  If you do not load the values at this time they won't be available later.
	 * if you pass in a null value for columns then the default limited set of columns for a cuid query
	 * is returned.  If you pass the wild card character "*" then all columns are returned.
	 * @param cuid the cuid
	 * @param specificColumns the columns
	 * @return the object
	 * @throws SDKException
	 */
	public IInfoObject getObjectByCUID(String cuid, String specificColumns) throws SDKException{
		StringBuilder query = new StringBuilder("cuid://<").append(cuid).append(">");
		if(specificColumns != null) {
			query.append("@").append(specificColumns);
		}
		return getObject(executeQuery(query.toString(), 10));
	}
	

	
	/**
	 * Retrieve a single object by it's SI_ID
	 * @param id
	 * @return IInfoObject with all columns
	 * @throws SDKException
	 */
	public IInfoObject getObjectByID(int id, String columns) throws SDKException {
		String query = "SELECT " + columns + " FROM CI_INFOOBJECTS, CI_SYSTEMOBJECTS, CI_APPOBJECTS WHERE SI_ID = " + id;
		return getObject(executeRawQuery(query));
	}
	
	/**
	 * Retrieve a single object by it's SI_ID
	 * @param id
	 * @return IInfoObject with all columns
	 * @throws SDKException
	 */
	public IInfoObject getObjectByID(int id) throws SDKException {
		return getObjectByID(id, "*");
	}
	
	/**
	 * Retrieves an Object by SI_NAME and SI_KIND.  Works for INFO, SYSTEM, and APP objects.
	 * Important to note that Name and Kind is not necessarily a unique key.  This method
	 * will return the first object found if multiple are found.
	 * @param name SI_NAME of object
	 * @param kind SI_KIND of object
	 * @return InfoObject with all columns
	 * @throws SDKException
	 */
	public IInfoObject getObjectByName(String name, String kind) throws SDKException {
		return getObjectByName(name, kind, true);
	}
	
	/**
	 * Retrieves an Object by SI_NAME and SI_KIND.  Works for INFO, SYSTEM, and APP objects.
	 * Important to note that Name and Kind is not necessarily a unique key.  This method
	 * will return the first object found if multiple are found.  This version allows
	 * the inclusion of instances whereas the default version does not.
	 * @param name SI_NAME of object
	 * @param kind SI_KIND of object
	 * @return InfoObject with all columns
	 * @throws SDKException
	 */
	public IInfoObject getObjectByName(String name, String kind, boolean noInstances) throws SDKException {
		String query = "SELECT * FROM CI_INFOOBJECTS, CI_SYSTEMOBJECTS, CI_APPOBJECTS WHERE SI_NAME = '"+name+"' AND SI_KIND = '"+kind+"'";
		if (noInstances) {
			query += " AND SI_INSTANCE=0";
		}
		return getObject(executeRawQuery(query));
	}
	
	/**
	 * This version of object by id takes a parent ID parameter to help narrow the search.  Good
	 * for finding instances if you know the parent report id.
	 * @param name
	 * @param parentId
	 * @return
	 * @throws SDKException
	 */
	public IInfoObject getObjectByName(String name, int parentId) throws SDKException {
		String query = "SELECT * FROM CI_INFOOBJECTS, CI_SYSTEMOBJECTS, CI_APPOBJECTS WHERE SI_NAME = '"+name+"' AND SI_PARENTID = "+parentId;
		return getObject(executeRawQuery(query));	
	}
	
	/** @see #getObjectByName(String, String) */
	public IUser getUserByName(String name) throws SDKException {
		return (IUser) getObjectByName(name, IUser.KIND);
	}
	/** @see #getObjectByName(String, String) */
	public IUserGroup getGroupByName(String name) throws SDKException {
		return (IUserGroup) getObjectByName(name, IUserGroup.KIND);
	}
	
	/**
	 * Cleanse a Profile object of its principals based on those passed in
	 * @param profileId The profile to cleanse.
	 * @param principalsToRemove list of principals to remove.
	 * @throws SDKException
	 */
	public void removeProfilePrincipals(int profileId, Collection principalsToRemove) throws SDKException{
		// Validate Arguments
		if (principalsToRemove == null) return;
		if (principalsToRemove.size() == 0) return;
		
		String principalQuery = "SELECT SI_NAME, SI_ML_NAME, SI_USERFULLNAME, SI_PROFILE_VALUES, SI_PRINCIPAL_PROFILES, SI_ID, SI_CUID, SI_OWNERID FROM CI_SYSTEMOBJECTS WHERE SI_ID IN " + StringHelper.inClause(principalsToRemove, false);
		IInfoObjects groupsToCleanse = executeRawQuery(principalQuery);
		for (Iterator iter = groupsToCleanse.iterator(); iter.hasNext();) {
			ISystemPrincipal  g = (ISystemPrincipal ) iter.next();
			IProfileValues values = g.getProfileValues();
			
			for (int i = 0; i < values.size(); i++) {
				IProfileValue next = (IProfileValue) values.get(i);
				if (next.getProfileID() == profileId) {
					values.remove(i);
				}				
			}
		}
		
		commitObjects(groupsToCleanse);
	}
	

	
	/**
	 * @param scopeBatchId ID of ScopeBatch to get Artifacts For
	 * @return The list of Artifact Documents
	 * @throws SDKException
	 */
	public IInfoObjects getArtifactsForScopeBatch(int scopeBatchId) throws SDKException{
		StringBuffer queryString = new StringBuffer(
				"select * from CI_INFOOBJECTS where children(\"SI_NAME='PublicationScopeBatch-Artifact'\", \"SI_ID=");
		queryString.append(scopeBatchId);
		queryString.append("\")");
		return executeRawQuery(queryString.toString());
	}
	
	/**
	 * Executes a plain sql style query with no limits on returned results or pagination.  
	 * Be sure you know the scale of the query being executed so as not to overwhelm the system.
	 * @param query The query
	 * @return The results
	 * @throws SDKException
	 */
	public IInfoObjects executeRawQuery(String query) throws SDKException {
		logger.debug(query);
		return store.query(query);
	}
	
	public static final String PATH_PROTOCALL = "path://";
	public static final String CUID_PROTOCALL = "cuid://";
	public static final String SEARCH_PROTOCALL = "search://";
	public static final String QUERY_PROTOCALL = "query://";
	
	
	/**
	 * Executes a query (<protocol>://<query>) and returns the IInfoObjects.
	 * Protcols are 
	 * path
	 * cuid
	 * search
	 * query
	 * Will only return up to maxResults objects.  Good for small known size queries.
	 * @param query
	 * @param maxResults
	 * @return
	 */
	public IInfoObjects executeQuery(String query, int maxResults) throws SDKException{
		PagingQueryOptions pqo = new PagingQueryOptions(maxResults);
		IPageResult result = store.getPagingQuery(query, pqo);
		if (result.getPageCount() > 0) {
			IStatelessPageInfo spi = (IStatelessPageInfo)store.getStatelessPageInfo(result.getPageURI(0), pqo);
			return store.query(spi.getPageSQL());			
		} else {
			return store.newInfoObjectCollection();
		}
	}
	 
	/**
	 * Retrieve a single object by arbitraty query.  Gives the most control over
	 * fields returned.  If multiple objects are retrieved by the query the method
	 * will only return the first one in the results.  If no objects are retrieved 
	 * the method will return null.
	 * @param query
	 * @return
	 * @throws SDKException
	 */
	public IInfoObject getObjectByQuery(String query) throws SDKException {
		return getObjectByQuery(query, false);
	}
	
	public IInfoObject getObjectByQuery(String query, boolean isRaw) throws SDKException {
		IInfoObjects results = (isRaw) ? executeRawQuery(query) : executeQuery(query, 1);
		if(results.size() > 0) {
			return (IInfoObject) results.get(0);
		} else {
			return null;
		}
	}
	
	/**
	 * Pass in any valid query (<protocol>://<query>) and an InfoObjectWorker implementation. 
	 * Method will handle paging of large result sets to limit load on CMS 
	 * and prevent memory exhaustion.
	 * 
	 * Valid Protocol Examples
	 * Query Protocol: query://{SELECT [Properties] FROM [Table] WHERE [Conditions] ORDER BY [Properties]}
	 * Search Protocol: search://{search_term_1 search_term_2 ... search_term_N}?search_option_1=[true | false]&...search_option_n=[true | false]
 	 * Cuid Protocol: cuid://<CUID_1, CUID_2,..., CUID_N>
 	 * Path Protocol: path://[Root table]/[Base folder]/folder1/folder2/.../[Target resource SI_NAME]
	 * @param query
	 * @param w
	 * @param pageSize
	 * @throws SDKException
	 */
	public void forEachResult(String query, InfoObjectWorker w) throws Exception {
		logger.debug("Executing Paged Query : " + query);
		PagingQueryOptions pqo = new PagingQueryOptions(maxBatchSize);
		IPageResult result = store.getPagingQuery(query, pqo);
		logger.debug("Page count for query = " + result.getPageCount());
		Iterator iter = result.iterator();		
		int pageNumber = 0;
		while(iter.hasNext()) {
			logger.debug("Fetching Page " + pageNumber);
			IStatelessPageInfo spi = (IStatelessPageInfo)store.getStatelessPageInfo((String)iter.next(), pqo);
			logger.debug("Executing page SQL: " + spi.getPageSQL());
			IInfoObjects page = store.query(spi.getPageSQL());
			Iterator infoIter = page.iterator();
			while(infoIter.hasNext()) {
				w.doWork((IInfoObject)infoIter.next());
			}
			pageNumber++;
		}
	}
	
	public interface InfoObjectWorker { void doWork(IInfoObject o) throws Exception;	}
	

	/**
	 * Retrieve the destination plugin object for the given destination type.
	 */
	public IDestinationPlugin getDestinationPlugin(String destinationType)
			throws SDKException {
		// The system parent for all destination plugin objects is 29.
		// This is one of the unfortunate business objects magic numbers.
		return (IDestinationPlugin) getObject(executeRawQuery("SELECT SI_DEST_SCHEDULEOPTIONS, SI_PROGID FROM CI_SYSTEMOBJECTS WHERE SI_PARENTID=29 AND SI_NAME='" + destinationType + "'"));
	}
	
	public IInfoObject getFirstOfKind(String kind) throws SDKException{
		String qry = "SELECT TOP 1 * FROM CI_INFOOBJECTS, CI_SYSTEMOBJECTS, CI_APPOBJECTS WHERE SI_KIND='" + kind + "'";
		return getObjectByQuery(qry, true);		
	}
	
	/** 
	 * Return the InfoObjects by name in a comma separated list.
	 * @param objects
	 * @return
	 * @throws SDKException
	 */
	public String inClauseByName(IInfoObjects objects) throws SDKException {
		List<String> names = new ArrayList<String>();
		for (Iterator i = objects.iterator(); i.hasNext();) {
			IInfoObject o = (IInfoObject) i.next();
			names.add(o.getTitle());
		}
		return StringHelper.inClause(names, false);		
	}
	
	/**
	 * Helper method to ensure one and only one object is returned.
	 */
	private IInfoObject getObject(IInfoObjects objs) throws SDKException {
		if (objs.size() > 0) {
			return (IInfoObject) objs.get(0);
		} else {
			return null;
		}
	}
	
	/**
	 * Dates in where clauses in BOE SDK must be in UTC.  So this function will
	 * convert the date from the given source TimeZone to UTC and format using 
	 * appropriate format for BOE SDK.
	 * @param date
	 * @return
	 */
	public static String formatDateForBOEQuery(Date date, TimeZone srcTZ){
		// Convert Date to UTC from current default locale.
		int offset = srcTZ.getOffset(date.getTime());
		
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("yyyy.MM.dd.HH.mm.ss");
		
		Date utcDate = new Date(date.getTime() - offset);
		
		return sdf.format(utcDate);
	}
	
}
