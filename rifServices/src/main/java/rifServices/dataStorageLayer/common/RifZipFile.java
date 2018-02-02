package rifServices.dataStorageLayer.common;

import rifServices.system.RIFServiceStartupOptions;
import rifServices.businessConceptLayer.AbstractStudy;
import rifGenericLibrary.util.RIFLogger;
import rifGenericLibrary.dataStorageLayer.SQLGeneralQueryFormatter;
import rifGenericLibrary.dataStorageLayer.common.SQLQueryUtility;
import rifServices.dataStorageLayer.common.SQLAbstractSQLManager;
import rifGenericLibrary.businessConceptLayer.User;
import rifGenericLibrary.dataStorageLayer.DatabaseType;
import rifServices.businessConceptLayer.RIFStudySubmission;
import rifServices.fileFormats.RIFStudySubmissionContentHandler;
import rifGenericLibrary.fileFormats.XMLCommentInjector;
import rifServices.graphics.RIFGraphicsOutputType;
import rifGenericLibrary.system.RIFServiceException;
import rifGenericLibrary.system.RIFServiceExceptionFactory;
import rifServices.system.RIFServiceError;
import rifServices.system.RIFServiceMessages;

import rifServices.graphics.RIFGraphics;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.*;
import java.io.*;
import org.json.*;
import java.lang.*;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.EnumSet;
import java.util.Iterator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.ArrayList;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
 * Peter Hambly
 * @author phambly
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

public class RifZipFile extends SQLAbstractSQLManager {
	// ==========================================
	// Section Constants
	// ==========================================
	private static final RIFLogger rifLogger = RIFLogger.getLogger();
	private static String lineSeparator = System.getProperty("line.separator");
	private Connection connection;
	private String studyID;
	private static String EXTRACT_DIRECTORY;
	private static int printingDPI;
	private static float jpegQuality=new Float(.8);
	
	private static final String STUDY_QUERY_SUBDIRECTORY = "study_query";
	private static final String STUDY_EXTRACT_SUBDIRECTORY = "study_extract";
	private static final String RATES_AND_RISKS_SUBDIRECTORY = "rates_and_risks";
	private static final String GEOGRAPHY_SUBDIRECTORY = "geography";
	private static final int BASE_FILE_STUDY_NAME_LENGTH = 100;
	
	private static Map<String, String> environmentalVariables = System.getenv();
	private static String catalinaHome = environmentalVariables.get("CATALINA_HOME");
	
	private RIFServiceStartupOptions rifServiceStartupOptions;
	private static DatabaseType databaseType;
	
	// ==========================================
	// Section Properties
	// ==========================================

	// ==========================================
	// Section Construction
	// ==========================================
	/**
     * Constructor.
     * 
     * @param RIFServiceStartupOptions rifServiceStartupOptions (required)
     */
	public RifZipFile(
			final RIFServiceStartupOptions rifServiceStartupOptions) {
		super(rifServiceStartupOptions.getRIFDatabaseProperties());
		
		this.rifServiceStartupOptions = rifServiceStartupOptions;
		
		EXTRACT_DIRECTORY = this.rifServiceStartupOptions.getExtractDirectory();
		databaseType=this.rifServiceStartupOptions.getRifDatabaseType();
		printingDPI=this.rifServiceStartupOptions.getPrintingDPI();
	}

	/**
	 * Get study extract
	 *
	 * @param  connection	Database specfic Connection object assigned from pool
	 * @param  user 		Database username of logged on user.
	 * @param  rifStudySubmission 		RIFStudySubmission object.
	 * @param  String 		zoomLevel (as text!).
	 * @param  studyID 		Study_id (as text!).
	 *
	 * @return 				FileInputStream 
	 * 
	 * @exception  			RIFServiceException		Catches all exceptions, logs, and re-throws as RIFServiceException
	 */
	public FileInputStream getStudyExtract(
			final Connection connection,
			final User user,
			final RIFStudySubmission rifStudySubmission,
			final String zoomLevel,
			final String studyID)
					throws RIFServiceException {
		//Validate parameters
		String temporaryDirectoryPath = null;
		File temporaryDirectory = null;
		File submissionZipFile = null;
		FileInputStream fileInputStream = null;
		
		try {
			//Establish the phrase that will be used to help name the main zip
			//file and data files within its directories
			String baseStudyName 
			= createBaseStudyFileName(rifStudySubmission, studyID);

			temporaryDirectoryPath = 
					createTemporaryDirectoryPath(
							user, 
							studyID);
			temporaryDirectory = new File(temporaryDirectoryPath);
			if (temporaryDirectory.exists()) {
				rifLogger.info(this.getClass(), "Found R temporary directory: "  + 
					temporaryDirectory.getAbsolutePath());
			}
			else {
				throw new Exception("R temporary directory: "  + 
					temporaryDirectory.getAbsolutePath() + " was not created by Adj_Cov_Smooth_JRI.R");
			}
			
			submissionZipFile 
			= createSubmissionZipFile(
					user,
					baseStudyName);
					
			if (submissionZipFile.isFile()) { // No file (i.e. NULL) handled in MSSQLAbstractRIFWebServiceResource.java
				fileInputStream = new FileInputStream(submissionZipFile);	
				rifLogger.info(this.getClass(), "Fetched ZIP file: " + 
					submissionZipFile.getAbsolutePath());
			}
			else {
				rifLogger.info(this.getClass(), "Unable to fetch ZIP file: " + 
					submissionZipFile.getAbsolutePath() + "; file does not exist");
			}
		}
		catch(Exception exception) {
			rifLogger.error(this.getClass(), "MSSQLStudyExtractManager ERROR", exception);
				
			String errorMessage
				= RIFServiceMessages.getMessage(
					"sqlStudyStateManager.error.unableToGetStudyExtract",
					user.getUserID(),
					submissionZipFile.getAbsolutePath());
			RIFServiceException rifServiceExeption
				= new RIFServiceException(
				RIFServiceError.ZIPFILE_CREATE_FAILED, 
					errorMessage);
			throw rifServiceExeption;
		}	

		return fileInputStream;
	}
	
	/**
	 * Get textual extract status of a study.                          
	 * <p>   
	 * This fucntion determines whether a study can be extracted from the database and the results returned to the user in a ZIP file 
	 * </p>
	 * <p>
	 * Returns the following textual strings:
	 * <il>
	 *   <li>STUDY_INCOMPLETE_NOT_ZIPPABLE: returned for the following rif40_studies.study_state codes/meanings:
	 *     <ul>
	 *	     <li>C: created, not verified;</li>
	 *	     <li>V: verified, but no other work done; [NOT USED BY MIDDLEWARE]</li>
	 *	     <li>E: extracted imported or created, but no results or maps created;</li> 
	 *	     <li>R: initial results population, create map table; [NOT USED BY MIDDLEWARE] design]</li>
	 *	     <li>W: R warning. [NOT USED BY MIDDLEWARE]</li>
	 *     <ul>
	 *   </li>
	 *   <li>STUDY_FAILED_NOT_ZIPPABLE: returned for the following rif40_studies.study_state codes/meanings:
	 *	     <li>G: Extract failure, extract, results or maps not created;</li> 
	 *	     <li>F: R failure, R has caught one or more exceptions [depends on the exception handler]</li> 
	 *   </li>
	 *   <li>STUDY_EXTRACTABLE_NEEDS_ZIPPING: returned for the following rif40_studies.study_state code/meaning of: S: R success; 
	 *       when the ZIP extrsct file has not yet been created
	 *   </il>
	 *   <li>STUDY_EXTRABLE_ZIPPID: returned for the following rif40_studies.study_statu  code/meaning of: S: R success; 
	 *       when the ZIP extrsct file has been created
	 *   </il>
	 *   <il>STUDY_NOT_FOUND: returned where the studyID was not found in rif40_studies
	 *   </il>
	 * </il>
	 * </p>
	 *
	 * @param  connection	Database specfic Connection object assigned from pool
	 * @param  user 		Database username of logged on user.
	 * @param  rifStudySubmission 		RIFStudySubmission object.
	 * @param  studyID 		Study_id (as text!).
	 *
	 * @return 				Textual extract status, e.g. {status: STUDY_NOT_FOUND} 
	 * 
	 * @exception  			RIFServiceException		Catches all exceptions, logs, and re-throws as RIFServiceException
	 */
	 public String getExtractStatus(
			final Connection connection,
			final User user,
			final RIFStudySubmission rifStudySubmission,
			final String studyID
			)
					throws RIFServiceException {
		String result=null;
		File submissionZipFile = null;
		String zipFileName="UNKNOWN";
		
		try {
			//Establish the phrase that will be used to help name the main zip
			//file and data files within its directories
			
			String studyStatus=getRif40StudyState(connection, studyID);
			if (studyStatus == null) { 	// Study ID does not exist. You will not get this
										// [this is raised as an exception in the calling function: RIFStudySubmission.getRIFStudySubmission()]
				throw new Exception("STUDY_NOT_FOUND: " + studyID);
			}
			if (result != null && studyStatus != null) { 
				switch (studyStatus.charAt(0)) {
					case 'C':
					case 'V':
					case 'E':
					case 'R':
					case 'W':
						result="STUDY_INCOMPLETE_NOT_ZIPPABLE";
						break;
					case 'G':
					case 'F':
						result="STUDY_FAILED_NOT_ZIPPABLE";
						break;
					case 'S':	/* R success */
						break;
					default:
						throw new Exception("Invalid rif40_studies.study_state: " + studyStatus);
				}
			}
			
			if (result == null) {
				String baseStudyName 
				= createBaseStudyFileName(rifStudySubmission, studyID);
				
				submissionZipFile = createSubmissionZipFile(
						user,
						baseStudyName);
				zipFileName=submissionZipFile.getAbsolutePath();
				if (submissionZipFile.isFile()) { // ZIP file exists - no need to recreate
					result="STUDY_EXTRACTBLE_ZIPPID";
				}
				else { // No zip file 
					result="STUDY_EXTRACTABLE_NEEDS_ZIPPING";
				}
			}
		}
		catch(Exception exception) {
			rifLogger.error(this.getClass(), "MSSQLStudyExtractManager ERROR", exception);
				
			String errorMessage
				= RIFServiceMessages.getMessage(
					"sqlStudyStateManager.error.unableToGetExtractStatus",
					user.getUserID(),
					studyID,
					zipFileName);
			RIFServiceException rifServiceExeption
				= new RIFServiceException(
					RIFServiceError.ZIPFILE_GET_STATUS_FAILED, 
					errorMessage);
			throw rifServiceExeption;
		}

		return "{\"status\":\"" + result + "\"}";
	}
	
