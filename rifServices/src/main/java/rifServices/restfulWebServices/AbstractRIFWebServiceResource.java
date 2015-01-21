package rifServices.restfulWebServices;

import java.io.ByteArrayOutputStream;



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.http.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;

import rifServices.dataStorageLayer.ProductionRIFStudyServiceBundle;
import rifServices.dataStorageLayer.RIFStudyResultRetrievalAPI;
import rifServices.dataStorageLayer.RIFStudySubmissionAPI;
import rifServices.system.RIFServiceException;
import rifServices.system.RIFServiceMessages;
import rifServices.system.RIFServiceStartupOptions;
import rifServices.businessConceptLayer.GeoLevelArea;
import rifServices.businessConceptLayer.GeoLevelSelect;
import rifServices.businessConceptLayer.GeoLevelView;
import rifServices.businessConceptLayer.Geography;
import rifServices.businessConceptLayer.HealthTheme;
import rifServices.businessConceptLayer.NumeratorDenominatorPair;
import rifServices.businessConceptLayer.User;
import rifServices.businessConceptLayer.YearRange;

/**
 * This is a web service class that is analoguous to  
 * to {@link rifServices.dataStorageLayer.AbstractRIFService}. Its purpose is
 * to wrap API methods that are common to both {@link rifServices.dataStorageLayer.RIFStudySubmissionAPI}
 * and {@link rifServices.dataStorageLayer.RIFStudyResultRetrievalAPI}.
 * 
 * <hr>
 * The Rapid Inquiry Facility (RIF) is an automated tool devised by SAHSU 
 * that rapidly addresses epidemiological and public health questions using 
 * routinely collected health and population data and generates standardised 
 * rates and relative risks for any given health outcome, for specified age 
 * and year ranges, for any given geographical area.
 *
 * Copyright 2014 Imperial College London, developed by the Small Area
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

abstract class AbstractRIFWebServiceResource {

	// ==========================================
	// Section Constants
	// ==========================================

	// ==========================================
	// Section Properties
	// ==========================================
	private static final ProductionRIFStudyServiceBundle rifStudyServiceBundle 
		= ProductionRIFStudyServiceBundle.getRIFServiceBundle();
	private SimpleDateFormat sd;
	private Date startTime;
	
	// ==========================================
	// Section Construction
	// ==========================================

	public AbstractRIFWebServiceResource() {

		startTime = new Date();
		sd = new SimpleDateFormat("HH:mm:ss:SSS");

		RIFServiceStartupOptions rifServiceStartupOptions
			= new RIFServiceStartupOptions(true);
		
		try {
			rifStudyServiceBundle.initialise(rifServiceStartupOptions);
			//rifStudyServiceBundle.login("ffabbri", new String("ffabbri").toCharArray());
			//rifStudyServiceBundle.login("kgarwood", new String("kgarwood").toCharArray());
		}
		catch(RIFServiceException exception) {
			exception.printStackTrace(System.out);
		}
	}

	protected Response isLoggedIn(
		final HttpServletRequest servletRequest,
		final String userID) {
		
		String result = "";
		try {			
			result = String.valueOf(rifStudyServiceBundle.isLoggedIn(userID));
		}
		catch(Exception exception) {
			exception.printStackTrace(System.out);
			//Convert exceptions to support JSON
			result 
				= serialiseException(
					servletRequest,
					exception);			
		}
	
		return generateAppropriateContentTypeResponse(
			servletRequest,
			result);
	}
	
	protected Response login(
		final HttpServletRequest servletRequest,
		final String userID,
		final String password) {

		String result = "";
		try {			
			rifStudyServiceBundle.login(userID, password);
			result
				= RIFServiceMessages.getMessage("general.login.success", userID);
		}
		catch(Exception exception) {
			exception.printStackTrace(System.out);
			//Convert exceptions to support JSON
			result 
				= serialiseException(
					servletRequest,
					exception);			
		}
	
		return generateAppropriateContentTypeResponse(
			servletRequest,
			result);
	}
	
	protected Response logout(
		final HttpServletRequest servletRequest,
		final String userID) {
		
		String result = "";
		try {
			//Convert URL parameters to RIF service API parameters
			User user = User.newInstance(userID, "xxx");
			rifStudyServiceBundle.logout(user);
			result = RIFServiceMessages.getMessage("general.logout.success", userID);
		}
		catch(Exception exception) {
			//Convert exceptions to support JSON
			result 
				= serialiseException(
					servletRequest,
					exception);			
		}
	

		return generateAppropriateContentTypeResponse(
			servletRequest,
			result);
	}
	
	// ==========================================
	// Section Accessors and Mutators
	// ==========================================
	protected RIFStudySubmissionAPI getRIFStudySubmissionService() {
		return rifStudyServiceBundle.getRIFStudySubmissionService();
	}
	
	protected RIFStudyResultRetrievalAPI getRIFStudyResultRetrievalService() {
		return rifStudyServiceBundle.getRIFStudyRetrievalService();
	}
	
	
	protected Response getGeographies(
		final HttpServletRequest servletRequest,
		final String userID) {
			
		String result = "";
		
		try {
			//Convert URL parameters to RIF service API parameters
			User user = User.newInstance(userID, "xxx");
			
			//Call service API
			RIFStudySubmissionAPI studySubmissionService
				= rifStudyServiceBundle.getRIFStudySubmissionService();
			
			ArrayList<Geography> geographies
				= studySubmissionService.getGeographies(user);

			if (geographies != null) {
				ArrayList<String> geographyNames = new ArrayList<String>();			
				for (Geography geography : geographies) {
					geographyNames.add(geography.getName());
				}
				GeographiesProxy geographiesProxy = new GeographiesProxy();		
				geographiesProxy.setNames(geographyNames.toArray(new String[0]));
				result 
					= serialiseSingleItemAsArrayResult(
						servletRequest,
						geographiesProxy);
			}			
		}
		catch(Exception exception) {
			exception.printStackTrace(System.out);
			//Convert exceptions to support JSON
			result 
				= serialiseException(
					servletRequest,
					exception);			
		}

		return generateAppropriateContentTypeResponse(
			servletRequest,
			result);		
	}

	protected Response getGeographicalLevelSelectValues(
		final HttpServletRequest servletRequest,
		final String userID,
		final String geographyName) {
			
		String result = "";
	
		try {
			//Convert URL parameters to RIF service API parameters
			User user = User.newInstance(userID, "xxxxxxxx");
			Geography geography = Geography.newInstance(geographyName, "xxx");

			//Call service API
			RIFStudySubmissionAPI studySubmissionService
				= rifStudyServiceBundle.getRIFStudySubmissionService();			
			ArrayList<GeoLevelSelect> geoLevelSelects
				= studySubmissionService.getGeoLevelSelectValues(
					user, 
					geography);

			//Convert results to support JSON			
			ArrayList<String> geoLevelSelectNames = new ArrayList<String>();			
			for (GeoLevelSelect geoLevelSelect : geoLevelSelects) {
				geoLevelSelectNames.add(geoLevelSelect.getName());
			}
			GeoLevelSelectsProxy geoLevelSelectProxy
				= new GeoLevelSelectsProxy();		
			geoLevelSelectProxy.setNames(geoLevelSelectNames.toArray(new String[0]));
			result 
				= serialiseSingleItemAsArrayResult(
					servletRequest, 
					geoLevelSelectProxy);
		}
		catch(Exception exception) {
			//Convert exceptions to support JSON
			result 
				= serialiseException(
					servletRequest,
					exception);			
		}
	
		
		return generateAppropriateContentTypeResponse(
			servletRequest,
			result);		
	}
		
	protected Response getDefaultGeoLevelSelectValue(
		final HttpServletRequest servletRequest,
		final String userID,
		final String geographyName) {
			
		String result = "";
		
		GeoLevelSelectsProxy geoLevelSelectProxy
			= new GeoLevelSelectsProxy();
	
		try {
			//Convert URL parameters to RIF service API parameters
			User user = User.newInstance(userID, "xxxxxxxx");
			Geography geography = Geography.newInstance(geographyName, "xxx");

			//Call service API
			RIFStudySubmissionAPI studySubmissionService
				= rifStudyServiceBundle.getRIFStudySubmissionService();
			GeoLevelSelect defaultGeoLevelSelect
				= studySubmissionService.getDefaultGeoLevelSelectValue(
					user, 
					geography);

			//Convert results to support JSON			
			String[] geoLevelSelectValues = new String[1];
			geoLevelSelectValues[0] = defaultGeoLevelSelect.getName();
			geoLevelSelectProxy.setNames(geoLevelSelectValues);
			result 
				= serialiseSingleItemAsArrayResult(
					servletRequest,
					geoLevelSelectProxy);
		}
		catch(Exception exception) {
			//Convert exceptions to support JSON
			result 
				= serialiseException(
					servletRequest, 
					exception);			
		}
	
		
		return generateAppropriateContentTypeResponse(
			servletRequest,
			result);		
	}

	protected Response getGeoLevelAreaValues(
		final HttpServletRequest servletRequest,
		final String userID,
		final String geographyName,
		final String geoLevelSelectName) {
						
		String result = "";
		
		GeoLevelAreasProxy geoLevelAreasProxy = new GeoLevelAreasProxy();
		
		try {
			//Convert URL parameters to RIF service API parameters
			User user = User.newInstance(userID, "xxx");
			Geography geography = Geography.newInstance(geographyName, "xxx");
			GeoLevelSelect geoLevelSelect
				= GeoLevelSelect.newInstance(geoLevelSelectName);

			//Call service API
			RIFStudySubmissionAPI studySubmissionService
				= rifStudyServiceBundle.getRIFStudySubmissionService();			
			ArrayList<GeoLevelArea> areas
				= studySubmissionService.getGeoLevelAreaValues(
					user, 
					geography, 
					geoLevelSelect);
			
			//Convert results to support JSON
			ArrayList<String> geoLevelAreaNames = new ArrayList<String>();
			for (GeoLevelArea area : areas) {
				geoLevelAreaNames.add(area.getName());
			}
			geoLevelAreasProxy.setNames(geoLevelAreaNames.toArray(new String[0]));
			result 
				= serialiseSingleItemAsArrayResult(
					servletRequest,
					geoLevelAreasProxy);
		}
		catch(Exception exception) {
			//Convert exceptions to support JSON
			result 
				= serialiseException(
					servletRequest,
					exception);			
		}
		
		
		return generateAppropriateContentTypeResponse(
			servletRequest,
			result);		
	}
	
	protected Response getGeoLevelViewValues(
		final HttpServletRequest servletRequest,
		final String userID,
		final String geographyName,
		final String geoLevelSelectName) {
				
		String result = "";
				
		try {
			//Convert URL parameters to RIF service API parameters
			User user = User.newInstance(userID, "xxxx");
			Geography geography = Geography.newInstance(geographyName, "");
			GeoLevelSelect geoLevelSelect
				= GeoLevelSelect.newInstance(geoLevelSelectName);

			//Call service API
			RIFStudySubmissionAPI studySubmissionService
				= rifStudyServiceBundle.getRIFStudySubmissionService();	
			ArrayList<GeoLevelView> geoLevelViews
				= studySubmissionService.getGeoLevelViewValues(
					user, 
					geography, 
					geoLevelSelect);
			
			//Convert results to support JSON
			GeoLevelViewsProxy geoLevelViewsProxy = new GeoLevelViewsProxy();			
			ArrayList<String> geoLevelViewNames = new ArrayList<String>();
			for (GeoLevelView geoLevelView : geoLevelViews) {
				geoLevelViewNames.add(geoLevelView.getName());
			}
			geoLevelViewsProxy.setNames(geoLevelViewNames.toArray(new String[0]));
			
			result 
				= serialiseSingleItemAsArrayResult(
					servletRequest,
					geoLevelViewsProxy);
		}
		catch(Exception exception) {
			//Convert exceptions to support JSON
			result 
				= serialiseException(
					servletRequest,
					exception);			
		}

		
		return generateAppropriateContentTypeResponse(
			servletRequest,
			result);		
	}	
	
	/**
	 * retrieves the numerator associated with a given health theme.
	 * @param userID
	 * @param geographyName
	 * @param healthThemeDescription
	 * @return
	 */
	
	protected Response getNumerator(
		final HttpServletRequest servletRequest,
		final String userID,
		final String geographyName,
		final String healthThemeDescription) {
		
		String result = "";
						
		try {
			//Convert URL parameters to RIF service API parameters
			User user 
				= User.newInstance(userID, "xxx");
			Geography geography 
				= Geography.newInstance(geographyName, "");
			HealthTheme healthTheme 
				= HealthTheme.newInstance("xxx", healthThemeDescription.trim());

			//Call service API
			RIFStudySubmissionAPI studySubmissionService
				= rifStudyServiceBundle.getRIFStudySubmissionService();				
			ArrayList<NumeratorDenominatorPair> ndPairs
				= studySubmissionService.getNumeratorDenominatorPairs(
					user, 
					geography, 
					healthTheme);

			//Convert results to support JSON
			ArrayList<NumeratorDenominatorPairProxy> ndPairProxies 
				= new ArrayList<NumeratorDenominatorPairProxy>();
			for (NumeratorDenominatorPair ndPair : ndPairs) {
				NumeratorDenominatorPairProxy ndPairProxy
					= new NumeratorDenominatorPairProxy();
				ndPairProxy.setNumeratorTableName(ndPair.getNumeratorTableName());
				ndPairProxy.setNumeratorTableDescription(ndPair.getNumeratorTableDescription());
				ndPairProxy.setDenominatorTableName(ndPair.getDenominatorTableName());
				ndPairProxy.setDenominatorTableDescription(ndPair.getDenominatorTableDescription());
				ndPairProxies.add(ndPairProxy);
			}			
			result 
				= serialiseArrayResult(
					servletRequest,
					ndPairProxies);
		}
		catch(Exception exception) {
			//Convert exceptions to support JSON
			result 
				= serialiseException(
					servletRequest,
					exception);			
		}
		
		return generateAppropriateContentTypeResponse(
			servletRequest,
			result);		
	}

	protected Response getDenominator(
		final HttpServletRequest servletRequest,
		final String userID,
		final String geographyName,
		final String healthThemeDescription) {
		
		String result = "";
				
		try {
			//Convert URL parameters to RIF service API parameters
			User user 	
				= User.newInstance(userID, "xxx");
			Geography geography 
				= Geography.newInstance(geographyName, "");
			HealthTheme healthTheme 
				= HealthTheme.newInstance("xxx", healthThemeDescription);

			//Call service API			
			RIFStudySubmissionAPI studySubmissionService
				= rifStudyServiceBundle.getRIFStudySubmissionService();				
			ArrayList<NumeratorDenominatorPair> ndPairs
				= studySubmissionService.getNumeratorDenominatorPairs(
					user, 
					geography, 
					healthTheme);
			
			//Convert results to support JSON

			//We should be guaranteed that at least one pair will be returned.
			//All the numerators returned should have the same denominator
			//Therefore, we should be able to pick the first ndPair and extract
			//the denominator.
			NumeratorDenominatorPair firstResult
				= ndPairs.get(0);
			NumeratorDenominatorPairProxy ndPairProxy
				= new NumeratorDenominatorPairProxy();
			ndPairProxy.setNumeratorTableName(firstResult.getNumeratorTableName());
			ndPairProxy.setNumeratorTableDescription(firstResult.getNumeratorTableDescription());
			ndPairProxy.setDenominatorTableName(firstResult.getDenominatorTableName());
			ndPairProxy.setDenominatorTableDescription(firstResult.getDenominatorTableDescription());							
			result 
				= serialiseSingleItemAsArrayResult(
					servletRequest,
					firstResult);
		}
		catch(Exception exception) {
			//Convert exceptions to support JSON
			result 
				= serialiseException(
					servletRequest,
					exception);			
		}
		
		return generateAppropriateContentTypeResponse(
			servletRequest,
			result);		
	}
	
	
	protected Response getYearRange(
		final HttpServletRequest servletRequest,
		final String userID,
		final String geographyName,
		final String numeratorTableName) {
			
		String result = "";
		
		try {
			//Convert URL parameters to RIF service API parameters
			User user = User.newInstance(userID, "xxx");
			Geography geography = Geography.newInstance(geographyName, "xxx");

			//Call service API
			RIFStudySubmissionAPI studySubmissionService
				= rifStudyServiceBundle.getRIFStudySubmissionService();			
			NumeratorDenominatorPair ndPair
				= studySubmissionService.getNumeratorDenominatorPairFromNumeratorTable(
					user, 
					geography, 
					numeratorTableName);			
			YearRange yearRange
				= studySubmissionService.getYearRange(user, geography, ndPair);
			
			//Convert results to support JSON
			YearRangeProxy yearRangeProxy = new YearRangeProxy();
			yearRangeProxy.setLowerBound(yearRange.getLowerBound());
			yearRangeProxy.setUpperBound(yearRange.getUpperBound());
			result 
				= serialiseSingleItemAsArrayResult(
					servletRequest,
					yearRangeProxy);
		}
		catch(Exception exception) {
			//Convert exceptions to support JSON			
			result 
				= serialiseException(
					servletRequest,
					exception);			
		}
		
		return generateAppropriateContentTypeResponse(
			servletRequest,
			result);
	}
	


	/**
	 * takes advantage of the Jackson project library to serialise objects
	 * for the JSON format.
	 * @param objectToWrite
	 * @return
	 * @throws Exception
	 */
	protected String serialiseArrayResult(
		final HttpServletRequest servletRequest,
		final Object objectToWrite) 
		throws Exception {

		printClientInformation("serialiseArrayResult", servletRequest);

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(out, objectToWrite);
		final byte[] data = out.toByteArray();
		return(new String(data));
	}
	
	protected String serialiseSingleItemAsArrayResult(
		final HttpServletRequest servletRequest,
		final Object objectToWrite) 
		throws Exception {

		printClientInformation("serialiseSingleItemAsArrayResult", servletRequest);
		
		final ArrayList<Object> objectArrayList = new ArrayList<Object>();
		objectArrayList.add(objectToWrite);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(out, objectArrayList);
		final byte[] data = out.toByteArray();
		
		return new String(data);
	}
	
	protected String serialiseStringResult(
		final HttpServletRequest servletRequest,
		final String result) 
		throws Exception {

		printClientInformation("serialiseStringResult", servletRequest);
		
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(out, result);
		final byte[] data = out.toByteArray();
		return(new String(data));
	}
	
	protected Response generateAppropriateContentTypeResponse(
		final HttpServletRequest servletRequest,
		final String data) {
		
		if (clientBrowserIsInternetExplorer(servletRequest)) {
			ResponseBuilder responseBuilder 
				= Response.ok(
					data, 
					MediaType.TEXT_PLAIN);
			return responseBuilder.build();			
		}
		else {
			ResponseBuilder responseBuilder 
				= Response.ok(
					data, 
					MediaType.APPLICATION_JSON);
			return responseBuilder.build();				
		}
	}

	
	// ==========================================
	// Section Errors and Validation
	// ==========================================

	protected String serialiseException(
		final HttpServletRequest servletRequest,
		final Exception exceptionThrownByRIFService) {
	
		printClientInformation("serialiseException", servletRequest);
		
		String result = "";
		try {			
			RIFServiceExceptionProxy rifServiceExceptionProxy
				= new RIFServiceExceptionProxy();
			if (exceptionThrownByRIFService instanceof RIFServiceException) {
				RIFServiceException rifServiceException
					= (RIFServiceException) exceptionThrownByRIFService;
				ArrayList<String> errorMessages
					= rifServiceException.getErrorMessages();
				rifServiceExceptionProxy.setErrorMessages(errorMessages.toArray(new String[0]));
			}
			else {
				/*
				 * We should never encounter this.  However, if we do, 
				 * then we should just indicate that an unexpected error has occurred.
				 * We may assume that the root cause of the error has been logged within
				 * the implementation of the service.
				 */
				String[] errorMessages = new String[1];
				String timeStamp = sd.format(new Date());
				errorMessages[0]
					= RIFServiceMessages.getMessage(
						"webServices.error.unexpectedError",
						timeStamp);
			
				rifServiceExceptionProxy.setErrorMessages(errorMessages);
			}
			result = serialiseSingleItemAsArrayResult(
				servletRequest, 
				rifServiceExceptionProxy);
		}
		catch(Exception exception) {
			//Convert exceptions to support JSON
			serialiseException(
				servletRequest,
				exception);
			String timeStamp = sd.format(new Date());
			result 
				= RIFServiceMessages.getMessage(
					"webServices.error.unableToProvideError",
					timeStamp);
		}
		
		return result;
	}

	/*
	 * Here we're trying to use some way of determining whether
	 * the browser will automatically display JSON.  This method is
	 * meant to use the 'feature detection' in the client browser.
	 * 
	 * In future this could be an extra parameter the client web 
	 * page stuffs into the request header after is uses something like
	 * Modernizr
	 * 
	 */
	private boolean clientBrowserSupportsJSONContentType(
		final HttpServletRequest servletRequest) {
		

		//@TODO
		return false;
	}
	
	/*
	 * Here we are interrogating the value of "user-agent" in the header.
	 * This approach would try to interrogate the free text field value
	 * to figure out the type and version of the browser client.  In future,
	 * it might use the WURFL project to do a look-up in a database of device
	 * profiles.
	 */
	private boolean clientBrowserIsInternetExplorer(
		final HttpServletRequest servletRequest) {
		
		//this approach is known to be weak but it will do for now until
		//we develop a more robust method
	
		//by default it's true
		boolean result = true;
		String browserType = servletRequest.getHeader("User-Agent");
		
		if (browserType != null) {
			browserType = browserType.toUpperCase();
			
			int foundIndex
				= browserType.indexOf("CHROME");
			if (foundIndex != -1) {
				result = false;
			}			
		}
		
		//we're going to make the incredibly lame assumption for now that if it
		//doesn't contain "Chrome", we'll assume it's Internet Explorer.
		//again, the question of what information do we use to determine if we
		//send JSON content type back or not needs to be discussed more
		System.out.println("isClientBrowserIE=="+result+"==");
		
		//System.out.println("clientBrowserIsInternetExplorer IS TRUE");
		//@TODO
		
		return result;
	}
	
	/*
	 * Here we are interrogating the value of "Accept" in the header
	 */
	private boolean clientBrowserMimeTypesIncludeJSON(
		final HttpServletRequest servletRequest) {
		
		
		return true;		
	}
	
	private void printClientInformation(
		final String messageHeader,
		final HttpServletRequest servletRequest) {
		
		String browserType = servletRequest.getHeader("User-Agent");
		String mimeTypes = servletRequest.getHeader("Accept");
		HttpSession session = servletRequest.getSession();
		String sessionID = session.getId();
		//String ipAddress = servletRequest.get
		
		StringBuilder message = new StringBuilder();
		message.append("==================================================\n");
		message.append(messageHeader);
		message.append(":");
		message.append("browser type:=="+browserType+"==\n");
		message.append("mime types:=="+mimeTypes+"==\n");
		message.append("session id:=="+sessionID+"==\n");
		message.append("==================================================\n");
		//message.append("IP address:=="+ipAdress+"")
		//System.out.println("study submission login browserType=="+browserType+"==mime type=="+mimeTypes+"== session id=="+ sessionID + "==ipAddress=="+ipAddress+"==");
		System.out.println(message.toString());
		
	}
	
	/**
	 * Used as a crude way to find how long individual service operations are taking to 
	 * complete.
	 * @param header
	 */
	protected void printTime(final String header) {
		Date date = new Date();
		StringBuilder buffer = new StringBuilder();
		buffer.append(header);
		buffer.append(":");
		buffer.append(sd.format(date));
		buffer.append("(");
		long elapsed = date.getTime() - startTime.getTime();
		buffer.append(elapsed);
		buffer.append(" milliseconds since start time");
		System.out.println(buffer.toString());		
	}
	

	// ==========================================
	// Section Interfaces
	// ==========================================

	// ==========================================
	// Section Override
	// ==========================================
}
