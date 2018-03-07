package rifServices.dataStorageLayer.common;

import rifServices.system.RIFServiceStartupOptions;
import rifServices.businessConceptLayer.AbstractStudy;
import rifGenericLibrary.util.RIFLogger;
import rifGenericLibrary.dataStorageLayer.SQLGeneralQueryFormatter;
import rifGenericLibrary.dataStorageLayer.common.SQLQueryUtility;
import rifServices.dataStorageLayer.common.SQLAbstractSQLManager;
import rifGenericLibrary.dataStorageLayer.DatabaseType;
import rifServices.businessConceptLayer.RIFStudySubmission;
import rifServices.graphics.RIFMaps;
import rifServices.graphics.RIFGraphicsOutputType;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.*;
import java.io.*;
import java.lang.*;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Map;
import java.util.Calendar;
import java.util.Locale;
import java.net.URL;
import java.util.Set;
import java.util.EnumSet;
import java.util.Iterator;

import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor; 
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.PropertyType;
import org.opengis.feature.type.GeometryDescriptor; 
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.geometry.BoundingBox;

import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.Geometries;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.GeometryBuilder;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.CRS;
import org.geotools.data.shapefile.ShapefileDataStore; 
import org.geotools.data.FeatureWriter; 
import org.geotools.data.Transaction; 
import org.geotools.map.MapViewport;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTReader;

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

public class RifGeospatialOutputs extends SQLAbstractSQLManager {
	// ==========================================
	// Section Constants
	// ==========================================
	private static final RIFLogger rifLogger = RIFLogger.getLogger();
	private static String lineSeparator = System.getProperty("line.separator");
	private Connection connection;
	private String studyID;
	private static String EXTRACT_DIRECTORY;
	private static int printingDPI;
	
	private static final String STUDY_QUERY_SUBDIRECTORY = "study_query";
	private static final String STUDY_EXTRACT_SUBDIRECTORY = "study_extract";
	private static final String RATES_AND_RISKS_SUBDIRECTORY = "rates_and_risks";
	private static final String GEOGRAPHY_SUBDIRECTORY = "geography";
	private static final String DATA_SUBDIRECTORY = "data";
	private static final int BASE_FILE_STUDY_NAME_LENGTH = 100;
	
	private RIFServiceStartupOptions rifServiceStartupOptions;
	private static DatabaseType databaseType;
	
	private static GeometryFactory geometryFactory = null;
	private static GeometryJSON geoJSONWriter = null;

	private static Map<String, String> environmentalVariables = System.getenv();
	private static String catalinaHome = environmentalVariables.get("CATALINA_HOME");
	
	private static RifCoordinateReferenceSystem rifCoordinateReferenceSystem = null;
	private static RIFMaps rifMaps = null;
	private static int roundDP=3;
	
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
	public RifGeospatialOutputs(
			final RIFServiceStartupOptions rifServiceStartupOptions) {
		super(rifServiceStartupOptions.getRIFDatabaseProperties());
		
		this.rifCoordinateReferenceSystem = new RifCoordinateReferenceSystem();
		geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
		geoJSONWriter = new GeometryJSON();
		
		this.rifServiceStartupOptions = rifServiceStartupOptions;
		
		try {
			databaseType=this.rifServiceStartupOptions.getRifDatabaseType();
			printingDPI=this.rifServiceStartupOptions.getOptionalRIfServiceProperty("printingDPI", 1000);
			roundDP=this.rifServiceStartupOptions.getOptionalRIfServiceProperty("roundDP", 3);
		}
		catch(Exception exception) {
			rifLogger.warning(this.getClass(), 
				"Error in RifGeospatialOutputs() constructor");
			throw new NullPointerException();
		}		
	}	

	/** 
	 * Write geospatial files in both geoJSON and shapefile format.
	 * a) rif study area to GEOGRAPHY_SUBDIRECTORY
	 * b) Comparison area to GEOGRAPHY_SUBDIRECTORY
	 * c) Map (results) table to DATA_SUBDIRECTORY
	 *    The column list for the map tsble is hard coded and reduced to 10 characters for DBF support
     * 
	 * @param Connection connection, 
	 * @param File temporaryDirectory,
	 * @param String baseStudyName,
	 * @param String zoomLevel,
	 * @param RIFStudySubmission rifStudySubmission,
	 * @param CachedRowSetImpl rif40Studies,
	 * @param Locale locale
	 *
	 * @returns String
	 */		
	public String writeGeospatialFiles(
			final Connection connection,
			final File temporaryDirectory,
			final String baseStudyName,
			final String zoomLevel,
			final RIFStudySubmission rifStudySubmission,
			final CachedRowSetImpl rif40Studies,
			final Locale locale)
					throws Exception {
						
	if (rifMaps == null) {
			rifMaps = new RIFMaps(rifServiceStartupOptions);
		}						
		
		String studyID = rifStudySubmission.getStudyID();
		String mapTable=getColumnFromResultSet(rif40Studies, "map_table");
		
		//Add geographies to zip file
		StringBuilder tileTableName = new StringBuilder();	
		tileTableName.append("geometry_");
		String geog = rifStudySubmission.getStudy().getGeography().getName();			
		tileTableName.append(geog);
		
		//Write study area
		StringBuilder tileFileName = new StringBuilder();
		tileFileName.append(baseStudyName);
		tileFileName.append("_studyArea");
		
		RifFeatureCollection studyFeatureCollection=writeMapQueryTogeoJSONFile(
				connection,
				rifStudySubmission,
				"rif40_study_areas",
				temporaryDirectory,
				GEOGRAPHY_SUBDIRECTORY,
				"rif_data",								/* Schema */
				tileTableName.toString(),
				tileFileName.toString(),
				zoomLevel,
				studyID,
				"S", 									/* areaType */
				", a.area_id, a.band_id, b.zoomlevel, c.areaname",	/* extraColumns */
				null 									/* additionalJoin */,
				locale);
		
		//Write comparison area
		tileFileName = new StringBuilder();
		tileFileName.append(baseStudyName);
		tileFileName.append("_comparisonArea");
		
		RifFeatureCollection comparisonFeatureCollection=writeMapQueryTogeoJSONFile(
				connection,
				rifStudySubmission,
				"rif40_comparison_areas",
				temporaryDirectory,
				GEOGRAPHY_SUBDIRECTORY,
				"rif_data",								/* Schema */
				tileTableName.toString(),
				tileFileName.toString(),
				zoomLevel,
				studyID,
				"C", 									/* areaType */
				", a.area_id, b.zoomlevel, c.areaname",	/* extraColumns */
				null 									/* additionalJoin */,
				locale);	
		
		//Write results
		tileFileName = new StringBuilder();
		tileFileName.append(baseStudyName);
		tileFileName.append("_map");
		
		String extraColumns=null;
		
		
		if (databaseType == DatabaseType.POSTGRESQL) { 
			extraColumns=", b.zoomlevel, c.area_id, c.username, c.study_id, c.inv_id, c.band_id, c.genders" +
//				"/*, c.direct_standardisation */" +
				", ROUND(c.adjusted::NUMERIC, " + roundDP + ") As adjusted, c.observed" +
				", ROUND(c.expected::NUMERIC, " + roundDP + ") AS expected" +
				", ROUND(c.lower95::NUMERIC, " + roundDP + ") AS lower95" +
				", ROUND(c.upper95::NUMERIC, " + roundDP + ") AS upper95" +
				", ROUND(c.relative_risk::NUMERIC, " + roundDP + ") AS rr" +
				", ROUND(c.smoothed_relative_risk::NUMERIC, " + roundDP + ") AS sm_rr" +
				", ROUND(c.posterior_probability::NUMERIC, " + roundDP + ") AS post_prob" +
//				"/*, c.posterior_probability_upper95, c.posterior_probability_lower95" +
//				", c.residual_relative_risk, c.residual_rr_lower95, c.residual_rr_upper95 */" +
				", ROUND(c.smoothed_smr::NUMERIC, " + roundDP + ") AS sm_smr" +
				", ROUND(c.smoothed_smr_lower95::NUMERIC, " + roundDP + ") AS sm_smr_l95" +
				", ROUND(c.smoothed_smr_upper95::NUMERIC, " + roundDP + ") AS sm_smr_u95";
		}
		else {
			extraColumns=", b.zoomlevel, c.area_id, c.username, c.study_id, c.inv_id, c.band_id, c.genders" +
//				"/*, c.direct_standardisation */" +
				", ROUND(c.adjusted, " + roundDP + ") As adjusted, c.observed" +
				", ROUND(c.expected, " + roundDP + ") AS expected" +
				", ROUND(c.lower95, " + roundDP + ") AS lower95" +
				", ROUND(c.upper95, " + roundDP + ") AS upper95" +
				", ROUND(c.relative_risk, " + roundDP + ") AS rr" +
				", ROUND(c.smoothed_relative_risk, " + roundDP + ") AS sm_rr" +
				", ROUND(c.posterior_probability, " + roundDP + ") AS post_prob" +
//				"/*, c.posterior_probability_upper95, c.posterior_probability_lower95" +
//				", c.residual_relative_risk, c.residual_rr_lower95, c.residual_rr_upper95 */" +
				", ROUND(c.smoothed_smr, " + roundDP + ") AS sm_smr" +
				", ROUND(c.smoothed_smr_lower95, " + roundDP + ") AS sm_smr_l95" +
				", ROUND(c.smoothed_smr_upper95, " + roundDP + ") AS sm_smr_u95";
		}
		RifFeatureCollection mapFeatureCollection=writeMapQueryTogeoJSONFile(
				connection,
				rifStudySubmission,
				"rif40_study_areas",
				temporaryDirectory,
				DATA_SUBDIRECTORY,
				"rif_data",				/* Schema */
				tileTableName.toString(),
				tileFileName.toString(),
				zoomLevel,
				studyID,
				null, 					/* areaType */
				extraColumns,			/* extraColumns: reduced to 10 characters */
				"LEFT OUTER JOIN rif_studies." + mapTable.toLowerCase() + 
					" c ON (a.area_id = c.area_id)"
														/* additionalJoin */,
				locale);
				
		rifMaps.writeResultsMaps(
				mapFeatureCollection,
				connection,
				temporaryDirectory,
				baseStudyName,
				zoomLevel,
				rifStudySubmission,
				rif40Studies,
				locale);
				
		return createMapsHTML(studyID);
	}		