	/** 
     * Create study extract. 
	 *
     * @param Connection connection (required)
     * @param User user (required)
     * @param RIFStudySubmission rifStudySubmission (required)
     * @param String zoomLevel (required)
     * @param String studyID (required)
     * @param Locale locale (required)
     * @param String tomcatServer [deduced from calling URL] (required)
     * @param String taxonomyServicesServer [parameter] (required)
     * @return JSONObject [front end saves as JSON5 file]
     */
	public void createStudyExtract(
			final Connection connection,
			final User user,
			final RIFStudySubmission rifStudySubmission,
			final String zoomLevel,
			final String studyID,
			final Locale locale,
			final String tomcatServer,
			final String taxonomyServicesServer)
					throws RIFServiceException {

		//Validate parameters
		String temporaryDirectoryPath = null;
		File temporaryDirectory = null;
		File submissionZipFile = null;
		ZipOutputStream submissionZipOutputStream = null;
		File submissionZipSavFile = null;
		
		try {
			//Establish the phrase that will be used to help name the main zip
			//file and data files within its directories
			String baseStudyName 
			= createBaseStudyFileName(rifStudySubmission, studyID);

			temporaryDirectoryPath = 
					createTemporaryDirectoryPath(
							user, 
							studyID);
			temporaryDirectory = new File(temporaryDirectoryPath);
			if (temporaryDirectory.exists()) {
				rifLogger.info(this.getClass(), "Found R temporary directory: "  + 
					temporaryDirectory.getAbsolutePath());
			}
			else {
				throw new Exception("R temporary directory: "  + 
					temporaryDirectory.getAbsolutePath() + " was not created by Adj_Cov_Smooth_JRI.R");
			}

			String denominatorHTML=addDenominator(
				user,
				connection, 
				temporaryDirectory,
				studyID,
				1 /* Header level */);
			
			submissionZipSavFile = createSubmissionZipFile(
					user,
					baseStudyName + ".sav");
			submissionZipFile = createSubmissionZipFile(
					user,
					baseStudyName);
			if (submissionZipFile.isFile()) { // ZIP file exists - no need to recreate
				Thread.sleep(500); // Sleep to allow JS promises time to work
				rifLogger.info(this.getClass(), "No need to create ZIP file: " + 
					submissionZipFile.getAbsolutePath() + "; already exists");
			}
			else if (submissionZipSavFile.isFile()) { // Sav file exists - being created
				Thread.sleep(500); // Sleep to allow JS promises time to work
				rifLogger.info(this.getClass(), "No need to create ZIP file: " + 
					submissionZipSavFile.getAbsolutePath() + "; being created");
			}
			else { // No zip file - can be created 
				submissionZipOutputStream = new ZipOutputStream(new FileOutputStream(submissionZipSavFile));

				addJsonFile(
						temporaryDirectory,
						submissionZipOutputStream,
						connection, user, studyID, locale, tomcatServer);

				addCssFile(
						temporaryDirectory,
						submissionZipOutputStream,
						studyID,
						"RIFStudyHeader.css");
				addCssFile(
						temporaryDirectory,
						submissionZipOutputStream,
						studyID,
						"RIFPopulationPyramid.css");
			
				addHtmlFile(
						temporaryDirectory,
						submissionZipOutputStream,
						connection, user, studyID, locale, tomcatServer, taxonomyServicesServer, 
						denominatorHTML);
						
				//write the study the user made when they first submitted their query
				writeQueryFile(
						submissionZipOutputStream,
						user,
						baseStudyName,
						rifStudySubmission);

				addRFiles(
					temporaryDirectory,
					submissionZipOutputStream,
					null);

				writeGeographyFiles(
						connection,
						temporaryDirectoryPath,
						submissionZipOutputStream,
						baseStudyName,
						zoomLevel,
						rifStudySubmission);

				/*
				writeStatisticalPostProcessingFiles(
					connection,
					temporaryDirectoryPath,
					submissionZipOutputStream,				
					baseStudyName,
					rifStudySubmission);

				writeTermsAndConditionsFiles(
					submissionZipOutputStream);	
				 */	
				submissionZipOutputStream.flush();
				submissionZipOutputStream.close();
				submissionZipSavFile.renameTo(submissionZipFile);
				rifLogger.info(this.getClass(), "Created ZIP file: " + 
					submissionZipFile.getAbsolutePath());
			}
		}
		catch(Exception exception) {
			rifLogger.error(this.getClass(), "createStudyExtract() ERROR", exception);
			String errorMessage = null;
			if (exception.getMessage() != null &&
			    exception.getMessage().
				equals("Taxonomy service still initialising; please run again in 5 minutes")) {
				errorMessage
					= RIFServiceMessages.getMessage(
						"sqlStudyStateManager.error.taxonomyInitialiseError",
						user.getUserID(),
						submissionZipFile.getAbsolutePath());	
			}
			else {		
				errorMessage
					= RIFServiceMessages.getMessage(
						"sqlStudyStateManager.error.unableToCreateStudyExtract",
						user.getUserID(),
						submissionZipFile.getAbsolutePath());
			}
//			temporaryDirectory.delete();
				
			try {
				submissionZipOutputStream.flush();
				submissionZipOutputStream.close();
				submissionZipSavFile.delete();
			}
			catch(Exception err) {
				rifLogger.warning(this.getClass(), 
					"createStudyExtract() close ZIP stream ERROR: " + err.getMessage());
			}
			RIFServiceException rifServiceExeption
				= new RIFServiceException(
					RIFServiceError.ZIPFILE_CREATE_FAILED, 
					errorMessage);
			throw rifServiceExeption;
		}
		finally {
//			throw new  RIFServiceException(RIFServiceError.ZIPFILE_CREATE_FAILED, "TEST ZIP ERROR");
//			temporaryDirectory.delete();
		}
	}

	private void addCssFile(
			final File temporaryDirectory,
			final ZipOutputStream submissionZipOutputStream,
			final String studyID,
			final String cssFileName) 
			throws Exception {
			
		String cssFileText=readFile(cssFileName);
		rifLogger.info(this.getClass(), "Adding CSS for report file: " + temporaryDirectory.getAbsolutePath() + File.separator + 
			cssFileName + " to ZIP file");
		
		File file=new File(temporaryDirectory.getAbsolutePath() + File.separator + cssFileName);
		ZipEntry zipEntry = new ZipEntry(cssFileName);

		submissionZipOutputStream.putNextEntry(zipEntry);
		byte[] b=cssFileText.toString().getBytes();
		submissionZipOutputStream.write(b, 0, b.length);

		submissionZipOutputStream.closeEntry();	
	}
	
	private void addZipDir(
			final File temporaryDirectory,
			final ZipOutputStream submissionZipOutputStream,
			final String dirName) 
			throws Exception {
			
		String zipDirName=temporaryDirectory.getAbsolutePath() + File.separator + dirName + File.separator;
		rifLogger.info(this.getClass(), "Adding " + dirName + " directory: " + zipDirName + 
			" to ZIP file");
		
		ZipEntry zipEntry = new ZipEntry(zipDirName);
		submissionZipOutputStream.putNextEntry(zipEntry);
		submissionZipOutputStream.closeEntry();	
	}
	
