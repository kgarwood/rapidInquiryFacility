package rifDataLoaderTool.businessConceptLayer;

import rifDataLoaderTool.system.RIFDataLoaderToolMessages;

import java.util.ArrayList;

/**
 *
 *
 * <hr>
 * Copyright 2016 Imperial College London, developed by the Small Area
 * Health Statistics Unit. 
 *
 * <pre> 
 * This file is part of the Rapid Inquiry Facility (RIF) project.
 * RIF is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * RIF is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with RIF.  If not, see <http://www.gnu.org/licenses/>.
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

public class DataLoaderToolSettings {

	// ==========================================
	// Section Constants
	// ==========================================

	// ==========================================
	// Section Properties
	// ==========================================
	private RIFDatabaseConnectionParameters databaseConnectionParameters;
	private ArrayList<GeographicalResolutionLevel> geographicalResolutionLevels;
	private ArrayList<ShapeFile> shapeFiles;
	private RIFDataTypeFactory rifDataTypeFactory;
	
	// ==========================================
	// Section Construction
	// ==========================================

	public DataLoaderToolSettings() {
		
		databaseConnectionParameters
			= RIFDatabaseConnectionParameters.newInstance();
		
		geographicalResolutionLevels 
			= new ArrayList<GeographicalResolutionLevel>();

		String regionLevelName
			= RIFDataLoaderToolMessages.getMessage("geographicalResolutionLevel.region.label");
		String regionLevelDescription	
			= RIFDataLoaderToolMessages.getMessage("geographicalResolutionLevel.region.description");
		GeographicalResolutionLevel regionLevel
			= GeographicalResolutionLevel.newInstance(
				regionLevelName, 
				regionLevelDescription);
		geographicalResolutionLevels.add(regionLevel);		

		String districtLevelName
			= RIFDataLoaderToolMessages.getMessage("geographicalResolutionLevel.district.label");
		String districtLevelDescription	
			= RIFDataLoaderToolMessages.getMessage("geographicalResolutionLevel.district.description");
		GeographicalResolutionLevel districtLevel
			= GeographicalResolutionLevel.newInstance(
				districtLevelName, 
				districtLevelDescription);
		geographicalResolutionLevels.add(districtLevel);		
		
		String wardLevelName
			= RIFDataLoaderToolMessages.getMessage("geographicalResolutionLevel.ward.label");
		String wardLevelDescription	
			= RIFDataLoaderToolMessages.getMessage("geographicalResolutionLevel.ward.description");
		GeographicalResolutionLevel wardLevel
			= GeographicalResolutionLevel.newInstance(
				wardLevelName, 
				wardLevelDescription);
		geographicalResolutionLevels.add(wardLevel);
		
		String oaLevelName
			= RIFDataLoaderToolMessages.getMessage("geographicalResolutionLevel.oa.label");
		String oaLevelDescription	
			= RIFDataLoaderToolMessages.getMessage("geographicalResolutionLevel.oa.description");
		GeographicalResolutionLevel oaLevel
			= GeographicalResolutionLevel.newInstance(
				oaLevelName, 
				oaLevelDescription);
		geographicalResolutionLevels.add(oaLevel);
			
		String soaLevelName
			= RIFDataLoaderToolMessages.getMessage("geographicalResolutionLevel.soa.label");
		String soaLevelDescription	
			= RIFDataLoaderToolMessages.getMessage("geographicalResolutionLevel.soa.description");
		GeographicalResolutionLevel soaLevel
			= GeographicalResolutionLevel.newInstance(
				soaLevelName, 
				soaLevelDescription);
		geographicalResolutionLevels.add(soaLevel);
		
		rifDataTypeFactory = RIFDataTypeFactory.newInstance();
		rifDataTypeFactory.populateFactoryWithBuiltInTypes();
		
		shapeFiles = new ArrayList<ShapeFile>();
		
		
	}

	// ==========================================
	// Section Accessors and Mutators
	// ==========================================
		
	public RIFDatabaseConnectionParameters getDatabaseConnectionParameters() {		
		return databaseConnectionParameters;
	}
	
	public void setDatabaseConnectionParameters(final RIFDatabaseConnectionParameters databaseConnectionParameters) {
		this.databaseConnectionParameters = databaseConnectionParameters;
	}
	
	public ArrayList<GeographicalResolutionLevel> getGeographicalResolutionLevels() {
		return geographicalResolutionLevels;
	}
	
	public void setGeographicalResolutionLevels(final ArrayList<GeographicalResolutionLevel> geographicalResolutionLevels) {
		this.geographicalResolutionLevels = geographicalResolutionLevels;
	}
	
	public RIFDataTypeFactory getRIFDataTypeFactory() {
		return rifDataTypeFactory;
	}
	
	public void setRIFDataTypeFactory(final RIFDataTypeFactory rifDataTypeFactory) {
		this.rifDataTypeFactory = rifDataTypeFactory;
	}
	
	public void setShapeFiles(final ArrayList<ShapeFile> shapeFiles) {
		this.shapeFiles = shapeFiles;
	}
	
	public ArrayList<ShapeFile> getShapeFiles() {
		return shapeFiles;
	}
	
	public boolean areDatabaseConnectionSettingsValid() {
		return true;		
	}
	
	public boolean areGeographicalResolutionLevelsValid() {
		if (geographicalResolutionLevels.isEmpty() == false) {
			return true;
		}
		
		return false;
	}
	
	public boolean areShapeFileSettingsValid() {
		if (shapeFiles.isEmpty() == false) {
			return true;
		}
		
		return false;
	}
	
	public boolean areDataTypesValid() {
		ArrayList<RIFDataType> rifDataTypes
			= rifDataTypeFactory.getRegisteredDataTypes();
		if (rifDataTypes.isEmpty() == false) {
			return true;
		}
		
		return false;
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