	/** 
	 * Create HTML to view maps in ZIP html app
     *  
	 * @param String studyID
	 *
	 * @returns HTML as string
	 */	
	private String createMapsHTML(
		final String studyID) {
			
		StringBuffer mapHTML=new StringBuffer();
		
		mapHTML.append("    <h1 id=\"maps\">Maps</h1>" + lineSeparator);
		mapHTML.append("      <p>" + lineSeparator);	
		mapHTML.append("      <div>" + lineSeparator);
		mapHTML.append("        <form id=\"downloadForm2\" method=\"get\" action=\"maps\\smoothed_smr_" + 
			studyID + "_" + printingDPI + "dpi.png\">" + lineSeparator);
		mapHTML.append("        Year: <select id=\"rifMapsList\">" + lineSeparator);					
		mapHTML.append("          <option value=\"maps\\relative_risk_" + 
			studyID + "_" + printingDPI + "dpi.png\" />Relative Risk</option>" + 
			lineSeparator);		
		mapHTML.append("          <option value=\"maps\\posterior_probability_" + 
			studyID + "_" + printingDPI + "dpi.png\" />Posterior Probability_</option>" + 
			lineSeparator);		
		mapHTML.append("          <option value=\"maps\\smoothed_smr_" + 
			studyID + "_" + printingDPI + "dpi.png\" selected />Smoothed SMR</option>" + 
			lineSeparator);
		mapHTML.append("        </select>" + lineSeparator);
		
		mapHTML.append("        Graphics Format: <select id=\"rifMapsFileType\">" + lineSeparator);
		Set<RIFGraphicsOutputType> htmlOutputTypes = EnumSet.of( // Can be viewed in browser
			RIFGraphicsOutputType.RIFGRAPHICS_PNG,
			RIFGraphicsOutputType.RIFGRAPHICS_JPEG,
			RIFGraphicsOutputType.RIFGRAPHICS_GEOTIFF, 
			RIFGraphicsOutputType.RIFGRAPHICS_SVG);
		Iterator <RIFGraphicsOutputType> htmlOutputTypeIter = htmlOutputTypes.iterator();
		int j=0;
		while (htmlOutputTypeIter.hasNext()) {
			String selected="";
			String disabled="";
			RIFGraphicsOutputType outputType=htmlOutputTypeIter.next();
			j++;
			if (outputType.getGraphicsExtentsion().equals("png")) {
				selected="selected";
			}
			if (!outputType.isRIFGraphicsOutputTypeEnabled()) {
				disabled="disabled";
			}
			mapHTML.append("          <option value=\"" + 
				outputType.getGraphicsExtentsion() +
				"\" " + disabled + " " +
				"id=\"" + outputType.getRIFGraphicsOutputTypeShortName().toLowerCase() + "Select\" " + 
				"title=\"" + outputType.getRIFGraphicsOutputTypeDescription() + "\" " + 
				selected + " />" + outputType.getRIFGraphicsOutputTypeShortName() + " (" + 
					outputType.getRIFGraphicsOutputTypeDescription() +
				")</option>" + lineSeparator);
		}
		mapHTML.append("        </select>" + lineSeparator);	
		mapHTML.append("        <button id=\"downloadButton2\" type=\"submit\">Download PNG</button>" + lineSeparator);
		mapHTML.append("        </form>" + lineSeparator);	
		mapHTML.append("      </div>" + lineSeparator);
		mapHTML.append("      <img src=\"maps\\smoothed_smr_" + 
			studyID + "_" + printingDPI + "dpi.png\" id=\"rifMaps\" width=\"80%\" />");
		mapHTML.append("      </p>" + lineSeparator);	
		
		return mapHTML.toString();
	}

	/** 
	 * Query geolevel_id, geolevel_name, geography, srid, max_geojson_digits from rif40_studies, 
	 * rif40_geographies,
	 * bg_geolevel_id, bg_geolevel_name: for geolevel 2 if geolevel_id>2
     * 
	 * @param Connection connection, 
	 * @param String studyID, 
	 * @param String areaTableName
	 */	
	private CachedRowSetImpl getRif40Geolevels(
			final Connection connection,
			final String studyID,
			final String areaTableName)
			throws Exception {
		SQLGeneralQueryFormatter geolevelQueryFormatter = new SQLGeneralQueryFormatter();	
		geolevelQueryFormatter.addQueryLine(0, "WITH a AS (");
		geolevelQueryFormatter.addQueryLine(1, "SELECT b.geolevel_id,");
		geolevelQueryFormatter.addQueryLine(1, "       CASE WHEN b.geolevel_id > 2 THEN 2 ELSE null END AS bg_geolevel_id,");
		geolevelQueryFormatter.addQueryLine(1, "       b.geolevel_name, c.geography, c.srid, c.max_geojson_digits");
		geolevelQueryFormatter.addQueryLine(1, "  FROM rif40.rif40_studies a, rif40.rif40_geolevels b, rif40.rif40_geographies c");
		geolevelQueryFormatter.addQueryLine(1, " WHERE study_id = ?");
		if (areaTableName.equals("rif40_comparison_areas")) {
			geolevelQueryFormatter.addQueryLine(1, "   AND a.comparison_geolevel_name = b.geolevel_name");
		} 
		else if (areaTableName.equals("rif40_study_areas")) {
			geolevelQueryFormatter.addQueryLine(1, "   AND a.study_geolevel_name = b.geolevel_name");
		} 
		else { // Map tables - same as study areas
			geolevelQueryFormatter.addQueryLine(1, "   AND a.study_geolevel_name = b.geolevel_name");
		}	
		geolevelQueryFormatter.addQueryLine(1, "   AND c.geography = b.geography");
		geolevelQueryFormatter.addQueryLine(0, ")");
		geolevelQueryFormatter.addQueryLine(0, "SELECT a.*, b1.geolevel_name AS bg_geolevel_name");
		geolevelQueryFormatter.addQueryLine(0, "   FROM a");
		geolevelQueryFormatter.addQueryLine(0, "		LEFT OUTER JOIN rif40.rif40_geolevels b1 ON (a.bg_geolevel_id = b1.geolevel_id");
		geolevelQueryFormatter.addQueryLine(0, "		 										 AND a.geography = b1.geography)");
		
		int[] params = new int[1];
		params[0]=Integer.parseInt(studyID);
		CachedRowSetImpl cachedRowSet=createCachedRowSet(connection, geolevelQueryFormatter,
			"writeMapQueryTogeoJSONFile", params);	
		
		return cachedRowSet;
	}
	
