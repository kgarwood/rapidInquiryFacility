package rifServices.dataStorageLayer.pg;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import rifGenericLibrary.businessConceptLayer.User;
import rifGenericLibrary.dataStorageLayer.RIFDatabaseProperties;
import rifGenericLibrary.dataStorageLayer.pg.PGSQLQueryUtility;
import rifGenericLibrary.dataStorageLayer.pg.PGSQLRecordExistsQueryFormatter;
import rifGenericLibrary.dataStorageLayer.pg.PGSQLSelectQueryFormatter;
import rifGenericLibrary.system.Messages;
import rifGenericLibrary.system.RIFServiceException;
import rifGenericLibrary.util.RIFLogger;
import rifServices.businessConceptLayer.AbstractGeographicalArea;
import rifServices.businessConceptLayer.ComparisonArea;
import rifServices.businessConceptLayer.DiseaseMappingStudy;
import rifServices.businessConceptLayer.DiseaseMappingStudyArea;
import rifServices.businessConceptLayer.GeoLevelSelect;
import rifServices.businessConceptLayer.GeoLevelToMap;
import rifServices.businessConceptLayer.GeoLevelView;
import rifServices.businessConceptLayer.Geography;
import rifServices.businessConceptLayer.Investigation;
import rifServices.businessConceptLayer.Project;
import rifServices.system.RIFServiceError;
import rifServices.system.RIFServiceMessages;

//import rifServices.businessConceptLayer.AbstractRIFConcept.ValidationPolicy;

