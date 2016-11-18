package rifDataLoaderTool.dataStorageLayer.ms;

import rifDataLoaderTool.system.RIFDataLoaderToolError;
import rifDataLoaderTool.system.RIFDataLoaderToolMessages;
import rifDataLoaderTool.businessConceptLayer.WorkflowState;
import rifDataLoaderTool.businessConceptLayer.DataLoaderToolSettings;
import rifDataLoaderTool.businessConceptLayer.RIFDatabaseConnectionParameters;
import rifGenericLibrary.dataStorageLayer.SQLGeneralQueryFormatter;
import rifGenericLibrary.dataStorageLayer.pg.PGSQLCreateTableQueryFormatter;
import rifGenericLibrary.dataStorageLayer.pg.PGSQLDeleteTableQueryFormatter;
import rifGenericLibrary.dataStorageLayer.pg.PGSQLQueryUtility;
import rifGenericLibrary.system.RIFGenericLibraryError;
import rifGenericLibrary.system.RIFServiceException;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.PropertyResourceBundle;



/**
 * Because the data loader tool can modify a lot of important tables in the RIF, we
 * use a fake rif database for testing purposes.  This class creates some of the
 * basic tables that appear in the production RIF database.
 *
 * <hr>
 * Copyright 2015 Imperial College London, developed by the Small Area
 * Health Statistics Unit. 
 *
 * <pre> 
 * This file is part of the Rapid Inquiry Facility (RIF) project.
 * RIF is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * RIF is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with RIF.  If not, see <http://www.gnu.org/licenses/>.
 * </pre>
 *
 * <hr>
 * Kevin Garwood
 * @author kgarwood
 */

/*
 * Code Road Map:
 * --------------
 * Code is organised into the following sections.  Wherever possible, 
 * methods are classified based on an order of precedence described in 
 * parentheses (..).  For example, if you're trying to find a method 
 * 'getName(...)' that is both an interface method and an accessor 
 * method, the order tells you it should appear under interface.
 * 
 * Order of 
 * Precedence     Section
 * ==========     ======
 * (1)            Section Constants
 * (2)            Section Properties
 * (3)            Section Construction
 * (7)            Section Accessors and Mutators
 * (6)            Section Errors and Validation
 * (5)            Section Interfaces
 * (4)            Section Override
 *
 */

public class MSSQLSampleRIFDatabaseCreationManager {
	
	// ==========================================
	// Section Constants
	// ==========================================

	// ==========================================
	// Section Properties
	// ==========================================
	private DataLoaderToolSettings dataLoaderToolSettings;
	
	// ==========================================
	// Section Construction
	// ==========================================

	public MSSQLSampleRIFDatabaseCreationManager(
		final DataLoaderToolSettings dataLoaderToolSettings) {

		this.dataLoaderToolSettings = dataLoaderToolSettings;
	}
	
	// ==========================================
	// Section Accessors and Mutators
	// ==========================================