	/** 
	 * Create shapefile data store. Does not currently support <filename.shp.xml>: fgdc metadata
	 * (Needs the MetadataLinkTypeBinding class)
	 *
	 * @param File temporaryDirectory,
	 * @param String dirName, 
	 * @param String outputFileName,
	 * @param boolean enableIndexes
	 *
	 * @returns ShapefileDataStore
     */	
	private ShapefileDataStore createShapefileDataStore(
		final File temporaryDirectory,
		final String dirName, 
		final String outputFileName,
		final boolean enableIndexes) 
			throws Exception {
			
		String shapefileDirName=temporaryDirectory.getAbsolutePath() + File.separator + dirName +
			File.separator + outputFileName;
		File shapefileDirectory = new File(shapefileDirName);
		if (shapefileDirectory.exists()) {
			rifLogger.debug(this.getClass(), 
				"Found directory: " + shapefileDirectory.getAbsolutePath());
		}
		else {
			shapefileDirectory.mkdirs();
			rifLogger.info(this.getClass(), 
				"Created directory: " + shapefileDirectory.getAbsolutePath());
		}			
		String shapefileName=shapefileDirName + File.separator + outputFileName + ".shp";
		File shapefile=new File(shapefileName);			
		ShapefileDataStore shapeDataStore = new ShapefileDataStore(
			shapefile.toURI().toURL());
		shapeDataStore.setFidIndexed(enableIndexes);		// Enable indexes (DBF and SHAPEFILE)
		shapeDataStore.setIndexCreationEnabled(enableIndexes);
		
		rifLogger.info(this.getClass(), "Add shapefile to ZIP file: " + shapefileName);
		
		return shapeDataStore;
	}
		
	/** 
	 * Get CoordinateReferenceSystem using SRID [EXPERIMENTAL DOES NOT WORK: see Exception comment below]
	 *
	 * Error: NoSuchAuthorityCodeException: No code "EPSG:27700" from authority "EPSG" found for object of type "EngineeringCRS"
	 *
	 * a) Could use SRID from database
	 * b) Probably needs to access an [external?] geotools datbase
	 * c) Some defaults may be hard codable
	 *
	 * No support at present for anything other than WGS85 in the shapefile code 
	 * (i.e. re-projection required)
	 *
	 * @param RIFStudySubmission rifStudySubmission
	 * @param CachedRowSetImpl rif40Geolevels
	 *
	 * @returns CoordinateReferenceSystem
     */	
	private CoordinateReferenceSystem getCRS(
			final RIFStudySubmission rifStudySubmission, 
			final CachedRowSetImpl rif40Geolevels) 
				throws Exception {
		
		CoordinateReferenceSystem crs=null;
		
		String geographyName = getColumnFromResultSet(rif40Geolevels, "geography");
		int srid=Integer.parseInt(getColumnFromResultSet(rif40Geolevels, "srid"));
		try {
			crs = rifCoordinateReferenceSystem.getCRS(srid);	
		}
		catch (Exception exception) {
			rifStudySubmission.addStudyWarning(this.getClass(), 
				"Unable to deduce Coordinate Reference System for SRID: " + srid + "; using WGS84" +
					lineSeparator + exception.getMessage());
			crs = DefaultGeographicCRS.WGS84;
		}	
				
		return crs;
	}
		
	/** 
	 * Create GeoJSON writer 
	 *
	 * @param File temporaryDirectory,
	 * @param String dirName, 
	 * @param String outputFileName
	 *
	 * @returns BufferedWriter
     */	
	private BufferedWriter createGeoJSonWriter(
		final File temporaryDirectory,
		final String dirName, 
		final String outputFileName) 
			throws Exception {
		String geojsonDirName=temporaryDirectory.getAbsolutePath() + File.separator + dirName;
		File geojsonDirectory = new File(geojsonDirName);
		File newDirectory = new File(geojsonDirName);
		if (newDirectory.exists()) {
			rifLogger.debug(this.getClass(), 
				"Found directory: " + newDirectory.getAbsolutePath());
		}
		else {
			newDirectory.mkdirs();
			rifLogger.info(this.getClass(), 
				"Created directory: " + newDirectory.getAbsolutePath());
		}
		
		String geojsonFile=geojsonDirName + File.separator + outputFileName + ".json";
		rifLogger.info(this.getClass(), "Add JSON to ZIP file: " + geojsonFile); 
		File file = new File(geojsonFile);
		if (file.exists()) {
			file.delete();
		}
		OutputStream ostream = new FileOutputStream(geojsonFile);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(ostream);
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
		
		return bufferedWriter;
	}
	
	/** 
	 * Create geometry from Well known text.
	 *
	 * Would need to ST_Transform to shapefile SRID is not WGS84
	 *
	 * @param String wkt
     *
	 * @returns Geometry
     */	
	private Geometry createGeometryFromWkt(final String wkt)
			throws Exception {
		Geometry geometry = null;
		if (wkt != null) {

			WKTReader reader = new WKTReader(geometryFactory);
			geometry = reader.read(wkt); // Geotools JTS
		}			
		else {
			throw new Exception("Null wkt for record: " + 1);
		}		
		
		return geometry;
	}
	
