package rifServices.dataStorageLayer;

import rifServices.system.RIFServiceMessages;
import rifServices.system.RIFServiceStartupOptions;
import rifServices.businessConceptLayer.RIFStudySubmission;
import rifServices.businessConceptLayer.StudyState;
import rifServices.businessConceptLayer.StudyStateMachine;
import rifGenericLibrary.businessConceptLayer.User;
import rifGenericLibrary.system.RIFServiceException;
import rifGenericLibrary.dataStorageLayer.RIFDatabaseProperties;

import java.io.File;
import java.sql.*;

/**
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

public class RunStudyThread 
	implements Runnable {

	public static void main(String[] arguments) {
		
		try {
			
			TestRIFStudyServiceBundle testServiceBundle
				= TestRIFStudyServiceBundle.getRIFServiceBundle();
			
			RIFServiceStartupOptions rifServiceStartupOptions
				= RIFServiceStartupOptions.newInstance(false, false);
			rifServiceStartupOptions.setHost("wpea-rif1");
			testServiceBundle.initialise(rifServiceStartupOptions);
			
			RIFServiceResources rifServiceResources
				= testServiceBundle.getRIFServiceResources();
			SQLConnectionManager connectionManager
				= rifServiceResources.getSqlConnectionManager();
			User user = User.newInstance("kgarwood", "kgarwood");
			connectionManager.login(user.getUserID(), "kgarwood");
			Connection connection
				= connectionManager.assignPooledWriteConnection(user);
			
			SampleTestObjectGenerator sampleTestObjectGenerator
				= new SampleTestObjectGenerator();
			RIFStudySubmission studySubmission
				= sampleTestObjectGenerator.createSampleRIFJobSubmission();
				
			RunStudyThread runStudyThread = new RunStudyThread();
			
			runStudyThread.initialise(
				connection, 
				user, 
				studySubmission,
				rifServiceStartupOptions,
				rifServiceResources);								
			runStudyThread.run();
		}
		catch(Exception exception) {
			exception.printStackTrace(System.out);
		}
		
		
		
	}
	
	// ==========================================
	// Section Constants
	// ==========================================
	private static final int SLEEP_TIME = 200;
	// ==========================================
	// Section Properties
	// ==========================================
	private Connection connection;
	private User user;
	private SQLRIFSubmissionManager studySubmissionManager;
	private RIFStudySubmission studySubmission;
	private String studyID;
	
	private StudyStateMachine studyStateMachine;
	
	
	
	private SQLStudyStateManager studyStateManager;
	private SQLCreateStudySubmissionStep createStudySubmissionStep;
	private SQLGenerateResultsSubmissionStep generateResultsSubmissionStep;
	private SQLSmoothResultsSubmissionStep smoothResultsSubmissionStep;
	private SQLPublishResultsSubmissionStep publishResultsSubmissionStep;
	
	// ==========================================
	// Section Construction
	// ==========================================

	public RunStudyThread() {
		studyStateMachine = new StudyStateMachine();
		studyStateMachine.initialiseState();
	}
	
	// ==========================================
	// Section Accessors and Mutators
	// ==========================================
	public void initialise(
		final Connection connection,
		final User user,
		final RIFStudySubmission studySubmission,
		final RIFServiceStartupOptions rifServiceStartupOptions,
		final RIFServiceResources rifServiceResources) {
		
		
		this.connection = connection;
		this.user = user;				
		this.studySubmission = studySubmission;
		
		RIFDatabaseProperties rifDatabaseProperties 
			= rifServiceStartupOptions.getRIFDatabaseProperties();
		
		studyStateManager = new SQLStudyStateManager(rifDatabaseProperties);
		studySubmissionManager = rifServiceResources.getRIFSubmissionManager();
		
		createStudySubmissionStep 
			= new SQLCreateStudySubmissionStep(
				rifDatabaseProperties,
				rifServiceResources.getSqlDiseaseMappingStudyManager(),
				rifServiceResources.getSQLMapDataManager());

		generateResultsSubmissionStep
			= new SQLGenerateResultsSubmissionStep(rifDatabaseProperties);
		
		smoothResultsSubmissionStep = new SQLSmoothResultsSubmissionStep();
		smoothResultsSubmissionStep.initialise(
			"kgarwood", 
			"kgarwood", 
			rifServiceStartupOptions);

		String extractDirectory
			= rifServiceStartupOptions.getExtractDirectory();
		File scratchSpaceDirectory = new File(extractDirectory);		
		String extraDirectoryForExtractFilesPath
			= rifServiceStartupOptions.getExtraExtractFilesDirectoryPath();
		File extraDirectoryForExtractFiles 
			= new File(extraDirectoryForExtractFilesPath);

		publishResultsSubmissionStep
			= new SQLPublishResultsSubmissionStep();
		publishResultsSubmissionStep.initialise(
			scratchSpaceDirectory, 
			extraDirectoryForExtractFiles);
			
	}
		
	// ==========================================
	// Section Errors and Validation
	// ==========================================

	// ==========================================
	// Section Interfaces
	// ==========================================
	public void run() {
		try {

			establishCurrentStudyState();

			while (studyStateMachine.isFinished() == false) {
				System.out.println("RunStudyThread run current state 1==");

				StudyState currentState
					= studyStateMachine.getCurrentStudyState();
				System.out.println("RunStudyThread run current state has name=="+currentState.getCode()+"=="+currentState.getDescription()+"==");

				if (currentState == StudyState.STUDY_NOT_CREATED) {
					//Study has been extracted but neither the extract nor map tables
					//for that study have been produced.
					System.out.println("run create study BEFORE state=="+studyStateMachine.getCurrentStudyState().getName()+"==");
					createStudy();
					System.out.println("run create study AFTER state=="+studyStateMachine.getCurrentStudyState().getName()+"==");
				}
				else if (currentState == StudyState.STUDY_CREATED) {
					System.out.println("run generate results BEFORE state=="+studyStateMachine.getCurrentStudyState().getName()+"==");
					generateResults();
					System.out.println("run generate results AFTER state=="+studyStateMachine.getCurrentStudyState().getName()+"==");
				}
				else if (currentState == StudyState.STUDY_EXTRACTED) {
					//we are done.  Break out of the loop so that the thread can stop
					System.out.println("run smooth results BEFORE state=="+studyStateMachine.getCurrentStudyState().getName()+"==");
					smoothResults();
					System.out.println("run smooth results AFTER state=="+studyStateMachine.getCurrentStudyState().getName()+"==");
				}
				else {
					System.out.println("run advertise results BEFORE state=="+studyStateMachine.getCurrentStudyState().getName()+"==");
					advertiseDataSet();
					System.out.println("run advertise results AFTER state=="+studyStateMachine.getCurrentStudyState().getName()+"==");
					break;
				}
				System.out.println("About to go to sleep");
				
				Thread.sleep(SLEEP_TIME);
				System.out.println("About to wake up from a sleep");
			}
			
			System.out.println("Finished!!");
			
		}
		catch(InterruptedException interruptedException) {
			interruptedException.printStackTrace(System.out);

		}
		catch(RIFServiceException rifServiceException) {
			rifServiceException.printErrors();
		}
		finally {
			
			
		}
	}
	
	private void establishCurrentStudyState() 
		throws RIFServiceException {
		
		/*
		 * Determine the initial state of the study state machine.  If there is no
		 * study ID yet, then it means it hasn't even been created yet and the first
		 * step should be to call the create study submission step.  Otherwise, if it
		 * already exists, then the study state manager will be able to determine the
		 * state by checking RIF table status flags.
		 */
		if (studyID == null) {
			//it means it hasn't been created yet
			studyStateMachine.setCurrentStudyState(StudyState.STUDY_NOT_CREATED);				
		}
		else {
			StudyState studyState 
				= studyStateManager.getStudyState(
					connection, 
					user, 
					studyID);				
			studyStateMachine.setCurrentStudyState(studyState);
		}		
		
	}
	
	private void createStudy() 
		throws RIFServiceException {

		System.out.println("CREATE STUDY =====================================START =============================");
		studyID = createStudySubmissionStep.performStep(
			connection, 
			user, 
			studySubmission);
		
		studySubmissionManager.createStudyStatusTable(
			connection, 
			user.getUserID(), 
			studyID);		
		
		String statusMessage
			= RIFServiceMessages.getMessage(
				"studyState.studyCreated.description");
		studyStateManager.addStatusMessage(
			user, 
			studyID, 
			statusMessage);

		studyStateMachine.next();
		System.out.println("CREATE STUDY =====================================END =============================");
		
	}
	
	private void generateResults() 
		throws RIFServiceException {

		generateResultsSubmissionStep.performStep(
			connection, 
			studyID);
		
		String statusMessage
			= RIFServiceMessages.getMessage(
				"studyState.studyExtracted.description");
		studyStateManager.addStatusMessage(user, studyID, statusMessage);

		studyStateMachine.next();	
	}
	
	private void smoothResults() 
		throws RIFServiceException {

		smoothResultsSubmissionStep.performStep(
			connection,
			studySubmission, 
			studyID);

		String statusMessage
			= RIFServiceMessages.getMessage(
				"studyState.studyResultsComputed.description");
		studyStateManager.addStatusMessage(
			user, 
			studyID, 
			statusMessage);
				
		studyStateMachine.next();
	}
	
	private void advertiseDataSet() 
		throws RIFServiceException {
		
		//This is where we should save the study to a ZIP file
		publishResultsSubmissionStep.performStep(
			connection, 
			user, 
			studySubmission, 
			studyID);
		
		String statusMessage
			= RIFServiceMessages.getMessage(
				"studyState.readyForUse");
		studyStateManager.addStatusMessage(
			user, 
			studyID, 
			statusMessage);
		
		studyStateMachine.next();
		System.out.println("RIF study should be FINISHED!!");
	}
	
	
	// ==========================================
	// Section Override
	// ==========================================
}