	private void addHtmlFile(
			final File temporaryDirectory,
			final ZipOutputStream submissionZipOutputStream,
			final Connection connection,
			final User user,
			final String studyID,
			final Locale locale,
			final String tomcatServer,
			final String taxonomyServicesServer,
			final String denominatorHTML) 
			throws Exception {
				
		GetStudyJSON getStudyJSON = new GetStudyJSON(rifServiceStartupOptions);

		StringBuilder htmlFileText=new StringBuilder();
		htmlFileText.append(readFile("RIFStudyHeader.html") + lineSeparator);
		htmlFileText.append("<body>" + lineSeparator);
		htmlFileText.append("<div>" + lineSeparator);
		htmlFileText.append("  <div>" + lineSeparator);
		htmlFileText.append("  <ul class=\"nav\">" + lineSeparator);
		htmlFileText.append("    <li class=\"nav\"><a class=\"active\" href=\"#rif40_studies\">Studies</a></li>" + lineSeparator);
		htmlFileText.append("    <li class=\"nav\"><a href=\"#rif40_study_status\">Status</a></li>" + lineSeparator);
		htmlFileText.append("    <li class=\"nav\"><a href=\"#rif40_investigations\">Investigations</a></li>" + lineSeparator);
		htmlFileText.append("    <li class=\"nav\"><a href=\"#rif40_inv_covariates\">Covariates</a></li>" + lineSeparator);
		htmlFileText.append("    <li class=\"nav\"><a href=\"#rif40_inv_conditions\">Conditions</a></li>" + lineSeparator);
		htmlFileText.append("    <li class=\"nav\"><a href=\"#rif40_study_areas\">Study area</a></li>" + lineSeparator);
		htmlFileText.append("    <li class=\"nav\"><a href=\"#rif40_comparison_areas\">Comparison area</a></li>" + lineSeparator);
		htmlFileText.append("    <li class=\"nav\"><a href=\"#denominator\">Denominator</a></li>" + lineSeparator);
		htmlFileText.append("    <li class=\"nav\"><a href=\"#numerator\">Numerator</a></li>" + lineSeparator);
		htmlFileText.append("    <li class=\"nav\"><a href=\"#maps\">Maps</a></li>" + lineSeparator);
		htmlFileText.append("  </ul>" + lineSeparator);
		htmlFileText.append("  </div>" + lineSeparator);
		htmlFileText.append("  <div style=\"margin-left:25%;padding:1px 16px;height:1000px;\">" + lineSeparator);

		addTableToHtmlReport(htmlFileText, connection, studyID,
			"rif40", // Owner
			"rif40", // Schema
			"rif40_studies", // Table 
			null, // Common table expression
			null, // Joined table
			"username,study_id,study_name," + lineSeparator +
			"summary,description,other_notes,study_date," + lineSeparator +
			"geography,study_type," + lineSeparator +
//			"denom_tab,direct_stand_tab,covariate_table," + lineSeparator +
//			"year_start,year_stop," + lineSeparator +
//			"max_age_group,min_age_group," + lineSeparator +
			"study_geolevel_name,comparison_geolevel_name," + lineSeparator +
//			"map_table,extract_table,description" + lineSeparator +
//			"suppression_value,extract_permitted,transfer_permitted,authorised_by,authorised_on,authorised_notes," + lineSeparator +
			"project,project_description," + lineSeparator +
			"CASE WHEN stats_method = 'HET' THEN 'Heterogenous'" + lineSeparator +
			"     WHEN stats_method = 'BYM' THEN 'Besag, York and Mollie'" + lineSeparator +
			"     WHEN stats_method = 'CAR' THEN 'Conditional Auto Regression'" + lineSeparator +
			"     ELSE 'NONE' END AS stats_method", // Column list
			null  	/* GROUP BY */,
			null  	/* ORDER BY */,
			"1"		/* Expected rows */,
			true	/* Rotate */, 1 /* headerLevel */, locale, tomcatServer); 

		addTableToHtmlReport(htmlFileText, connection, studyID,
			"rif40", // Owner
			"rif40", // Schema
			"rif40_study_status", // Table 	
			null, // Common table expression
			null, // Joined table
			"study_state,creation_date,message", // Column list
			null  	/* GROUP BY */,
			"ith_update"    	/* ORDER BY */,
			"1+"		/* Expected rows 0+ */,
			false		/* Rotate */, 1 /* headerLevel */, locale, tomcatServer); 
			
		addTableToHtmlReport(htmlFileText, connection, studyID,
			"rif40", // Owner
			"rif40", // Schema
			"rif40_investigations", // Table 	
			"WITH study_num_denom AS (" + lineSeparator +
			"	SELECT a.study_id, b.geography, b.theme_description," + lineSeparator +
			"		   b.numerator_table, b.numerator_description," + lineSeparator +
			"		   b.denominator_table, b.denominator_description," + lineSeparator +
			"		   dmin.fieldname min_age_group, dmax.fieldname max_age_group" + lineSeparator +
			"	  FROM rif40.rif40_studies a, rif40_num_denom b, rif40.rif40_tables c," + lineSeparator +
			"		   rif40.rif40_age_groups dmin, rif40.rif40_age_groups dmax" + lineSeparator +
			"	 WHERE a.geography       = b.geography" + lineSeparator +
			"	   AND a.denom_tab       = b.denominator_table" + lineSeparator +
			"	   AND b.numerator_table = c.table_name" + lineSeparator +
			"	   AND c.age_group_id    = dmin.age_group_id" + lineSeparator +
			"	   AND c.age_group_id    = dmax.age_group_id" + lineSeparator +
			"	   AND a.min_age_group   = dmin.offset" + lineSeparator +
			"	   AND a.max_age_group   = dmax.offset" + lineSeparator +
			")", // Common table expression
			" LEFT OUTER JOIN study_num_denom d ON (d.study_id = t.study_id)", // Joined table
			"t.inv_id,t.inv_name,t.inv_description,t.year_start,t.year_stop," + lineSeparator +
			"CASE"+ lineSeparator +
			"	WHEN t.genders = 1 THEN 'Males'" + lineSeparator +
			"	WHEN t.genders = 2 THEN 'Females'" + lineSeparator +
			"	WHEN t.genders = 3 THEN 'Males and Females'" + lineSeparator +
			"	ELSE 'Unknown'" + lineSeparator +
			"END AS genders," + lineSeparator +
			"d.theme_description,d.numerator_table, d.numerator_description," + lineSeparator + 
			"d.denominator_table, d.denominator_description," + lineSeparator +
			"d.min_age_group, d.max_age_group," + lineSeparator +
			"t.mh_test_type,t.classifier,t.classifier_bands", // Column list
			null  	/* GROUP BY */,
			"t.inv_id"  	/* ORDER BY */,
			"1+"		/* Expected rows 1+ */,
			true		/* Rotate */, 1 /* headerLevel */, locale, tomcatServer); 
			
		addTableToHtmlReport(htmlFileText, connection, studyID,
			"rif40", // Owner
			"rif40", // Schema
			"rif40_inv_covariates", // Table 
			null, // Common table expression	
			null, // Joined table
			"inv_id,covariate_name,min,max,geography,study_geolevel_name", // Column list
			null  	/* GROUP BY */,
			"inv_id,covariate_name"    	/* ORDER BY */,
			"0+"		/* Expected rows 0+ */,
			false		/* Rotate */, 1 /* headerLevel */, locale, tomcatServer); 
	
		addInvConditions(htmlFileText, connection, studyID,
			"rif40", // Owner
			"rif40", // Schema
			getStudyJSON, locale, 1 /* headerLevel */, tomcatServer, taxonomyServicesServer);
			
		addStudyAndComparisonAreas(htmlFileText, connection, studyID,
			"rif40", // Owner
			"rif40", // Schema
			getStudyJSON, locale, tomcatServer);
		
		htmlFileText.append(denominatorHTML);
		
		htmlFileText.append("  </div>" + lineSeparator);
		htmlFileText.append("</div>" + lineSeparator);
		htmlFileText.append("</body>" + lineSeparator);
		htmlFileText.append("</html>" + lineSeparator);
		String htmlFileName="RIFstudy_" + studyID + ".html";
		rifLogger.info(this.getClass(), "Adding HTML report file: " + temporaryDirectory.getAbsolutePath() + File.separator + 
			htmlFileName + " to ZIP file");
		
		File file=new File(temporaryDirectory.getAbsolutePath() + File.separator + htmlFileName);
		ZipEntry zipEntry = new ZipEntry(htmlFileName);

		submissionZipOutputStream.putNextEntry(zipEntry);
		byte[] b=htmlFileText.toString().getBytes();
		submissionZipOutputStream.write(b, 0, b.length);

		submissionZipOutputStream.closeEntry();	

		if (getStudyJSON.getTaxonomyInitialiseError()) {	
			rifLogger.error(this.getClass(), 
				"Taxonomy service still initialising; please run again in 5 minutes");
			throw new Exception("Taxonomy service still initialising; please run again in 5 minutes");
		}		
	}
	
