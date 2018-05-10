package rifServices.dataStorageLayer.pg;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import rifGenericLibrary.dataStorageLayer.SQLGeneralQueryFormatter;
import rifGenericLibrary.dataStorageLayer.common.SQLQueryUtility;
import rifGenericLibrary.dataStorageLayer.pg.PGSQLSelectQueryFormatter;
import rifGenericLibrary.system.RIFServiceException;
import rifGenericLibrary.util.RIFLogger;
import rifServices.businessConceptLayer.AbstractGeographicalArea;
import rifServices.businessConceptLayer.GeoLevelSelect;
import rifServices.businessConceptLayer.GeoLevelToMap;
import rifServices.businessConceptLayer.Geography;
import rifServices.businessConceptLayer.MapArea;
import rifServices.dataStorageLayer.common.BaseSQLManager;
import rifServices.dataStorageLayer.common.MapDataManager;
import rifServices.dataStorageLayer.common.RIFContextManager;
import rifServices.system.RIFServiceError;
import rifServices.system.RIFServiceMessages;
import rifServices.system.RIFServiceStartupOptions;

public final class PGSQLMapDataManager extends BaseSQLManager implements MapDataManager {
	
	private static final RIFLogger rifLogger = RIFLogger.getLogger();

	public PGSQLMapDataManager(
			final RIFServiceStartupOptions rifServiceStartupOptions) {

		super(rifServiceStartupOptions);
	}

	@Override
	public ArrayList<MapArea> getAllRelevantMapAreas(
			final Connection connection,
			final Geography geography,
			final AbstractGeographicalArea geographicalArea)
		throws RIFServiceException {
		
		rifLogger.info(this.getClass(), "SQLMapDataManager getAllRelevantAreas!!!!!!!!!!!");
		ArrayList<MapArea> allRelevantMapAreas = new ArrayList<MapArea>();

		GeoLevelSelect geoLevelSelect
			= geographicalArea.getGeoLevelSelect();		
		GeoLevelToMap geoLevelToMap
			= geographicalArea.getGeoLevelToMap();
		
		
		ArrayList<MapArea> selectedMapAreas
			= geographicalArea.getMapAreas();
		
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		
		try {
			
			/*
			 * Step 1: Obtain the geography table; This maps the map identifier as it is known
			 * at the GeoLevelSelect level to the map identifier as it is known at the finer
			 * resolution of GeoLevelToMap
			 */
			//Obtain geography table eg: sahsuland_geography
			String mapAreaResolutionMappingTableName
				= getMapAreaResolutionMappingAreaTableName(
					connection,
					geography);

			String geoLevelToMapTableName
				= getGeoLevelLookupTableName(
					connection,
					geography,
					geoLevelToMap.getName());
			
			/*
			 * Example:
			 * 
			 * SELECT
			 *    gid,
			 *    level4
			 * FROM
			 *    mapAreaResolutionMappingTableName,  //eg: sahsuland_geography
			 *    geoLevelToMapTableName //eg: sahsuland_level4
			 * WHERE
			 *    level2='01.001' OR  //iteratively read in each map area provided by
			 *    level2='01.002' OR  //by client
			 *    level2='01.003' OR
			 *    ...
			 * 
			 * 
			 * 
			 * 
			 * 			
			 */
			SQLGeneralQueryFormatter queryFormatter = new SQLGeneralQueryFormatter();
			queryFormatter.addQueryLine(0, "SELECT DISTINCT");
			queryFormatter.addQueryPhrase(1, geoLevelToMapTableName);			
			queryFormatter.addQueryPhrase(".gid,");
			queryFormatter.addQueryPhrase(geoLevelToMapTableName);			
			queryFormatter.addQueryPhrase(".");			
			queryFormatter.addQueryPhrase(geoLevelToMap.getName());			
			queryFormatter.padAndFinishLine();			
			queryFormatter.addQueryLine(0, "FROM");
			queryFormatter.addQueryPhrase(1, mapAreaResolutionMappingTableName);
			queryFormatter.addQueryPhrase(",");
			queryFormatter.addQueryPhrase(geoLevelToMapTableName);
			queryFormatter.padAndFinishLine();			
			queryFormatter.addQueryLine(0, "WHERE");
			
			queryFormatter.addQueryPhrase(1, geoLevelToMapTableName);
			queryFormatter.addQueryPhrase(".");
			queryFormatter.addQueryPhrase(geoLevelToMap.getName());			
			queryFormatter.addQueryPhrase("=");
			queryFormatter.addQueryPhrase(mapAreaResolutionMappingTableName);
			queryFormatter.addQueryPhrase(".");			
			queryFormatter.addQueryPhrase(geoLevelToMap.getName());			
			
			
			int totalSelectedMapAreas = selectedMapAreas.size();
			if (totalSelectedMapAreas > 0) {

				queryFormatter.addQueryPhrase(" AND (");			
				queryFormatter.padAndFinishLine();
				
				String geoLevelSelectLevelName = geoLevelSelect.getName();
				//String geoLevelSelectLevelName = geoLevelToMap.getName();
			
				for (int i = 0 ; i < selectedMapAreas.size(); i++) {
					if (i != 0) {
						queryFormatter.padAndFinishLine();			
						queryFormatter.addQueryPhrase(1, " OR ");
					}
					
					queryFormatter.addQueryPhrase(mapAreaResolutionMappingTableName);					
					//queryFormatter.addQueryPhrase(geoLevelToMapTableName);					
					queryFormatter.addQueryPhrase(".");
					queryFormatter.addQueryPhrase(geoLevelSelectLevelName);
					queryFormatter.addQueryPhrase("=\'");
					queryFormatter.addQueryPhrase(selectedMapAreas.get(i).getIdentifier());
					queryFormatter.addQueryPhrase("'");
				}
				
				queryFormatter.addQueryPhrase(")");
			}
			
			queryFormatter.addQueryPhrase(";");
			
			logSQLQuery(
				"getAllRelevantMapAreas", 
				queryFormatter, 
				geography.getName(),
				geoLevelToMap.getName());
			
			statement
				= createPreparedStatement(
					connection, 
					queryFormatter);
			
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				String identifier
					= resultSet.getString(1);
				String name
					= resultSet.getString(2);
				
				MapArea mapArea
					= MapArea.newInstance(
						identifier, 
						identifier, 
						name);
				allRelevantMapAreas.add(mapArea);
				
			}
		}
		catch(SQLException sqlException) {
			logException(sqlException);
			String errorMessage
				= RIFServiceMessages.getMessage(
					"sqlMapDataManager.error.unableToRetrievaAllRelevantMapAreas");
			RIFServiceException rifServiceException
				= new RIFServiceException(
					RIFServiceError.UNABLE_TO_RETRIEVE_ALL_RELEVANT_MAP_AREAS, 
					errorMessage);
			throw rifServiceException;
		}
		finally {
			SQLQueryUtility.close(statement);
			SQLQueryUtility.close(resultSet);
		}
	