	/** 
	 * Get referenced envelope for map, using the map study area extent
	 * the default is the defined extent of data Coordinate Reference System
	 * (i.e. the projection bounding box as a ReferencedEnvelope). 
	 * 
	 * Postgres SQL>
	 *
	 * WITH c AS (
	 * 		SELECT SST_Envelope(ST_Union(ST_Envelope(b.geom))) AS envelope
	 * 		  FROM rif40.rif40_study_areas a, rif_data.geometry_sahsuland b
	 *     WHERE a.study_id    = ?
	 * 		 AND b.geolevel_id = ? AND b.zoomlevel = ?
	 *		 AND a.area_id     = b.areaid
	 * )
	 * SELECT ST_Xmin(c.envelope) AS xmin,
	 *        ST_Xmax(c.envelope) AS xmax,
	 *        ST_Ymin(c.envelope) AS ymin,
	 *        ST_Ymax(c.envelope) AS ymax
	 *   FROM c;
	 *   
	 * SQL Server SQL>
	 *
	 * WITH c AS (
	 *    SELECT geometry::EnvelopeAggregate(b.geom) AS envelope
	 *      FROM rif40.rif40_study_areas a, rif_data.geometry_usa_2014 b
	 *     WHERE a.study_id    = ?
	 * 		 AND b.geolevel_id = ? AND b.zoomlevel = ?
	 *       AND a.area_id     = b.areaid
	 * )
	 * SELECT CAST(c.envelope.STPointN(1).STX AS numeric(8,5)) AS Xmin,
	 *        CAST(c.envelope.STPointN(3).STX AS numeric(8,5)) AS Xmax,
	 *        CAST(c.envelope.STPointN(1).STY AS numeric(8,5)) AS Ymin,
	 *        CAST(c.envelope.STPointN(3).STY AS numeric(8,5)) AS Ymax
	 *   FROM c;
	 *   
	 * @param Connection connection,
	 * @param String schemaName,
	 * @param String areaTableName,
	 * @param String tileTableName,
	 * @param String geolevel,
	 * @param String zoomLevel,
	 * @param String studyID,
	 * @param CoordinateReferenceSystem rif40GeographiesCRS,
	 * @param int srid
	 *
	 * @returns ReferencedEnvelope in database CRS (4326)
	 */
	private ReferencedEnvelope getMapReferencedEnvelope(
		final Connection connection,
		final String schemaName,
		final String areaTableName,
		final String tileTableName,
		final String geolevel,
		final String zoomLevel,
		final String studyID,
		final CoordinateReferenceSystem rif40GeographiesCRS,
		final int srid)  
			throws Exception {
				
		CoordinateReferenceSystem databaseCRS=DefaultGeographicCRS.WGS84; // 4326
		ReferencedEnvelope dbEnvelope = rifCoordinateReferenceSystem.getDefaultReferencedEnvelope(
			rif40GeographiesCRS); // Default is the geographical extent of rif40GeographiesCRS
		SQLGeneralQueryFormatter queryFormatter = new SQLGeneralQueryFormatter();
		queryFormatter.addQueryLine(0, "WITH c AS (");
		if (databaseType == DatabaseType.POSTGRESQL) { 
			queryFormatter.addQueryLine(1, "SELECT ST_Envelope(ST_Union(ST_Envelope(b.geom))) AS envelope");			
		}
		else if (databaseType == DatabaseType.SQL_SERVER) {
			queryFormatter.addQueryLine(1, "SELECT geometry::EnvelopeAggregate(b.geom) AS envelope");	
		}
		queryFormatter.addQueryLine(1, "  FROM rif40." + areaTableName + " a, "  + 
											schemaName + "." + tileTableName.toLowerCase() + " b");
		queryFormatter.addQueryLine(1, " WHERE a.study_id    = ?");	
		queryFormatter.addQueryLine(1, "   AND b.geolevel_id = ? AND b.zoomlevel = ?");
		queryFormatter.addQueryLine(1, "   AND a.area_id     = b.areaid");
		queryFormatter.addQueryLine(0, ")");
		if (databaseType == DatabaseType.POSTGRESQL) { 
			queryFormatter.addQueryLine(0, "SELECT ST_AsText(ST_Transform(c.envelope, " + srid + ")) AS envelope_" + srid + ",");	
			queryFormatter.addQueryLine(0, "       ST_Xmin(c.envelope) AS xmin,");	
			queryFormatter.addQueryLine(0, "       ST_Xmax(c.envelope) AS xmax,");	
			queryFormatter.addQueryLine(0, "       ST_Ymin(c.envelope) AS ymin,");	
			queryFormatter.addQueryLine(0, "       ST_Ymax(c.envelope) AS ymax");	
		}
		else if (databaseType == DatabaseType.SQL_SERVER) {
			queryFormatter.addQueryLine(0, "SELECT c.envelope.STAsText() AS envelope_4326,"); // SQL Server cannot transform!!!
			queryFormatter.addQueryLine(0, "       CAST(c.envelope.STPointN(1).STX AS numeric(8,5)) AS Xmin,");
			queryFormatter.addQueryLine(0, "       CAST(c.envelope.STPointN(3).STX AS numeric(8,5)) AS Xmax,");
			queryFormatter.addQueryLine(0, "       CAST(c.envelope.STPointN(1).STY AS numeric(8,5)) AS Ymin,");
			queryFormatter.addQueryLine(0, "       CAST(c.envelope.STPointN(3).STY AS numeric(8,5)) AS Ymax");	
		}
		queryFormatter.addQueryLine(0, "  FROM c");
		
		PreparedStatement statement = createPreparedStatement(connection, queryFormatter);	
		ResultSet resultSet = null;
		try {	
			String[] queryArgs = new String[3];
			queryArgs[0]=studyID;
			queryArgs[1]=geolevel;
			queryArgs[2]=zoomLevel;
			logSQLQuery("getMapReferencedEnvelope", queryFormatter, queryArgs);
			statement.setInt(1, Integer.parseInt(studyID));	
			statement.setInt(2, Integer.parseInt(geolevel));
			statement.setInt(3, Integer.parseInt(zoomLevel));				
			resultSet = statement.executeQuery();
			
			if (resultSet.next()) {
				String envelopeText=resultSet.getString(1);
				Float xMin=resultSet.getFloat(2);
				Float xMax=resultSet.getFloat(3);
				Float yMin=resultSet.getFloat(4);
				Float yMax=resultSet.getFloat(5); // In 4326
				
				dbEnvelope = new ReferencedEnvelope(
					xMin /* bounds.getWestBoundLongitude() */,
					xMax /* bounds.getEastBoundLongitude() */,
					yMin /* bounds.getSouthBoundLatitude() */,
					yMax /* bounds.getNorthBoundLatitude() */,
					databaseCRS
				);		

				if (databaseType == DatabaseType.POSTGRESQL) { 
					rifLogger.info(this.getClass(), 
						"Get bounds from database bbox: [" + xMin + "," + yMin + " " + xMax + "," + yMax + "]" + lineSeparator +
						"dbEnvelope: " + dbEnvelope.toString() + lineSeparator +
						"db(in " + srid + "): "+ envelopeText);
				}
				else { 
					rifLogger.info(this.getClass(), 
						"bbox: [" + xMin + "," + yMin + " " + xMax + "," + yMax + "]" + lineSeparator +
						"dbEnvelope: " + dbEnvelope.toString() + lineSeparator +
						"db(in 4326): "+ envelopeText);
				}				
				if (resultSet.next()) {
					throw new Exception("getMapReferencedEnvelope(): expected 1 row, got many");
				}
			}
			else {
				throw new Exception("getMapReferencedEnvelope(): expected 1 row, got none");
			}
		}
		catch (Exception exception) {
			rifLogger.error(this.getClass(), "Error in SQL Statement: >>> " + 
				lineSeparator + queryFormatter.generateQuery(),
				exception);
			throw exception;
		}
		finally {
			SQLQueryUtility.close(statement);
		}
		
		return dbEnvelope;
	}
	