	private String addDenominator(
			final User user,
			final Connection connection,
			final File temporaryDirectory,
			final String studyID,
			final int headerLevel
			)
			throws Exception {
					
		StringBuilder htmlFileText=new StringBuilder();
		
		GetStudyJSON getStudyJSON = new GetStudyJSON(rifServiceStartupOptions);
		RIFGraphics rifGraphics = new RIFGraphics(rifServiceStartupOptions);
		
		JSONObject studyData=getStudyJSON.getStudyData(
			connection, studyID);
		int yearStart=studyData.getInt("year_start");
		int yearStop=studyData.getInt("year_stop");
		String svgCss=readFile("RIFPopulationPyramid.css");

		htmlFileText.append("    <h" + headerLevel + " id=\"denominator\">Denominator</h1>" + lineSeparator);
		htmlFileText.append("      <h" + (headerLevel+1) + ">Denominator by year</h1>" + lineSeparator);
		htmlFileText.append("      <p>" + lineSeparator);
		htmlFileText.append("      </p>" + lineSeparator);		

		String denominatorDirName=addDirToTemporaryDirectoryPath(user, studyID, 
			"reports" + File.separator + "denominator");
		CachedRowSetImpl rif40Studies=getRif40Studies(connection, studyID);
		String extractTable=getColumnFromResultSet(rif40Studies, "extract_table");
		String denominatorTable=getColumnFromResultSet(rif40Studies, "denom_tab");
		String studyDescription=getColumnFromResultSet(rif40Studies, "description", 
			true /* allowNulls */, false /* allowNoRows */);
		if (studyDescription == null) {
			studyDescription="No description";
		}
		CachedRowSetImpl rif40ExtraxctMaxMinYear=getStudyStartEndYear(connection, extractTable);
		int minYear=Integer.parseInt(getColumnFromResultSet(rif40ExtraxctMaxMinYear, "min_year"));
		int maxYear=Integer.parseInt(getColumnFromResultSet(rif40ExtraxctMaxMinYear, "max_year"));
		
		addTableToHtmlReport(htmlFileText, connection, null /* studyID */,
			"rif40",		// Owner
			"rif_studies",	// Schema
			extractTable, // Table
			null, // Common table expression
			" WHERE study_or_comparison = 'S'", // Joined table 	
			"t.year," + lineSeparator +
			"       CASE WHEN t.sex = 1 THEN 'Males' WHEN t.sex = 2 THEN 'Females' ELSE 'Other' END AS sex," + lineSeparator +
			"SUM(t.total_pop) AS total_pop", // Column list
			"t.year, t.sex"  	/* GROUP BY */,
			"1, 2"   	/* ORDER BY */,
			"1+"		/* Expected rows 0+ */,
			false		/* Rotate */, (headerLevel+1) /* headerLevel */, null /* locale */, null /* tomcatServer */); 

		htmlFileText.append("      <h" + (headerLevel+1) + ">Population Pyramids</h1>" + lineSeparator);
		htmlFileText.append("      <p>" + lineSeparator);		
		htmlFileText.append("      <div>" + lineSeparator);
		htmlFileText.append("        <form id=\"downloadForm\" method=\"get\" action=\"reports\\denominator\\RIFdenominator_pyramid_" + 
			studyID + "_" + printingDPI + "dpi_" + minYear + ".png\">" + lineSeparator);
		htmlFileText.append("        Year: <select id=\"populationPyramidList\">" + lineSeparator);					

		for (int i=minYear; i<=maxYear; i++) {
			if (i == yearStart) { // Selected
				htmlFileText.append("          <option value=\"reports\\denominator\\RIFdenominator_pyramid_" + 
					studyID + "_" + printingDPI + "dpi_" + i + ".png\" selected />" + i + "</option>" + 
					lineSeparator);

			}
			else {
				htmlFileText.append("          <option value=\"reports\\denominator\\RIFdenominator_pyramid_" + 
					studyID + "_" + printingDPI + "dpi_" + i + ".png\" />" + i + "</option>" + lineSeparator);

			}

			String svgText=rifGraphics.getPopulationPyramid(connection, extractTable, denominatorTable, 
				studyDescription, studyID, i,
				true /* treeForm: true - classic tree form; false: stack to right */);	
			rifGraphics.addSvgFile(
				temporaryDirectory,
				"reports" + File.separator + "denominator",
				"RIFdenominator_treepyramid_",
				studyID,
				i,
				svgText); 
			svgText=rifGraphics.getPopulationPyramid(connection, extractTable, denominatorTable, 
				studyDescription, studyID, i,
				false /* treeForm: true - classic tree form; false: stack to right */);	
			rifGraphics.addSvgFile(
				temporaryDirectory,
				"reports" + File.separator + "denominator",
				"RIFdenominator_pyramid_",
				studyID,
				i,
				svgText); 
		}
		htmlFileText.append("        </select>" + lineSeparator);
		
		htmlFileText.append("        Graphics Format: <select id=\"populationPyramidFileType\">" + lineSeparator);
		Set<RIFGraphicsOutputType> htmlOutputTypes = EnumSet.of( // Can be viewed in browser
			RIFGraphicsOutputType.RIFGRAPHICS_PNG,
			RIFGraphicsOutputType.RIFGRAPHICS_JPEG,
			RIFGraphicsOutputType.RIFGRAPHICS_TIFF,  
			RIFGraphicsOutputType.RIFGRAPHICS_SVG);
		Iterator <RIFGraphicsOutputType> htmlOutputTypeIter = htmlOutputTypes.iterator();
		int j=0;
		while (htmlOutputTypeIter.hasNext()) {
			String selected="";
			String disabled="";
			RIFGraphicsOutputType outputType=htmlOutputTypeIter.next();
			j++;
			if (j==0) {
				selected="selected";
			}
			if (!outputType.isRIFGraphicsOutputTypeEnabled()) {
				disabled="disabled";
			}
			htmlFileText.append("          <option value=\"" + 
				outputType.getRIFGraphicsOutputTypeShortName().toLowerCase() +
				"\" " + disabled + " " +
				"id=\"" + outputType.getRIFGraphicsOutputTypeShortName().toLowerCase() + "Select\" " + 
				"title=\"" + outputType.getRIFGraphicsOutputTypeDescription() + "\" " + 
				selected + " />" + outputType.getRIFGraphicsOutputTypeShortName() + " (" + 
					outputType.getRIFGraphicsOutputTypeDescription() +
				")</option>" + lineSeparator);
		}
		htmlFileText.append("        </select>" + lineSeparator);
		
		htmlFileText.append("        Pyramid type: <select id=\"populationPyramidType\">" + lineSeparator);
		htmlFileText.append("          <option value=\"tree\" title=\"Tree\" />Tree</option>" + lineSeparator);
		htmlFileText.append("          <option value=\"stackedRight\" tile=\"Stacked to the right\" selected />Stacked to the right</option>" + lineSeparator);
		htmlFileText.append("        </select>" + lineSeparator);
		

		htmlFileText.append("          <button id=\"downloadButton\" type=\"submit\">Download PNG</button>" + lineSeparator);
		htmlFileText.append("        </form>" + lineSeparator);
		htmlFileText.append("      </div>" + lineSeparator);
		htmlFileText.append("      <img src=\"reports\\denominator\\RIFdenominator_pyramid_" + 
			studyID + "_" + printingDPI +	"dpi_" + minYear + 
			".png\" id=\"denominator_pyramid\" width=\"80%\" />" + lineSeparator);
		htmlFileText.append("    </p>" + lineSeparator);
		
		for (int year=minYear; year<=maxYear; year++) {
			Set<RIFGraphicsOutputType> allOutputTypes = EnumSet.of(
				RIFGraphicsOutputType.RIFGRAPHICS_JPEG,
				RIFGraphicsOutputType.RIFGRAPHICS_PNG,
				RIFGraphicsOutputType.RIFGRAPHICS_TIFF,    // Requires 1.9.2 or higher Batik
				RIFGraphicsOutputType.RIFGRAPHICS_EPS,
				RIFGraphicsOutputType.RIFGRAPHICS_PS);
			Iterator <RIFGraphicsOutputType> allOutputTypeIter = allOutputTypes.iterator();
			while (allOutputTypeIter.hasNext()) {
				RIFGraphicsOutputType outputType=allOutputTypeIter.next();
				if (outputType.isRIFGraphicsOutputTypeEnabled()) {					
					rifGraphics.addGraphicsFile(
						temporaryDirectory,							/* Study scratch space diretory */
						"reports" + File.separator + "denominator", /* directory */
						"RIFdenominator_treepyramid_", 				/* File prefix */
						studyID,
						year,
						outputType);
					rifGraphics.addGraphicsFile(
						temporaryDirectory,							/* Study scratch space diretory */
						"reports" + File.separator + "denominator", /* directory */
						"RIFdenominator_pyramid_", 					/* File prefix */
						studyID,
						year,
						outputType);
				}
			}				
		}	
		
		return htmlFileText.toString();
	}
		
	private CachedRowSetImpl getStudyStartEndYear(
			final Connection connection,
			final String extractTable)
			throws Exception {
		SQLGeneralQueryFormatter extractTableQueryFormatter = new SQLGeneralQueryFormatter();		
		
		ResultSet resultSet = null;
		CachedRowSetImpl cachedRowSet = null;
		
		extractTableQueryFormatter.addQueryLine(0, "SELECT MIN(year) AS min_year, MAX(year) AS max_year");
		extractTableQueryFormatter.addQueryLine(0, "  FROM rif_studies." + extractTable.toLowerCase());
		extractTableQueryFormatter.addQueryLine(0, " WHERE study_or_comparison = 'S'");

		PreparedStatement statement = createPreparedStatement(connection, extractTableQueryFormatter);
		try {	
			resultSet = statement.executeQuery();
			 // create CachedRowSet and populate
			cachedRowSet = new CachedRowSetImpl();
			cachedRowSet.populate(resultSet);
		}
		catch (Exception exception) {
			rifLogger.error(this.getClass(), "Error in SQL Statement: >>> " + 
				lineSeparator + extractTableQueryFormatter.generateQuery(),
				exception);
			throw exception;
		}
		finally {
			SQLQueryUtility.close(statement);
		}
		
		return cachedRowSet;
	}
	
	private CachedRowSetImpl getRif40Studies(
			final Connection connection,
			final String studyID)
			throws Exception {
		SQLGeneralQueryFormatter rif40StudiesQueryFormatter = new SQLGeneralQueryFormatter();		
		
		ResultSet resultSet = null;
		CachedRowSetImpl cachedRowSet = null;
		
		rif40StudiesQueryFormatter.addQueryLine(0, "SELECT extract_table, map_table, denom_tab, description");
		rif40StudiesQueryFormatter.addQueryLine(0, "  FROM rif40.rif40_studies");
		rif40StudiesQueryFormatter.addQueryLine(0, " WHERE study_id = ?");

		PreparedStatement statement = createPreparedStatement(connection, rif40StudiesQueryFormatter);
		try {
			statement.setInt(1, Integer.parseInt(studyID));		
			resultSet = statement.executeQuery();
			 // create CachedRowSet and populate
			cachedRowSet = new CachedRowSetImpl();
			cachedRowSet.populate(resultSet);
		}
		catch (Exception exception) {
			rifLogger.error(this.getClass(), "Error in SQL Statement: >>> " + 
				lineSeparator + rif40StudiesQueryFormatter.generateQuery(),
				exception);
			throw exception;
		}
		finally {
			SQLQueryUtility.close(statement);
		}
		
		return cachedRowSet;
	}
	
	private void addStudyAndComparisonAreas(
			final StringBuilder htmlFileText,
			final Connection connection,
			final String studyID,
			final String ownerName,
			final String schemaName,
			final GetStudyJSON getStudyJSON,
			final Locale locale,
			final String tomcatServer)
			throws Exception {
		SQLGeneralQueryFormatter studyAndComparisonReportQueryFormatter = new SQLGeneralQueryFormatter();		
		
		ResultSet resultSet = null;
		
		studyAndComparisonReportQueryFormatter.addQueryLine(0, "SELECT study_geolevel_name, comparison_geolevel_name, geography");
		studyAndComparisonReportQueryFormatter.addQueryLine(0, "  FROM " + schemaName + ".rif40_studies");
		studyAndComparisonReportQueryFormatter.addQueryLine(0, " WHERE study_id = ?");

		PreparedStatement statement = createPreparedStatement(connection, studyAndComparisonReportQueryFormatter);
		try {
			statement.setInt(1, Integer.parseInt(studyID));		
			resultSet = statement.executeQuery();
			String studyGeolevelName=null;
			String comparisonGeolevelName=null;
			String geographyName=null;
			
			if (resultSet.next()) {
				studyGeolevelName=resultSet.getString(1);
				comparisonGeolevelName=resultSet.getString(2);
				geographyName=resultSet.getString(3);	
				JSONObject studyGeolevel=getStudyJSON.getLookupTableName(
					connection, studyGeolevelName, geographyName);
				JSONObject comparisonGeolevel=getStudyJSON.getLookupTableName(
					connection, comparisonGeolevelName, geographyName);
		
				addTableToHtmlReport(htmlFileText, connection, studyID,
					ownerName,	// Owner
					schemaName,	// Schema
					"rif40_study_areas", // Table
					null, // Common table expression
					"LEFT OUTER JOIN rif_data." +			
						studyGeolevel.getString("lookup_table").toLowerCase() + 
						" b ON (t.area_id = b." + studyGeolevelName.toLowerCase() + ")", // Joined table 	
					"t.area_id, t.band_id, b." + 
						studyGeolevel.getString("lookup_desc_column").toLowerCase() + 
						" AS label", // Column list
					null  	/* GROUP BY */,
					"2, 1"    	/* ORDER BY */,
					"1+"		/* Expected rows 0+ */,
					false		/* Rotate */, 1 /* headerLevel */, locale, tomcatServer); 
	
				addTableToHtmlReport(htmlFileText, connection, studyID,
					ownerName,
					schemaName,
					"rif40_comparison_areas", // Table
					null, // Common table expression
					"LEFT OUTER JOIN rif_data." +			
						comparisonGeolevel.getString("lookup_table").toLowerCase() + 
						" b ON (t.area_id = b." + comparisonGeolevelName.toLowerCase() + ")", // Joined table 	
					"t.area_id, b." + 
						comparisonGeolevel.getString("lookup_desc_column").toLowerCase() + 
						" AS label", // Column list
					null  	/* GROUP BY */,
					"1"    	/* ORDER BY */,
					"1+"		/* Expected rows 0+ */,
					false		/* Rotate */, 1 /* headerLevel */, locale, tomcatServer); 
			}		
		}
		catch (Exception exception) {
			rifLogger.error(this.getClass(), "Error in SQL Statement: >>> " + 
				lineSeparator + studyAndComparisonReportQueryFormatter.generateQuery(),
				exception);
			throw exception;
		}
		finally {
			SQLQueryUtility.close(statement);
		}	
	}
	
