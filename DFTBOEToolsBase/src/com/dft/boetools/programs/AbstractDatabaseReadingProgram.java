package com.dft.boetools.programs;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Collections;

/**
 * A subclass of AbstractProgram that handles the common scenario of needing
 * to connect to a SQL Database via JDBC.  We define several re-useable program
 * arguments for passing in Database Driver Information, and methods for dealing with
 * JDBC Boiler Plate.
 * 
 * Arguments Are...
 * 
 * DB_DRIVER: The fully qualified name of the database driver the program will use at runtime
 * DB_URL: The JDBC connection URL in the format specified by the driver
 * DB_USER: The user name to connect to the database with
 * DB_PASS: The password of the user to connect to the database with
 * 
 * @author roy.wells
 *
 */
public abstract class AbstractDatabaseReadingProgram extends AbstractProgram {

	// Argument Keys
	protected static final String DB_DRIVER = "DB_DRIVER";
	protected static final String DB_URL = "DB_URL";
	protected static final String DB_USER = "DB_USER";
	protected static final String DB_PASS = "DB_PASS";		
	
	@Override
	protected void addRequiredArguments(List<String> required) {
		super.addRequiredArguments(required);
		Collections.addAll(required, new String[] {DB_DRIVER, DB_URL, DB_USER, DB_PASS});
	}
	
	private boolean driverInitialized = false;	
	
	private Connection getConnection() throws Exception{
		if(!driverInitialized) {
			Class.forName(getArgument(DB_DRIVER));
			driverInitialized = true;
		}
		return DriverManager.getConnection(getArgument(DB_URL),
				getArgument(DB_USER), 
				getArgument(DB_PASS));				
	}
	
	protected interface ResultSetProcessor { void processResults(ResultSet rs) throws Exception; }
	
	protected abstract class RowProcessor implements ResultSetProcessor {
		
		protected abstract void processRow(ResultSet rs) throws Exception;
		
		public void processResults(ResultSet rs) throws Exception {
			try{
				while(rs.next()){
					processRow(rs);
				}
			} catch (SQLException e) {
				logger.debug(e,e);
				throw(e);
			}
		}
		
	}
	
	protected void executeQuery(String sql, ResultSetProcessor worker) throws Exception {
		executeParameterizedQuery(sql, null, worker);
	}
	
	protected void executeParameterizedQuery(String sql, Object[] queryParameters, ResultSetProcessor worker) throws Exception {		
		Connection con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try {			
			logger.debug("Executing Database Query : " + sql);
			
			con = getConnection();
			st = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			
			if (queryParameters != null) {
				logger.debug("SQL Query Parameters: " + queryParameters);
				for (int i = 0; i < queryParameters.length; i++) {
					Object p = queryParameters[i];
					if (p instanceof BigDecimal) st.setBigDecimal(i, (BigDecimal)  p);
					else if (p instanceof Boolean) st.setBoolean(i, (Boolean)  p);
					else if (p instanceof Byte) st.setByte(i, (Byte)  p);
					else if (p instanceof Date) st.setTimestamp(i, new Timestamp(((Date) p).getTime()));
					else if (p instanceof Double) st.setDouble(i, (Double) p);
					else if (p instanceof Float) st.setFloat(i, (Float) p);
					else if (p instanceof Integer) st.setInt(i, (Integer) p);
					else if (p instanceof Long) st.setLong(i, (Long) p);
					else if (p instanceof String) st.setString(i, (String) p);
					else st.setObject(i, p);	
				}
			}
		
			rs = st.executeQuery();
			worker.processResults(rs);
			
			
		} finally {
			try {
				if (rs != null) rs.close();
				if (st != null) st.close();
				if (con != null) con.close();				
			} catch (SQLException e) {					
				logger.warn("Exception in database connection cleanup: " + e.getMessage(), e );
			}	
		}
	}

	//protected void executeParameterizedQuery(String sql, Object[] queryParameters, ResultSet. ,ResultSetProcessor worker) throws Exception 
}