	/**
	 * Get background areas (as geolevel 2) so that partial mapping of a geography can have the rest of the 
	 * administrative boundaries added.
	 *
	 * SELECT b.wkt, b.areaid AS area_id, b.zoomlevel, c.areaname
     *   FROM rif_data.geometry_sahsuland b
     * 		LEFT OUTER JOIN rif_data.lookup_sahsu_grd_level1 c ON (b.areaid = c.sahsu_grd_level1)
     *  WHERE b.geolevel_id = ? AND b.zoomlevel = ?;	
	 *
	 * @param Connection connection,
	 * @param RIFStudySubmission rifStudySubmission
	 * @param String areaTableName,
	 * @param File temporaryDirectory,
	 * @param String dirName,
	 * @param String schemaName,
	 * @param String tileTableName,
	 * @param String geolevelName,
	 * @param String outputFileName,
	 * @param String zoomLevel,
	 * @param String geolevel, 
	 * @param CoordinateReferenceSystem rif40GeographiesCRS,
	 * @param Locale locale,
	 * @param MathTransform transform,
	 * @param int srid,
	 * @param String geographyName
	 *
	 * @returns DefaultFeatureCollection
     */	 
	private DefaultFeatureCollection getBackgroundAreas(
			final Connection connection,
			final RIFStudySubmission rifStudySubmission,
			final String areaTableName,
			final File temporaryDirectory,
			final String dirName,
			final String schemaName,
			final String tileTableName,
			final String geolevelName,
			final String outputFileName,
			final String zoomLevel,
			final String geolevel,
			final CoordinateReferenceSystem rif40GeographiesCRS,
			final Locale locale,
			final MathTransform transform,
			final int srid,
			final String geographyName)
					throws Exception {
			
		ShapefileDataStore shapeDataStore = createShapefileDataStore(temporaryDirectory,
			dirName, outputFileName, true /* enableIndexes */);	
		FeatureWriter<SimpleFeatureType, SimpleFeature> shapefileWriter = null; 
			// Created once feature types are defined

		DefaultFeatureCollection backgroundAreasFeatureCollection=null;
	
		SQLGeneralQueryFormatter queryFormatter = new SQLGeneralQueryFormatter();	
		queryFormatter.addQueryLine(0, "SELECT b.wkt, b.areaid AS area_id, b.zoomlevel, c.areaname");	
		queryFormatter.addQueryLine(0, "  FROM "  + schemaName + "." + tileTableName.toLowerCase() + " b");												
		queryFormatter.addQueryLine(2, "LEFT OUTER JOIN rif_data.lookup_" + geolevelName.toLowerCase() + 
			" c ON (b.areaid = c." + geolevelName.toLowerCase() + ")");
		queryFormatter.addQueryLine(0, " WHERE b.geolevel_id = ? AND b.zoomlevel = ?");		
		
		PreparedStatement statement = createPreparedStatement(connection, queryFormatter);	
		
		SimpleFeatureType simpleFeatureType=null;
		
		try {	
			ResultSet resultSet = null;
			String[] queryArgs = new String[2];
			queryArgs[0]=geolevel;
			queryArgs[1]=zoomLevel;
			logSQLQuery("getBackgroundAreas", queryFormatter, queryArgs);
			statement = createPreparedStatement(connection, queryFormatter);
			statement.setInt(1, Integer.parseInt(geolevel));
			statement.setInt(2, Integer.parseInt(zoomLevel));				
			resultSet = statement.executeQuery();
			
			int i = 0;
		
			while (resultSet.next()) {
				
				ResultSetMetaData rsmd = resultSet.getMetaData();
				int columnCount = rsmd.getColumnCount();
				i++;
				
				Geometry geometry = createGeometryFromWkt(resultSet.getString(1));
				if (i == 1) {
					simpleFeatureType=setupShapefile(rsmd, columnCount, shapeDataStore, null /* areaType */, 
						outputFileName, geometry, rif40GeographiesCRS);
					
					shapefileWriter = shapeDataStore.getFeatureWriter(shapeDataStore.getTypeNames()[0],
							Transaction.AUTO_COMMIT);				
					backgroundAreasFeatureCollection = new DefaultFeatureCollection(geolevelName, 
						simpleFeatureType);
				}
				SimpleFeature shapefileFeature = (SimpleFeature) shapefileWriter.next(); 
				SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureType);
				if (i == 1) {		
					printShapefileColumns(shapefileFeature, rsmd, outputFileName, rif40GeographiesCRS, geographyName, srid);
				}	
				
				AttributeDescriptor ad = shapefileFeature.getType().getDescriptor(0); // Create shapefile feature
					// Add first hsapefile feature attribute
				if (ad instanceof GeometryDescriptor) { 
					// Need to handle CoordinateReferenceSystem
					if (CRS.toSRS(rif40GeographiesCRS).equals(CRS.toSRS(DefaultGeographicCRS.WGS84))) {
						Geometries geomType = Geometries.get(geometry);
						switch (geomType) {
							case POLYGON: // Convert POLYGON to MULTIPOLYGON
								GeometryBuilder geometryBuilder = new GeometryBuilder(geometryFactory);
								Polygon polygons[] = new Polygon[1];
								polygons[0]=(Polygon)geometry;
								MultiPolygon multipolygon=geometryBuilder.multiPolygon(polygons);
								shapefileFeature.setAttribute(0, multipolygon); 
								builder.set(0, multipolygon); 
								break;
							case MULTIPOLYGON:
								shapefileFeature.setAttribute(0, geometry); 
								builder.set(0, geometry); 
								break;
							default:
								throw new Exception("Unsupported Geometry:" + geomType.toString());
						}
					} 
					else if (transform == null) {
						throw new Exception("Null transform from: " + CRS.toSRS(rif40GeographiesCRS) + " to: " +
							CRS.toSRS(DefaultGeographicCRS.WGS84));
					}
					else { // Transform from WGS84 to SRID CRS
						Geometry newGeometry = JTS.transform(geometry, transform); // Re-project
						shapefileFeature.setAttribute(0, newGeometry); 
						builder.set(0, newGeometry); 
					}
				} 
				else { 
					throw new Exception("First attribute is not MultiPolygon: " + 
						ad.getName().toString());
				}

				shapefileFeature.setAttribute(1, geolevelName); 
				builder.set(1, geolevelName); 
				
				// The column count starts from 2
				for (int j = 2; j <= columnCount; j++ ) {		
					String name = rsmd.getColumnName(j);
					String value = resultSet.getString(j);	
					String columnType = rsmd.getColumnTypeName(j);
					
					addDatumToShapefile(shapefileFeature, builder,
						null, name, value, columnType, j, i,
						locale);
				}
					
				backgroundAreasFeatureCollection.add(builder.buildFeature("id" + i));
				shapefileWriter.write();
			} // End of while loop			
		}
		catch (Exception exception) {
			rifLogger.error(this.getClass(), "Error in SQL Statement: >>> " + 
				lineSeparator + queryFormatter.generateQuery(),
				exception);
			throw exception;
		}
		finally {
			SQLQueryUtility.close(statement);
			if (shapefileWriter != null) {
				shapefileWriter.close();	
			}	
			connection.commit();
		}
		
