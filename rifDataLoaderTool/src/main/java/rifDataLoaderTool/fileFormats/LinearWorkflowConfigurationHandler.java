
package rifDataLoaderTool.fileFormats;


import rifDataLoaderTool.businessConceptLayer.DataSetConfiguration;
import rifDataLoaderTool.businessConceptLayer.LinearWorkflow;
import rifDataLoaderTool.businessConceptLayer.LinearWorkflowStateMachine;
import rifDataLoaderTool.businessConceptLayer.WorkflowState;


import rifServices.fileFormats.XMLCommentInjector;
import rifServices.fileFormats.XMLUtility;




import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;


/**
 *
 *
 *
 * <hr>
 * The Rapid Inquiry Facility (RIF) is an automated tool devised by SAHSU 
 * that rapidly addresses epidemiological and public health questions using 
 * routinely collected health and population data and generates standardised 
 * rates and relative risks for any given health outcome, for specified age 
 * and year ranges, for any given geographical area.
 *
 * <p>
 * Copyright 2014 Imperial College London, developed by the Small Area
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


final class LinearWorkflowConfigurationHandler 
	extends AbstractDataLoaderConfigurationHandler {

// ==========================================
// Section Constants
// ==========================================

// ==========================================
// Section Properties
// ==========================================
	
	private LinearWorkflow linearWorkflow;
	private DataSetConfigurationHandler dataSetConfigurationHandler;
		
// ==========================================
// Section Construction
// ==========================================
    /**
     * Instantiates a new disease mapping study content handler.
     */
	public LinearWorkflowConfigurationHandler() {
		
		setSingularRecordName("linear_workflow");
			
		dataSetConfigurationHandler = new DataSetConfigurationHandler();
	}


	@Override
	public void initialise(
		final OutputStream outputStream,
		final XMLCommentInjector commentInjector) 
		throws UnsupportedEncodingException {

		super.initialise(outputStream, commentInjector);
		dataSetConfigurationHandler.initialise(outputStream, commentInjector);
	}

	public void initialise(
		final OutputStream outputStream) 
		throws UnsupportedEncodingException {

		super.initialise(outputStream);
		dataSetConfigurationHandler.initialise(outputStream);
	}
	
	
// ==========================================
// Section Accessors and Mutators
// ==========================================
    	
	/**
	 * Gets the disease mapping study.
	 *
	 * @return the disease mapping study
	 */
	public LinearWorkflow getLinearWorkflow() {
		return linearWorkflow;
	}

	public void writeXML(
		final LinearWorkflow linearWorkflow)
		throws IOException {
			
		XMLUtility xmlUtility = getXMLUtility();
		xmlUtility.writeStartXML();		

		xmlUtility.writeRecordStartTag("linear_workflow");

		xmlUtility.writeField(
			"linear_workflow", 
			"start_workflow_state",
			linearWorkflow.getStartWorkflowState().getStateName());

		xmlUtility.writeField(
			"linear_workflow", 
			"stop_workflow_state", 
			linearWorkflow.getStopWorkflowState().getStateName());		
		
		ArrayList<DataSetConfiguration> dataSetConfigurations
			= linearWorkflow.getDataSetConfigurations();
		dataSetConfigurationHandler.writeXML(dataSetConfigurations);

		xmlUtility.writeRecordStartTag("linear_workflow");	
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
			

	@Override
    public void startElement(
		final String nameSpaceURI,
		final String localName,
		final String qualifiedName,
		final Attributes attributes) 
		throws SAXException {
		
		if (isSingularRecordName(qualifiedName)) {
			activate();
		}
		else if (isDelegatedHandlerAssigned()) {
			AbstractDataLoaderConfigurationHandler currentDelegatedHandler
				= getCurrentDelegatedHandler();
			currentDelegatedHandler.startElement(
				nameSpaceURI, 
				localName, 
				qualifiedName, 
				attributes);
		}
		else {
			
			//check to see if handlers could be assigned to delegate parsing			
			if (dataSetConfigurationHandler.isPluralRecordTypeApplicable(qualifiedName)) {
				assignDelegatedHandler(dataSetConfigurationHandler);
			}
						
			//delegate to a handler.  If not, then scan for fields relating to this handler
			if (isDelegatedHandlerAssigned()) {

				AbstractDataLoaderConfigurationHandler currentDelegatedHandler
					= getCurrentDelegatedHandler();
				currentDelegatedHandler.startElement(
					nameSpaceURI, 
					localName, 
					qualifiedName, 
					attributes);
			}
			else {
				assert false;
			}

		}
	}
	
	@Override
	public void endElement(
		final String nameSpaceURI,
		final String localName,
		final String qualifiedName) 
		throws SAXException {
		
		if (isSingularRecordName(qualifiedName)) {
			deactivate();
		}
		else if (isDelegatedHandlerAssigned()) {
			AbstractDataLoaderConfigurationHandler currentDelegatedHandler
				= getCurrentDelegatedHandler();
			currentDelegatedHandler.endElement(
				nameSpaceURI, 
				localName, 
				qualifiedName);
						
			if (currentDelegatedHandler.isActive() == false) {
				if (currentDelegatedHandler == dataSetConfigurationHandler) {
					ArrayList<DataSetConfiguration> dataSetConfigurations
						= dataSetConfigurationHandler.getDataSetConfigurations();
					linearWorkflow.setDataSetConfigurations(dataSetConfigurations);
				}
				else {
					assert false;
				}				
				
				//handler just finished				
				unassignDelegatedHandler();				
			}
		}
		else {
			assert false;
		}
	}
}