	public void setup()
		throws RIFServiceException {
		
		PreparedStatement statement = null;
		Connection connection = null;
		try {
			
			File userLoginDetailsFile = new File("C://rif_scripts//db//RIFDatabaseProperties.txt");
			FileReader fileReader = new FileReader(userLoginDetailsFile);
			PropertyResourceBundle userLoginResourceBundle
				= new PropertyResourceBundle(fileReader);
			
			String userID = (String) userLoginResourceBundle.getObject("userID");
			String password = (String) userLoginResourceBundle.getObject("password");
			
			RIFDatabaseConnectionParameters dbParameters
				= dataLoaderToolSettings.getDatabaseConnectionParameters();
			StringBuilder urlText = new StringBuilder();
			urlText.append(dbParameters.getDatabaseDriverPrefix());
			urlText.append(":");
			urlText.append("//");
			urlText.append(dbParameters.getHostName());
			urlText.append(":");
			urlText.append(dbParameters.getPortName());
			urlText.append("/");			
			
			String databaseURL = urlText.toString();	

			/*
			 * #POSSIBLE_PORTING_ISSUE
			 * drop-if-exists may have to be implemented separately for
			 * each database type
			 */				
			connection
				= DriverManager.getConnection(databaseURL, userID, password);
			SQLGeneralQueryFormatter queryFormatter
				= new SQLGeneralQueryFormatter();
			queryFormatter.addQueryPhrase("DROP DATABASE IF EXISTS ");
			queryFormatter.addQueryPhrase(dbParameters.getDatabaseName());
			queryFormatter.addQueryPhrase(";");
			queryFormatter.finishLine();
			queryFormatter.addQueryPhrase("CREATE DATABASE ");
			queryFormatter.addQueryPhrase(dbParameters.getDatabaseName());
			queryFormatter.addQueryPhrase(";");
			queryFormatter.finishLine();

			statement
				= connection.prepareStatement(queryFormatter.generateQuery());
			statement.executeUpdate();
			createDatabaseTables(userID, password);
		}
		catch(Exception exception) {
			exception.printStackTrace(System.out);
			String errorMessage	
				= RIFDataLoaderToolMessages.getMessage(
					"sampleRIFDatabaseCreationManager.error.unableToInitialiseDatabase");
			RIFServiceException rifServiceException
				= new RIFServiceException(
					RIFGenericLibraryError.DATABASE_QUERY_FAILED, 
					errorMessage);
			throw rifServiceException;			
		}
		finally {
			PGSQLQueryUtility.close(statement);
			PGSQLQueryUtility.close(connection);
		}
		
	}
		
	public void createDatabaseTables(
		final String userID,
		final String password) 
		throws RIFServiceException {

		PreparedStatement statement = null;
		Connection connection = null;		
		try {
			StringBuilder urlText = new StringBuilder();
			RIFDatabaseConnectionParameters dbParameters
				= dataLoaderToolSettings.getDatabaseConnectionParameters();
			urlText.append(dbParameters.getDatabaseDriverPrefix());
			urlText.append(":");
			urlText.append("//");
			urlText.append(dbParameters.getHostName());
			urlText.append(":");
			urlText.append(dbParameters.getPortName());
			urlText.append("/");
			urlText.append(dbParameters.getDatabaseName());
			String databaseURL = urlText.toString();			
			
			
			connection
				= DriverManager.getConnection(databaseURL, userID, password);

			createCovariatesTable(connection);
			createDataSetConfigurationsTable(connection);
			createAuditChangesTable(connection);
			createAuditFailedValidationTable(connection);		
		}
		catch(Exception exception) {
			exception.printStackTrace(System.out);
			String errorMessage	
				= RIFDataLoaderToolMessages.getMessage(
					"sampleRIFDatabaseCreationManager.error.unableToInitialiseDatabase");
			RIFServiceException rifServiceException
				= new RIFServiceException(
					RIFGenericLibraryError.DATABASE_QUERY_FAILED, 
					errorMessage);
			throw rifServiceException;			
		}
		finally {
			PGSQLQueryUtility.close(statement);
			PGSQLQueryUtility.close(connection);
		}
		
	}
	
	private void createCovariatesTable(
		final Connection connection) 
		throws RIFServiceException {
		
		PreparedStatement statement = null;
		try {			
			//create covariates table
			PGSQLCreateTableQueryFormatter createCovariateTableQueryFormatter
				= new PGSQLCreateTableQueryFormatter();
			createCovariateTableQueryFormatter.setTableName("rif40_covariates");
			createCovariateTableQueryFormatter.addTextFieldDeclaration(
				"geography", 
				50, 
				false);
			createCovariateTableQueryFormatter.addTextFieldDeclaration(
				"geolevel_name", 
				30, 
				false);
			createCovariateTableQueryFormatter.addTextFieldDeclaration(
				"covariate_name", 
				30, 
				false);
			
			createCovariateTableQueryFormatter.addDoubleFieldDeclaration(
				"min", 
				false);

			createCovariateTableQueryFormatter.addDoubleFieldDeclaration(
				"max", 
				false);

			createCovariateTableQueryFormatter.addDoubleFieldDeclaration(
				"type", 
				false);
		
			statement
				= PGSQLQueryUtility.createPreparedStatement(
					connection, 
					createCovariateTableQueryFormatter);
		
			statement.executeUpdate();
		}
		catch(SQLException sqlException) {
			sqlException.printStackTrace(System.out);
			String errorMessage
				= RIFDataLoaderToolMessages.getMessage(
					"sampleRIFDatabaseCreationManager.error.unableToInitialiseDB");
			RIFServiceException rifServiceException
				= new RIFServiceException(
					RIFDataLoaderToolError.UNABLE_TO_CREATE_FAKE_RIF_DB, 
					errorMessage);
			throw rifServiceException;
		}
		finally {
			PGSQLQueryUtility.close(statement);
			
		}
	}