		return allRelevantMapAreas;
	}
	
	private String getMapAreaResolutionMappingAreaTableName(
		final Connection connection,
		final Geography geography) 
		throws SQLException,
		RIFServiceException {
				
		String result = "";
				
		PGSQLSelectQueryFormatter queryFormatter = new PGSQLSelectQueryFormatter();
		queryFormatter.addSelectField("hierarchytable");
		queryFormatter.addFromTable("rif40_geographies");
		queryFormatter.addWhereParameter("geography");
		
		logSQLQuery(
			"getMapAreaResolutionMappingAreaTableName", 
			queryFormatter, 
			geography.getName());
		
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			statement 
				= createPreparedStatement(
					connection, 
					queryFormatter);
			statement.setString(1, geography.getName());
			resultSet = statement.executeQuery();
			
			resultSet.next();
			result = resultSet.getString(1);
		}
		finally {
			SQLQueryUtility.close(statement);
			SQLQueryUtility.close(resultSet);
		}
		
		return result;
	}	
		
	/**
	 * Gets the geo level lookup table name.
	 *
	 * @param connection the connection
	 * @param geography the geography
	 * @param resolutionLevel the resolution level
	 * @return the geo level lookup table name
	 * @throws RIFServiceException the RIF service exception
	 */
	private String getGeoLevelLookupTableName( 
		final Connection connection,
		final Geography geography,
		final String resolutionLevel) 
		throws SQLException,
		RIFServiceException { 

		PreparedStatement statement = null;
		ResultSet resultSet = null;
		String result = null;
		try {
		
			PGSQLSelectQueryFormatter queryFormatter 
				= new PGSQLSelectQueryFormatter();
			configureQueryFormatterForDB(queryFormatter);
			queryFormatter.addSelectField("lookup_table");
			queryFormatter.addFromTable("rif40_geolevels");
			queryFormatter.addWhereParameter("geography");
			queryFormatter.addWhereParameter("geolevel_name");
		
			logSQLQuery(
				"getGeoLevelLookupTableName",
				queryFormatter,
				geography.getName(),
				resolutionLevel);
		
			statement 
				= createPreparedStatement(
					connection, 
					queryFormatter);
			statement.setString(1, geography.getName());
			statement.setString(2, resolutionLevel);
			resultSet = statement.executeQuery();
			connection.commit();

			// This method assumes that geoLevelSelect is valid
			// Therefore, it must be associated with a lookup table
			resultSet.next();
			
			result
				= useAppropriateTableNameCase(resultSet.getString(1));
		}
		finally {
			//Cleanup database resources			
			SQLQueryUtility.close(statement);
			SQLQueryUtility.close(resultSet);
		}
		
		return result;
	}
}
