package rifServices.dataStorageLayer.common;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.sun.rowset.CachedRowSetImpl;

import rifGenericLibrary.businessConceptLayer.User;
import rifGenericLibrary.dataStorageLayer.ConnectionQueue;
import rifGenericLibrary.dataStorageLayer.DatabaseType;
import rifGenericLibrary.dataStorageLayer.QueryFormatter;
import rifGenericLibrary.dataStorageLayer.RIFDatabaseProperties;
import rifGenericLibrary.dataStorageLayer.SQLGeneralQueryFormatter;
import rifGenericLibrary.dataStorageLayer.common.SQLFunctionCallerQueryFormatter;
import rifGenericLibrary.dataStorageLayer.common.SQLQueryUtility;
import rifGenericLibrary.system.Messages;
import rifGenericLibrary.system.RIFServiceException;
import rifGenericLibrary.system.RIFServiceExceptionFactory;
import rifGenericLibrary.util.RIFLogger;
import rifServices.businessConceptLayer.AbstractRIFConcept.ValidationPolicy;
import rifServices.system.RIFServiceError;
import rifServices.system.RIFServiceStartupOptions;
import rifServices.system.files.TomcatBase;
import rifServices.system.files.TomcatFile;

public class BaseSQLManager implements SQLManager {

	static final Messages SERVICE_MESSAGES = Messages.serviceMessages();
	private static final String ABSTRACT_SQLMANAGER_PROPERTIES = "AbstractSQLManager.properties";
	private static final int MAXIMUM_SUSPICIOUS_EVENTS_THRESHOLD = 5;
	private static final int POOLED_READ_ONLY_CONNECTIONS_PER_PERSON = 10;
	private static final int POOLED_WRITE_CONNECTIONS_PER_PERSON = 5;
	private static final String POSTGRES_INITIALISATION_QUERY =
			"SELECT rif40_startup(?) AS rif40_init;";
	private static final String MS_SQL_INITIALISATION_QUERY = "EXEC rif40.rif40_startup ?";

	protected static final RIFLogger rifLogger = RIFLogger.getLogger();
	private static final Set<String> registeredUserIDs = new HashSet<>();
	private static final Set<String> userIDsToBlock = new HashSet<>();;

	private static final Map<String, ConnectionQueue> readOnlyConnectionsFromUser
			= new HashMap<>();

	private static final Map<String, ConnectionQueue> writeConnectionsFromUser = new HashMap<>();
	private static final HashMap<String, Integer> suspiciousEventCounterFromUser = new HashMap<>();
	protected final RIFServiceStartupOptions rifServiceStartupOptions;
	private final String initialisationQuery;
	private final String databaseURL;
	private final HashMap<String, String> passwordHashList;

	protected RIFDatabaseProperties rifDatabaseProperties;

	private static Properties prop = null;
	private static String lineSeparator = System.getProperty("line.separator");
	private static DatabaseType databaseType;

	private ValidationPolicy validationPolicy = ValidationPolicy.STRICT;
	private boolean enableLogging = true;

	public BaseSQLManager(final RIFServiceStartupOptions rifServiceStartupOptions) {

		rifDatabaseProperties = rifServiceStartupOptions.getRIFDatabaseProperties();
		databaseType = this.rifDatabaseProperties.getDatabaseType();
		this.rifServiceStartupOptions = rifServiceStartupOptions;

		initialisationQuery =
				rifDatabaseProperties.getDatabaseType() == DatabaseType.SQL_SERVER
						? MS_SQL_INITIALISATION_QUERY : POSTGRES_INITIALISATION_QUERY;

		passwordHashList = new HashMap<>();
		databaseURL = generateURLText();
	}

	@Override
	public ValidationPolicy getValidationPolicy() {
		return validationPolicy;
	}
		
	@Override
	public void setValidationPolicy(
			final ValidationPolicy validationPolicy) {
		
		this.validationPolicy = validationPolicy;
	}
	
	/**
	 * Use appropriate table name case.
	 *
	 * @param tableComponentName the table component name
	 * @return the string
	 */
	protected String useAppropriateTableNameCase(
			final String tableComponentName) {
		
		//TODO: KLG - find out more about when we will need to convert
		//to one case or another
		return tableComponentName;
	}
	