	private void createDataSetConfigurationsTable(
		final Connection connection) 
		throws RIFServiceException {
		
		PreparedStatement createIDSequenceStatement = null;
		PreparedStatement createDataSetConfigurationTableStatement = null;
		PreparedStatement sequenceOwnershipStatement = null;
		try {

			/*
			 * #POSSIBLE_PORTING_ISSUE
			 * CREATE SEQUENCE is something in PostgreSQL that will likely
			 * require porting in SQL Server
			 */							
			//Create a sequence that will auto-increment
			SQLGeneralQueryFormatter createIDSequenceQueryFormatter = new SQLGeneralQueryFormatter();
			createIDSequenceQueryFormatter.addQueryPhrase(
				0, 
				"CREATE SEQUENCE data_set_sequence;");
			createIDSequenceStatement
				= PGSQLQueryUtility.createPreparedStatement(
						connection, 
						createIDSequenceQueryFormatter);
			createIDSequenceStatement.executeUpdate();
			
			//Make the 'id' field get its value from the sequence
			PGSQLCreateTableQueryFormatter createDataSetConfigurationsTableFormatter 
				= new PGSQLCreateTableQueryFormatter();
			createDataSetConfigurationsTableFormatter.setTableName("data_set_configurations");
			createDataSetConfigurationsTableFormatter.addSequenceField("id", "data_set_sequence");
			createDataSetConfigurationsTableFormatter.addTextFieldDeclaration("core_data_set_name", 50, false);
			createDataSetConfigurationsTableFormatter.addTextFieldDeclaration("version", 20, false);
			createDataSetConfigurationsTableFormatter.addCreationTimestampField("creation_date");
			String startCode = WorkflowState.START.getCode();
			createDataSetConfigurationsTableFormatter.addTextFieldDeclaration(
				"current_workflow_state", 
				20, 
				startCode,
				false);
			
			createDataSetConfigurationTableStatement
				= PGSQLQueryUtility.createPreparedStatement(
					connection, 
					createDataSetConfigurationsTableFormatter);
			
			createDataSetConfigurationTableStatement.executeUpdate();

			/*
			 * #POSSIBLE_PORTING_ISSUE
			 * CREATE SEQUENCE is something in PostgreSQL that will likely
			 * require porting in SQL Server
			 */							

			
			//Ensure that the id field owns the sequence so that no other table could
			//increment it
			SQLGeneralQueryFormatter sequenceOwnershipQueryFormatter
				= new SQLGeneralQueryFormatter();
			sequenceOwnershipQueryFormatter.addQueryPhrase(0, "ALTER SEQUENCE data_set_sequence ");
			sequenceOwnershipQueryFormatter.addQueryPhrase("OWNED BY ");
			sequenceOwnershipQueryFormatter.addQueryPhrase("data_set_configurations.id;");
			sequenceOwnershipStatement
				= PGSQLQueryUtility.createPreparedStatement(
					connection, 
					sequenceOwnershipQueryFormatter);
			sequenceOwnershipStatement.executeUpdate();			
		}
		catch(SQLException sqlException) {
			sqlException.printStackTrace(System.out);
			String errorMessage
				= RIFDataLoaderToolMessages.getMessage(
					"sampleRIFDatabaseCreationManager.error.unableToInitialiseDB");
			RIFServiceException rifServiceException
				= new RIFServiceException(
					RIFDataLoaderToolError.UNABLE_TO_CREATE_FAKE_RIF_DB, 
					errorMessage);
			throw rifServiceException;
		}
		finally {
			PGSQLQueryUtility.close(createIDSequenceStatement);
			PGSQLQueryUtility.close(createDataSetConfigurationTableStatement);
			PGSQLQueryUtility.close(sequenceOwnershipStatement);		
		}	
		
	}
	