	private String getTableComment(Connection connection, String schemaName, String tableName)
			throws Exception {
		SQLGeneralQueryFormatter tableCommentQueryFormatter = new SQLGeneralQueryFormatter();		
		ResultSet resultSet = null;
		if (databaseType == DatabaseType.POSTGRESQL) {
			tableCommentQueryFormatter.addQueryLine(0, // Postgres
				"SELECT obj_description('" + schemaName + "." + tableName + "'::regclass) AS table_comment");
		}
		else if (databaseType == DatabaseType.SQL_SERVER) {
			tableCommentQueryFormatter.addQueryLine(0, "SELECT CAST(value AS VARCHAR(2000)) AS table_comment"); // SQL Server
			tableCommentQueryFormatter.addQueryLine(0, "FROM fn_listextendedproperty (NULL, 'schema', ?, 'table', ?, NULL, NULL)");
			tableCommentQueryFormatter.addQueryLine(0, "UNION");
			tableCommentQueryFormatter.addQueryLine(0, "SELECT CAST(value AS VARCHAR(2000)) AS table_comment");
			tableCommentQueryFormatter.addQueryLine(0, "FROM fn_listextendedproperty (NULL, 'schema', ?, 'view', ?, NULL, NULL)");
		}
		else {
			throw new Exception("getTableComment(): invalid databaseType: " + 
				databaseType);
		}
		PreparedStatement statement = createPreparedStatement(connection, tableCommentQueryFormatter);
		
		String tableComment=tableName;
		try {			
			
			if (databaseType == DatabaseType.SQL_SERVER) {
				statement.setString(1, schemaName);	
				statement.setString(2, tableName);
				statement.setString(3, schemaName);	
				statement.setString(4, tableName);	
			}			
			resultSet = statement.executeQuery();
			if (resultSet.next()) {		
				tableComment=resultSet.getString(1);
				if (resultSet.next()) {		
					throw new Exception("getTableComment(): expected 1 row, got >1");
				}
			}
			else {
				rifLogger.warning(this.getClass(), "getTableComment(): expected 1 row, got none");
			}
		}
		catch (Exception exception) {
			rifLogger.error(this.getClass(), "Error in SQL Statement: >>> " + 
				lineSeparator + tableCommentQueryFormatter.generateQuery(),
				exception);
			throw exception;
		}
		finally {
			SQLQueryUtility.close(statement);
		}
		
		return tableComment;
	}

