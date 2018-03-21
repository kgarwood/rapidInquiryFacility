package rifServices.dataStorageLayer.common;

import java.sql.Connection;
import java.util.Locale;

import rifGenericLibrary.businessConceptLayer.User;
import rifGenericLibrary.system.Messages;
import rifGenericLibrary.system.RIFServiceException;
import rifGenericLibrary.util.FieldValidationUtility;
import rifGenericLibrary.util.RIFLogger;
import rifServices.businessConceptLayer.RIFStudySubmission;
import rifServices.dataStorageLayer.ms.MSSQLConnectionManager;
import rifServices.dataStorageLayer.ms.MSSQLRIFServiceResources;
import rifServices.dataStorageLayer.ms.MSSQLRIFSubmissionManager;
import rifServices.dataStorageLayer.ms.MSSQLStudyExtractManager;
import rifServices.system.RIFServiceMessages;

/**
 * Creates a study extract.
 *
 * <p>This code was extracted from
 * {@link rifServices.dataStorageLayer.ms.MSSQLRIFStudySubmissionService}.
 * </p>
 */
public class StudyExtract {
	
	private static final Messages SERVICE_MESSAGES = Messages.serviceMessages();
	
	private User user;
	private String studyID;
	private String zoomLevel;
	private Locale locale;
	private String tomcatServer;
	private MSSQLRIFServiceResources rifServiceResources;
	private Connection connection;
	
	public StudyExtract(User user, String studyID, String zoomLevel, Locale locale,
			String tomcatServer, MSSQLRIFServiceResources rifServiceResources) {
		
		this.user = user;
		this.studyID = studyID;
		this.zoomLevel = zoomLevel;
		this.locale = locale;
		this.tomcatServer = tomcatServer;
		this.rifServiceResources = rifServiceResources;
	}
	
	public void create() throws RIFServiceException {
		
		MSSQLConnectionManager sqlConnectionManager = rifServiceResources.getSqlConnectionManager();
		if (sqlConnectionManager.isUserBlocked(user)) {
			return;
		}
		
		RIFLogger rifLogger = RIFLogger.getLogger();
		try {

			FieldValidationUtility fieldValidationUtility = new FieldValidationUtility();
			fieldValidationUtility.checkNullMethodParameter("createStudyExtract",
					"user", user);
			fieldValidationUtility.checkNullMethodParameter("createStudyExtract",
					"studyID", studyID);
			fieldValidationUtility.checkNullMethodParameter("createStudyExtract",
					"zoomLevel", zoomLevel);

			//Check for security violations
			new ValidateUser(user, sqlConnectionManager).validate();
			fieldValidationUtility.checkMaliciousMethodParameter("createStudyExtract",
					"studyID", studyID);

			//Audit attempt to do operation
			String auditTrailMessage =
					SERVICE_MESSAGES.getMessage("logging.createStudyExtract",
							user.getUserID(), user.getIPAddress(), studyID, zoomLevel);
			rifLogger.info(getClass(), auditTrailMessage);

			connection = sqlConnectionManager.assignPooledWriteConnection(user);

			MSSQLRIFSubmissionManager sqlRIFSubmissionManager =
					rifServiceResources.getRIFSubmissionManager();
			RIFStudySubmission rifStudySubmission =
					sqlRIFSubmissionManager.getRIFStudySubmission(connection, user, studyID);

			MSSQLStudyExtractManager studyExtractManager =
					rifServiceResources.getSQLStudyExtractManager();
			studyExtractManager.createStudyExtract(connection, user, rifStudySubmission, zoomLevel,
					studyID, locale, tomcatServer);
			rifLogger.info(getClass(), "Create ZIP file completed OK");

		} catch(RIFServiceException rifServiceException) {

			new ExceptionLog(user, "createStudyExtract", rifServiceException,
					rifServiceResources, rifLogger).log();
		} finally {
			
			sqlConnectionManager.reclaimPooledWriteConnection(user, connection);
		}
	}
}