	public void createAuditChangesTable(
		final Connection connection) 
		throws RIFServiceException {
		
		PreparedStatement statement = null;
		try {
			PGSQLCreateTableQueryFormatter queryFormatter
				= new PGSQLCreateTableQueryFormatter();
			queryFormatter.setTableName("rif_change_log");

			queryFormatter.addIntegerFieldDeclaration(
				"data_set_id", 
				false);

			queryFormatter.addIntegerFieldDeclaration(
				"row_number", 
				false);
			
			queryFormatter.addTextFieldDeclaration(
				"field_name", 
				30, 
				false);

			
			queryFormatter.addTextFieldDeclaration(
				"old_value", 
				30, 
				true);

			
			queryFormatter.addTextFieldDeclaration(
				"new_value", 
				30, 
				true);
		
			queryFormatter.addCreationTimestampField("time_stamp");
			statement
				= PGSQLQueryUtility.createPreparedStatement(
				connection, 
				queryFormatter);
			statement.executeUpdate();
		}
		catch(SQLException sqlException) {
			String errorMessage
				= RIFDataLoaderToolMessages.getMessage(
					"sampleRIFDatabaseCreationManager.error.unableToInitialiseDB");
			RIFServiceException rifServiceException
				= new RIFServiceException(
					RIFDataLoaderToolError.DATABASE_QUERY_FAILED, 
					errorMessage);
			throw rifServiceException;
		}	
	}

	
	public void createAuditFailedValidationTable(
		final Connection connection) 
		throws RIFServiceException {
		
		PreparedStatement statement = null;
		try {
			PGSQLCreateTableQueryFormatter queryFormatter
				= new PGSQLCreateTableQueryFormatter();
			queryFormatter.setTableName("rif_failed_val_log");

			queryFormatter.addIntegerFieldDeclaration(
				"data_set_id", 
				false);

			queryFormatter.addIntegerFieldDeclaration(
				"row_number", 
				false);
			
			queryFormatter.addTextFieldDeclaration(
				"field_name", 
				30, 
				false);

			
			queryFormatter.addTextFieldDeclaration(
				"invalid_value", 
				30, 
				true);
		
			queryFormatter.addCreationTimestampField("time_stamp");
			statement
				= PGSQLQueryUtility.createPreparedStatement(
				connection, 
				queryFormatter);
			statement.executeUpdate();
		}
		catch(SQLException sqlException) {
			String errorMessage
				= RIFDataLoaderToolMessages.getMessage(
					"sampleRIFDatabaseCreationManager.error.unableToInitialiseDB");
			RIFServiceException rifServiceException
				= new RIFServiceException(
					RIFDataLoaderToolError.DATABASE_QUERY_FAILED, 
					errorMessage);
			throw rifServiceException;
		}	
	}
	
	
	public void deleteTables(
		final Connection connection) 
		throws RIFServiceException {
		
		PreparedStatement deleteCovariateTableStatement = null;
		try {
			PGSQLDeleteTableQueryFormatter deleteCovariateTableQueryFormatter
				= new PGSQLDeleteTableQueryFormatter();
			deleteCovariateTableQueryFormatter.setTableToDelete("rif40_covariates");

			deleteCovariateTableStatement
				= PGSQLQueryUtility.createPreparedStatement(
					connection, 
					deleteCovariateTableQueryFormatter);
			
			deleteCovariateTableStatement.executeUpdate();
		}
		catch(SQLException sqlException) {
			sqlException.printStackTrace(System.out);
			String errorMessage
				= RIFDataLoaderToolMessages.getMessage(
					"sampleRIFDatabaseCreationManager.error.unableToInitialiseDB");
			RIFServiceException rifServiceException
				= new RIFServiceException(
					RIFDataLoaderToolError.UNABLE_TO_CREATE_FAKE_RIF_DB, 
					errorMessage);
			throw rifServiceException;
		}
		finally {
			PGSQLQueryUtility.close(deleteCovariateTableStatement);
		}	
	}
	
	
	
	// ==========================================
	// Section Errors and Validation
	// ==========================================

	// ==========================================
	// Section Interfaces
	// ==========================================

	// ==========================================
	// Section Override
	// ==========================================

}

