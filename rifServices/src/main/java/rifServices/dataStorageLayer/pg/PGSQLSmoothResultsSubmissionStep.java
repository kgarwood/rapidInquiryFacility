package rifServices.dataStorageLayer.pg;

import rifServices.businessConceptLayer.*;
import rifServices.system.RIFServiceStartupOptions;
import rifGenericLibrary.system.RIFServiceException;
import rifGenericLibrary.system.RIFServiceExceptionFactory;
import rifGenericLibrary.businessConceptLayer.Parameter;
import rifGenericLibrary.dataStorageLayer.pg.PGSQLQueryUtility;
import rifGenericLibrary.dataStorageLayer.pg.PGSQLSelectQueryFormatter;

import java.sql.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.io.*;

import org.rosuda.JRI.*;

import rifGenericLibrary.util.RIFLogger;

/**
 *
 * <hr>
 * The Rapid Inquiry Facility (RIF) is an automated tool devised by SAHSU 
 * that rapidly addresses epidemiological and public health questions using 
 * routinely collected health and population data and generates standardised 
 * rates and relative risks for any given health outcome, for specified age 
 * and year ranges, for any given geographical area.
 *
 * Copyright 2017 Imperial College London, developed by the Small Area
 * Health Statistics Unit. The work of the Small Area Health Statistics Unit 
 * is funded by the Public Health England as part of the MRC-PHE Centre for 
 * Environment and Health. Funding for this project has also been received 
 * from the United States Centers for Disease Control and Prevention.  
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

public class PGSQLSmoothResultsSubmissionStep extends PGSQLAbstractRService {

	// ==========================================
	// Section Constants
	// ==========================================

	private static final RIFLogger rifLogger = RIFLogger.getLogger();
	private static String lineSeparator = System.getProperty("line.separator");	

	// Logging for JRI	
	private static LogManager logManager;
	private Logger log;	// Not used - uses RIFLogger
	private LoggingConsole loggingConsole;
	
	// ==========================================
	// Section Properties
	// ==========================================	
	private RIFServiceStartupOptions rifStartupOptions;

	// ==========================================
	// Section Construction
	// ==========================================

	public PGSQLSmoothResultsSubmissionStep() {
	/*
	Also change tomcat startup options:
	
From: 

-Dcatalina.home=C:\Program Files\Apache Software Foundation\Tomcat 8.5
-Dcatalina.base=C:\Program Files\Apache Software Foundation\Tomcat 8.5
-Djava.io.tmpdir=C:\Program Files\Apache Software Foundation\Tomcat 8.5\temp
-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager
-Djava.util.logging.config.file=C:\Program Files\Apache Software Foundation\Tomcat 8.5\conf\logging.properties

To:

-Dcatalina.home=C:\Program Files\Apache Software Foundation\Tomcat 8.5
-Dcatalina.base=C:\Program Files\Apache Software Foundation\Tomcat 8.5
-Djava.io.tmpdir=C:\Program Files\Apache Software Foundation\Tomcat 8.5\temp
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager
-Djava.util.logging.config.file=C:\Program Files\Apache Software Foundation\Tomcat 8.5\conf\logging.properties

     */	
		String logManagerName=System.getProperty("java.util.logging.manager");
		if (logManagerName == null || !logManagerName.equals("java.util.logging.manager")) {
			System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
			rifLogger.info(this.getClass(), "Set java.util.logging.manager=" +
				System.getProperty("java.util.logging.manager"));
		}
		logManager = LogManager.getLogManager();
		log=Logger.getLogger("rifGenericLibrary.util.RIFLogger");
		loggingConsole=new LoggingConsole(log);
		Enumeration<String> loggerNames = logManager.getLoggerNames();
		if (!loggerNames.hasMoreElements()) {
			rifLogger.warning(this.getClass(), "java.util.logging.manager has no loggers");
		}
		while (loggerNames.hasMoreElements()) {
			String name = loggerNames.nextElement();
			if (name.equals("rifGenericLibrary.util.RIFLogger")) {
				rifLogger.info(this.getClass(), "Found java.util.logging.manager logger: " + name);
			}
			else {
				rifLogger.debug(this.getClass(), "Other java.util.logging.manager logger: " + name);
			}
		}
		
		rifLogger.printLoggers();
	}

	// ==========================================
	// Section Accessors and Mutators
	// ==========================================
	public void initialise(
			final String userID,
			final String password,
			final RIFServiceStartupOptions rifStartupOptions) {

		this.rifStartupOptions = rifStartupOptions;

		setUser(userID, password);

		ArrayList<Parameter> rifStartupOptionParameters
		= rifStartupOptions.extractParameters();
		addParameters(rifStartupOptionParameters);

		setODBCDataSourceName(rifStartupOptions.getODBCDataSourceName());

		//register the names of parameters that we will want to check are
		//not empty
		ArrayList<String> startupOptionParameterNames
		= Parameter.extractParameterNames(rifStartupOptionParameters);
		addParameterToVerify(startupOptionParameterNames);

	}
			
	public void performStep(
			final Connection connection,
			final RIFStudySubmission studySubmission, 
			final String studyID) 
					throws RIFServiceException {
		
		StringBuilder rifScriptPath = new StringBuilder();	

		try {		
			addParameterToVerify("study_id");		

			//KLG: For now it only works with the first study.  For some reason, newer extract
			//tables cause the R program we use to generate an error.
			addParameter("study_id", studyID);

			//add a parameter for investigation name.  This will appear as a column in the extract
			//table that the R program will need to know about.  Note that specifying a single
			//investigation name assumes that eventually we will make a study have one investigation
			//rather than multiple investigations.
			Investigation firstInvestigation
			= studySubmission.getStudy().getInvestigations().get(0);

			addParameter(
					"investigation_name", 
					createDatabaseFriendlyInvestigationName(firstInvestigation.getTitle()));
			addParameterToVerify("investigation_name");

			String covariateName = getCovariateName(studySubmission);
			addParameter(
					"covariate_name", 
					covariateName);

			Integer investigationID
			= getInvestigationID(
					connection,
					studyID, 
					firstInvestigation);
			rifLogger.info(this.getClass(), "Investigation name=="+firstInvestigation.getTitle() + "  ID=="+investigationID+"==");

			addParameterToVerify("investigation_id");
			addParameter(
					"investigation_id", 
					String.valueOf(investigationID));

			setCalculationMethod(studySubmission.getCalculationMethods().get(0));

			int exitValue = 0;

			Rengine rengine = null;
			try {	
				//Create an R engine with JRI
				rengine = Rengine.getMainEngine();
				if(rengine == null) {
					rengine = new Rengine(new String[] {"--vanilla"}, 	// Args
						false, 											// runMainLoop
						loggingConsole); 								// RMainLoopCallbacks implementaton
																		// Logger log not used - uses RIFLogger
				}

				if (!rengine.waitForR()) {
					rifLogger.warning(this.getClass(), "Cannot load the R engine");
				}
				Rengine.DEBUG = 10;
				rifLogger.info(this.getClass(), "Rengine Started");

				//Start R operations

				//Check library path
				rengine.eval("rm(list=ls())"); //just in case!
				rengine.eval("print(.libPaths())");

				//Session Info
				rengine.eval("print(sessionInfo())");

				//set connection details and parameters
				String[] parameters = generateParameterArray();
				rengine.assign("userID", parameters[12]);
				rengine.assign("password", parameters[11]);
				rengine.assign("dbName", parameters[3]);
				rengine.assign("dbHost", parameters[1]);
				rengine.assign("dbPort", parameters[2]);
				rengine.assign("db_driver_prefix", parameters[0]);
				rengine.assign("db_driver_class_name", parameters[4]);
				rengine.assign("studyID", parameters[5]);
				rengine.assign("investigationName", parameters[6]);
				rengine.assign("investigationId", parameters[8]);
				rengine.assign("odbcDataSource", parameters[10]);
				rengine.assign("model", getRRoutineModelCode(parameters[9]));
				rengine.assign("names.adj.1", parameters[7]);			
				rengine.assign("adj.1", getRAdjust(parameters[7]));

				//RUN "Adj_Cov_Smooth_JRI.R"
				
				rifScriptPath.append(rifStartupOptions.getRIFServiceResourcePath());
				rifScriptPath.append(File.separator);
				rifScriptPath.append(File.separator);
				rifScriptPath.append("Adj_Cov_Smooth_JRI.R");
				rifLogger.info(this.getClass(), "rScriptPath=="+rifScriptPath+"==");
				rengine.eval("source(\"" + rifScriptPath + "\")");

				//RUN the actual smoothing
				REXP exitValueFromR = rengine.eval("as.integer(a <- runRSmoothingFunctions())");
				if (exitValueFromR != null) {
					exitValue  = exitValueFromR.asInt();
				}
				else {
					rifLogger.warning(this.getClass(), "JRI R ERROR: exitValueFromR is NULL");
					exitValue = 1;
				}
			}
			catch(Exception error) {
				try {
					loggingConsole.rFlushConsole(rengine);
				}
				catch(Exception error2) {
					rifLogger.error(this.getClass(), "JRI rFlushConsole() ERROR", error2);
				}
				finally {
					rifLogger.error(this.getClass(), "JRI R ERROR", error);
					exitValue = 1;
				}
			} 
			finally {
				try {
					loggingConsole.rFlushConsole(rengine);
				}
				catch(Exception error2) {
					rifLogger.error(this.getClass(), "JRI rFlushConsole() ERROR", error2);
				}
				finally {
					if (exitValue != 0) {
						try {
//							rengine.eval("q(\"no\", " + exitValue + ")");	// Causes Java to quit
//							rengine.destroy(); 								// causes thread to exit; 
																			// rengine.end() then causes exception)
						}
						catch(Exception error3) {
							rifLogger.error(this.getClass(), "JRI rengine.destroy() ERROR", error3);
						}
						finally {
							rifLogger.info(this.getClass(), "Rengine Stopped, exit value=="+ exitValue +"==");
						}					
					}
					else {	
						try {
							rengine.end();
						}
						catch(Exception error3) {
							rifLogger.error(this.getClass(), "JRI rengine.end() ERROR", error3);
							exitValue = 1;
						}
						finally {
							rifLogger.info(this.getClass(), "Rengine Stopped, exit value=="+ exitValue +"==");
						}			
					}
				}
			}
		}
		catch(Exception ioException) {
			rifLogger.error(this.getClass(), "JRI R exception", ioException);
			
			RIFServiceExceptionFactory rifServiceExceptionFactory
			= new RIFServiceExceptionFactory();
			rifServiceExceptionFactory.createFileCommandLineRunException(rifScriptPath.toString());
		}		
	}

	private String getRRoutineModelCode(String proc) {
		String model = "NONE";
		if (proc.equals("het_r_procedure")) {
			model = "HET";
		} else if (proc.equals("car_r_procedure")) {
			model = "CAR";
		} else if (proc.equals("bym_r_procedure")) {
			model = "BYM";
		}
		return model;
	}

	private String getRAdjust(String covar) {
		String name = covar.toUpperCase();
		if (!name.equals("NONE")) {
			return "TRUE";
		} else {
			return "FALSE";
		}	
	}

	/*
	 * @TODO: KLG - Currently the study submission data model allows for it to have a study with multiple investigations,
	 * each of which can have multiple different covariates.  In practice, we are finding that we are using one covariate.
	 * This is a method that should be migrated into the RIFStudySubmission class.
	 */
	private String getCovariateName(final RIFStudySubmission studySubmission) {
		AbstractStudy study
		= studySubmission.getStudy();
		ArrayList<Investigation> investigations = study.getInvestigations();
		//Get the covariates from the first investigation
		ArrayList<AbstractCovariate> covariates = investigations.get(0).getCovariates();


		//This just takes the first covariate of the first investigation and returns its name.  That's the one we will
		//assume will appear in the extract table.  Note though that this needs to be changed in future because at the 
		//moment our model accommodates multiple covariates in multiple investigations.
		if (covariates.isEmpty()) {
			return "NONE";
		}

		String covariateName
		= covariates.get(0).getName();
		return covariateName;
	}


	private String createDatabaseFriendlyInvestigationName(final String investigationName) {
		return investigationName.trim().toUpperCase().replaceAll(" ", "_");		
	}


	public Integer getInvestigationID(
			final Connection connection,
			final String studyID, 
			final Investigation investigation) 
					throws SQLException,
					RIFServiceException {


		rifLogger.info(this.getClass(), "SQLSmoothedResultsSubmissionStep getInvestigationID studyID=="+studyID+"==investigation_name=="+investigation.getTitle()+"==inv_description=="+investigation.getDescription()+"==");

		PGSQLSelectQueryFormatter queryFormatter = new PGSQLSelectQueryFormatter();
		queryFormatter.setDatabaseSchemaName("rif40");
		queryFormatter.addFromTable("rif40_investigations");
		queryFormatter.addSelectField("inv_id");
		queryFormatter.addWhereParameter("study_id");
		queryFormatter.addWhereParameter("inv_name");

		rifLogger.info(this.getClass(), "=======getInvestigationID========1===" + lineSeparator +
			"StudyID=="+studyID+"=="  + lineSeparator +
			"Inv_name=="+investigation.getTitle().toUpperCase()+"==" + lineSeparator +
			queryFormatter.generateQuery()  + lineSeparator +
			"=======getInvestigationID========2===");

		String databaseFriendlyInvestigationName
		= createDatabaseFriendlyInvestigationName(investigation.getTitle());

		Integer investigationID = null;
		PreparedStatement statement = null;		
		ResultSet resultSet = null;
		try {
			statement 
			= connection.prepareStatement(queryFormatter.generateQuery());
			statement.setInt(1, Integer.valueOf(studyID));
			statement.setString(2, databaseFriendlyInvestigationName);
			resultSet = statement.executeQuery();
			rifLogger.info(this.getClass(), "About to call next");
			resultSet.next();
			rifLogger.info(this.getClass(), "called next");
			investigationID = resultSet.getInt(1);
		}
		finally {
			PGSQLQueryUtility.close(resultSet);			
			PGSQLQueryUtility.close(statement);			
		}

		return investigationID;
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