		return backgroundAreasFeatureCollection;
	}
	
	/** 
     * Write results map query to geoJSON file and shapefile
     * 
	 * Query types:
	 *
     * Type 1: areaType: C; extraColumns: a.area_id, b.zoomlevel; no additionalJoin
     * 
     * WITH a AS (
     * 	SELECT *
     * 	  FROM rif40.rif40_comparison_areas
     * 	 WHERE study_id = ?
     * )
     * SELECT ST_AsText(ST_ForceRHR(b.geom)) AS wkt, a.area_id, b.zoomlevel, c.areaname
     *   FROM a
     *         LEFT OUTER JOIN rif_data.geometry_sahsuland b ON (a.area_id = b.areaid)
     * 		LEFT OUTER JOIN rif_data.lookup_sahsu_grd_level1 c ON (a.area_id = c.sahsu_grd_level1)
     *  WHERE b.geolevel_id = ? AND b.zoomlevel = ?;	
     * 
     * Type 2: areaType: S; extraColumns: a.area_id, a.band_id, b.zoomlevel; no additionalJoin
     * 
     * WITH a AS (
     * 	SELECT *
     * 	  FROM rif40.rif40_study_areas
     * 	 WHERE study_id = ?
     * )
     * SELECT ST_AsText(ST_ForceRHR(b.geom)) AS wkt, a.area_id, a.band_id, b.zoomlevel, c.areaname
     *   FROM a
     *         	LEFT OUTER JOIN rif_data.geometry_sahsuland b ON (a.area_id = b.areaid)
     * 			LEFT OUTER JOIN rif_data.lookup_sahsu_grd_level4 c ON (a.area_id = c.sahsu_grd_level4)
     *  WHERE b.geolevel_id = ? AND b.zoomlevel = ?;	
     * 
     * Type 3: areaType IS NULL; extraColumns: b.zoomlevel, c.*; additionalJoin: 
     * 			LEFT OUTER JOIN rif_studies.s367_map c ON (a.area_id = c.area_id)
     *
     * WITH a AS (
     * 	SELECT *
     *	  FROM rif40.rif40_study_areas
     *	 WHERE study_id = ?
     * )
     * SELECT ST_AsText(ST_ForceRHR(b.geom)) AS wkt, b.zoomlevel, c.*
     *   FROM a
     *        	LEFT OUTER JOIN rif_data.geometry_sahsuland b ON (a.area_id = b.areaid)
     *        	LEFT OUTER JOIN rif_studies.s367_map c ON (a.area_id = c.area_id);
     *
	 * @param Connection connection,
	 * @param RIFStudySubmission rifStudySubmission
	 * @param String areaTableName,
	 * @param File temporaryDirectory,
	 * @param String dirName,
	 * @param String schemaName,
	 * @param String tileTableName,
	 * @param String outputFileName,
	 * @param String zoomLevel,
	 * @param String studyID,
	 * @param String areaType,
	 * @param String extraColumns,
	 * @param String additionalJoin,
	 * @param Locale locale	 
	 *
	 * @returns RifFeatureCollection
     */	 
	private RifFeatureCollection writeMapQueryTogeoJSONFile(
			final Connection connection,
			final RIFStudySubmission rifStudySubmission,
			final String areaTableName,
			final File temporaryDirectory,
			final String dirName,
			final String schemaName,
			final String tileTableName,
			final String outputFileName,
			final String zoomLevel,
			final String studyID,
			final String areaType,
			final String extraColumns,
			final String additionalJoin,
			final Locale locale)
					throws Exception {
			
		RifLocale rifLocale = new RifLocale(locale);			
		Calendar calendar = rifLocale.getCalendar();			
		DateFormat df = rifLocale.getDateFormat();

		BufferedWriter bufferedWriter = createGeoJSonWriter(temporaryDirectory,
			dirName, outputFileName);

		ShapefileDataStore shapeDataStore = createShapefileDataStore(temporaryDirectory,
			dirName, outputFileName, true /* enableIndexes */);	
		FeatureWriter<SimpleFeatureType, SimpleFeature> shapefileWriter = null; 
			// Created once feature types are defined
		
		CachedRowSetImpl rif40Geolevels=getRif40Geolevels(connection, studyID, areaTableName);	
			//get geolevel
		String geolevel=getColumnFromResultSet(rif40Geolevels, "geolevel_id");
		String geolevelName = getColumnFromResultSet(rif40Geolevels, "geolevel_name");
		int max_geojson_digits=Integer.parseInt(getColumnFromResultSet(rif40Geolevels, "max_geojson_digits"));
		int srid=Integer.parseInt(getColumnFromResultSet(rif40Geolevels, "srid"));
		String geographyName = getColumnFromResultSet(rif40Geolevels, "geography");
		CoordinateReferenceSystem rif40GeographiesCRS = getCRS(rifStudySubmission, rif40Geolevels);
		MathTransform transform = null; // For re-projection
		if (!CRS.toSRS(rif40GeographiesCRS).equals(CRS.toSRS(DefaultGeographicCRS.WGS84))) {
			transform = rifCoordinateReferenceSystem.getMathTransform(rif40GeographiesCRS);
		}
		
		ReferencedEnvelope envelope=getMapReferencedEnvelope(connection, schemaName, areaTableName,tileTableName, 
			geolevel, zoomLevel, studyID, rif40GeographiesCRS, srid);
			
		SQLGeneralQueryFormatter queryFormatter = new SQLGeneralQueryFormatter();
		queryFormatter.addQueryLine(0, "WITH a AS (");
		queryFormatter.addQueryLine(0, "	SELECT *");
		queryFormatter.addQueryLine(0, "	  FROM rif40." + areaTableName);
		queryFormatter.addQueryLine(0, "	 WHERE study_id = ?");
		queryFormatter.addQueryLine(0, ")");
		queryFormatter.addQueryLine(0, "SELECT b.wkt");					
		if (extraColumns != null) {
			queryFormatter.addQueryLine(0, "      " + extraColumns);
		}
		queryFormatter.addQueryLine(0, "  FROM a");
		queryFormatter.addQueryLine(0, "        LEFT OUTER JOIN "  + 
															schemaName + "." + tileTableName.toLowerCase() + 
															" b ON (a.area_id = b.areaid)");												
		if (additionalJoin != null) {
			queryFormatter.addQueryLine(0, additionalJoin);
		}
		else {
			queryFormatter.addQueryLine(0, "LEFT OUTER JOIN rif_data.lookup_" + geolevelName + 
				" c ON (a.area_id = c." + geolevelName + ")");
		}
		queryFormatter.addQueryLine(0, " WHERE b.geolevel_id = ? AND b.zoomlevel = ?");		
		
		PreparedStatement statement = createPreparedStatement(connection, queryFormatter);	
		
		DefaultFeatureCollection featureCollection = null;
		SimpleFeatureType simpleFeatureType=null;
		
		try {	
			ResultSet resultSet = null;
			String[] queryArgs = new String[3];
			queryArgs[0]=studyID;
			queryArgs[1]=geolevel;
			queryArgs[2]=zoomLevel;
			logSQLQuery("writeMapQueryTogeoJSONFile", queryFormatter, queryArgs);
			statement = createPreparedStatement(connection, queryFormatter);
			statement.setInt(1, Integer.parseInt(studyID));	
			statement.setInt(2, Integer.parseInt(geolevel));
			statement.setInt(3, Integer.parseInt(zoomLevel));				
			resultSet = statement.executeQuery();
			
			//Write WKT to geoJSON
			int i = 0;
			
			rifLogger.debug(this.getClass(), "Bounding box: " + geoJSONWriter.toString((BoundingBox)envelope));
				// In 4236
			bufferedWriter.write("{\"type\":\"FeatureCollection\",");
			bufferedWriter.write("\"bbox\":" + geoJSONWriter.toString((BoundingBox)envelope) + ","); 
				// e.g. "bbox": [52.6876106262207,-7.588294982910156,55.52680969238281,-4.886538028717041],
			bufferedWriter.write("\"features\":[");	
		
			while (resultSet.next()) {
				StringBuffer stringFeature = new StringBuffer();
				
				ResultSetMetaData rsmd = resultSet.getMetaData();
				int columnCount = rsmd.getColumnCount();
				i++;
				
				Geometry geometry = createGeometryFromWkt(resultSet.getString(1));
				stringFeature.append("{\"type\":\"Feature\",\"geometry\":"); // GeoJSON feature header 	
				stringFeature.append(geoJSONWriter.toString(geometry));
				if (i == 1) {
					simpleFeatureType=setupShapefile(rsmd, columnCount, shapeDataStore, areaType, outputFileName, 
						geometry, rif40GeographiesCRS);
					
					shapefileWriter = shapeDataStore.getFeatureWriter(shapeDataStore.getTypeNames()[0],
							Transaction.AUTO_COMMIT);				
							
					if (areaType != null) {	
						featureCollection = new DefaultFeatureCollection(areaType, simpleFeatureType);
					}
					else {
						featureCollection = new DefaultFeatureCollection("Results", simpleFeatureType);
					}
				}
				SimpleFeature shapefileFeature = (SimpleFeature) shapefileWriter.next(); 
				SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureType);
				if (i == 1) {		
					printShapefileColumns(shapefileFeature, rsmd, outputFileName, rif40GeographiesCRS, geographyName, srid);
				}	
				
				AttributeDescriptor ad = shapefileFeature.getType().getDescriptor(0); // Create shapefile feature
					// Add first hsapefile feature attribute
				if (ad instanceof GeometryDescriptor) { 
					// Need to handle CoordinateReferenceSystem
					if (CRS.toSRS(rif40GeographiesCRS).equals(CRS.toSRS(DefaultGeographicCRS.WGS84))) {
						Geometries geomType = Geometries.get(geometry);
						switch (geomType) {
							case POLYGON: // Convert POLYGON to MULTIPOLYGON
								GeometryBuilder geometryBuilder = new GeometryBuilder(geometryFactory);
								Polygon polygons[] = new Polygon[1];
								polygons[0]=(Polygon)geometry;
								MultiPolygon multipolygon=geometryBuilder.multiPolygon(polygons);
								shapefileFeature.setAttribute(0, multipolygon); 
								builder.set(0, multipolygon); 
								break;
							case MULTIPOLYGON:
								shapefileFeature.setAttribute(0, geometry); 
								builder.set(0, geometry); 
								break;
							default:
								throw new Exception("Unsupported Geometry:" + geomType.toString());
						}
					} 
					else if (transform == null) {
						throw new Exception("Null transform from: " + CRS.toSRS(rif40GeographiesCRS) + " to: " +
							CRS.toSRS(DefaultGeographicCRS.WGS84));
					}
					else { // Transform from WGS84 to SRID CRS
						Geometry newGeometry = JTS.transform(geometry, transform); // Re-project
						shapefileFeature.setAttribute(0, newGeometry); 
						builder.set(0, newGeometry); 
					}
				} 
				else { 
					throw new Exception("First attribute is not MultiPolygon: " + 
						ad.getName().toString());
				}
				
				stringFeature.append(",\"properties\":{"); // Add DBF properties
				if (areaType != null) {
					stringFeature.append("\"areatype\":\"" + areaType + "\"");
					shapefileFeature.setAttribute(1, areaType); 
					builder.set(1, areaType); 
				}
				else {
					stringFeature.append("\"maptype\":\"Results\"");
					shapefileFeature.setAttribute(1, "results"); 
					builder.set(1, "results"); 
				}
				
				if (extraColumns != null) {
					
					// The column count starts from 2
					for (int j = 2; j <= columnCount; j++ ) {		
						String name = rsmd.getColumnName(j);
						String value = resultSet.getString(j);	
						String columnType = rsmd.getColumnTypeName(j);
						if (columnType.equals("timestamp") ||
							columnType.equals("timestamptz") ||
							columnType.equals("datetime")) {
							Timestamp dateTimeValue=resultSet.getTimestamp(i, calendar);
							value=df.format(dateTimeValue);
						}
						addDatumToShapefile(shapefileFeature, builder,
							stringFeature, name, value, columnType, j, i,
							locale);
					}
				}				
				
				featureCollection.add(builder.buildFeature("id" + i));		
				stringFeature.append("}");
				stringFeature.append("}");
				
				if (i > 1) {
					bufferedWriter.write(","); 	// Array separator between features
				}
				bufferedWriter.write(stringFeature.toString());	
				shapefileWriter.write();
			} // End of while loop
			
			bufferedWriter.write("]}"); // End FeatureCollection
		}
		catch (Exception exception) {
			rifLogger.error(this.getClass(), "Error in SQL Statement: >>> " + 
				lineSeparator + queryFormatter.generateQuery(),
				exception);
			throw exception;
		}
		finally {
			SQLQueryUtility.close(statement);

			bufferedWriter.flush();
			bufferedWriter.close();	
			if (shapefileWriter != null) {
				shapefileWriter.close();	
			}	
			connection.commit();
		}
		
		String backgroundAreasGeolevel=getColumnFromResultSet(rif40Geolevels, "bg_geolevel_id",
			true /* allowNulls */, false /*  allowNoRows */);
		String backgroundAreasGeolevelName=getColumnFromResultSet(rif40Geolevels, "bg_geolevel_name",
			true /* allowNulls */, false /*  allowNoRows */);
		DefaultFeatureCollection backgroundAreasFeatureCollection=null;
		if (backgroundAreasGeolevelName != null) {
			String backgroundAreasOutputFileName=backgroundAreasGeolevelName.toLowerCase() + "_map";
			
			backgroundAreasFeatureCollection=getBackgroundAreas(
				connection,
				rifStudySubmission,
				areaTableName,
				temporaryDirectory,
				dirName,
				schemaName,
				tileTableName,
				backgroundAreasGeolevelName,
				backgroundAreasOutputFileName,
				zoomLevel,
				backgroundAreasGeolevel,
				rif40GeographiesCRS,
				locale,
				transform,
				srid,
				geographyName);
		}
		
		RifFeatureCollection rifFeatureCollection=new RifFeatureCollection(
			featureCollection, 
			backgroundAreasFeatureCollection,
			rif40GeographiesCRS);
		rifFeatureCollection.SetupRifFeatureCollection();
		
		return rifFeatureCollection;
	}
	
	/** 
	 * Setup shapefile using feature builder. Defines field meta data and the coordinate reference 
	 * system, currently WGS84. Could use the SRID from rif40_geographies to use the original SRID.
	 * Supports POLYGONs, MULTIPOLYGONs, Double and Long. Everything else stays as String
	 *
	 * @param ResultSetMetaData rsmd, 
	 * @param int columnCount, 
	 * @param ShapefileDataStore dataStore, 
	 * @param String areaType, 
	 * @param String featureSetName, 
	 * @param Geometry geometry,
	 * @param CoordinateReferenceSystem crs
	 *
	 * @returns SimpleFeatureType
	 *
	 * https://www.programcreek.com/java-api-examples/index.php?source_dir=geotools-old-master/modules/library/render/src/test/java/org/geotools/renderer/lite/LabelObstacleTest.java
     */
	  private SimpleFeatureType setupShapefile(
			final ResultSetMetaData rsmd, 
			final int columnCount, 
			final ShapefileDataStore dataStore, 
			final String areaType, 
			final String featureSetName, 
			final Geometry geometry,
			final CoordinateReferenceSystem crs)
			throws Exception {
		
		SimpleFeatureTypeBuilder featureBuilder = new SimpleFeatureTypeBuilder();
		
		if (crs == null) {
			featureBuilder.setCRS(DefaultGeographicCRS.WGS84); // <- Default coordinate reference system
		}
		else if (CRS.toSRS(crs).equals(CRS.toSRS(DefaultGeographicCRS.WGS84))) {
			featureBuilder.setCRS(DefaultGeographicCRS.WGS84); // <- Default coordinate reference system
		}
		else {
			featureBuilder.setCRS(crs); // <- Geography SRID coordinate reference system
		}
        featureBuilder.setName(featureSetName);  
		Geometries geomType = Geometries.get(geometry);
		switch (geomType) {
			case POLYGON: // Force to multipolygon
				featureBuilder.add("geometry", MultiPolygon.class);
				break;
			case MULTIPOLYGON:
				featureBuilder.add("geometry", MultiPolygon.class);
				break;
			default:
				throw new Exception("Unsupported Geometry:" + geomType.toString());
		}
		featureBuilder.length(7).add("areatype", String.class);
		
		// The column count starts from 2
		for (int k = 2; k <= columnCount; k++ ) {
			String name = rsmd.getColumnName(k);
			String columnType = rsmd.getColumnTypeName(k);
			if (columnType.equals("integer") || 
				columnType.equals("bigint") || 
				columnType.equals("int4") ||
				columnType.equals("int") ||
				columnType.equals("smallint")) {	
				featureBuilder.add(name, Long.class);
			}				
			else if (columnType.equals("float") || 
					 columnType.equals("float8") || 
					 columnType.equals("double precision") ||
					 columnType.equals("numeric")) {
				featureBuilder.add(name, Double.class); // geotools uses double instead of float
			}
			else {
				featureBuilder.add(name, String.class);	
			}
		}								
		
		// build the type
		SimpleFeatureType featureType = featureBuilder.buildFeatureType();
		
		dataStore.createSchema(featureType);
		
		return featureType;
	}
	
	/**
	 * Add datum point to shapefile
	 *	
     * @param SimpleFeature shapefileFeature (required)
     * @param SimpleFeatureBuilder builder (required)
     * @param StringBuffer stringFeature (required)
     * @param String name (required)
     * @param String value (required)
     * @param String columnType (required)
     * @param int columnIndex (required)
     * @param int rowCount (required)
     * @param int Locale locale (required)
	 */	
	private void addDatumToShapefile(
		final SimpleFeature shapefileFeature, 
		final SimpleFeatureBuilder builder, 
		final StringBuffer stringFeature, 
		final String name, 
		final String value, 
		final String columnType, 
		final int columnIndex, 
		final int rowCount, 
		final Locale locale)
			throws Exception {
	
		AttributeDescriptor ad = shapefileFeature.getType().getDescriptor(columnIndex); 
		String featureName = ad.getName().toString();	
		if (rowCount < 2) {
			if (ad instanceof GeometryDescriptor) { 
				throw new Exception("Shapefile attribute is Geometry when expecting non geospatial type for column: " + 
					name + ", columnIndex: " + columnIndex + "; type: " + ad.getType().getBinding());
			}
			
			if (!name.equals(featureName)) {
				throw new Exception("Shapefile attribute name: " + featureName + 
					" does not match [truncated] column name: " + name + ", columnIndex: " + columnIndex + 
					"; type: " + ad.getType().getBinding());
			}
		}

		String newValue=value;
		Long longVal=null;
		Double doubleVal=null;
		if (value != null && (
			columnType.equals("integer") || 
			columnType.equals("bigint") || 
			columnType.equals("int4") ||
			columnType.equals("int") ||
			columnType.equals("smallint"))) {
			try {
				longVal=Long.parseLong(value);
				newValue=NumberFormat.getNumberInstance(locale).format(longVal);
			}
			catch (Exception exception) {
				rifLogger.error(this.getClass(), "Unable to parseLong(" + 
					columnType + "): " + value +
					"; row: " + rowCount +
					"; column: " + name + ", columnIndex: " + columnIndex,
					exception);
				throw exception;
			}
		}
		else if (value != null && (
				 columnType.equals("float") || 
				 columnType.equals("float8") || 
				 columnType.equals("double precision") ||
				 columnType.equals("numeric"))) {
			try {
				doubleVal=Double.parseDouble(value);
				newValue=NumberFormat.getNumberInstance(locale).format(doubleVal);
			}
			catch (Exception exception) {
				rifLogger.error(this.getClass(), "Unable to parseDouble(" + 
					columnType + "): " + value +
					"; row: " + rowCount +
					"; column: " + name + ", columnIndex: " + columnIndex,
					exception);
				throw exception;
			}
		}		
		
		if (stringFeature != null) {			
			stringFeature.append(",\"" + name + "\":\"" + newValue + "\"");
		}
		
		try {
			if (ad.getType().getBinding() == Double.class) {
				shapefileFeature.setAttribute(columnIndex, doubleVal);
				builder.set(columnIndex, doubleVal);
			}
			else if (ad.getType().getBinding() == Long.class) {
				shapefileFeature.setAttribute(columnIndex, longVal);
				builder.set(columnIndex, longVal);
			}
			else if (ad.getType().getBinding() == String.class) {
				shapefileFeature.setAttribute(columnIndex, newValue);
				builder.set(columnIndex, newValue);
			}
			else {
				throw new Exception("Unsupported attribute type: " + ad.getType().getBinding());
			}
		}
		catch (Exception exception) {
			rifLogger.error(this.getClass(), "Error in addDatumToShapefile() row: " + rowCount +
				"; column: " + name + ", columnIndex: " + columnIndex + 
				"; type: " + ad.getType().getBinding(),
				exception);
			throw exception;
		}
	} 
	
	/**
	 * Print shapefile and database cilumn names and types
	 *	
     * @param SimpleFeature feature (required),
     * @param ResultSetMetaData rsmd (required),
     * @param String outputFileName (required),
	 * @param CoordinateReferenceSystem crs,
	 * @param String geographyName,
	 * @param int srid
	 *
	 * E.g.
	 * 11:32:25.396 [http-nio-8080-exec-105] INFO  rifGenericLibrary.util.RIFLogger : [rifServices.dataStorageLayer.common.RifGeospatialOutputs]:
	 * Database: POSTGRESQL
	 * Column[1]: wkt; DBF name: WKT; truncated: false; type: text
	 * Column[2]: area_id; DBF name: AREA_ID; truncated: false; type: varchar
	 * Column[3]: band_id; DBF name: BAND_ID; truncated: false; type: int4
	 * Column[4]: zoomlevel; DBF name: ZOOMLEVEL; truncated: false; type: int4
	 * Column[5]: areaname; DBF name: AREANAME; truncated: false; type: varchar
	 * Shapefile: s367_1002_lung_cancer_studyArea
	 * Feature[0]: the_geom; type: GeometryTypeImpl MultiPolygon<MultiPolygon>
	 * Feature[1]: areatype; type: AttributeTypeImpl areatype<String>
	 * restrictions=[ length([.]) <= 7 ]
	 * Feature[2]: area_id; type: AttributeTypeImpl area_id<String>
	 * restrictions=[ length([.]) <= 254 ]
	 * Feature[3]: band_id; type: AttributeTypeImpl band_id<String>
	 * restrictions=[ length([.]) <= 254 ]
	 * Feature[4]: zoomlevel; type: AttributeTypeImpl zoomlevel<String>
	 * restrictions=[ length([.]) <= 254 ]
	 * Feature[5]: areaname; type: AttributeTypeImpl areaname<String>
	 * restrictions=[ length([.]) <= 254 ]
	 * 	
	 */
	private void printShapefileColumns(
		final SimpleFeature feature, 
		final ResultSetMetaData rsmd,
		final String outputFileName,
		final CoordinateReferenceSystem crs,
		final String geographyName,
		final int srid)
			throws Exception {
							
		StringBuilder sb = new StringBuilder();
		
		int truncatedCount=0;
		
		String crsWkt = crs.toWKT();
		sb.append("Geography: " + geographyName + "; SRID: " + srid + "; CRS wkt: " + crsWkt);
		sb.append("Database: " + databaseType + lineSeparator);
		for (int j = 1; j <= rsmd.getColumnCount(); j++) { 
			String name = rsmd.getColumnName(j);
			String dbfName = name.substring(0, Math.min(name.length(), 9)).toUpperCase(); // Trim to 10 chars
			boolean isTruncated=false;
			if (name.length() > 10) {
				isTruncated=true;
				truncatedCount++;
			}
			String columnType = rsmd.getColumnTypeName(j);
			sb.append("Column[" + j + "]: " + name +
				"; DBF name: " + dbfName +
				"; truncated: " + isTruncated +
				"; type: " + columnType + lineSeparator);
		}
		sb.append("Shapefile: " + outputFileName + lineSeparator);
		for (int k = 0; k < feature.getAttributeCount(); k++) { 			
			AttributeDescriptor ad = feature.getType().getDescriptor(k); 
			String featureName = ad.getName().toString();	
			String featureType = ad.getType().toString();
			sb.append("Feature[" + k + "]: " + featureName +
				"; type: " + featureType + lineSeparator);	
		}
		rifLogger.info(this.getClass(), sb.toString());
		if (truncatedCount > 0) {
			throw new Exception("Shapefile: " + outputFileName + 
				truncatedCount + " columns will be truncated; names will be unpredictable");
		}
	}

}	