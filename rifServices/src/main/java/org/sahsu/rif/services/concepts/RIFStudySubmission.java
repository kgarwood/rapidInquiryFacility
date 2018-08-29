
package org.sahsu.rif.services.concepts;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import org.json.JSONObject;

import org.sahsu.rif.generic.system.Messages;
import org.sahsu.rif.generic.system.RIFServiceException;
import org.sahsu.rif.generic.system.RIFServiceSecurityException;
import org.sahsu.rif.generic.util.RIFLogger;
import org.sahsu.rif.services.system.RIFServiceError;
import org.sahsu.rif.services.system.RIFServiceMessages;

public final class RIFStudySubmission extends AbstractRIFConcept {

	private static final RIFLogger rifLogger = RIFLogger.getLogger();
	private static String lineSeparator = System.getProperty("line.separator");
	private static final Messages GENERIC_MESSAGES = Messages.genericMessages();

	/** The job submission time. */
	private Date jobSubmissionTime;
	
	/** The project. */
	private Project project;
	
	/** The study. */
	private AbstractStudy study;
	
	/** The calculation methods. */
	private ArrayList<CalculationMethod> calculationMethods;
	
	/** The rif output options. */
	private ArrayList<RIFOutputOption> rifOutputOptions;
	
	private JSONObject studySelection;
	
	   
    /**
     * Instantiates a new RIF job submission.
 	*/
	private RIFStudySubmission() {

    	jobSubmissionTime = new Date();
    	project = Project.newInstance();
		calculationMethods = new ArrayList<>();
		rifOutputOptions = new ArrayList<>();
		rifOutputOptions.add(RIFOutputOption.DATA);
		rifOutputOptions.add(RIFOutputOption.MAPS);
		rifOutputOptions.add(RIFOutputOption.POPULATION_HOLES);
		rifOutputOptions.add(RIFOutputOption.RATIOS_AND_RATES);
		studySelection = null;

	}

    /**
     * New instance.
     *
     * @return the RIF job submission
     */
    public static RIFStudySubmission newInstance() {

    	return new RIFStudySubmission();
    }
	
    /* studySelection get/set methods */
	public void setStudySelection(JSONObject studySelection) {
		this.studySelection = studySelection;
	}
	public JSONObject getStudySelection() {
		return(studySelection);
	}
	
    /**
     * Creates the copy.
     *
     * @param originalRIFJobSubmission the original rif job submission
     * @return the RIF job submission
     */
    public static RIFStudySubmission createCopy(
    	final RIFStudySubmission originalRIFStudySubmission) {
   
    	if (originalRIFStudySubmission == null) {
    		return null;
    	}
    	
    	RIFStudySubmission cloneRIFStudySubmission = new RIFStudySubmission();

    	Date originalJobSubmissionTime
    		= originalRIFStudySubmission.getJobSubmissionTime();
    	if (originalJobSubmissionTime != null) {
    		Date cloneJobSubmissionTime
    			= new Date(originalJobSubmissionTime.getTime());
    		cloneRIFStudySubmission.setJobSubmissionTime(cloneJobSubmissionTime);
    	}
    	
    	Project originalProject = originalRIFStudySubmission.getProject();
    	cloneRIFStudySubmission.setProject(Project.createCopy(originalProject)); 	
    	
    	AbstractStudy originalDiseaseMappingStudy = originalRIFStudySubmission.getStudy();
    	AbstractStudy cloneDiseaseMappingStudy =
			    AbstractStudy.createCopy(originalDiseaseMappingStudy);
    	cloneRIFStudySubmission.setStudy(cloneDiseaseMappingStudy);
    	
    	ArrayList<CalculationMethod> originalCalculationMethods
    		= originalRIFStudySubmission.getCalculationMethods(); 	
    	ArrayList<CalculationMethod> clonedCalculationMethods
    		= CalculationMethod.createCopy(originalCalculationMethods);
    	
    	cloneRIFStudySubmission.setCalculationMethods(clonedCalculationMethods);
   
		cloneRIFStudySubmission.setStudySelection(originalRIFStudySubmission.getStudySelection());
		
    	/*
    	ArrayList<RIFOutputOption> originalRIFOutputOptions
    		= originalRIFStudySubmission.getRIFOutputOptions();
    	for (RIFOutputOption originalRIFOutputOption : originalRIFOutputOptions) {
    		cloneRIFStudySubmission.addRIFOutputOption(originalRIFOutputOption);
    	}
    	*/
    	
    	return cloneRIFStudySubmission;

    }
    