	protected RIFDatabaseProperties getRIFDatabaseProperties() {
		return rifDatabaseProperties;
	}
	
	@Override
	public void configureQueryFormatterForDB(
			final QueryFormatter queryFormatter) {
		
		queryFormatter.setDatabaseType(
			rifDatabaseProperties.getDatabaseType());
		queryFormatter.setCaseSensitive(
			rifDatabaseProperties.isCaseSensitive());
		
	}
	
	@Override
	public PreparedStatement createPreparedStatement(final Connection connection, final QueryFormatter queryFormatter)
			throws SQLException {
				
		return SQLQueryUtility.createPreparedStatement(
			connection,
			queryFormatter);
	}

	/** 
	 * Create cached row set from AbstractSQLQueryFormatter.
	 * No checks 0,1 or 1+ rows returned
	 * 
	 * @param connection an SQL connection
	 * @param queryFormatter  the Formatter
	 * @param queryName the name
	 * @param params the query parameters
	 *
	 * @return CachedRowSetImpl cached row set
	 */		
	protected CachedRowSetImpl createCachedRowSet(
			final Connection connection,
			QueryFormatter queryFormatter,
			final String queryName,
			final String[] params)
				throws Exception {
			
		CachedRowSetImpl cachedRowSet=null;	
		ResultSet resultSet=null;
		PreparedStatement statement = createPreparedStatement(connection, queryFormatter);		
		try {
			for (int i=0; i < params.length; i++) {
				statement.setString((i+1), params[i]);	
			}
			logSQLQuery(queryName, queryFormatter, params);	
			resultSet = statement.executeQuery();
			 // create CachedRowSet and populate
			cachedRowSet = new CachedRowSetImpl();
			cachedRowSet.populate(resultSet);
		}
		catch (Exception exception) {
			rifLogger.error(this.getClass(), "Error in SQL Statement: >>> " + 
				lineSeparator + queryFormatter.generateQuery(),
				exception);
			throw exception;
		}
		finally {
			closeStatement(statement);
		}
		
		return cachedRowSet;			
	}

	/** 
	 * Create cached row set from AbstractSQLQueryFormatter.
	 * No checks 0,1 or 1+ rows returned
	 * 
	 * @param connection,
	 * @param queryFormatter,
	 * @param queryName
	 *
	 * @return CachedRowSetImpl cached row set
	 */
	@Override
	public CachedRowSetImpl createCachedRowSet(
			final Connection connection,
			QueryFormatter queryFormatter,
			final String queryName)
				throws Exception {
			
		CachedRowSetImpl cachedRowSet=null;	
		ResultSet resultSet=null;
		PreparedStatement statement = createPreparedStatement(connection, queryFormatter);		
		try {
			logSQLQuery(queryName, queryFormatter);	
			resultSet = statement.executeQuery();
			 // create CachedRowSet and populate
			cachedRowSet = new CachedRowSetImpl();
			cachedRowSet.populate(resultSet);
		}
		catch (Exception exception) {
			rifLogger.error(this.getClass(), "Error in SQL Statement: >>> " + 
				lineSeparator + queryFormatter.generateQuery(),
				exception);
			throw exception;
		}
		finally {
			closeStatement(statement);
		}
		
		return cachedRowSet;			
	}
	
	/** 
	 * Create cached row set from AbstractSQLQueryFormatter.
	 * No checks 0,1 or 1+ rows returned
	 * 
	 * @param connection,
	 * @param queryFormatter,
	 * @param queryName,
	 * @param params
	 *
	 * @return CachedRowSetImpl cached row set
	 */
	@Override
	public CachedRowSetImpl createCachedRowSet(
			final Connection connection,
			QueryFormatter queryFormatter,
			final String queryName,
			final int[] params)
				throws Exception {
			
		CachedRowSetImpl cachedRowSet=null;	
		ResultSet resultSet;
		PreparedStatement statement = createPreparedStatement(connection, queryFormatter);		
		try {
			for (int i=0; i < params.length; i++) {
				statement.setInt((i+1), params[i]);	
			}
			logSQLQuery(queryName, queryFormatter, params);	
			resultSet = statement.executeQuery();
			 // create CachedRowSet and populate
			cachedRowSet = new CachedRowSetImpl();
			cachedRowSet.populate(resultSet);
		}
		catch (Exception exception) {
			rifLogger.error(this.getClass(), "Error in SQL Statement: >>> " + 
				lineSeparator + queryFormatter.generateQuery(),
				exception);
			throw exception;
		}
		finally {
			closeStatement(statement);
		}
		
		return cachedRowSet;			
	}

