package org.sahsu.rif.generic.system;

import java.util.ArrayList;

import org.sahsu.rif.generic.util.RIFLogger;

/**
 *<p>
 * The typed exception that is used by the RIF services project.  The class is 
 * designed to facilitate testing and reporting errors.  Each object may be
 * associated with multiple error messages, but it can only be given one 
 * error code.  
 * </p>
 * <p>
 * Multiple error messages are typically used in the <code>checkErrors()</code>
 * methods that appear in the business concept layer classes.  The 
 * enumerated error object is used in the automated test suites
 * so they can be precise in identifying the kind of exception that may be expected
 * by test cases that are exercising scenarios with errors.
 */

public class RIFServiceException extends Exception {

	private static final long serialVersionUID = 609449213280772202L;
	private static final RIFLogger rifLogger = RIFLogger.getLogger();

	/** The error. */
	private Object error;
	
	/** The error messages. */
	private ArrayList<String> errorMessages;

	/** The cause of this exception, if any */
	private Throwable cause;

	/**
	 * Instantiates a new RIF service exception.
	 *
	 * @param error the error
	 * @param errorMessage the error message
	 * @param cause the Throwable that caused this Exception
	 */
	public RIFServiceException(final Object error, final String errorMessage,
			final Throwable cause) {
		
		super(errorMessage, cause);
		
		this.error = error;
		this.cause = cause;
		errorMessages = new ArrayList<>();
		errorMessages.add(errorMessage);
	}
	
	public RIFServiceException(final String errorMessage, Throwable cause) {
			
		super(errorMessage, cause);
		this.cause = cause;
			
		errorMessages = new ArrayList<>();
		errorMessages.add(errorMessage);
	}

	/**
	 * Instantiates a new RIF service exception.
	 *
	 * @param error the error
	 * @param errorMessages the error messages
	 */
	public RIFServiceException(final Object error, final ArrayList<String> errorMessages,
			Throwable cause) {

		super(cause);
		this.cause = cause;
		this.error = error;
		this.errorMessages = new ArrayList<>();
		this.errorMessages.addAll(errorMessages);
	}

	public RIFServiceException(final ArrayList<String> errorMessages, Throwable cause) {

		super(cause);
		this.cause = cause;
		this.errorMessages = new ArrayList<>();
		this.errorMessages.addAll(errorMessages);
	}	

	/**
	 * Instantiates a new RIF service exception.
	 *
	 * @param error the error
	 * @param errorMessage the error message
	 */
	public RIFServiceException(
			final Object error,
			final String errorMessage) {

		super(errorMessage);

		this.error = error;
		errorMessages = new ArrayList<>();
		errorMessages.add(errorMessage);
	}

	public RIFServiceException(
			final String errorMessage) {

		super(errorMessage);

		errorMessages = new ArrayList<>();
		errorMessages.add(errorMessage);
	}

	/**
	 * Instantiates a new RIF service exception.
	 *
	 * @param error the error
	 * @param errorMessages the error messages
	 */
	public RIFServiceException(
			final Object error,
			final ArrayList<String> errorMessages) {

		this.error = error;
		this.errorMessages = new ArrayList<>();
		this.errorMessages.addAll(errorMessages);
	}

	public RIFServiceException(
			final ArrayList<String> errorMessages) {

		this.errorMessages = new ArrayList<>();
		this.errorMessages.addAll(errorMessages);
	}

	/**
	 * Gets the error.
	 *
	 * @return the error
	 */
	public Object getError() {

		return error;
	}

	/**
	 * Gets the error messages.
	 *
	 * @return the error messages
	 */
	public ArrayList<String> getErrorMessages() {

		return errorMessages;
	}

	/**
	 * Gets the error message count.
	 *
	 * @return the error message count
	 */
	public int getErrorMessageCount() {

		return errorMessages.size();
	}

	public void printErrors() {
		for (String errorMessage : errorMessages) {
			rifLogger.error(this.getClass(), (errorMessage));
		}
	}

	@Override
	public String toString() {

		String msg = "%d error(s). Error code is '%s'. Message list is: %s";
		StringBuilder msgs = new StringBuilder();

		for (String s : getErrorMessages()) {

			msgs.append("'").append(s).append("'").append(" | ");
		}

		// The regex below strips the last divider off the end of the messages list
		String detail =  String.format(msg, getErrorMessageCount(), getError(),
		                     msgs.toString().replaceAll(" \\| $", ""));

		if (cause == null) {

			return detail;
		} else {

			StringBuilder builder = new StringBuilder(detail)
					                        .append("\n\n")
					                        .append("=============================================")
					                        .append("\n")
					                        .append("Stack trace of cause follows")
					                        .append("\n")
					                        .append("=============================================")
					                        .append("\n");

			for (StackTraceElement element : cause.getStackTrace()) {

				builder.append(element.toString()).append("\n");
			}

			return builder.toString();
		}
	}
}