package org.sahsu.rif.services.system;

import org.sahsu.rif.generic.system.RifError;

/**
 * An enumerated type which attempts to give a kind of error code to most types of 
 * errors we would expect to be generated by the software.  RIFServiceError is used
 * extensively in the automated test suites that are developed alongside the main code
 * base.  Using the error codes, we are able to be specific about what kind of exceptions
 * we expect to be generated in certain test scenarios.
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

public enum RIFServiceError implements RifError {
		
	DB_UNABLE_TO_MAINTAIN_DEBUG,
			
	/* Schema version cecks in BaseSQLManager.java failed */
	DB_SCHEMA_VERSION_CHECK_FAILED,
			
	/** The db unable check geo level area exists. */
	DB_UNABLE_CHECK_GEO_LEVEL_AREA_EXISTS,
	
	DB_UNABLE_TO_RESET_CONNECTION_POOL,
	
	/** The get top level icd terms. */
	GET_TOP_LEVEL_ICD_TERMS,
	
	/** The get children for icd code. */
	GET_CHILDREN_FOR_ICD_CODE,
	
	/** The get parent for icd code. */
	GET_PARENT_FOR_ICD_CODE,
	
	/** The empty required field. */
	EMPTY_REQUIRED_FIELD,
	
	/** The invalid age group. */
	INVALID_AGE_GROUP,
	
	/** The invalid age band. */
	INVALID_AGE_BAND,
	
	/** The age bands have gaps or overlaps. */
	AGE_BANDS_HAVE_GAPS_OR_OVERLAPS,
	
	/** The non existent age group. */
	NON_EXISTENT_AGE_GROUP,
	
	/** The invalid icd code. */
	INVALID_ICD_CODE,
	
	/** The invalid health code. */
	INVALID_HEALTH_CODE,
	
	/** The invalid health code taxonomy. */
	INVALID_HEALTH_CODE_TAXONOMY,
		
	/** The invalid adjustable covariate. */
	INVALID_ADJUSTABLE_COVARIATE,
	
	/** The invalid exposure covariate. */
	INVALID_EXPOSURE_COVARIATE,
	
	/** The invalid year interval. */
	INVALID_YEAR_INTERVAL,
	
	/** The invalid geography. */
	INVALID_GEOGRAPHY,
	
	/** The invalid geolevel area. */
	INVALID_GEOLEVEL_AREA,
	
	/** The invalid geolevel select. */
	INVALID_GEOLEVEL_SELECT,
	
	/** The invalid geolevel to map. */
	INVALID_GEOLEVEL_TO_MAP,
	
	/** The invalid geolevel view. */
	INVALID_GEOLEVEL_VIEW,
	
	/** The get covariates. */
	GET_COVARIATES,
	
	/** The get health themes. */
	GET_HEALTH_THEMES,
	
	/** The no nd pair for health theme. */
	NO_ND_PAIR_FOR_HEALTH_THEME,
	
	/** The get numerator denominator pair. */
	GET_NUMERATOR_DENOMINATOR_PAIR,
	
	/** The empty api method parameter. */
	EMPTY_API_METHOD_PARAMETER,
	
	/** The get geographies. */
	GET_GEOGRAPHIES,
	
	/** The get geolevel select values. */
	GET_GEOLEVEL_SELECT_VALUES,
	
	/** The get default geolevel select value. */
	GET_DEFAULT_GEOLEVEL_SELECT_VALUE,	
	
	/** The get geolevel area values. */
	GET_GEOLEVEL_AREA_VALUES,
	
	/** The get geolevel view values. */
	GET_GEOLEVEL_VIEW_VALUES,
	
	/** The get geolevel to map values. */
	GET_GEOLEVEL_TO_MAP_VALUES,
	
	/** The non existent geography. */
	NON_EXISTENT_GEOGRAPHY,
	
	/** The non existent geolevel select value. */
	NON_EXISTENT_GEOLEVEL_SELECT_VALUE,
	
	/** The non existent health theme. */
	NON_EXISTENT_HEALTH_THEME,
	

	/** The invalid health theme. */
	INVALID_HEALTH_THEME,
	
	/** The invalid numerator denominator pair. */
	INVALID_NUMERATOR_DENOMINATOR_PAIR,
	
	/** The no age group id for numerator. */
	NO_AGE_GROUP_ID_FOR_NUMERATOR,
		
	/** The no start end year for numerator. */
	NO_START_END_YEAR_FOR_NUMERATOR,

	/** The non existent nd pair. */
	NON_EXISTENT_ND_PAIR,
	
	NON_EXISTENT_NUMERATOR_TABLE,
	
	UPDATE_SELECTSTATE_FAILED,
	UPDATE_PRINTSTATE_FAILED,
	JSON_PARSE_ERROR,
	SETSTUDYEXTRACTTOFAIL_FAILED,
	
	/** The year range interval too high. */
	YEAR_RANGE_INTERVAL_TOO_HIGH,

	/** The non existent geolevel to map value. */
	NON_EXISTENT_GEOLEVEL_VIEW_VALUE,
	
	/** The non existent geolevel to map value. */
	NON_EXISTENT_GEOLEVEL_TO_MAP_VALUE,
	
	/** The invalid map area. */
	INVALID_MAP_AREA,
	
	/** The map area start index more than end index. */
	MAP_AREA_START_INDEX_MORE_THAN_END_INDEX,
	
	/** The no lookup table for map areas. */
	NO_LOOKUP_TABLE_FOR_MAP_AREAS,
	
	/** The non existent geolevel area value. */
	NON_EXISTENT_GEOLEVEL_AREA_VALUE,
	
	/** The duplicate map areas. */
	DUPLICATE_MAP_AREAS,
	
	/** The invalid year range. */
	INVALID_YEAR_RANGE,
	
	/** The invalid investigation. */
	INVALID_INVESTIGATION,
	
	/** The invalid comparison area. */
	INVALID_COMPARISON_AREA,
	
	/** The invalid disease mapping study area. */
	INVALID_DISEASE_MAPPING_STUDY_AREA,
	
	/** The invalid disease mapping study. */
	INVALID_DISEASE_MAPPING_STUDY,
	
	/** The invalid calculation method. */
	INVALID_CALCULATION_METHOD,
	
	/** The invalid parameter. */
	INVALID_PARAMETER,
	
	/** The duplicate parameters. */
	DUPLICATE_PARAMETERS,
	
	/** The invalid rif job submission. */
	INVALID_RIF_JOB_SUBMISSION,
	
	/** The invalid project. */
	INVALID_PROJECT,
	
	/** The invalid startup options. */
	INVALID_STARTUP_OPTIONS,
	
	/** The xml file parsing problem. */
	//XML_FILE_PARSING_PROBLEM,
	
	/** The invalid rif service information. */
	INVALID_RIF_SERVICE_INFORMATION,
	
	/** The xml problem writing health code list. */
	XML_PROBLEM_WRITING_HEALTH_CODE_LIST,
	
	/** The xml problem reading health code list. */
	XML_PROBLEM_READING_HEALTH_CODE_LIST,
	
	/** The xml problem writing map area list. */
	XML_PROBLEM_WRITING_MAP_AREA_LIST,
	
	/** The xml problem reading map area list. */
	XML_PROBLEM_READING_MAP_AREA_LIST,
	
	/** The get covariates for investigation. */
	GET_COVARIATES_FOR_INVESTIGATION,
	
	/** The get health outcomes for investigation. */
	GET_HEALTH_OUTCOMES_FOR_INVESTIGATION,
	
	/** The non existent health code provider. */
	NON_EXISTENT_HEALTH_CODE_PROVIDER,
	
	NON_EXISTENT_HEALTH_CODE,
	
	/** The get health codes. */
	GET_HEALTH_CODES,
	
	/** The xml problem reading health code taxonomy. */
	XML_PROBLEM_READING_HEALTH_CODE_TAXONOMY,
	
	/** The xml taxonomy reader no input file specified. */
	XML_TAXONOMY_READER_NO_INPUT_FILE_SPECIFIED,
	
	/** unable to add a study to the database */
	UNABLE_TO_ADD_STUDY,
	
	/** covariate does not exist */
	NON_EXISTENT_COVARIATE,
	
	ZIPFILE_CREATE_FAILED,/* Unable to create Zipfile */
	ZIPFILE_GET_STATUS_FAILED,/* Unable to get Zipfile status*/
	JSONFILE_CREATE_FAILED, /* Unable to create JSON setup file for a run study, including the print setup */
	
	NO_ND_PAIR_FOR_NUMERATOR_TABLE_NAME,
	NO_HEALTH_TAXONOMY_FOR_NAMESPACE,	
	HEALTH_CODE_NOT_KNOWN_TO_PROVIDER,
	UNREALISTIC_RESULT_BLOCK_START_ROW,
	RESULT_BLOCK_START_MORE_THAN_END,
	NON_EXISTENT_RESULT_TABLE,
	NON_EXISTENT_RESULT_TABLE_FIELD_NAME,
	NON_EXISTENT_MAP_AREA,
	INVALID_GEO_LEVEL_ATTRIBUTE_SOURCE,
	INVALID_GEO_LEVEL_ATTRIBUTE_THEME,
	NON_EXISTENT_DISEASE_MAPPING_STUDY,
	UNABLE_TO_GET_EXTRACT_TABLE_NAME,
	UNABLE_TO_COUNT_ROWS_IN_RESULT_TABLE,
	UNABLE_TO_GET_GEOMETRY_COLUMN_NAMES,
	UNABLE_TO_DETERMINE_EXTRACT_PERMISSION_FOR_STUDY,
	EXTRACT_NOT_PERMITTED_FOR_STUDY,
	NON_EXISTENT_GEO_LEVEL_ATTRIBUTE_THEME,
	NON_EXISTENT_GEO_LEVEL_ATTRIBUTE,
	DATABASE_QUERY_FAILED,
	DATABASE_UPDATE_FAILED,
	STATE_MACHINE_ERROR,
	INVALID_STUDY_RESULT_RETRIEVAL_CONTEXT,
	NON_EXISTENT_PROJECT,
	INVALID_BOUNDARY_RECTANGLE,
	INVALID_STUDY_SUMMARY,
	INVALID_TABLE_FIELD_NAMES,
	NON_EXISTENT_TABLE_FIELD_NAME,
	NON_EXISTENT_GEO_LEVEL_ATTRIBUTE_SOURCE,
	NON_EXISTENT_CALCULATION_METHOD,
	NO_MAP_AREAS_SPECIFIED,
	INVALID_ZOOM_FACTOR,
	THRESHOLD_MAP_AREAS_PER_DISPLAY_EXCEEDED, 
	NON_EXISTENT_STUDY,
	HEALTH_CODE_TAXONOMY_SERVICE_ERROR,
	INVALID_TILE_TYPE,
	GRAPHICS_IO_ERROR,
	UNABLE_TO_PARSE_JSON_SUBMISSION,
	UNABLE_TO_RETRIEVE_ALL_RELEVANT_MAP_AREAS,
	UNABLE_TO_GET_STUDY_STATE,
	UNABLE_TO_WRITE_STUDY_ZIP_FILE,
	TILE_CACHE_FILE_READ_ERROR,
	TILE_CACHE_FILE_WRITE_ERROR,
	TILE_GENERATE_SQL_ERROR
	
}
