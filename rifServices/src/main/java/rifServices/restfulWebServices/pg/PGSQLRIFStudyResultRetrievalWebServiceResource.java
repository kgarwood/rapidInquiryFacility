package rifServices.restfulWebServices.pg;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import rifGenericLibrary.businessConceptLayer.RIFResultTable;
import rifGenericLibrary.businessConceptLayer.User;
import rifGenericLibrary.util.RIFLogger;
import rifServices.businessConceptLayer.RIFStudyResultRetrievalAPI;
import rifServices.businessConceptLayer.Sex;
import rifServices.restfulWebServices.AbstractWebServiceResource;
import rifServices.restfulWebServices.RIFResultTableJSONGenerator;
import rifServices.restfulWebServices.SexesProxy;
import rifServices.restfulWebServices.WebServiceResponseGenerator;

/**
 * This class advertises API methods found in 
 * {@link rifServices.businessConceptLayer.RIFJobSubmissionAPI}
 * as a web service.  
 * 
 * Two issues have dominated the design of this class:
 * <ul>
 * <li>
 * the slight mismatch between URL parameter values and corresponding instances of Java
 * objects
 * </li>
 * <li>
 * the level of granularity in the conversations we would expect the web service to have
 * with the client
 * </li>
 * <li>
 * The efficiency with which 
 * </ul>
 * 
 * <p>
 * 
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

@Path("/")
public class PGSQLRIFStudyResultRetrievalWebServiceResource extends AbstractWebServiceResource {

	// ==========================================
	// Section Constants
	// ==========================================
	private static final RIFLogger rifLogger = RIFLogger.getLogger();

	// ==========================================
	// Section Properties
	// ==========================================

	// ==========================================
	// Section Construction
	// ==========================================

	public PGSQLRIFStudyResultRetrievalWebServiceResource() {

	}

	// ==========================================
	// Section Accessors and Mutators
	// ==========================================

	//KLG: 

	@GET
	@Produces({"application/json"})	
	@Path("/getCurrentStatusAllStudies")
	public Response getCurrentStatusAllStudies(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID) {

		String result = "";
		try {
			//Convert URL parameters to RIF service API parameters
			User user = createUser(servletRequest, userID);


			//Call service API
			RIFStudyResultRetrievalAPI studyResultRetrievalService
			= getRIFStudyResultRetrievalService();
			RIFResultTable resultTable
			= studyResultRetrievalService.getCurrentStatusAllStudies(user);
			RIFResultTableJSONGenerator rifResultTableJSONGenerator
			= new RIFResultTableJSONGenerator();
			result = rifResultTableJSONGenerator.writeResultTable(resultTable);			
		}
		catch(Exception exception) {		
			rifLogger.error(
				this.getClass(), 
				"GET /getCurrentStatusAllStudies method failed: ", 
				exception);	
			//Convert exceptions to support JSON
			result 
			= serialiseException(
					servletRequest,
					exception);			
		}

		WebServiceResponseGenerator webServiceResponseGenerator
		= getWebServiceResponseGenerator();
		return webServiceResponseGenerator.generateWebServiceResponse(
				servletRequest,
				result);
	}

	@GET
	@Produces({"application/json"})	
	@Path("/getGeographies")
	public Response getGeographies(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID) {

		return 
				super.getGeographies(
						servletRequest,
						userID);
	}

	@GET
	@Produces({"application/json"})	
	@Path("/getGeoLevelSelectValues")
	public Response getGeographicalLevelSelectValues(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("geographyName") String geographyName) {

		return super.getGeographicalLevelSelectValues(
				servletRequest,
				userID, 
				geographyName);
	}	


	@GET
	@Produces({"application/json"})	
	@Path("/getDefaultGeoLevelSelectValue")
	public Response getDefaultGeoLevelSelectValue(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("geographyName") String geographyName) {

		return super.getDefaultGeoLevelSelectValue(
				servletRequest,
				userID,
				geographyName);
	}	

	@GET
	@Produces({"application/json"})	
	@Path("/getGeoLevelAreaValues")
	public Response getGeoLevelAreaValues(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("geographyName") String geographyName,
			@QueryParam("geoLevelSelectName") String geoLevelSelectName) {

		return super.getGeoLevelAreaValues(
				servletRequest,
				userID, 
				geographyName, 
				geoLevelSelectName);
	}	

	@GET
	@Produces({"application/json"})	
	@Path("/getGeoLevelViews")
	public Response getGeoLevelViewValues(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("geographyName") String geographyName,
			@QueryParam("geoLevelSelectName") String geoLevelSelectName) {

		return super.getGeoLevelViewValues(
				servletRequest,
				userID, 
				geographyName, 
				geoLevelSelectName);
	}

	@GET
	@Produces({"application/json"})	
	@Path("/getNumerator")
	public Response getNumerator(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("geographyName") String geographyName,		
			@QueryParam("healthThemeDescription") String healthThemeDescription) {


		return super.getNumerator(
				servletRequest,
				userID,
				geographyName,
				healthThemeDescription);
	}	

	@GET
	@Produces({"application/json"})	
	@Path("/getDenominator")
	public Response getDenominator(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("geographyName") String geographyName,		
			@QueryParam("healthThemeDescription") String healthThemeDescription) {

		return super.getDenominator(
				servletRequest,
				userID,
				geographyName,
				healthThemeDescription);
	}

	@GET
	@Produces({"application/json"})	
	@Path("/getYearRange")
	public Response getYearRange(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("geographyName") String geographyName,	
			@QueryParam("numeratorTableName") String numeratorTableName) {

		return super.getYearRange(
				servletRequest,
				userID, 
				geographyName, 
				numeratorTableName);		
	}


	/**
	 * @param userID
	 * @param study_id
	 * @return
	 */
	@GET
	@Produces({"application/json"})	
	@Path("/getYearsForStudy")
	public Response getYearsForStudy(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("study_id") String studyID) {

		String result = "";
		try {
			//Convert URL parameters to RIF service API parameters
			User user = createUser(servletRequest, userID);

			//Call service API
			RIFStudyResultRetrievalAPI studyResultRetrievalService
			= getRIFStudyResultRetrievalService();
			ArrayList<Integer> years
			= studyResultRetrievalService.getYearsForStudy(
					user, 
					studyID);
			ArrayList<String> yearsAsStrings = new ArrayList<String>();
			for (Integer year : years) {
				yearsAsStrings.add(String.valueOf(year));
			}

			result 
			= serialiseNamedArray("years", yearsAsStrings);
		}
		catch(Exception exception) {
			rifLogger.error(
				this.getClass(), 
				"GET /getYearsForStudy method failed: ", 
				exception);	
			//Convert exceptions to support JSON
			result 
			= serialiseException(
					servletRequest,
					exception);			
		}

		WebServiceResponseGenerator webServiceResponseGenerator
		= getWebServiceResponseGenerator();
		return webServiceResponseGenerator.generateWebServiceResponse(
				servletRequest,
				result);		
	}	


	/**
	 * @param userID
	 * @param study_id
	 * @return
	 */
	@GET
	@Produces({"application/json"})	
	@Path("/getSexesForStudy")
	public Response getSexesForStudy(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("study_id") String studyID) {

		String result = "";
		try {
			//Convert URL parameters to RIF service API parameters
			User user = createUser(servletRequest, userID);

			//Call service API
			RIFStudyResultRetrievalAPI studyResultRetrievalService
			= getRIFStudyResultRetrievalService();

			ArrayList<Sex> sexes
			= studyResultRetrievalService.getSexesForStudy(
					user, 
					studyID);
			String[] sexNames = new String[sexes.size()];
			for (int i = 0; i < sexNames.length; i++) {
				sexNames[i] = sexes.get(i).getName();
			}

			SexesProxy sexesProxy = new SexesProxy();
			sexesProxy.setNames(sexNames);

			result 
			= serialiseSingleItemAsArrayResult(
					servletRequest,
					sexesProxy);
		}
		catch(Exception exception) {
			rifLogger.error(
				this.getClass(), 
				"GET /getSexesForStudy method failed: ", 
				exception);	
			//Convert exceptions to support JSON
			result 
			= serialiseException(
					servletRequest,
					exception);			
		}

		WebServiceResponseGenerator webServiceResponseGenerator
		= getWebServiceResponseGenerator();
		return webServiceResponseGenerator.generateWebServiceResponse(
				servletRequest,
				result);		
	}	


	/**
	 * @param userID
	 * @param geographyName
	 * @param geoLevelSelectName
	 * @param geoLevelToMapName
	 * @param mapAreaValues
	 * @return
	 */
	@GET
	@Produces({"application/json"})	
	@Path("/getSmoothedResults")
	public String getSmoothedResults(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("studyID") String studyID,
			@QueryParam("sex") String sex) {

		String result = "";
		try {
			//Convert URL parameters to RIF service API parameters
			User user = createUser(servletRequest, userID);

			//Call service API
			RIFStudyResultRetrievalAPI studyResultRetrievalService
			= getRIFStudyResultRetrievalService();
			RIFResultTable resultTable
			= studyResultRetrievalService.getSmoothedResults(
					user, 
					studyID,
					sex);

			RIFResultTableJSONGenerator rifResultTableJSONGenerator
			= new RIFResultTableJSONGenerator();
			result = rifResultTableJSONGenerator.writeResultTable(resultTable);

		}
		catch(Exception exception) {
			rifLogger.error(
				this.getClass(), 
				"GET /getSmoothedResults method failed: ", 
				exception);	
			//Convert exceptions to support JSON
			result 
			= serialiseException(
					servletRequest,
					exception);			
		}

		return result;
	}	

	@GET
	@Produces({"application/json"})	
	@Path("/getAllPopulationPyramidData")
	public String getAllPopulationPyramidData(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("studyID") String studyID,
			@QueryParam("year") String year) {

		String result = "";
		try {
			//Convert URL parameters to RIF service API parameters
			User user = createUser(servletRequest, userID);

			//Call service API
			RIFStudyResultRetrievalAPI studyResultRetrievalService
			= getRIFStudyResultRetrievalService();
			RIFResultTable resultTable
			= studyResultRetrievalService.getPopulationPyramidData(
					user, 
					studyID,
					year);

			RIFResultTableJSONGenerator rifResultTableJSONGenerator
			= new RIFResultTableJSONGenerator();
			result = rifResultTableJSONGenerator.writeResultTable(resultTable);				
		}
		catch(Exception exception) {
			rifLogger.error(
				this.getClass(), 
				"GET /getAllPopulationPyramidData method failed: ", 
				exception);	
			//Convert exceptions to support JSON
			result 
			= serialiseException(
					servletRequest,
					exception);			
		}

		return result;
	}	

	@GET
	@Produces({"application/json"})	
	@Path("/getTileMakerTiles")
	public Response getTileMakerTiles(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("geographyName") String geographyName, //SAHSU
			@QueryParam("geoLevelSelectName") String geoLevelSelectName, //LEVEL2
			@QueryParam("zoomlevel") Integer zoomlevel,	//3	
			@QueryParam("x") Integer x, //3
			@QueryParam("y") Integer y) { //2

		return super.getTileMakerTiles(
				servletRequest, 
				userID, 
				geographyName, 
				geoLevelSelectName, 
				zoomlevel, 
				x, 
				y);	
	}

	@GET
	@Produces({"application/json"})	
	@Path("/getTileMakerCentroids")
	public Response getTileMakerCentroids(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("geographyName") String geographyName, //SAHSU
			@QueryParam("geoLevelSelectName") String geoLevelSelectName ) { //LEVEL2

		return super.getTileMakerCentroids(
				servletRequest, 
				userID, 
				geographyName, 
				geoLevelSelectName);	
	}

	@GET	
	@Produces({"application/json"})	
	@Path("/getGeographyAndLevelForStudy")
	public String getGeographyAndLevelForStudy(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("studyID") String studyID) {

		String result = "";

		try {
			//Convert URL parameters to RIF service API parameters			
			User user = createUser(servletRequest, userID);

			//Call service API
			RIFStudyResultRetrievalAPI studyResultRetrievalService
			= getRIFStudyResultRetrievalService();
			String[] results
			= studyResultRetrievalService.getGeographyAndLevelForStudy(user, studyID);

			//Convert results to support JSON
			result 
			= serialiseSingleItemAsArrayResult(
					servletRequest,
					results);
		}
		catch(Exception exception) {
			rifLogger.error(
				this.getClass(), 
				"GET /getGeographyAndLevelForStudy method failed: ", 
				exception);	
			//Convert exceptions to support JSON
			result 
			= serialiseException(
					servletRequest,
					exception);			
		}

		return result;

	}	

	@GET	
	@Produces({"application/json"})	
	@Path("/getDetailsForProcessedStudy")
	public String getDetailsForProcessedStudy(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("studyID") String studyID) {

		String result = "";

		try {
			//Convert URL parameters to RIF service API parameters			
			User user = createUser(servletRequest, userID);

			//Call service API
			RIFStudyResultRetrievalAPI studyResultRetrievalService
			= getRIFStudyResultRetrievalService();
			String[] results
			= studyResultRetrievalService.getDetailsForProcessedStudy(user, studyID);

			//Convert results to support JSON
			result 
			= serialiseSingleItemAsArrayResult(
					servletRequest,
					results);
		}
		catch(Exception exception) {
			rifLogger.error(
				this.getClass(), 
				"GET /getDetailsForProcessedStudy method failed: ", 
				exception);
			//Convert exceptions to support JSON
			result 
			= serialiseException(
					servletRequest,
					exception);			
		}

		return result;

	}	

	@GET	
	@Produces({"application/json"})	
	@Path("/getStudyTableForProcessedStudy")
	public String getStudyTableForProcessedStudy(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("studyID") String studyID,
			@QueryParam("type") String type,
			@QueryParam("stt") String stt,
			@QueryParam("stp") String stp) {

		String result = "";

		try {
			//Convert URL parameters to RIF service API parameters			
			User user = createUser(servletRequest, userID);

			//Call service API
			RIFStudyResultRetrievalAPI studyResultRetrievalService
			= getRIFStudyResultRetrievalService();

			RIFResultTable rifResultTable
			= studyResultRetrievalService.getStudyTableForProcessedStudy(user, studyID, type, stt, stp);

			//Convert results to support JSON
			result 
			= serialiseSingleItemAsArrayResult(
					servletRequest,
					rifResultTable);	
		}
		catch(Exception exception) {
			rifLogger.error(
				this.getClass(), 
				"GET /getStudyTableForProcessedStudy method failed: ", 
				exception);
			//Convert exceptions to support JSON
			result 
			= serialiseException(
					servletRequest,
					exception);			
		}

		return result;

	}	

	@GET	
	@Produces({"application/json"})	
	@Path("/getHealthCodesForProcessedStudy")
	public String getHealthCodesForProcessedStudy(
			@Context HttpServletRequest servletRequest,	
			@QueryParam("userID") String userID,
			@QueryParam("studyID") String studyID) {

		String result = "";

		try {
			//Convert URL parameters to RIF service API parameters			
			User user = createUser(servletRequest, userID);

			//Call service API
			RIFStudyResultRetrievalAPI studyResultRetrievalService
			= getRIFStudyResultRetrievalService();
			String[] results
			= studyResultRetrievalService.getHealthCodesForProcessedStudy(user, studyID);

			//Convert results to support JSON
			result 
			= serialiseSingleItemAsArrayResult(
					servletRequest,
					results);
		}
		catch(Exception exception) {
			rifLogger.error(
				this.getClass(), 
				"GET /getHealthCodesForProcessedStudy method failed: ", 
				exception);
			//Convert exceptions to support JSON
			result 
			= serialiseException(
					servletRequest,
					exception);			
		}

		return result;

	}	

	// ==========================================
	// Section Interfaces
	// ==========================================

	// ==========================================
	// Section Override
	// ==========================================
}
