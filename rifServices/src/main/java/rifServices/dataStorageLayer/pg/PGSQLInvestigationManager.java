package rifServices.dataStorageLayer.pg;

import rifGenericLibrary.businessConceptLayer.User;
import rifGenericLibrary.dataStorageLayer.RIFDatabaseProperties;
import rifGenericLibrary.dataStorageLayer.pg.PGSQLQueryUtility;
import rifGenericLibrary.dataStorageLayer.pg.PGSQLRecordExistsQueryFormatter;
import rifGenericLibrary.system.RIFServiceException;
import rifGenericLibrary.util.RIFLogger;
import rifServices.businessConceptLayer.AbstractStudy;
import rifServices.businessConceptLayer.AgeBand;
import rifServices.businessConceptLayer.Investigation;
import rifServices.businessConceptLayer.AbstractCovariate;
import rifServices.businessConceptLayer.Geography;
import rifServices.businessConceptLayer.GeoLevelToMap;
import rifServices.businessConceptLayer.HealthTheme;
import rifServices.businessConceptLayer.NumeratorDenominatorPair;
import rifServices.dataStorageLayer.common.AgeGenderYearManager;
import rifServices.dataStorageLayer.common.CovariateManager;
import rifServices.dataStorageLayer.common.HealthOutcomeManager;
import rifServices.dataStorageLayer.common.RIFContextManager;
import rifServices.system.RIFServiceError;
import rifServices.system.RIFServiceMessages;

import java.sql.*;
import java.util.ArrayList;


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

final class PGSQLInvestigationManager 
	extends PGSQLAbstractSQLManager {

	// ==========================================
	// Section Constants
	// ==========================================

	// ==========================================
	// Section Properties
	// ==========================================
	private RIFContextManager rifContextManager;
	private AgeGenderYearManager ageGenderYearManager;
	private CovariateManager covariateManager;
	
	// ==========================================
	// Section Construction
	// ==========================================

	/**
	 * Instantiates a new SQL investigation manager.
	 */
	public PGSQLInvestigationManager(
		final RIFDatabaseProperties rifDatabaseProperties,
		final RIFContextManager rifContextManager,
		final AgeGenderYearManager ageGenderYearManager,
		final CovariateManager covariateManager,
		final HealthOutcomeManager healthOutcomeManager) {

		super(rifDatabaseProperties);
		this.rifContextManager = rifContextManager;
		this.ageGenderYearManager = ageGenderYearManager;
		this.covariateManager = covariateManager;
	}

	// ==========================================
	// Section Accessors and Mutators
	// ==========================================
/*
	public ArrayList<Investigation> getInvestigationsForStudy(
		final Connection connection,
		final User user,
		final DiseaseMappingStudy diseaseMappingStudy) {
		
		
		SQLSelectQueryFormatter formatter
			= new SQLSelectQueryFormatter();
		formatter.addSelectField("inv_id");
		formatter.addSelectField("geography");
		formatter.addSelectField("inv_name");
		formatter.addSelectField("inv_description");
		formatter.addSelectField("classifier");
		formatter.addSelectField("classifier_bands");
		formatter.addSelectField("genders");
		formatter.addSelectField("numer_tab");
		formatter.addSelectField("year_start");
		formatter.addSelectField("year_stop");
		formatter.addSelectField("max_age_group");
		formatter.addSelectField("min_age_group");
		formatter.addSelectField("investigation_state");
		
		
		formatter.addFromTable("t_rif40_investigations");
		
	}
		
*/

	
	// ==========================================
	// Section Errors and Validation
	// ==========================================
	
	public void checkNonExistentItems(
		final Connection connection, 
		final Geography geography,
		final GeoLevelToMap geoLevelToMap,
		final Investigation investigation)
		throws RIFServiceException {
		

		ArrayList<AbstractCovariate> covariates
			= investigation.getCovariates();
		covariateManager.checkNonExistentCovariates(
			connection, 
			geography,
			geoLevelToMap,
			covariates);

		//we will not check whether the health codes exist
		//for now, we'll assume they do
		//ArrayList<HealthCode> healthCodes
		//	= investigation.getHealthCodes();
		//healthOutcomeManager.checkNonExistentHealthCodes(healthCodes);

		
		HealthTheme healthTheme
			= investigation.getHealthTheme();
		rifContextManager.checkHealthThemeExists(
			connection, 
			healthTheme.getDescription());

		NumeratorDenominatorPair ndPair = investigation.getNdPair();
		rifContextManager.checkNDPairExists(User.NULL_USER,
			connection, 
			geography, 
			ndPair);
		
		ArrayList<AgeBand> ageBands
			= investigation.getAgeBands();
		ageGenderYearManager.checkNonExistentAgeGroups(
			connection, 
			ndPair,
			ageBands);
		
		
	}
	
	public void checkInvestigationExists(
		final Connection connection,
		final AbstractStudy study,
		final Investigation investigation) 
		throws RIFServiceException {
		
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			PGSQLRecordExistsQueryFormatter queryFormatter
				= new PGSQLRecordExistsQueryFormatter();
			configureQueryFormatterForDB(queryFormatter);		
			queryFormatter.setFromTable("rif40_investigations");
			queryFormatter.addWhereParameter("study_id");
			queryFormatter.addWhereParameter("inv_id");

			logSQLQuery(
				"checkInvestigationExists",
				queryFormatter,
				study.getIdentifier(),
				investigation.getIdentifier());
				
			statement 
				= createPreparedStatement(
					connection, 
					queryFormatter);
			statement.setString(1, study.getIdentifier());
			statement.setString(2, investigation.getIdentifier());
			resultSet = statement.executeQuery();
			RIFServiceException rifServiceException = null;
			if (resultSet.next() == false) {
				String recordType
					= investigation.getRecordType();
				String errorMessage
					= RIFServiceMessages.getMessage(
						"general.validation.nonExistentRecord",
						recordType,
						investigation.getDisplayName());

				rifServiceException
					= new RIFServiceException(
						RIFServiceError.NON_EXISTENT_AGE_GROUP, 
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
			String errorMessage
				= RIFServiceMessages.getMessage(
					"general.validation.unableCheckNonExistentRecord",
					investigation.getRecordType(),
					investigation.getDisplayName());

			RIFLogger rifLogger = RIFLogger.getLogger();
			rifLogger.error(
				PGSQLInvestigationManager.class, 
				errorMessage, 
				sqlException);										
			
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