/**
 *
 *
 * <hr>
 * The Rapid Inquiry Facility (RIF) is an automated tool devised by SAHSU 
 * that rapidly addresses epidemiological and public health questions using 
 * routinely collected health and population data and generates standardised 
 * rates and relative risks for any given health outcome, for specified age 
 * and year ranges, for any given geographical area.
 *
 * <p>
 * Copyright 2017 Imperial College London, developed by the Small Area
 * Health Statistics Unit. The work of the Small Area Health Statistics Unit 
 * is funded by the Public Health England as part of the MRC-PHE Centre for 
 * Environment and Health. Funding for this project has also been received 
 * from the United States Centers for Disease Control and Prevention.  
 * </p>
 *
 * <pre> 
 * This file is part of the Rapid Inquiry Facility (RIF) project.
 * RIF is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RIF is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with RIF. If not, see <http://www.gnu.org/licenses/>; or write 
 * to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, 
 * Boston, MA 02110-1301 USA
 * </pre>
 *
 * <hr>
 * Kevin Garwood
 * @author kgarwood
 * @version
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

final class PGSQLDiseaseMappingStudyManager 
	extends PGSQLAbstractSQLManager {

	// ==========================================
	// Section Constants
	// ==========================================
	
	private Messages GENERIC_MESSAGES = Messages.genericMessages();
	
	// ==========================================
	// Section Properties
	// ==========================================
	private PGSQLRIFContextManager rifContextManager;
	private PGSQLInvestigationManager investigationManager;

	// ==========================================
	// Section Construction
	// ==========================================

	/**
	 * Instantiates a new SQL disease mapping study manager.
	 */
	public PGSQLDiseaseMappingStudyManager(
		final RIFDatabaseProperties rifDatabaseProperties,
		final PGSQLRIFContextManager rifContextManager,
		final PGSQLInvestigationManager investigationManager,
		final PGSQLMapDataManager mapDataManager) {

		super(rifDatabaseProperties);
		this.rifContextManager = rifContextManager;
		this.investigationManager = investigationManager;

	}

	// ==========================================
	// Section Accessors and Mutators
	// ==========================================

	/**
	 * Gets the projects.
	 *
	 * @param connection the connection
	 * @param user the user
	 * @return the projects
	 * @throws RIFServiceException the RIF service exception
	 */
	public ArrayList<Project> getProjects(
		final Connection connection,
		final User user) 
		throws RIFServiceException {
		
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		ArrayList<Project> results = new ArrayList<Project>();
		try {
			
			PGSQLSelectQueryFormatter queryFormatter = new PGSQLSelectQueryFormatter();
			configureQueryFormatterForDB(queryFormatter);		
			queryFormatter.addSelectField("project");
			queryFormatter.addSelectField("description");
			queryFormatter.addSelectField("date_started");		
			queryFormatter.addSelectField("date_ended");		
			queryFormatter.addFromTable("rif40_projects");
				
			logSQLQuery(
				"getProjects",
				queryFormatter);
									
			statement 
				= createPreparedStatement(
					connection, 
					queryFormatter);
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				Project project = Project.newInstance();
				project.setName(resultSet.getString(1));
				project.setDescription(resultSet.getString(2));
				Date startDate
					= resultSet.getDate(3);
				String startDatePhrase
					= GENERIC_MESSAGES.getDatePhrase(startDate);
				project.setStartDate(startDatePhrase);
				Date endDate
					= resultSet.getDate(4);
				if (endDate != null) {
					String endDatePhrase
						= GENERIC_MESSAGES.getDatePhrase(endDate);
					project.setEndDate(endDatePhrase);					
				}
				results.add(project);
			}
			
			connection.commit();
		}
		catch(SQLException sqlException) {
			//Record original exception, throw sanitised, human-readable version						
			logSQLException(sqlException);
			PGSQLQueryUtility.rollback(connection);
			String errorMessage
				= RIFServiceMessages.getMessage(
					"diseaseMappingStudyManager.error.unableToGetProjects",
					user.getUserID());
			
			RIFLogger rifLogger = RIFLogger.getLogger();
			rifLogger.error(
					PGSQLDiseaseMappingStudyManager.class, 
				errorMessage, 
				sqlException);
			
			RIFServiceException rifServiceException
				= new RIFServiceException(
					RIFServiceError.DATABASE_QUERY_FAILED,
					errorMessage);
			throw rifServiceException;
		}
		finally {
			//Cleanup database resources			
			PGSQLQueryUtility.close(statement);
			PGSQLQueryUtility.close(resultSet);			
		}
		
		return results;
	}
	
	
	public void clearStudiesForUser(
		final Connection connection,
		final User user) 
		throws RIFServiceException {
		
		//KLG: To Do
		
	}
	
	// ==========================================
	// Section Errors and Validation
	// ==========================================
	
	public void checkNonExistentItems(
		final Connection connection,
		final DiseaseMappingStudy diseaseMappingStudy)
		throws RIFServiceException {
		
		//check non-existent items in the Comparison and Study areas
		Geography geography = diseaseMappingStudy.getGeography();
		rifContextManager.checkGeographyExists(
			connection, 
			geography.getName());
		DiseaseMappingStudyArea diseaseMappingStudyArea
			= diseaseMappingStudy.getDiseaseMappingStudyArea();

		
		
		checkAreaNonExistentItems(
			connection,
			geography.getName(),
			diseaseMappingStudyArea);
		ComparisonArea comparisonArea
			= diseaseMappingStudy.getComparisonArea();
		checkAreaNonExistentItems(
			connection,
			geography.getName(),
			comparisonArea);

		//we need to know the geoLevelToMap resolution to check
		//whether we have data for investigation covariates for
		//a given geography for that resolution.  What is important here
		//is the geoLevelToMap of the study area; with respects to covariate
		//analysis the geoLevelToMap of the comparison area has no meaning.
		GeoLevelToMap geoLevelToMap
			= diseaseMappingStudyArea.getGeoLevelToMap();		

		//Check non-existent items in the investigations
		ArrayList<Investigation> investigations
			= diseaseMappingStudy.getInvestigations();
		for (Investigation investigation : investigations) {
			investigationManager.checkNonExistentItems(
				connection, 
				geography,
				geoLevelToMap,
				investigation);
		}
		
	}
	
	private void checkAreaNonExistentItems(
		final Connection connection,
		final String geographyName,
		final AbstractGeographicalArea area) 
		throws RIFServiceException {
	
		GeoLevelSelect geoLevelSelect
			= area.getGeoLevelSelect();
		rifContextManager.checkGeoLevelSelectExists(
			connection, 
			geographyName, 
			geoLevelSelect.getName());
		/*
		ValidationPolicy validationPolicy
			= getValidationPolicy();
		

		if (getValidationPolicy() == ValidationPolicy.STRICT) {
			GeoLevelArea geoLevelArea
				= area.getGeoLevelArea();
			rifContextManager.checkGeoLevelAreaExists(
				connection, 
				geographyName, 
				geoLevelSelect.getName(), 
				geoLevelArea.getName());
		}
		*/
		
		GeoLevelView geoLevelView
			= area.getGeoLevelView();
		rifContextManager.checkGeoLevelToMapOrViewValueExists(
			connection, 
			geographyName, 
			geoLevelSelect.getName(), 
			geoLevelView.getName(),
			false);
		
		GeoLevelToMap geoLevelToMap
			= area.getGeoLevelToMap();
		rifContextManager.checkGeoLevelToMapOrViewValueExists(
			connection, 
			geographyName, 
			geoLevelSelect.getName(), 
			geoLevelToMap.getName(),
			true);	
	}
		
	public void checkDiseaseMappingStudyExists(
		final Connection connection,
		final String studyID)
		throws RIFServiceException {
		
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {

			PGSQLRecordExistsQueryFormatter queryFormatter
				= new PGSQLRecordExistsQueryFormatter();
			configureQueryFormatterForDB(queryFormatter);
			queryFormatter.setFromTable("rif40_studies");
			queryFormatter.setLookupKeyFieldName("study_id");

			logSQLQuery(
				"checkDiseaseMappingStudyExists",
				queryFormatter,
				studyID);
		
			statement 
				= createPreparedStatement(
					connection, 
					queryFormatter);
			statement.setInt(1, Integer.valueOf(studyID));
			resultSet = statement.executeQuery();
			if (resultSet.next() == false) {
				String recordType
					= RIFServiceMessages.getMessage("diseaseMappingStudy.label");
				String errorMessage
					= RIFServiceMessages.getMessage(
						"general.validation.nonExistentRecord",
						recordType,
						studyID);
				RIFServiceException rifServiceException
					= new RIFServiceException(
						RIFServiceError.NON_EXISTENT_DISEASE_MAPPING_STUDY, 
						errorMessage);
				
				connection.commit();
				
				throw rifServiceException;
			}

			connection.commit();
		}
		catch(SQLException sqlException) {			
			//Record original exception, throw sanitised, human-readable version			
			logSQLException(sqlException);
			PGSQLQueryUtility.rollback(connection);
			String recordType
				= RIFServiceMessages.getMessage("diseaseMappingStudy.label");			
			String errorMessage
				= RIFServiceMessages.getMessage(
					"general.validation.unableCheckNonExistentRecord",
					recordType,
					studyID);			
						
			RIFServiceException rifServiceException
				= new RIFServiceException(
					RIFServiceError.DATABASE_QUERY_FAILED, 
					errorMessage);
			throw rifServiceException;
		}
		finally {
			PGSQLQueryUtility.close(statement);
			PGSQLQueryUtility.close(resultSet);
		}
		
		
	}
	
	// ==========================================
	// Section Interfaces
	// ==========================================

	// ==========================================
	// Section Override
	// ==========================================
}