    /**
     * Adds the calculation method.
     *
     * @param calculationMethod the calculation method
     */
    public void addCalculationMethod(
    	final CalculationMethod calculationMethod) {

    	calculationMethods.add(calculationMethod);
	}
	
	/**
	 * Gets the calculation methods.
	 *
	 * @return the calculation methods
	 */
	public ArrayList<CalculationMethod> getCalculationMethods() {
		
		return calculationMethods;
	}
	
	/**
	 * Gets the RIF output options.
	 *
	 * @return the RIF output options
	 */
	public ArrayList<RIFOutputOption> getRIFOutputOptions() {
		
		return rifOutputOptions;
	}
	
	/**
	 * Sets the calculation methods.
	 *
	 * @param calculationMethods the new calculation methods
	 */
	public void setCalculationMethods(
		final ArrayList<CalculationMethod> calculationMethods) {

		this.calculationMethods = calculationMethods;
	}
	
	/**
	 * Sets the project.
	 *
	 * @param project the new project
	 */
	public void setProject(
		final Project project) {

		this.project = project;
	}
	
	/**
	 * Gets the project.
	 *
	 * @return the project
	 */
	public Project getProject() {
		
		return project;
	}
	
	/**
	 * Gets the study.
	 *
	 * @return the study
	 */
	public AbstractStudy getStudy() {
		
		return study;
	}
	
	/**
	 * Sets the study.
	 *
	 * @param study the new study
	 */
	public void setStudy(
		final AbstractStudy study) {

		this.study = study;
	}

	/**
	 * Clear rif output options.
	 */
	public void clearRIFOutputOptions() {
		
		rifOutputOptions.clear();
	}
	
	/**
	 * Sets the RIF output options.
	 *
	 * @param rifOutputOptions the new RIF output options
	 */
	public void setRIFOutputOptions(
		final ArrayList<RIFOutputOption> rifOutputOptions) {

		this.rifOutputOptions = rifOutputOptions;
	}
	


	
	/**
	 * Gets the job submission time.
	 *
	 * @return the job submission time
	 */
	public Date getJobSubmissionTime() {
		
		return jobSubmissionTime;
	}
	
	/**
	 * Sets the job submission time.
	 *
	 * @param jobSubmissionTime the new job submission time
	 */
	public void setJobSubmissionTime(
		final Date jobSubmissionTime) {
		
		this.jobSubmissionTime = jobSubmissionTime;
	}

	public String getStudyID() {
		if (study == null) {
			return null;
		}
		else {
			return study.getIdentifier();
		}		
	}
	
	public void addStudyWarning(Class callingClass, String warningMessage) {
		
		// Add to rif40_warning_messages when available
		
		if (study == null) {	
			rifLogger.warning(callingClass, warningMessage); 
		}
		else {	
			rifLogger.warning(callingClass, "Study ID: " + study.getIdentifier() + lineSeparator +
				warningMessage);
		}
	}
	
	public void identifyDifferences(
		final RIFStudySubmission anotherStudySubmission,
		final ArrayList<String> differences) {
		
		super.identifyDifferences(
			anotherStudySubmission, 
			differences);		
	}

	public void checkSecurityViolations() 
		throws RIFServiceSecurityException {		

		super.checkSecurityViolations();
		
		project.checkSecurityViolations();
		study.checkSecurityViolations();
		
		for (CalculationMethod calculationMethod : calculationMethods) {
			calculationMethod.checkSecurityViolations();
		}
	}
	
