package rifServices.test.performance.ms;

import rifGenericLibrary.businessConceptLayer.User;
import rifGenericLibrary.dataStorageLayer.DisplayableItemSorter;
import rifGenericLibrary.system.RIFServiceException;
import rifServices.test.services.ms.AbstractRIFServiceTestCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ArrayList;

import rifServices.businessConceptLayer.GeoLevelSelect;
import rifServices.businessConceptLayer.Geography;

public final class RIFStudySubmissionServiceCalls extends AbstractRIFServiceTestCase {

	private static final long NUMBER_REPETITIONS = 1000;
	

	public RIFStudySubmissionServiceCalls() {

	}

	@Test
	public void getGeoLevelSelectValues() throws RIFServiceException {

		User validUser = cloneValidUser();
		ArrayList<Geography> geographies
			= rifStudySubmissionService.getGeographies(validUser);
		assertEquals(3, geographies.size());
		
		DisplayableItemSorter sorter = new DisplayableItemSorter();
		for (Geography geography : geographies) {
			sorter.addDisplayableListItem(geography);
		}

		//the second one should be "SAHSU"
		Geography sahsuGeography
			= (Geography) sorter.sortList().get(1);
		
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < NUMBER_REPETITIONS; i++) {
			rifStudySubmissionService.getGeoLevelSelectValues(
				validUser,
				sahsuGeography);
		}
		long finishTime = System.currentTimeMillis();
		
		printPerformanceResult(
			"getGeoLevelSelectValues",
			startTime,
			finishTime);
	}

	@Test
	public void getGeoLevelAreaValues() throws RIFServiceException {

		User validUser = cloneValidUser();
		Geography validGeography = cloneValidGeography();
		GeoLevelSelect validGeoLevelSelectValue = cloneValidGeoLevelSelect();
		
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < NUMBER_REPETITIONS; i++) {
			rifStudySubmissionService.getGeoLevelAreaValues(
				validUser,
				validGeography,
				validGeoLevelSelectValue);
		}
		long finishTime = System.currentTimeMillis();

		printPerformanceResult(
			"getGeoLevelSelectValues",
			startTime,
			finishTime);
	}
	
	@Test
	public void getGeographies() throws RIFServiceException {

		//test check
		User validUser = cloneValidUser();
		
		long startTime = System.currentTimeMillis();
		
		for (int i = 0; i < NUMBER_REPETITIONS; i++) {
			rifStudySubmissionService.getGeographies(validUser);
		}

		long finishTime = System.currentTimeMillis();
		
		printPerformanceResult(
			"getGeographies",
			startTime,
			finishTime);
	}
	
	private void printPerformanceResult(
		final String methodName,
		long startTime,
		long finishTime) {
		
		double duration = finishTime - startTime;
		double averageRoundTripTime = (duration/(double) NUMBER_REPETITIONS);
		
		String message = "Method \""
		                 + methodName
		                 + "\""
		                 + " average time over "
		                 + NUMBER_REPETITIONS
		                 + " repetitions is "
		                 + averageRoundTripTime
		                 + " milliseconds.";
		
		System.out.println(message);
	}
}