	private void addInvConditions(
			final StringBuilder htmlFileText,
			final Connection connection,
			final String studyID,
			final String ownerName,
			final String schemaName,
			final GetStudyJSON getStudyJSON,
			final Locale locale,
			final int headerLevel,
			final String tomcatServer,
			final String taxonomyServicesServer)
			throws Exception {
			
		String tableName="rif40_inv_conditions";	
		String tableComment=getTableComment(connection, "rif40", tableName);
		
		SQLGeneralQueryFormatter invConditionsQueryFormatter = new SQLGeneralQueryFormatter();		
				
		invConditionsQueryFormatter.addQueryLine(0, "SELECT inv_id,numer_tab,min_condition,max_condition,");
		invConditionsQueryFormatter.addQueryLine(0, "       outcome_group_name,condition");
		invConditionsQueryFormatter.addQueryLine(0, "  FROM rif40.rif40_inv_conditions");
		invConditionsQueryFormatter.addQueryLine(0, " WHERE study_id = ?");
		invConditionsQueryFormatter.addQueryLine(0, " ORDER BY inv_id,line_number");
		PreparedStatement statement = createPreparedStatement(connection, invConditionsQueryFormatter);	
		ResultSet resultSet = null;
		htmlFileText.append("    <h" + headerLevel + " id=\"" + tableName + "\">Conditions</h1>" + lineSeparator);
			
		try {
			int rowCount = 0;
				
			statement.setInt(1, Integer.parseInt(studyID));		
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				ResultSetMetaData rsmd = resultSet.getMetaData();
				int columnCount = rsmd.getColumnCount();
				StringBuffer headerText = new StringBuffer();
				String minCondition;
				String maxCondition;
				String outComeType;
						
				Boolean endSpan=false;
				String invId="";
				String numerTab="";
				int spanCount=0;
				
				String[] commentArray = new String[columnCount+1];
				int commentArrayLength=0;
					
				htmlFileText.append("    <p>" + lineSeparator);
				htmlFileText.append("      <table id=\"" + tableName + "_table\" border=\"1\" summary=\"" + tableName + "\">" +  lineSeparator);
				htmlFileText.append("        <caption><em>" + tableComment + "</em></caption>" + 
					lineSeparator);

				headerText.append("        <tr>" + lineSeparator);
				do {	
					rowCount++;
					
					String statementNumber=null;
					StringBuffer bodyText = new StringBuffer();
					minCondition="";
					maxCondition=null;
					outComeType="";
					
					bodyText.append("        <tr>" + lineSeparator);
					ArrayList<String> conditionList = new ArrayList<String>();
					ArrayList<String> descriptionList = new ArrayList<String>();
					ArrayList<String> outcomeTypeList = new ArrayList<String>();
					
					// The column count starts from 1
					for (int i = 1; i <= columnCount; i++ ) {
						String name = rsmd.getColumnName(i);
						String value = resultSet.getString(i);	
						String columnType = rsmd.getColumnTypeName(i);
						
						if (value == null) {
							value="&nbsp;";
						}
						
						if (name.equals("min_condition")) {
							if (!value.equals("&nbsp;")) {
								JSONObject taxonomyObject = getStudyJSON.getHealthCodeDesription(tomcatServer, taxonomyServicesServer, value);
								minCondition=taxonomyObject.getString("description");
							}
						}
						else if (name.equals("max_condition")) {
							if (!value.equals("&nbsp;")) {
								// Add: please run again in 5 minutes support
								JSONObject taxonomyObject = getStudyJSON.getHealthCodeDesription(tomcatServer, taxonomyServicesServer, value);
								maxCondition=taxonomyObject.getString("description");
							}
						}						
						else if (name.equals("outcome_group_name")) {
							if (!value.equals("&nbsp;")) {
								outComeType=getOutcomeType(connection, value);
							}
						}
						else {
							if (rowCount == 1) {
								
								String columnComment=getColumnComment(connection, 
									schemaName, tableName, name /* Column name */);
								if (name.equals("numer_tab")) {
									name="numerator_table";
								}
								headerText.append("          <th title=\"" + columnComment + "\">" + 
									name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", " ") + 
									"<!-- " + columnType + " -->" + "</th>" + lineSeparator);
						
								commentArray[commentArrayLength]="        <li class=\"dictionary\"><em>" + name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", " ") +
									"</em>: " + columnComment + "</li>" + lineSeparator;
								commentArrayLength++;
							}
							
							bodyText.append("          <td>" + value + 
								"       </td><!-- Column: " + i +
								"; row: " + rowCount +
								" -->" + lineSeparator);
							if (name.equals("inv_id") && invId.equals("")) {
								invId=value;
							}
							else if (name.equals("numer_tab") && numerTab.equals("")) {
								numerTab=value;
							}
							else if (name.equals("inv_id") && !invId.equals(value)) {
								bodyText.append("          <!-- Detect end span, name: " + name +
									"; old value: " + invId + "; new value: " + value + " -->" + 
									lineSeparator);
								endSpan=true;
								invId=value;
							}	
							else if (name.equals("numer_tab") && !numerTab.equals(value)) {
								bodyText.append("          <!-- Detect end span, name: " + name +
									"; old value: " + numerTab + "; new value: " + value + " -->" + 
									lineSeparator);
								endSpan=true;
								numerTab=value;
							}	
							else if (name.equals("condition")) {
								conditionList.add(value);
							}
						}
				
					}
					
					if (rowCount == 1) {
						headerText.append("          <th title=\"Taxonomy outcome type\">Outcome type</th>" + lineSeparator);
						headerText.append("          <th title=\"Taxonomy description\">Description</th>" + lineSeparator);
						headerText.append("        </tr>" + lineSeparator);
						htmlFileText.append(headerText.toString());
					}

					bodyText.append("          <td>");
					
					if (outComeType != null) {
						bodyText.append(outComeType);
					}
					else {
						bodyText.append("&nbsp;");
					}
					bodyText.append("</td>" + lineSeparator);
					outcomeTypeList.add(outComeType);
					
					bodyText.append("          <td>" + minCondition);
					if (maxCondition != null) {
						bodyText.append(" - " + maxCondition);
						descriptionList.add(minCondition + " - " + maxCondition);
					}
					else {
						descriptionList.add(minCondition);
					}
					bodyText.append("</td>" + lineSeparator);
					
					bodyText.append("        </tr>" + lineSeparator);
					
					if (endSpan) {
						bodyText.append("        <!-- endSpan: " + spanCount + 
							"; outcomeTypeList: " + outcomeTypeList.size() + 
							"; conditionList: " + conditionList.size() + 
							"; descriptionList: " + descriptionList.size() + 
							" -->" + lineSeparator);
						endSpan=false;
						outcomeTypeList.clear();
						conditionList.clear();
						descriptionList.clear();
						spanCount=0;
					}
					else {
						spanCount++;
					}
					htmlFileText.append(bodyText.toString());
				} while (resultSet.next());
				
				htmlFileText.append("      </table>" + lineSeparator);
				
				htmlFileText.append("    </p>" + lineSeparator);
				htmlFileText.append("    <p>" + lineSeparator);
				
				commentArray[commentArrayLength]="        <li class=\"dictionary\"><em>Outcome type</em>: Taxonmomy Outcome type</li>" + lineSeparator;
				commentArray[commentArrayLength]="        <li class=\"dictionary\"><em>Description</em>: Taxonmomy description</li>" + lineSeparator;
				commentArrayLength++;
				htmlFileText.append("      <ul class=\"dictionary\">" + lineSeparator);
				for (int j = 0; j < commentArrayLength; j++) {
					htmlFileText.append(commentArray[j]);
				}
				htmlFileText.append("      </ul>" + lineSeparator);	
				htmlFileText.append("    </p>" + lineSeparator);		
			}
			else {
				htmlFileText.append("    <p>No data found</p>" + lineSeparator);
			}			
		}
		catch (Exception exception) {
			rifLogger.error(this.getClass(), "Error in SQL Statement: >>> " + 
				lineSeparator + invConditionsQueryFormatter.generateQuery(),
				exception);
			throw exception;
		}
		finally {
			SQLQueryUtility.close(statement);
		}	
	}	
	
	/**
	 * Get outcome type. Will return the current ontology version e.g. icd10 even if icd9 codes 
	 * are actually being used
	 *
     * @param String outcome_group_name (required)
	 * @return outcome type string
     */	
	private String getOutcomeType(Connection connection, String outcome_group_name) 
					throws Exception {
		SQLGeneralQueryFormatter rifOutcomeGroupsQueryFormatter = new SQLGeneralQueryFormatter();		
		ResultSet resultSet = null;
		
		if (outcome_group_name == null) {
			throw new Exception("Null outcome_group_name");
		}
		
 		rifOutcomeGroupsQueryFormatter.addQueryLine(0, 
			"SELECT a.outcome_type, b.current_version"); 
		rifOutcomeGroupsQueryFormatter.addQueryLine(0, 
			"  FROM rif40.rif40_outcome_groups a, rif40.rif40_outcomes b"); 
		rifOutcomeGroupsQueryFormatter.addQueryLine(0, 
			" WHERE a.outcome_group_name = ? AND a.outcome_type = b.outcome_type");
		PreparedStatement statement = createPreparedStatement(connection, 
			rifOutcomeGroupsQueryFormatter);
		String outcomeGroup=null;
		try {			
			statement.setString(1, outcome_group_name);	
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				outcomeGroup=resultSet.getString(1) + resultSet.getString(2);
				if (resultSet.next()) {
					throw new Exception("getOutcomeType(): expected 1 row, got >1");
				}
			}
			else {
				throw new Exception("getOutcomeType(): expected 1 row, got none");
			}
		}
		catch (Exception exception) {
			rifLogger.error(this.getClass(), "Error in SQL Statement: >>> " + lineSeparator + rifOutcomeGroupsQueryFormatter.generateQuery(),
				exception);
			throw exception;
		}
		finally {
			SQLQueryUtility.close(statement);
		}
		
		return outcomeGroup;
	}	
	
	private void addTableToHtmlReport(
			final StringBuilder htmlFileText,
			final Connection connection,
			final String studyID,
			final String ownerName,
			final String schemaName,
			final String tableName,
			final String commonTableExpression,
			final String joinedTable,
			final String columnList,
			final String groupBy,
			final String orderBy,
			final String expectedRows,
			final boolean rotate,
			final int headerLevel,
			final Locale locale,
			final String tomcatServer)
			throws Exception {
	
		Calendar calendar = null;
		DateFormat df = null;
		if (locale != null) {
			df=DateFormat.getDateTimeInstance(
				DateFormat.DEFAULT /* Date style */, 
				DateFormat.DEFAULT /* Time style */, 
				locale);
			calendar = df.getCalendar();
		}
		else { // assume US
			df=new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"); // MM/DD/YY HH24:MI:SS
			calendar = Calendar.getInstance();
		}
		
		String tableComment=getTableComment(connection, schemaName, tableName);
		String tableHeader=tableName.replace("rif40_", "");
		if (tableHeader.substring(0, 4).equals("inv_")) {
			tableHeader=tableName.replace("rif40_inv_", "");
		}
		htmlFileText.append("    <h" + headerLevel + " id=\"" + tableName + "\">" + 
			tableHeader.substring(0, 1).toUpperCase() + tableHeader.substring(1).replace("_", " ") + 
			"</h1>" + lineSeparator);
		String valueLavel="Value";
		if (tableHeader.length() > 2 && 
		    tableHeader.substring(tableHeader.length()-1, tableHeader.length()).
				equals("s")) {
			valueLavel=tableHeader.substring(0, 1).toUpperCase() + 
				tableHeader.substring(1, tableHeader.length()-1).
					replace("_", " ");
		}
		else if (tableHeader.equals("Studies")) {
			valueLavel="Study";
		}
		SQLGeneralQueryFormatter htmlReportQueryFormatter = new SQLGeneralQueryFormatter();		
		ResultSet resultSet = null;
		if (commonTableExpression != null) {
			htmlReportQueryFormatter.addQueryLine(0, commonTableExpression);
		}
		htmlReportQueryFormatter.addQueryLine(0, "SELECT " + columnList);
		htmlReportQueryFormatter.addQueryLine(0, "  FROM " + schemaName + "." + tableName + " t ");
			// Note the alias is always t!
		if (joinedTable != null) {
			htmlReportQueryFormatter.addQueryLine(0, joinedTable);
		}
		else {
			htmlReportQueryFormatter.addQueryLine(0, "/* No joined table */");
		}
		if (studyID != null) {
			htmlReportQueryFormatter.addQueryLine(0, " WHERE t.study_id = ?");
		}
		if (groupBy != null) {
			htmlReportQueryFormatter.addQueryLine(0, " GROUP BY " + groupBy);
		}
		if (orderBy != null) {
			htmlReportQueryFormatter.addQueryLine(0, " ORDER BY " + orderBy);
		}
		PreparedStatement statement = createPreparedStatement(connection, htmlReportQueryFormatter);
		try {			
			int rowCount = 0;
				
			if (studyID != null) {
				statement.setInt(1, Integer.parseInt(studyID));	
			}	
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				ResultSetMetaData rsmd = resultSet.getMetaData();
				int columnCount = rsmd.getColumnCount();
				StringBuffer headerText = new StringBuffer();
				
				String[] commentArray = new String[columnCount];
					
				htmlFileText.append("    <p>" + lineSeparator);
				htmlFileText.append("      <table id=\"" + tableName + "_table\" border=\"1\" summary=\"" + tableName + "\">" +  lineSeparator);
				htmlFileText.append("        <caption><em>" + tableComment + "</em></caption>" + 
					lineSeparator);

				if (rotate) {
					headerText.append("        <tr>" + lineSeparator +
						"          <th>Attribute</th>" + lineSeparator);	
				}
				else {
					headerText.append("        <tr>" + lineSeparator);
				}
				do {	
					rowCount++;
					
					String statementNumber=null;
					StringBuffer bodyText = new StringBuffer();
					String[] rotatedRowsArray = new String[columnCount];
					
					if (!rotate) {
						bodyText.append("        <tr>" + lineSeparator);
					}
					else {
						headerText.append("          <th>" + valueLavel + ": " + rowCount + "</th>" + lineSeparator);
					}
					// The column count starts from 1
					for (int i = 1; i <= columnCount; i++ ) {
						String name = rsmd.getColumnName(i);
						String value = resultSet.getString(i);	
						String columnType = rsmd.getColumnTypeName(i);
						
						if (columnType.equals("timestamp") /* Postgres */) {
							Timestamp dateTimeValue=resultSet.getTimestamp(i, calendar);
							value=df.format(dateTimeValue) + "<!-- DATE -->";
						}
						else if (value == null) {
							value="&nbsp;";
						}
						
						if (rowCount == 1) {
							
							String columnComment=getColumnComment(connection, 
								schemaName, tableName, name /* Column name */);
							if (rotate) {
								rotatedRowsArray[i-1]="          <td title=\"" + columnComment + "\">" + 
									name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", " ") + 
									"<!-- " + columnType + " -->" +
									"          </td>" +
									lineSeparator; //Initialise
							}
							else {
								headerText.append("          <th title=\"" + columnComment + "\">" + 
									name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", " ") + 
									"<!-- " + columnType + " -->" + "</th>" + lineSeparator);
							}
							commentArray[i-1]="        <li class=\"dictionary\"><em>" + name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", " ") +
								"</em>: " + columnComment + "</li>" + lineSeparator;
						}
						
						if (rotate) {
							rotatedRowsArray[i-1]+="          <td>" + value + "</td><!-- Column: " + i +
								"; row: " + rowCount +
								" -->" + lineSeparator;
						}
						else {
							bodyText.append("          <td>" + value + 
								"       </td><!-- Column: " + i +
								"; row: " + rowCount +
								" -->" + lineSeparator);
						}
					}
					
					if (rowCount == 1) {
						headerText.append("        </tr>" + lineSeparator);
						htmlFileText.append(headerText.toString());
					}
					
					if (!rotate) {
						bodyText.append("        </tr>" + lineSeparator);
					}
					else {
						for (int j = 0; j < rotatedRowsArray.length; j++) {
							bodyText.append("        <tr>" + lineSeparator);
							bodyText.append(rotatedRowsArray[j]);
							bodyText.append("        </tr>" + lineSeparator);
						}
					}
					htmlFileText.append(bodyText.toString());
				} while (resultSet.next());
				
				htmlFileText.append("      </table>" + lineSeparator);
				
				htmlFileText.append("    </p>" + lineSeparator);
				htmlFileText.append("    <p>" + lineSeparator);
					htmlFileText.append("      <ul class=\"dictionary\">" + lineSeparator);
				for (int j = 0; j < commentArray.length; j++) {
					htmlFileText.append(commentArray[j]);
				}
				htmlFileText.append("      </ul>" + lineSeparator);
				htmlFileText.append("    </p>" + lineSeparator);
					
			}
			else {
				htmlFileText.append("    <p>No data found</p>" + lineSeparator);
			}	
			
			if (expectedRows.equals("1") && rowCount == 1) { // OK
			}		
			else if (expectedRows.equals("1+") && rowCount > 0) { // OK
			}			
			else if (expectedRows.equals("0+")) { // No need to check
			}
			else {
				throw new Exception("Expecting: " + expectedRows + "; got: " + rowCount);
			}			
		}
		catch (Exception exception) {
			rifLogger.error(this.getClass(), "Error in SQL Statement: >>> " + 
				lineSeparator + htmlReportQueryFormatter.generateQuery(),
				exception);
			throw exception;
		}
		finally {
			SQLQueryUtility.close(statement);
		}					
	}
	
	private void addJsonFile(
			final File temporaryDirectory,
			final ZipOutputStream submissionZipOutputStream,
			final Connection connection,
			final User user,
			final String studyID,
			final Locale locale,
			final String tomcatServer) 
			throws Exception {
				
		JSONObject json=new JSONObject();
		GetStudyJSON getStudyJSON = new GetStudyJSON(rifServiceStartupOptions);
		JSONObject rif_job_submission=getStudyJSON.addRifStudiesJson(connection, 
			studyID, locale, tomcatServer, null);
		rif_job_submission.put("created_by", user.getUserID());
		json.put("rif_job_submission", rif_job_submission);
		
		String jsonFileText=readFile("RIFStudyHeader.json") + json.toString(2);
		String JSONFileName="RIFstudy_" + studyID + ".json";
		
		rifLogger.info(this.getClass(), "Adding JSONfile: " + temporaryDirectory.getAbsolutePath() + File.separator + 
			JSONFileName + " to ZIP file");
		
		File file=new File(temporaryDirectory.getAbsolutePath() + File.separator + JSONFileName);
		ZipEntry zipEntry = new ZipEntry(JSONFileName);

		submissionZipOutputStream.putNextEntry(zipEntry);
		byte[] b=jsonFileText.getBytes();
		submissionZipOutputStream.write(b, 0, b.length);

		submissionZipOutputStream.closeEntry();		
	}

	private String readFile(String file) throws IOException {
		String line = null;
		StringBuilder stringBuilder = new StringBuilder();
		String file1;
		String file2;
		FileReader input = null;
		
		if (catalinaHome != null) {
			file1=catalinaHome + "\\conf\\" + file;
			file2=catalinaHome + "\\webapps\\rifServices\\WEB-INF\\classes\\" + file;
		}
		else {
			rifLogger.warning(this.getClass(), 
				"MSSQLAbstractRIFStudySubmissionService.getFrontEndParameters: CATALINA_HOME not set in environment"); 
			file1="C:\\Program Files\\Apache Software Foundation\\Tomcat 8.5\\conf\\" + file;
			file2="C:\\Program Files\\Apache Software Foundation\\Tomcat 8.5\\webapps\\rifServices\\WEB-INF\\classes\\" + file;
		}
		
		try {
			input=new FileReader(file1);
			rifLogger.info(this.getClass(), 
				"RifZipFile.readFile: using: " + file1);
		} 
		catch (IOException ioException) {
			try {
				input=new FileReader(file2);
				rifLogger.info(this.getClass(), 
					"RifZipFile.readFile: using: " + file2);
			}
			catch (IOException ioException2) {				
				rifLogger.warning(this.getClass(), 
					"RifZipFile.readFile error for files: " + 
						file1 + " and " + file2, 
					ioException2);
				return "/* No header file found */";
			}
		}	
				
		BufferedReader reader = new BufferedReader(input);
		while((line = reader.readLine()) != null) {
			stringBuilder.append(line);
			stringBuilder.append(lineSeparator);
		}

		reader.close();
		return stringBuilder.toString();

	}
	
	private String addDirToTemporaryDirectoryPath(
		final User user,
		final String studyID,
		final String dirName) throws Exception {
		
		String temporaryDirectoryPath = 
				createTemporaryDirectoryPath(
						user, 
						studyID);
		File temporaryDirectory = new File(temporaryDirectoryPath);
		File newDirectory = null;
		
		if (temporaryDirectory.exists()) {
			newDirectory = new File(temporaryDirectoryPath + File.separator + dirName);
			if (newDirectory.exists()) {
				rifLogger.info(this.getClass(), 
					"Found directory: " + newDirectory.getAbsolutePath());
			}
			else {
				newDirectory.mkdirs();
				rifLogger.info(this.getClass(), 
					"Created directory: " + newDirectory.getAbsolutePath());
			}
		}
		else {
			throw new Exception("R temporary directory: "  + 
				temporaryDirectory.getAbsolutePath() + " was not created by Adj_Cov_Smooth_JRI.R");
		}
		
		return newDirectory.getAbsolutePath();
	}
			
	private File createSubmissionZipFile(
		final User user,
		final String baseStudyName) {

		StringBuilder fileName = new StringBuilder();
		fileName.append(EXTRACT_DIRECTORY);
		fileName.append(File.separator);
		fileName.append(user.getUserID());		
		fileName.append("_");
		fileName.append(baseStudyName);
		fileName.append(".zip");
		
		return new File(fileName.toString());		
	}
	/*
	 * Produces the base name for result files.
	 */
	private String createBaseStudyFileName(
		final RIFStudySubmission rifStudySubmission,
		final String studyID) {
		
		AbstractStudy study = rifStudySubmission.getStudy();
//		String name = study.getName().toLowerCase();
		String name = "s" + studyID + "_" + study.getName().toLowerCase();
		//concatenate study name length.  We need to be mindful about
		//the length of file names we produce so that they are not too
		//long for some operating systems to handle.
		
		if (name.length() > BASE_FILE_STUDY_NAME_LENGTH) {
			name = name.substring(0, BASE_FILE_STUDY_NAME_LENGTH);
		}
		
		
		//replace any spaces with underscores
		name = name.replaceAll(" ", "_");
		
		return name;
	}
	
	private String createTemporaryDirectoryPath(
		final User user,
		final String studyID) {
		
		StringBuilder fileName = new StringBuilder();
		fileName.append(EXTRACT_DIRECTORY);
		
		// Numbered directory support (1-100 etc) to reduce the number of files/directories per directory to 100. This is to improve filesystem 
		// performance on Windows Tomcat servers 	
		Integer centile=Integer.parseInt(studyID) / 100; // 1273 = 12
		// Number directory: d1201-1300
		String numberDir = "d" + ((centile*100)+1) + "-" + (centile+1)*100;
		fileName.append(File.separator);
		fileName.append(numberDir);
		
		fileName.append(File.separator);
		fileName.append("s" + studyID);
	
		return fileName.toString();
	}
	
	private void writeQueryFile(
		final ZipOutputStream submissionZipOutputStream,
		final User user,
		final String baseStudyName,
		final RIFStudySubmission rifStudySubmission)
		throws Exception {
		
		XMLCommentInjector commentInjector = new XMLCommentInjector();
		RIFStudySubmissionContentHandler rifStudySubmissionContentHandler
			= new RIFStudySubmissionContentHandler();
		rifStudySubmissionContentHandler.initialise(
			submissionZipOutputStream, 
			commentInjector);
	
		//KLG @TODO.  Right now we have only 
		
		//write the query file to a special directory.
		//this folder should only contain one file
		StringBuilder queryFileName = new StringBuilder();
		queryFileName.append(STUDY_QUERY_SUBDIRECTORY);
		queryFileName.append(File.separator);
		queryFileName.append(baseStudyName);
		queryFileName.append("_query.xml");
		
		ZipEntry rifQueryFileNameZipEntry = new ZipEntry(queryFileName.toString());
		submissionZipOutputStream.putNextEntry(rifQueryFileNameZipEntry);
		rifStudySubmissionContentHandler.writeXML(
			user, 
			rifStudySubmission);
		submissionZipOutputStream.closeEntry();

		rifLogger.info(this.getClass(), "Add to ZIP file: " + queryFileName);		
	}
	
	private void addRFiles(
			final File startDirectory,
			final ZipOutputStream submissionZipOutputStream,
			final String relativePath)
					throws Exception {
						
		rifLogger.debug(this.getClass(), "Adding R files start directory: " + startDirectory.getAbsolutePath() + lineSeparator + 
			"; relativePath: " + relativePath);
		File[] listOfFiles = startDirectory.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {	
		
			if (listOfFiles[i].isFile()) {
				rifLogger.debug(this.getClass(), "Adding R file: " + startDirectory.getAbsolutePath() + File.separator + 
					listOfFiles[i].getName() + " to ZIP file" + lineSeparator + "; relativePath: " + relativePath);
				
				File file=new File(startDirectory.getAbsolutePath() + File.separator + listOfFiles[i].getName());
				ZipEntry zipEntry = null;
				if (relativePath != null) {
					zipEntry = new ZipEntry(relativePath + File.separator + listOfFiles[i].getName());
				}
				else {
					zipEntry = new ZipEntry(listOfFiles[i].getName());
				}
				submissionZipOutputStream.putNextEntry(zipEntry);

				FileInputStream fileInputStream  = new FileInputStream(file);
				byte[] buffer = new byte[4092];
				int byteCount = 0;
				while ((byteCount = fileInputStream.read(buffer)) != -1) {
					submissionZipOutputStream.write(buffer, 0, byteCount);
				}

				fileInputStream.close();
				submissionZipOutputStream.closeEntry();
			}
			else if (listOfFiles[i].isDirectory()) {
				rifLogger.debug(this.getClass(), "Adding R directory: " + startDirectory.getAbsolutePath() + File.separator + 
					listOfFiles[i].getName() + File.separator + " to ZIP file" + 
					lineSeparator + "; relativePath: " + relativePath);
					/*
				if (relativePath != null) {
					submissionZipOutputStream.putNextEntry(
						new ZipEntry(relativePath + File.separator + listOfFiles[i].getName() + File.separator));
				}
				else {
					submissionZipOutputStream.putNextEntry(
						new ZipEntry(listOfFiles[i].getName() + File.separator));
				} */
				
				if (relativePath == null) {
					addRFiles(listOfFiles[i], submissionZipOutputStream, 
						listOfFiles[i].getName()); // Recurse!!
				}
				else {
					addRFiles(listOfFiles[i], submissionZipOutputStream, 
						relativePath + File.separator + listOfFiles[i].getName()); // Recurse!!
				}
			}
			else {
				rifLogger.debug(this.getClass(), "Ignoring R file: " + startDirectory.getAbsolutePath() + File.separator + 
					listOfFiles[i].getName());
			}
    	}
	}
	
	private void writeGeographyFiles(
			final Connection connection,
			final String temporaryDirectoryPath,
			final ZipOutputStream submissionZipOutputStream,
			final String baseStudyName,
			final String zoomLevel,
			final RIFStudySubmission rifStudySubmission)
					throws Exception {
		
		String studyID = rifStudySubmission.getStudyID();
	
		//Add geographies to zip file
		StringBuilder tileTableName = new StringBuilder();	
		tileTableName.append("rif_data.geometry_");
		String geog = rifStudySubmission.getStudy().getGeography().getName();			
		tileTableName.append(geog);
						
		StringBuilder tileFilePath = new StringBuilder();
		tileFilePath.append(GEOGRAPHY_SUBDIRECTORY);
		tileFilePath.append(File.separator);
		tileFilePath.append(baseStudyName);
		
		//Write study area
		StringBuilder tileFileName = null;
		tileFileName = new StringBuilder();
		tileFileName.append(tileFilePath.toString());
		tileFileName.append("_studyArea");
		tileFileName.append(".txt");
		
		writeMapQueryTogeoJSONFile(
				connection,
				submissionZipOutputStream,
				"rif40_study_areas",
				tileTableName.toString(),
				tileFileName.toString(),
				zoomLevel,
				studyID);
		
		//Write comparison area
		tileFileName = new StringBuilder();
		tileFileName.append(tileFilePath.toString());
		tileFileName.append("_comparisonArea");
		tileFileName.append(".txt");
		
		writeMapQueryTogeoJSONFile(
				connection,
				submissionZipOutputStream,
				"rif40_comparison_areas",
				tileTableName.toString(),
				tileFileName.toString(),
				zoomLevel,
				studyID);
		rifLogger.info(this.getClass(), "Add to ZIP file: " + tileFileName);
	}		
	
						
	private void writeMapQueryTogeoJSONFile(
			final Connection connection,
			final ZipOutputStream submissionZipOutputStream,	
			final String areaTableName,
			final String tableName,
			final String outputFilePath,
			final String zoomLevel,
			final String studyID)
					throws Exception {
		
		//Type of area
		String type = "S";
		
		//get geolevel
		SQLGeneralQueryFormatter geolevelQueryFormatter = new SQLGeneralQueryFormatter();	
		geolevelQueryFormatter.addQueryLine(0, "SELECT b.geolevel_id");
		geolevelQueryFormatter.addQueryLine(0, "FROM rif40.rif40_studies a, rif40.rif40_geolevels b");
		geolevelQueryFormatter.addQueryLine(0, "WHERE study_id = ?");
		if (areaTableName.equals("rif40_comparison_areas")) {
			geolevelQueryFormatter.addQueryLine(0, "AND a.comparison_geolevel_name = b.geolevel_name");
			type = "C";
		} else {
			geolevelQueryFormatter.addQueryLine(0, "AND a.study_geolevel_name = b.geolevel_name");
		}
	
		//count areas
		SQLGeneralQueryFormatter countQueryFormatter = new SQLGeneralQueryFormatter();
		countQueryFormatter.addQueryLine(0, "SELECT count(area_id) from rif40." + areaTableName + " where study_id = ?");
		
		//TODO: possible issues with Multi-polygon and point arrays
		SQLGeneralQueryFormatter queryFormatter = new SQLGeneralQueryFormatter();
		queryFormatter.addQueryLine(0, "SELECT b.areaid, b.zoomlevel, b.wkt from (select area_id from rif40." + areaTableName + " where study_id = ?) a");
		queryFormatter.addQueryLine(0, "left join " + tableName + " b ");
		queryFormatter.addQueryLine(0, "on a.area_id = b.areaid");
		queryFormatter.addQueryLine(0, "WHERE geolevel_id = ? AND zoomlevel = ?");
		
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(submissionZipOutputStream);
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
		
		PreparedStatement geolevelStatement = createPreparedStatement(connection, geolevelQueryFormatter);		
		ResultSet geolevelResultSet = null;
		PreparedStatement countStatement = createPreparedStatement(connection, countQueryFormatter);		
		ResultSet countResultSet = null;
		PreparedStatement statement = createPreparedStatement(connection, queryFormatter);		
		ResultSet resultSet = null;
		
		try {
			geolevelStatement = createPreparedStatement(connection, geolevelQueryFormatter);
			geolevelStatement.setInt(1, Integer.parseInt(studyID));	
			geolevelResultSet = geolevelStatement.executeQuery();
			geolevelResultSet.next();
			Integer geolevel = geolevelResultSet.getInt(1);
			
			
			countStatement = createPreparedStatement(connection, countQueryFormatter);
			countStatement.setInt(1, Integer.parseInt(studyID));	
			countResultSet = countStatement.executeQuery();
			countResultSet.next();
			int rows = countResultSet.getInt(1);

			statement = createPreparedStatement(connection, queryFormatter);
			statement.setInt(1, Integer.parseInt(studyID));	
			statement.setInt(2, geolevel);
			statement.setInt(3, Integer.parseInt(zoomLevel));
						
			resultSet = statement.executeQuery();

			ZipEntry zipEntry = new ZipEntry(outputFilePath);
			submissionZipOutputStream.putNextEntry(zipEntry);
			
			//Write WKT to geoJSON
			int i = 0;
			bufferedWriter.write("{ \"type\": \"FeatureCollection\", \"features\": [\r\n");	
			while (resultSet.next()) {
				bufferedWriter.write("{ \"type\": \"Feature\",\r\n");
				bufferedWriter.write("\"geometry\": {\r\n\"type\": \"Polygon\",\r\n\"coordinates\": [");
				bufferedWriter.write("[\r\n");
				//Full wkt string
				String polygon = resultSet.getString(3);				
				//trim head and tail
				polygon = polygon.replaceAll("MULTIPOLYGON", "");
				polygon = polygon.replaceAll("[()]", "");				
				//get coordinate pairs
				String[] coords = polygon.split(",");
				for (Integer j = 0; j < coords.length; j++) {
					String node = coords[j].replaceFirst(" ", ",");
					bufferedWriter.write("[" + node + "]");		
					if (j != coords.length - 1) {
						bufferedWriter.write(",");	
					}
				}				
				//get properties
				bufferedWriter.write("]\r\n");					
				bufferedWriter.write("]},\r\n\"properties\": {\r\n");
				bufferedWriter.write("\"area_id\": \"" + resultSet.getString(1) + "\",\r\n");
				bufferedWriter.write("\"zoomLevel\": \"" + resultSet.getString(2) + "\",\r\n");
				bufferedWriter.write("\"areatype\": \"" + type + "\"\r\n");
				bufferedWriter.write("}\r\n");
				bufferedWriter.write("}");
				if (i != rows) {
					bufferedWriter.write(","); 
				}
				bufferedWriter.write("\r\n");
				i++;
			}
			
			bufferedWriter.write("]\r\n");
			bufferedWriter.write("}");

			bufferedWriter.flush();
			submissionZipOutputStream.closeEntry();

			connection.commit();
		}
		finally {
			SQLQueryUtility.close(statement);
			SQLQueryUtility.close(countStatement);
			SQLQueryUtility.close(geolevelStatement);
		}
	}
	
	public String getRif40StudyState(
			final Connection connection,
			final String studyID)
					throws Exception {
						
		//get study_state
		SQLGeneralQueryFormatter studyStatusQueryFormatter = new SQLGeneralQueryFormatter();	
		studyStatusQueryFormatter.addQueryLine(0, "SELECT a.study_state");
		studyStatusQueryFormatter.addQueryLine(0, "FROM rif40.rif40_studies a");
		studyStatusQueryFormatter.addQueryLine(0, "WHERE a.study_id = ?");
					
		ResultSet studyStatusResultSet = null;
		String studyStatus = null;
		
		try {
			logSQLQuery("getRif40StudyState", studyStatusQueryFormatter, studyID);
			PreparedStatement studyStatusStatement = createPreparedStatement(connection, studyStatusQueryFormatter);
			studyStatusStatement.setInt(1, Integer.parseInt(studyID));	
			studyStatusResultSet = studyStatusStatement.executeQuery();
			studyStatusResultSet.next();
			studyStatus = studyStatusResultSet.getString(1);
		}
		catch (Exception exception) {
			rifLogger.error(this.getClass(), "Error in SQL Statement: >>> " + lineSeparator + studyStatusQueryFormatter.generateQuery(),
				exception);
			throw exception;
		}
		return studyStatus;

	}
		
}