	public void checkErrors(
		final ValidationPolicy validationPolicy) 
		throws RIFServiceException {	
		
		ArrayList<String> errorMessages = new ArrayList<>();
		
		String recordType = getRecordType();

		if (study == null) {
			String studyFieldName = RIFServiceMessages.getMessage("diseaseMappingStudy.label");
			String errorMessage = GENERIC_MESSAGES.getMessage(
					"general.validation.emptyRequiredRecordField",
					recordType,
					studyFieldName);
			errorMessages.add(errorMessage);			
		}
		else {
			try {
				study.checkErrors(validationPolicy);
			} catch(RIFServiceException rifServiceException) {

				rifLogger.debug(this.getClass(), "AbstractStudy.checkErrors(): " +
					rifServiceException.getErrorMessages().size());
				errorMessages.addAll(rifServiceException.getErrorMessages());
			}
		}

		if (calculationMethods == null) {
			String ageBandsFieldName
				= RIFServiceMessages.getMessage("calculationMethod.label.plural");
			String errorMessage
				= GENERIC_MESSAGES.getMessage(
					"general.validation.emptyRequiredRecordField",
					recordType,
					ageBandsFieldName);
			errorMessages.add(errorMessage);
		}

		boolean calculationMethodsAllNonNull = true;
		for (CalculationMethod calculationMethod : calculationMethods) {
			if (calculationMethod == null) {
				String calculationMethodRecordType
					= RIFServiceMessages.getMessage("calculationMethod.label");
				String errorMessage
					= RIFServiceMessages.getMessage(
						"general.validation.nullListItem",
						getRecordType(),
						calculationMethodRecordType);
				errorMessages.add(errorMessage);	
				calculationMethodsAllNonNull = false;
			}
			else {
				try {
					calculationMethod.checkErrors(validationPolicy);
				}
				catch(RIFServiceException rifServiceException) {
					rifLogger.debug(this.getClass(), "CalculationMethod.checkErrors(): " + 
						rifServiceException.getErrorMessages().size());
				}
			}
		}
		
		if (calculationMethodsAllNonNull) {
			HashSet<String> uniqueCalculationMethodNames = new HashSet<String>();
			for (CalculationMethod calculationMethod : calculationMethods) {
				try {
					String displayName = calculationMethod.getDisplayName();
					if (uniqueCalculationMethodNames.contains(displayName)) {
						String errorMessage
							= RIFServiceMessages.getMessage(
								"rifStudySubmission.error.duplicateCalculationMethod", 
								displayName);
						errorMessages.add(errorMessage);
					}
					else {
						uniqueCalculationMethodNames.add(displayName);
					}
					calculationMethod.checkErrors(validationPolicy);				
				}
				catch(RIFServiceException rifServiceException) {
					rifLogger.debug(this.getClass(), "[Unique] CalculationMethod.checkErrors(): " + 
						rifServiceException.getErrorMessages().size());
					errorMessages.addAll(rifServiceException.getErrorMessages());				
				}
			}
		}
		
		if (rifOutputOptions == null) {
			String rifOutputOptionsFieldName
				= RIFServiceMessages.getMessage("rifOutputOption.plural.label");
			String errorMessage
				= GENERIC_MESSAGES.getMessage(
					"general.validation.emptyRequiredRecordField",
					recordType,
					rifOutputOptionsFieldName);
			errorMessages.add(errorMessage);			
		}
		else if (rifOutputOptions.isEmpty()) {
			String errorMessage
				= RIFServiceMessages.getMessage(
					"rifStudySubmission.error.noRIFOutputOptionsSpecified");
			errorMessages.add(errorMessage);			
		} else {
			
			for (RIFOutputOption option : rifOutputOptions) {
				
				if (option == null) {
					
					String errorMessage = RIFServiceMessages.getMessage(
						"rifStudySubmission.error.nullRIFOutputOptionSpecified");
					errorMessages.add(errorMessage);
				}
			}
		}
		
		if (errorMessages.size() > 0) {
			for (int i = 0; i < errorMessages.size(); i++) {
				rifLogger.warning(this.getClass(), "JSON parse error [" + (i+1) + "/" + errorMessages.size() +"]: " + 
					errorMessages.get(i));
			}
		}
		else if (study != null) {
			rifLogger.info(this.getClass(), "JSON parse OK for " + study.getName());
		}
		else {
			errorMessages.add("JSON parse OK but study object is null");
			Exception exception=new Exception("JSON parse OK but study object is null");
			rifLogger.error(this.getClass(), "JSON parse OK but study object is null", exception);
		}

		countErrors(
				RIFServiceError.INVALID_RIF_JOB_SUBMISSION,
				errorMessages);
	}
	
	@Override
	public String getDisplayName() {
		return study.getDisplayName();
	}
	

	@Override
	public String getRecordType() {

		return RIFServiceMessages.getMessage("rifStudySubmission.label");
	}
	
}
