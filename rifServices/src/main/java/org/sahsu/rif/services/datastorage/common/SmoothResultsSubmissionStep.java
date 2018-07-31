package org.sahsu.rif.services.datastorage.common;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;
import org.sahsu.rif.generic.concepts.Parameter;
import org.sahsu.rif.generic.datastorage.SelectQueryFormatter;
import org.sahsu.rif.generic.datastorage.SQLQueryUtility;
import org.sahsu.rif.generic.system.RIFServiceException;
import org.sahsu.rif.generic.system.RIFServiceExceptionFactory;
import org.sahsu.rif.generic.util.RIFLogger;
import org.sahsu.rif.generic.util.RIFMemoryManager;
import org.sahsu.rif.services.concepts.AbstractCovariate;
import org.sahsu.rif.services.concepts.AbstractStudy;
import org.sahsu.rif.services.concepts.Investigation;
import org.sahsu.rif.services.concepts.RIFStudySubmission;
import org.sahsu.rif.services.system.RIFServiceStartupOptions;

public class SmoothResultsSubmissionStep extends CommonRService {

	private static final RIFLogger rifLogger = RIFLogger.getLogger();
	private static final RIFMemoryManager rifMemoryManager = RIFMemoryManager.getMemoryManager();
	private static String lineSeparator = System.getProperty("line.separator");

	private Logger log;	// Not used - uses RIFLogger
	private LoggingConsole loggingConsole;
	private RIFServiceStartupOptions rifStartupOptions;

