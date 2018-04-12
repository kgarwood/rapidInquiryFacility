package rifServices.dataStorageLayer.pg;

import rifServices.businessConceptLayer.RIFStudyResultRetrievalAPI;
import rifServices.businessConceptLayer.RIFStudySubmissionAPI;
import rifServices.system.RIFServiceStartupOptions;

public final class PGSQLProductionRIFStudyServiceBundle
	extends PGSQLAbstractStudyServiceBundle {

	public PGSQLProductionRIFStudyServiceBundle(final RIFServiceStartupOptions options,
			final RIFStudySubmissionAPI submissionService,
			final RIFStudyResultRetrievalAPI retrievalService) {

		super(options, submissionService, retrievalService);
		PGSQLProductionRIFStudySubmissionService rifStudySubmissionService
			= new PGSQLProductionRIFStudySubmissionService();
		setRIFStudySubmissionService(rifStudySubmissionService);

		PGSQLProductionRIFStudyRetrievalService rifStudyRetrievalService
			= new PGSQLProductionRIFStudyRetrievalService();
		setRIFStudyRetrievalService(rifStudyRetrievalService);
		
	}
}