	/** 
	 * Get row from cached row set
	 * Row set must contain one row, and the value must not be null
	 * 
	 * @param cachedRowSet,
	 * @param columnName
	 *
	 * @return String column comment
	 */
	@Override
	public String getColumnFromResultSet(
			final CachedRowSetImpl cachedRowSet,
			final String columnName)
			throws Exception {
		return getColumnFromResultSet(cachedRowSet, columnName, 
			false /* allowNulls */, false /* allowNoRows */);
	}

	/** 
	 * Get row from cached row set
	 * Checks 0,1 or 1+ rows; assumed multiple rows not permitted
	 * Flag controls 0/1 null/not null checks
	 * 
	 * @param cachedRowSet,
	 * @param columnName,
	 * @param allowNulls,
	 * @param allowNoRows
	 *
	 * @return String column comment
	 */
	@Override
	public String getColumnFromResultSet(
			final CachedRowSetImpl cachedRowSet,
			final String columnName,
			final boolean allowNulls,
			final boolean allowNoRows)
			throws Exception {
			
		String columnValue=null;
		boolean columnFound=false;
		if (cachedRowSet.first()) {			
			ResultSetMetaData rsmd = cachedRowSet.getMetaData();
			int columnCount = rsmd.getColumnCount();

			// The column count starts from 1
			for (int i = 1; i <= columnCount; i++ ) {
				String name = rsmd.getColumnName(i);
				String value = cachedRowSet.getString(i);	
				
				if (name.toUpperCase().equals(columnName.toUpperCase())) {
					columnValue=value;
					columnFound=true;
				} 
			}
			if (cachedRowSet.next()) {
				throw new Exception("getColumnFromResultSet(): expected 1 row, got >1");
			}
			if (!columnFound) {
				throw new Exception("getColumnFromResultSet(): column not found: " + columnName);
			}
			if (columnValue == null && !allowNulls) {
				throw new Exception("getColumnFromResultSet(): got null for column: " + columnName);
			}
		}
		else if (!allowNoRows) {
			throw new Exception("getColumnFromResultSet(): expected 1 row, got none");
		}
		
		return columnValue;
	}

	/**
	 * Get column comment from data dictionary
	 *
	 * @param connection,
	 * @param schemaName,
	 * @param tableName,
	 * @param columnName
	 */
	@Override
	public String getColumnComment(Connection connection, String schemaName, String tableName,
			String columnName) throws Exception {
		
		SQLGeneralQueryFormatter columnCommentQueryFormatter = new SQLGeneralQueryFormatter();
		ResultSet resultSet;
		if (databaseType == DatabaseType.POSTGRESQL) {
			columnCommentQueryFormatter.addQueryLine(0, // Postgres
				"SELECT pg_catalog.col_description(c.oid, cols.ordinal_position::int) AS column_comment");
			columnCommentQueryFormatter.addQueryLine(0, "  FROM pg_catalog.pg_class c, information_schema.columns cols");
			columnCommentQueryFormatter.addQueryLine(0, " WHERE cols.table_catalog = current_database()");
			columnCommentQueryFormatter.addQueryLine(0, "   AND cols.table_schema  = ?");
			columnCommentQueryFormatter.addQueryLine(0, "   AND cols.table_name    = ?");
			columnCommentQueryFormatter.addQueryLine(0, "   AND cols.table_name    = c.relname");
			columnCommentQueryFormatter.addQueryLine(0, "   AND cols.column_name   = ?");
		}
		else if (databaseType == DatabaseType.SQL_SERVER) {
			columnCommentQueryFormatter.addQueryLine(0, "SELECT CAST(value AS VARCHAR(2000)) AS column_comment"); // SQL Server
			columnCommentQueryFormatter.addQueryLine(0, "FROM fn_listextendedproperty (NULL, 'schema', ?, 'table', ?, 'column', ?)");
			columnCommentQueryFormatter.addQueryLine(0, "UNION");
			columnCommentQueryFormatter.addQueryLine(0, "SELECT CAST(value AS VARCHAR(2000)) AS column_comment");
			columnCommentQueryFormatter.addQueryLine(0, "FROM fn_listextendedproperty (NULL, 'schema', ?, 'view', ?, 'column', ?)");
		}
		else {
			throw new Exception("getColumnComment(): invalid databaseType: " +
				databaseType);
		}
		PreparedStatement statement = createPreparedStatement(connection, columnCommentQueryFormatter);
		
		String columnComment=columnName.substring(0, 1).toUpperCase() +
			columnName.substring(1).replace("_", " "); // Default if not found [initcap, remove underscores]
		try {
		
			statement.setString(1, schemaName);
			statement.setString(2, tableName);
			statement.setString(3, columnName);
			if (databaseType == DatabaseType.SQL_SERVER) {
				statement.setString(4, schemaName);
				statement.setString(5, tableName);
				statement.setString(6, columnName);
			}
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				columnComment=resultSet.getString(1);
				if (resultSet.next()) {
					throw new Exception("getColumnComment() database: " + databaseType +
						"; expected 1 row, got >1");
				}
			}
			else {
				rifLogger.debug(this.getClass(), "getColumnComment() database: " + databaseType +
					"; expected 1 row, got none");
			}
		}
		catch (Exception exception) {
			rifLogger.error(this.getClass(), "Error in SQL Statement (" + databaseType + ") >>> " +
				lineSeparator + columnCommentQueryFormatter.generateQuery(),
				exception);
			throw exception;
		}
		finally {
			closeStatement(statement);
		}
		