	public SmoothResultsSubmissionStep() {
		String logManagerName=System.getProperty("java.util.logging.manager");
		if (logManagerName == null || !logManagerName.equals("java.util.logging.manager")) {
			System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
			rifLogger.info(this.getClass(), "Set java.util.logging.manager=" +
				System.getProperty("java.util.logging.manager"));
		}
		// Logging for JRI
		final LogManager logManager = LogManager.getLogManager();
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

	public void initialise(
			final String userID,
			final String password,
			final RIFServiceStartupOptions rifStartupOptions) throws RIFServiceException {

		this.rifStartupOptions = rifStartupOptions;

		setUser(userID, password);

		List<Parameter> rifStartupOptionParameters = rifStartupOptions.getDbParametersForRScripts();
		addParameters(rifStartupOptionParameters);

		// setODBCDataSourceName(rifStartupOptions.getODBCDataSourceName());
	}
	
	void performStep(final Connection connection, final RIFStudySubmission studySubmission,
			final String studyID) throws RIFServiceException {
		
		StringBuilder rifScriptPath = new StringBuilder();
		StringBuilder adjCovSmoothJri = new StringBuilder();
		StringBuilder performSmoothingActivity = new StringBuilder();
		String rErrorTrace="No R error tracer (see Tomcat log)";

		try {		

			//KLG: For now it only works with the first study.  For some reason, newer extract
			//tables cause the R program we use to generate an error.
			addParameter("studyID", studyID);

			//add a parameter for investigation name.  This will appear as a column in the extract
			//table that the R program will need to know about.  Note that specifying a single
			//investigation name assumes that eventually we will make a study have one investigation
			//rather than multiple investigations.
			Investigation firstInvestigation = studySubmission.getStudy().getInvestigations().get(0);

			addParameter("investigationName",
			             createDatabaseFriendlyInvestigationName(firstInvestigation.getTitle()));

			String covariateName = getCovariateName(studySubmission);
			addParameter("covariate_name", covariateName);

			Integer investigationID = getInvestigationID(connection, studyID, firstInvestigation);
					
			String studyName=studySubmission.getStudy().getName();
			addParameter("studyName", studyName);
					
			String studyDescription=studySubmission.getStudy().getDescription();
			addParameter("studyDescription", studyDescription);
					
			rifLogger.info(this.getClass(), "Study id: " + studyID + 
				"; Study name: " + studyName + 
				"; Study description: " + studyDescription + 
				"; Investigation name: " + firstInvestigation.getTitle() + 
				"; ID: "+ investigationID);

			addParameter("investigationId", String.valueOf(investigationID));

			setCalculationMethod(studySubmission.getCalculationMethods().get(0));

			int exitValue = 0;

			Rengine rengine = null;

			try {	
				//Create an R engine with JRI
				rengine = Rengine.getMainEngine();
				if (rengine == null) {
					String[] rArgs={"--vanilla"};
					rengine = new Rengine(
						rArgs,								// Args
						false, 							// runMainLoop
						new LoggingConsole(log)); 			// RMainLoopCallbacks implementation
															// Logger log not used - uses RIFLogger
				}

				if (!rengine.waitForR()) {
					rifLogger.warning(getClass(),
					                  "Cannot load the R engine (probably already loaded)");
				}
				Rengine.DEBUG = 10;
				rengine.eval("Rpid<-Sys.getpid()");
				REXP rPid = rengine.eval("Rpid");
				rifLogger.info(this.getClass(), "Rengine Started" +
				                                "; Rpid: " + rPid.asInt() +
				                                "; JRI version: " + Rengine.getVersion() +
                                                "; thread ID: " + Thread.currentThread().getId());

				//Start R operations
				
				//Check library path
				rengine.eval("rm(list=ls())"); //just in case!
				rengine.eval("print(.libPaths())");

				// Session Info
				rengine.eval("print(sessionInfo())");

				// Set connection details and parameters
				StringBuilder logMsg = new StringBuilder();
				for (Parameter parameter : getParameterArray()) {
					String name = parameter.getName();
					String value = parameter.getValue();

					switch (name) {
						case "password":
							// Hide password
							logMsg.append(name).append("=XXXXXXXX").append(lineSeparator);
							rengine.assign(name, value);
							break;
						case "covariate_name":
							logMsg.append("names.adj.1=").append(value).append(lineSeparator);
							rengine.assign("names.adj.1", value);
							logMsg.append("adj.1=").append(getRAdjust(value)).append(lineSeparator);
							rengine.assign("adj.1", getRAdjust(value));
							break;
						default:
							logMsg.append(name).append("=").append(value).append(lineSeparator);
							rengine.assign(name, value);
							break;
					}
				}

				rengine.assign("working_dir", rifStartupOptions.getExtractDirectory());
				
				rifLogger.info(this.getClass(), "R parameters: " + lineSeparator +
				                                logMsg.toString());

				rifScriptPath.append(rifStartupOptions.getClassesDirectory());
				rifScriptPath.append(File.separator);
				adjCovSmoothJri.append(rifScriptPath);
				sourceRScript(rengine, rifScriptPath + "JdbcHandler.R");

				// We do either Risk Analysis or Smoothing
				REXP exitValueFromR;
				if (studySubmission.getStudy().isRiskAnalysis()) {

					sourceRScript(rengine, rifScriptPath + "performRiskAnal.R");
					rengine.eval("returnValues <- performRiskAnal");
					/* TODO: that's the script name, not the name of a function in it; but
					 * there doesn't at present appear to be a suitable one.
					 */
				} else {

					// Run the actual smoothing
					adjCovSmoothJri.append("Adj_Cov_Smooth_JRI.R");
					performSmoothingActivity.append(rifScriptPath);
					performSmoothingActivity.append("performSmoothingActivity.R");
					sourceRScript(rengine, adjCovSmoothJri.toString());
					sourceRScript(rengine, performSmoothingActivity.toString());
					rengine.eval("returnValues <- runRSmoothingFunctions()");

					// REXP errorTraceFromR = rengine.eval("returnValues$errorTrace");
					// if (errorTraceFromR != null) {
					// 	String[] strArr=errorTraceFromR.asStringArray();
					// 	StringBuilder strBuilder = new StringBuilder();
					// 	for (final String aStrArr : strArr) {
					// 		strBuilder.append(aStrArr).append(lineSeparator);
					// 	}
					// 	int index = -1;
					// 	String toReplace="'";
					// 	while ((index = strBuilder.lastIndexOf(toReplace)) != -1) {
					// 		strBuilder.replace(index, index + toReplace.length(), "\""); // Replace ' with " to reduce JSON parse errors
					// 	}
					// 	rErrorTrace = strBuilder.toString();
					// }
					// else {
					// 	rifLogger.warning(this.getClass(), "JRI R ERROR: errorTraceFromR is NULL");
					// }
				}

				exitValueFromR = rengine.eval("as.integer(returnValues$exitValue)");
				if (exitValueFromR != null) {
					exitValue = exitValueFromR.asInt();
				} else {
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
					rifLogger.error(this.getClass(), "JRI R script ERROR", error);
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
					rifMemoryManager.printThreadMemory();
					if (exitValue != 0) {
						try {
//							rengine.eval("q(\"no\", " + exitValue + ")");	// Causes Java to quit
//							rengine.destroy(); 								// causes thread to exit; 
																			// rengine.end() then causes exception)
							rengine.end();
						}
						catch(Exception error3) {
							rifLogger.error(this.getClass(), "JRI rengine.destroy() ERROR", error3);
						}
						finally {
							rifLogger.info(this.getClass(), "Rengine Stopped, exit value=="+ exitValue +"==");
							
						}				

						RIFServiceExceptionFactory rifServiceExceptionFactory
						= new RIFServiceExceptionFactory();
						RIFServiceException rifServiceException =
							rifServiceExceptionFactory.createRScriptException(rErrorTrace);
						throw rifServiceException;
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
		catch (RIFServiceException rifServiceException) {
			rifLogger.error(this.getClass(), "JRI R script exception", rifServiceException);
			throw rifServiceException;
		}
		catch(Exception rException) {
			rifLogger.error(this.getClass(), "JRI R engine exception", rException);		
			RIFServiceExceptionFactory rifServiceExceptionFactory
			= new RIFServiceExceptionFactory();
			throw rifServiceExceptionFactory.createREngineException(rifScriptPath.toString());
		}		
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

		return covariates.get(0).getName();
	}


	private String createDatabaseFriendlyInvestigationName(final String investigationName) {
		return investigationName.trim().toUpperCase().replaceAll(" ", "_");		
	}


	private Integer getInvestigationID(
			final Connection connection,
			final String studyID,
			final Investigation investigation) 
					throws SQLException,
					RIFServiceException {


		rifLogger.info(this.getClass(), "SQLSmoothedResultsSubmissionStep getInvestigationID studyID=="+studyID+"==investigation_name=="+investigation.getTitle()+"==inv_description=="+investigation.getDescription()+"==");

		SelectQueryFormatter queryFormatter = SelectQueryFormatter.getInstance(
     				rifStartupOptions.getRifDatabaseType());
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
			SQLQueryUtility.close(resultSet);
			SQLQueryUtility.close(statement);
		}

		return investigationID;
	}
}
