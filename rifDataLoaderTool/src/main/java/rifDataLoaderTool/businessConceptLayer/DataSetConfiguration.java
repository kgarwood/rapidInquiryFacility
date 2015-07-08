package rifDataLoaderTool.businessConceptLayer;

import rifDataLoaderTool.system.RIFDataLoaderToolError;
import rifDataLoaderTool.system.RIFDataLoaderToolMessages;
import rifServices.system.RIFServiceMessages;
import rifServices.system.RIFServiceException;
import rifServices.system.RIFServiceSecurityException;
import rifServices.util.FieldValidationUtility;

import java.text.Collator;
import java.util.ArrayList;

/**
 *
 *
 * <hr>
 * Copyright 2015 Imperial College London, developed by the Small Area
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

public class DataSetConfiguration 
	extends AbstractRIFDataLoaderToolConcept {

	// ==========================================
	// Section Constants
	// ==========================================

	// ==========================================
	// Section Properties
	// ==========================================

	
	private String name;
	private String version;
	private String description;
	private RIFSchemaArea rifSchemaArea;
	private ArrayList<DataSetFieldConfiguration> fieldConfigurations;
	public WorkflowState currentWorkflowState;

	// ==========================================
	// Section Construction
	// ==========================================

	private DataSetConfiguration() {
		currentWorkflowState = WorkflowState.LOAD;
		version = "1.0";
	}

	/*
	 * Used when imported data does not specify field names
	 */
	public static DataSetConfiguration newInstance(
		final String name,
		final int numberOfFields) {
		
		DataSetConfiguration dataSetConfiguration
			= new DataSetConfiguration();
		
		ArrayList<DataSetFieldConfiguration> fieldConfigurations
			= new ArrayList<DataSetFieldConfiguration>();
		
		String baseDefaultFieldName
			= RIFDataLoaderToolMessages.getMessage(
				"dataSetFieldConfiguration.baseDefaultFieldName");
		for (int i = 0; i < numberOfFields; i++) {
			String defaultCoreFieldName
				= baseDefaultFieldName + String.valueOf(i + 1);
			DataSetFieldConfiguration dataSetFieldConfiguration
				= DataSetFieldConfiguration.newInstance(
					name, 
					defaultCoreFieldName);
			fieldConfigurations.add(dataSetFieldConfiguration);
		}
		
		dataSetConfiguration.setFieldConfigurations(fieldConfigurations);
		
		return dataSetConfiguration;
		
	}

	public static DataSetConfiguration newInstance() {
		DataSetConfiguration dataSetConfiguration
			= new DataSetConfiguration();
		return dataSetConfiguration;
	}

	public static DataSetConfiguration newInstance(
		final String name,
		final String[] fieldNames) {
		
		DataSetConfiguration dataSetConfiguration
			= new DataSetConfiguration();
		
		ArrayList<DataSetFieldConfiguration> fieldConfigurations
			= new ArrayList<DataSetFieldConfiguration>();
		
		for (String fieldName : fieldNames) {
			DataSetFieldConfiguration dataSetFieldConfiguration
				= DataSetFieldConfiguration.newInstance(
					name,
					fieldName);
			fieldConfigurations.add(dataSetFieldConfiguration);
		}
		
		dataSetConfiguration.setFieldConfigurations(fieldConfigurations);
		
		return dataSetConfiguration;		
	}

	public static DataSetConfiguration createCopy(
		final DataSetConfiguration originalDataSetConfiguration) {
		
		DataSetConfiguration cloneDataSetConfiguration
			= new DataSetConfiguration();
		cloneDataSetConfiguration.setName(
			originalDataSetConfiguration.getName());
		cloneDataSetConfiguration.setVersion(
			originalDataSetConfiguration.getVersion());
		cloneDataSetConfiguration.setDescription(
			originalDataSetConfiguration.getDescription());
		cloneDataSetConfiguration.setCurrentWorkflowState(
			originalDataSetConfiguration.getCurrentWorkflowState());
		
		ArrayList<DataSetFieldConfiguration> originalFieldConfigurations
			= originalDataSetConfiguration.getFieldConfigurations();
		ArrayList<DataSetFieldConfiguration> cloneFieldConfigurations
			= DataSetFieldConfiguration.createCopy(originalFieldConfigurations);		
		cloneDataSetConfiguration.setFieldConfigurations(cloneFieldConfigurations);
		
		return cloneDataSetConfiguration;
	}
		
	// ==========================================
	// Section Accessors and Mutators
	// ==========================================


	
	public String getName() {
		
		return name;
	}

	public void setName(
		final String name) {
		
		this.name = name;
	}

	public String getVersion() {
		return version;
	}
	
	public void setVersion(
		final String version) {

		this.version = version;
	}
	
	public String getDescription() {
		
		return description;
	}

	public void setDescription(
		final String description) {
		
		this.description = description;
	}

	public WorkflowState getCurrentWorkflowState() {
		return currentWorkflowState;
	}
	
	public void setCurrentWorkflowState(
		final WorkflowState currentWorkflowState) {
		
		this.currentWorkflowState = currentWorkflowState;
	}
	
	public RIFSchemaArea getRIFSchemaArea() {
		
		return rifSchemaArea;
	}

	public void setRIFSchemaArea(
		final RIFSchemaArea rifSchemaArea) {

		this.rifSchemaArea = rifSchemaArea;
	}

	public ArrayList<DataSetFieldConfiguration> getFieldConfigurations() {
		
		return fieldConfigurations;
	}

	public DataSetFieldConfiguration getFieldConfiguration(
		final int index) {
		
		return fieldConfigurations.get(index);		
	}
	
	public void setFieldConfigurations(
			final ArrayList<DataSetFieldConfiguration> fieldConfigurations) {
		this.fieldConfigurations = fieldConfigurations;
	}
	
	public String[] getConvertFieldNames() {
		ArrayList<String> convertFieldNames
			= new ArrayList<String>();
		
		for (DataSetFieldConfiguration fieldConfiguration : fieldConfigurations) {
			convertFieldNames.add(fieldConfiguration.getConvertFieldName());
		}
		
		String[] results
			= convertFieldNames.toArray(new String[0]);
		return results;
	}
	
	public int getTotalFieldCount() {
		return fieldConfigurations.size();
	}
	
	// ==========================================
	// Section Errors and Validation
	// ==========================================
	
	public void checkSecurityViolations() 
		throws RIFServiceSecurityException {
				
		String recordType = getRecordType();
			
		String nameLabel
			= RIFDataLoaderToolMessages.getMessage("dataSetConfiguration.name.label");
		String versionLabel
			= RIFDataLoaderToolMessages.getMessage("dataSetConfiguration.version.label");
		String descriptionLabel
			= RIFServiceMessages.getMessage("dataSetConfiguration.description.label");
			
		//Check for security problems.  Ensure EVERY text field is checked
		//These checks will throw a security exception and stop further validation
		FieldValidationUtility fieldValidationUtility
			= new FieldValidationUtility();
		fieldValidationUtility.checkMaliciousCode(
			recordType, 
			nameLabel, 
			name);
		
		fieldValidationUtility.checkMaliciousCode(
			recordType, 
			versionLabel, 
			version);
		
		
		fieldValidationUtility.checkMaliciousCode(
			recordType, 
			descriptionLabel, 
			description);

		for (DataSetFieldConfiguration fieldConfiguration : fieldConfigurations) {
			fieldConfiguration.checkSecurityViolations();
		}
	
	}
	
	public void checkErrors() 
		throws RIFServiceException {
		
		ArrayList<String> errorMessages = new ArrayList<String>();
		
		checkEmptyFields(errorMessages);
		
		countErrors(
			RIFDataLoaderToolError.INVALID_DATA_SET_CONFIGURATION, 
			errorMessages);
		
	}
	
	public void checkEmptyFields(
		final ArrayList<String> errorMessages) {
					
		FieldValidationUtility fieldValidationUtility
			= new FieldValidationUtility();

		
		if (fieldValidationUtility.isEmpty(name)) {
			String nameFieldLabel
				= RIFDataLoaderToolMessages.getMessage(
					"dataSetFieldConfiguration.name.label");
			String errorMessage
				= RIFDataLoaderToolMessages.getMessage(
					"general.validation.emptyRequiredField",
					nameFieldLabel);
			errorMessages.add(errorMessage);		
		}		

		
		if (fieldValidationUtility.isEmpty(version)) {
			String versionFieldLabel
				= RIFDataLoaderToolMessages.getMessage(
					"dataSetFieldConfiguration.version.label");
			String errorMessage
				= RIFDataLoaderToolMessages.getMessage(
					"general.validation.emptyRequiredField",
					versionFieldLabel);
			errorMessages.add(errorMessage);		
		}
				
		//description may be empty
		if (currentWorkflowState == null) {
			String currentWorkflowStateFieldLabel
				= RIFDataLoaderToolMessages.getMessage(
					"dataSetConfiguration.currentWorkflowState.label");
			String errorMessage
				= RIFDataLoaderToolMessages.getMessage(
					"general.validation.emptyRequiredField",
					currentWorkflowStateFieldLabel);
			errorMessages.add(errorMessage);
		}

		if (rifSchemaArea == null) {
			String currentWorkflowStateFieldLabel
				= RIFDataLoaderToolMessages.getMessage(
					"dataSetConfiguration.rifSchemaArea.label");
			String errorMessage
				= RIFDataLoaderToolMessages.getMessage(
					"general.validation.emptyRequiredField",
					currentWorkflowStateFieldLabel);
			errorMessages.add(errorMessage);						
		}
		
		if (fieldConfigurations.isEmpty()) {
			String currentWorkflowStateFieldLabel
				= RIFDataLoaderToolMessages.getMessage(
					"dataSetFieldConfiguration.plural.label");
			String errorMessage
				= RIFDataLoaderToolMessages.getMessage(
					"general.validation.emptyRequiredList",
					currentWorkflowStateFieldLabel);
			errorMessages.add(errorMessage);	
		}
			
		for (DataSetFieldConfiguration fieldConfiguration : fieldConfigurations) {
			fieldConfiguration.checkEmptyFields(errorMessages);
		}
	}
	
	public int getNumberOfCovariateFields() {
		
		int numberOfCovariateFields = 0;
		
		for (DataSetFieldConfiguration fieldConfiguration : fieldConfigurations) {
			if (fieldConfiguration.getFieldPurpose() == FieldPurpose.COVARIATE) {
				numberOfCovariateFields++;
			}
		}

		return numberOfCovariateFields;
	}
	
	public DataSetFieldConfiguration getFieldHavingConvertFieldName(
		final String convertFieldName) {
		
		Collator collator
			= RIFDataLoaderToolMessages.getCollator();
		
		for (DataSetFieldConfiguration fieldConfiguration : fieldConfigurations) {
			String currentFieldName
				= fieldConfiguration.getConvertFieldName();
			if (collator.equals(currentFieldName, convertFieldName)) {
				return fieldConfiguration;
			}
		}

		return null;
		
	}
	
	public int getNumberOfGeospatialFields() {
		int numberOfCovariateFields = 0;
				
		for (DataSetFieldConfiguration fieldConfiguration : fieldConfigurations) {
			if (fieldConfiguration.getFieldPurpose() == FieldPurpose.GEOGRAPHICAL_RESOLUTION) {
				numberOfCovariateFields++;
			}
		}

		return numberOfCovariateFields;
	}

	
	public int getNumberOfHealthCodeFields() {
		
		int numberOfHealthCodeFields = 0;
				
		for (DataSetFieldConfiguration fieldConfiguration : fieldConfigurations) {
			if (fieldConfiguration.getFieldPurpose() == FieldPurpose.HEALTH_CODE) {
				numberOfHealthCodeFields++;
			}
		}

		return numberOfHealthCodeFields;
	}	
	
	public String[] getFieldsUsedForDuplicationChecks() {
		
		ArrayList<String> duplicateCriteriaFieldNames = new ArrayList<String>();
		for (DataSetFieldConfiguration fieldConfiguration : fieldConfigurations) {
			if (fieldConfiguration.isDuplicateIdentificationField()) {
				duplicateCriteriaFieldNames.add(fieldConfiguration.getConvertFieldName());
			}			
		}
		
		String[] results
			= duplicateCriteriaFieldNames.toArray(new String[0]);
		return results;
		
	}

	public String[] getIndexFieldNames() {
		
		ArrayList<String> indexFieldNames = new ArrayList<String>();
		for (DataSetFieldConfiguration fieldConfiguration : fieldConfigurations) {
			if (fieldConfiguration.optimiseUsingIndex() == true) {
				indexFieldNames.add(fieldConfiguration.getConvertFieldName());
			}
		}
		
		String[] results
			= indexFieldNames.toArray(new String[0]);		
		return results;
		
	}
	
	public String[] getLoadFieldNames() {
		
		ArrayList<String> cleanFieldNames = new ArrayList<String>();
		for (DataSetFieldConfiguration fieldConfiguration : fieldConfigurations) {
			cleanFieldNames.add(fieldConfiguration.getLoadFieldName());
		}
		
		String[] results
			= cleanFieldNames.toArray(new String[0]);
		return results;
	}	
	
	public String[] getCleanFieldNames() {
		
		ArrayList<String> cleanFieldNames = new ArrayList<String>();
		for (DataSetFieldConfiguration fieldConfiguration : fieldConfigurations) {
			cleanFieldNames.add(fieldConfiguration.getCleanFieldName());
		}
		
		String[] results
			= cleanFieldNames.toArray(new String[0]);
		return results;
	}
	
	public String getDisplayName() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(name);
		buffer.append("-");
		buffer.append(version);
		return buffer.toString();		
	}
	
	public String getRecordType() {
		String recordType
			= RIFDataLoaderToolMessages.getMessage("dataSetConfiguration.recordType");
		return recordType;		
	}
		
	
	// ==========================================
	// Section Interfaces
	// ==========================================

	// ==========================================
	// Section Override
	// ==========================================

}