		return columnComment;
	}
	
	@Override
	public void enableDatabaseDebugMessages(final Connection connection)
			throws RIFServiceException {
			
		SQLFunctionCallerQueryFormatter setupDatabaseLogQueryFormatter 
			= new SQLFunctionCallerQueryFormatter();
		setupDatabaseLogQueryFormatter.setDatabaseSchemaName("rif40_log_pkg");
		setupDatabaseLogQueryFormatter.setFunctionName("rif40_log_setup");
		setupDatabaseLogQueryFormatter.setNumberOfFunctionParameters(0);		
		PreparedStatement setupLogStatement = null;
		
		SQLFunctionCallerQueryFormatter sendDebugToInfoQueryFormatter 
			= new SQLFunctionCallerQueryFormatter();
		sendDebugToInfoQueryFormatter.setDatabaseSchemaName("rif40_log_pkg");
		sendDebugToInfoQueryFormatter.setFunctionName("rif40_send_debug_to_info");
		sendDebugToInfoQueryFormatter.setNumberOfFunctionParameters(1);		
		
		
		PreparedStatement sendDebugToInfoStatement = null;
		try {
			setupLogStatement 
				= createPreparedStatement(
					connection, 
					setupDatabaseLogQueryFormatter);
			setupLogStatement.executeQuery();
			
			sendDebugToInfoStatement 
				= createPreparedStatement(
					connection, 
					sendDebugToInfoQueryFormatter);
			sendDebugToInfoStatement.setBoolean(1, true);
			sendDebugToInfoStatement.executeQuery();						
		}
		catch(SQLException sqlException) {
			String errorMessage
				= SERVICE_MESSAGES.getMessage("abstractSQLManager.error.unableToEnableDatabaseDebugging");

			throw new RIFServiceException(
				RIFServiceError.DB_UNABLE_TO_MAINTAIN_DEBUG,
				errorMessage);
		}
		finally {
			closeStatement(setupLogStatement);
			closeStatement(sendDebugToInfoStatement);	
		}		
	}
	
	@Override
	public void setEnableLogging(final boolean enableLogging) {
		this.enableLogging = enableLogging;
	}	
	
	@Override
	public void logSQLQuery(final String queryName, final QueryFormatter queryFormatter,
			final String... parameters) {
		
		if (!enableLogging || queryLoggingIsDisabled(queryName)) {
			return;
		}

		StringBuilder queryLog = new StringBuilder();
		queryLog.append("QUERY NAME: ").append(queryName).append(lineSeparator);
		queryLog.append("PARAMETERS:").append(lineSeparator);
		for (int i = 0; i < parameters.length; i++) {
			queryLog.append("\t");
			queryLog.append(i + 1);
			queryLog.append(":\"");
			queryLog.append(parameters[i]);
			queryLog.append("\"").append(lineSeparator);
		}
		queryLog.append("SQL QUERY TEXT: ").append(lineSeparator);
		queryLog.append(queryFormatter.generateQuery()).append(lineSeparator);
		queryLog.append("<<< End BaseSQLManager logSQLQuery").append(lineSeparator);
	
		rifLogger.info(this.getClass(), "BaseSQLManager logSQLQuery >>>" +
			lineSeparator + queryLog.toString());
	}
	
	protected void logSQLQuery(
		final String queryName,
		final QueryFormatter queryFormatter,
		final int[] parameters) {
		
		if (!enableLogging || queryLoggingIsDisabled(queryName)) {
			return;
		}

		StringBuilder queryLog = new StringBuilder();
		queryLog.append("QUERY NAME: ").append(queryName).append(lineSeparator);
		queryLog.append("PARAMETERS:").append(lineSeparator);
		for (int i = 0; i < parameters.length; i++) {
			queryLog.append("\t");
			queryLog.append(i + 1);
			queryLog.append(":\"");
			queryLog.append(parameters[i]);
			queryLog.append("\"").append(lineSeparator);
		}
		queryLog.append("SQL QUERY TEXT: ").append(lineSeparator);
		queryLog.append(queryFormatter.generateQuery()).append(lineSeparator);
		queryLog.append("<<< End BaseSQLManager logSQLQuery").append(lineSeparator);
	
		rifLogger.info(this.getClass(), "BaseSQLManager logSQLQuery >>>" +
			lineSeparator + queryLog.toString());	

	}
	
	protected void logSQLQuery(
		final String queryName,
		final QueryFormatter queryFormatter) {
		
		if (!enableLogging || queryLoggingIsDisabled(queryName)) {
			return;
		}
		
		String queryLog = ("QUERY NAME: " + queryName + lineSeparator)
		                  + "NO PARAMETERS." + lineSeparator
		                  + "SQL QUERY TEXT: " + lineSeparator
		                  + queryFormatter.generateQuery() + lineSeparator
		                  + "<<< End BaseSQLManager logSQLQuery" + lineSeparator;
		rifLogger.info(this.getClass(), "BaseSQLManager logSQLQuery >>>" +
		                                lineSeparator + queryLog);
	}
		
	@Override
	public void logSQLException(final SQLException sqlException) {
		rifLogger.error(getClass(), "BaseSQLManager.logSQLException error",
				sqlException);
	}

	protected void logException(final Exception exception) {
		rifLogger.error(this.getClass(), "BaseSQLManager.logException error",
				 exception);
	}

	protected boolean queryLoggingIsDisabled(
			final String queryName) {

		if (prop == null) {

			try {
				prop = new TomcatFile(
						new TomcatBase(),
						BaseSQLManager.ABSTRACT_SQLMANAGER_PROPERTIES).properties();
			} catch (IOException e) {
				rifLogger.warning(this.getClass(),
				                  "BaseSQLManager.checkIfQueryLoggingEnabled error for" +
				                  BaseSQLManager.ABSTRACT_SQLMANAGER_PROPERTIES, e);
				return false;
			}
		}
		String value = prop.getProperty(queryName);
		if (value != null) {
			if (value.toLowerCase().equals("true")) {
				rifLogger.debug(this.getClass(),
						"BaseSQLManager checkIfQueryLoggingEnabled=TRUE property: " +
								queryName + "=" + value);
				return false;
			} else {
				rifLogger.debug(this.getClass(),
						"BaseSQLManager checkIfQueryLoggingEnabled=FALSE property: " +
								queryName + "=" + value);
				return true;
			}
		} else {
			rifLogger.warning(this.getClass(),
					"BaseSQLManager checkIfQueryLoggingEnabled=FALSE property: " +
							queryName + " NOT FOUND");
			return true;
		}
	}

	@Override
	public boolean isUserBlocked(
			final User user) {
		
		if (user == null) {
			return false;
		}
		
		String userID = user.getUserID();
		return userID != null && userIDsToBlock.contains(userID);
		
	}
	
	@Override
	public void logSuspiciousUserEvent(
			final User user) {

		String userID = user.getUserID();

		Integer suspiciousEventCounter = suspiciousEventCounterFromUser.get(userID);
		if (suspiciousEventCounter == null) {

			//no incidents recorded yet, this is the first
			suspiciousEventCounterFromUser.put(userID, 1);
		}
		else {
			suspiciousEventCounterFromUser.put(userID, ++suspiciousEventCounter);
		}
	}
	
	/**
	 * Assumes that user is valid.
	 *
	 * @param user the user
	 * @return the connection
	 * @throws RIFServiceException the RIF service exception
	 */
	@Override
	public Connection assignPooledReadConnection(final User user) throws RIFServiceException {

		Connection result;

		String userID = user.getUserID();
		if (userIDsToBlock.contains(userID)) {
			return null;
		}

		ConnectionQueue availableReadConnectionQueue =
						readOnlyConnectionsFromUser.get(user.getUserID());

		try {
			result = availableReadConnectionQueue.assignConnection();
		}
		catch(Exception exception) {
			//Record original exception, throw sanitised, human-readable version
			logException(exception);
			String errorMessage = SERVICE_MESSAGES.getMessage(
					"sqlConnectionManager.error.unableToAssignReadConnection");

			rifLogger.error(
					getClass(),
					errorMessage,
					exception);
			
			throw new RIFServiceException(
					RIFServiceError.DATABASE_QUERY_FAILED,
					errorMessage);
		}
		return result;
	}
	
	@Override
	public void reclaimPooledWriteConnection(
			final User user,
			final Connection connection)
					throws RIFServiceException {

		try {

			if (user == null) {
				return;
			}
			if (connection == null) {
				return;
			}

			//connection.setAutoCommit(true);
			ConnectionQueue writeOnlyConnectionQueue =
					writeConnectionsFromUser.get(user.getUserID());
			writeOnlyConnectionQueue.reclaimConnection(connection);
		}
		catch(Exception exception) {
			//Record original exception, throw sanitised, human-readable version
			logException(exception);
			String errorMessage = SERVICE_MESSAGES.getMessage(
					"sqlConnectionManager.error.unableToReclaimWriteConnection");

			rifLogger.error(getClass(), errorMessage, exception);
			
			throw new RIFServiceException(
					RIFServiceError.DATABASE_QUERY_FAILED,
					errorMessage);
		}
	}
	
	@Override
	public void logout(final User user) throws RIFServiceException {

		if (user == null) {
			return;
		}

		String userID = user.getUserID();
		if (userID == null) {
			return;
		}

		if (!registeredUserIDs.contains(userID)) {
			//Here we anticipate the possibility that the user
			//may not be registered.  In this case, there is no chance
			//that there are connections that need to be closed for that ID
			return;
		}

		closeConnectionsForUser(userID);
		registeredUserIDs.remove(userID);
		suspiciousEventCounterFromUser.remove(userID);
	}

	/**
	 * Deregister user.
	 *
	 * @param userID the user
	 * @throws RIFServiceException the RIF service exception
	 */
	protected void closeConnectionsForUser(
			final String userID)
					throws RIFServiceException {

		ConnectionQueue readOnlyConnectionQueue = readOnlyConnectionsFromUser.get(userID);
		if (readOnlyConnectionQueue != null) {
			readOnlyConnectionQueue.closeAllConnections();
		}

		ConnectionQueue writeConnectionQueue = writeConnectionsFromUser.get(userID);
		if (writeConnectionQueue != null) {
			writeConnectionQueue.closeAllConnections();
		}
	}
	
	@Override
	public boolean isLoggedIn(
			final String userID) {

		return registeredUserIDs.contains(userID);
	}
	
	@Override
	public boolean userExceededMaximumSuspiciousEvents(final User user) {
		
		String userID = user.getUserID();
		Integer suspiciousEventCounter
						= suspiciousEventCounterFromUser.get(userID);
		return suspiciousEventCounter != null
		       && suspiciousEventCounter >= MAXIMUM_SUSPICIOUS_EVENTS_THRESHOLD;
	}
	
	/**
	 * User exists.
	 *
	 * @param userID the user id
	 * @return true, if successful
	 */
	@Override
	public boolean userExists(final String userID) {

		return isLoggedIn(userID);
	}
	
	@Override
	public void addUserIDToBlock(final User user) {

		if (user == null) {
			return;
		}

		String userID = user.getUserID();
		if (userID == null) {
			return;
		}

		if (userIDsToBlock.contains(userID)) {
			return;
		}

		userIDsToBlock.add(userID);
	}
	
	/**
	 * Assumes that user is valid.
	 *
	 * @param user the user
	 * @return the connection
	 * @throws RIFServiceException the RIF service exception
	 */
	@Override
	public Connection assignPooledWriteConnection(
			final User user)
					throws RIFServiceException {

		Connection result;
		try {

			String userID = user.getUserID();
			if (userIDsToBlock.contains(userID)) {
				return null;
			}

			ConnectionQueue writeConnectionQueue = writeConnectionsFromUser.get(user.getUserID());
			result = writeConnectionQueue.assignConnection();
		}
		catch(Exception exception) {
			//Record original exception, throw sanitised, human-readable version
			logException(exception);
			String errorMessage = SERVICE_MESSAGES.getMessage(
					"sqlConnectionManager.error.unableToAssignWriteConnection");

			rifLogger.error(
					getClass(),
					errorMessage,
					exception);
			
			throw new RIFServiceException(
					RIFServiceError.DATABASE_QUERY_FAILED,
					errorMessage);
		}

		return result;
	}

	private void closeStatement(PreparedStatement statement) {

		if (statement == null) {
			return;
		}

		try {
			statement.close();
		}
		catch(SQLException ignore) {}
	}

	/**
	 * Register user.
	 *
	 * @param userID the user id
	 * @param password the password
	 * @throws RIFServiceException the RIF service exception
	 */
	public void login(
		final String userID,
		final String password)
		throws RIFServiceException {

		if (userIDsToBlock.contains(userID)) {
			return;
		}

		/*
		 * First, check whether person is already logged in.  We can do this
		 * by checking whether
		 */

		if (userExists(userID)) {
			return;
		}

		ConnectionQueue readOnlyConnectionQueue = new ConnectionQueue();
		ConnectionQueue writeOnlyConnectionQueue = new ConnectionQueue();
		try {
			Class.forName(rifServiceStartupOptions.getDatabaseDriverClassName());

			//note that in order to optimise the setup of connections,
			//we call rif40_init(boolean no_checks).  The first time we call it
			//for a user, we let the checks occur (set flag to false)
			//for all other times, set the flag to true, to ignore checks

			//Establish read-only connections
			for (int i = 0; i < POOLED_READ_ONLY_CONNECTIONS_PER_PERSON; i++) {
				boolean isFirstConnectionForUser = false;
				if (i == 0) {
					isFirstConnectionForUser = true;
				}
				Connection currentConnection
					= createConnection(
						userID,
						password,
						isFirstConnectionForUser,
						true);
				readOnlyConnectionQueue.addConnection(currentConnection);
			}
			readOnlyConnectionsFromUser.put(userID, readOnlyConnectionQueue);

			//Establish write-only connections
			for (int i = 0; i < POOLED_WRITE_CONNECTIONS_PER_PERSON; i++) {
				Connection currentConnection
					= createConnection(
						userID,
						password,
						false,
						false);
				writeOnlyConnectionQueue.addConnection(currentConnection);
			}
			writeConnectionsFromUser.put(userID, writeOnlyConnectionQueue);

			passwordHashList.put(userID, password);
			registeredUserIDs.add(userID);

		//	rifLogger.info(this.getClass(), "JAVA LIBRARY PATH >>>");
		//	rifLogger.info(this.getClass(), System.getProperty("java.library.path"));

			rifLogger.info(this.getClass(), "XXXXXXXXXXX M S S Q L S E R V E R XXXXXXXXXX");
		}
		catch(ClassNotFoundException classNotFoundException) {
			RIFServiceExceptionFactory exceptionFactory
				= new RIFServiceExceptionFactory();
			throw exceptionFactory.createUnableLoadDBDriver();
		}
		catch(SQLException sqlException) {
			readOnlyConnectionQueue.closeAllConnections();
			writeOnlyConnectionQueue.closeAllConnections();
			String errorMessage = SERVICE_MESSAGES.getMessage(
					"sqlConnectionManager.error.unableToRegisterUser",
					userID);

			rifLogger.error(
					getClass(),
				errorMessage,
				sqlException);

			RIFServiceExceptionFactory exceptionFactory
				= new RIFServiceExceptionFactory();
			throw exceptionFactory.createUnableToRegisterUser(userID);
		}

	}

	private Connection createConnection(
		final String userID,
		final String password,
		final boolean isFirstConnectionForUser,
		final boolean isReadOnly)
		throws SQLException,
		RIFServiceException {

		Connection connection;
		PreparedStatement statement = null;
		try {

			Properties databaseProperties = new Properties();
			databaseProperties.setProperty("user", userID);
			databaseProperties.setProperty("password", password);

			if (rifServiceStartupOptions.getRIFDatabaseProperties().isSSLSupported()) {
				databaseProperties.setProperty("ssl", "true");
			}

			databaseProperties.setProperty("prepareThreshold", "3");

			connection = DriverManager.getConnection(databaseURL, databaseProperties);

			//Execute RIF start-up function
			//MSSQL > EXEC rif40.rif40_startup ?
			//PGSQL > SELECT rif40_startup(?) AS rif40_init;

			statement = SQLQueryUtility.createPreparedStatement(
					connection,
					initialisationQuery);

			setDbSpecificStatementValues(statement, isFirstConnectionForUser);

			statement.execute();
			statement.close();

			if (isReadOnly) {
				connection.setReadOnly(true);
			}
			else {
				connection.setReadOnly(false);
			}
			connection.setAutoCommit(false);
		}
		finally {
			SQLQueryUtility.close(statement);
		}

		return connection;
	}

	private void setDbSpecificStatementValues(
			final PreparedStatement statement, final boolean isFirstConnectionForUser)
			throws SQLException {

		switch (rifDatabaseProperties.getDatabaseType()) {
			case POSTGRESQL:
				statement.setBoolean(1, isFirstConnectionForUser);
				break;

			case SQL_SERVER:
				statement.setInt(1, isFirstConnectionForUser ? 1 : 0);
				break;

			case UNKNOWN:
				// Shouldn't be possible.
				break;
		}


	}

	/**
	 * Generate url text.
	 *
	 * @return the string
	 */
	private String generateURLText() {

		return rifServiceStartupOptions.getDatabaseDriverPrefix()
		       + ":"
		       + "//"
		       + rifServiceStartupOptions.getHost()
		       + ":"
		       + rifServiceStartupOptions.getPort()
		       + "/"
		       + rifServiceStartupOptions.getDatabaseName();
	}

	public void deregisterAllUsers() throws RIFServiceException {

		for (String registeredUserID : registeredUserIDs) {
			closeConnectionsForUser(registeredUserID);
		}

		registeredUserIDs.clear();
	}

	public void reclaimPooledReadConnection(
		final User user,
		final Connection connection)
		throws RIFServiceException {

		try {

			if (user == null) {
				return;
			}
			if (connection == null) {
				return;
			}
			String userID = user.getUserID();
			ConnectionQueue availableReadConnections
				= readOnlyConnectionsFromUser.get(userID);
			availableReadConnections.reclaimConnection(connection);
		}
		catch(Exception exception) {
			//Record original exception, throw sanitised, human-readable version
			logException(exception);
			String errorMessage
				= SERVICE_MESSAGES.getMessage(
					"sqlConnectionManager.error.unableToReclaimReadConnection");


			rifLogger.error(
				getClass(),
				errorMessage,
				exception);

			throw new RIFServiceException(
				RIFServiceError.DATABASE_QUERY_FAILED,
				errorMessage);
		}

	}

	/**
	 * User password.
	 *
	 * @param user the user id
	 * @return password String, if successful
	 */
	public String getUserPassword(
			final User user) {

		if (userExists(user.getUserID()) && !isUserBlocked(user)) {
			return passwordHashList.get(user.getUserID());
		}
		else {
			return null;
		}
	}

	@Override
	public CallableStatement createPreparedCall( // Use MSSQLQueryUtility
			final Connection connection,
			final String query)
		throws SQLException {

		return SQLQueryUtility.createPreparedCall(
			connection,
			query);

	}
}
