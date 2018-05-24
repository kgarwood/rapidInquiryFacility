package org.sahsu.rif.generic.taxonomyservices;

import java.util.ArrayList;


/**
 * Describes the concept of a taxonomy term.  It comprises the following fields:
 * <ul>
 * <li>label</li>
 * <ll>description</li>
 * <li>parentTerm</li>
 * <li>childTerms</li>
 * </ul>
 *
 *<p>
 * There are two aspects of design I considered in making the class.  Initially 
 * I wanted to add a machine-readable <code>identifier</code> field to complement
 * the human-readable <code>label</code> field.  To show the difference, consider
 * a term like "testosterone", which could mean a hormone or a steroid.  If you wanted
 * to have both contexts preserved in a taxonomy, you'd have two terms which had
 * different identifiers, the same human-readable labels, and different descriptions.
 * </p>
 * 
 * <p>
 * However, most of the taxonomies the RIF uses expose human users to the codes that would
 * seem better reserved for machines.  For example, "J45" is a label and "asthma" would
 * be a description.  Some users would actually look up and remember ICD codes.
 * </p>
 * 
 * <p>
 * Note that in the ICD codes, the different contexts of "asthma" are
 * reflected in different ICD codes (eg: J45.20 is "uncomplicated mild intermittent asthma" and
 * J45.30 is "uncomplicated mild persistent asthma").
 * </p>
 * 
 * <p>
 * We could have made "J45" as an identifier, "asthma" as a label, but then there may not be
 * a description.  To simplify the taxonomy services for the RIF, I decided to go with just
 * having label and description.
 * </p>
 * 
 * <p>
 * A taxonomy term should also have a name space, but rather than storing this information with
 * each term, I've made it a property of the taxonomy service.  For example, "icd10" could be
 * used to set a name space field in each term.  But instead, the name space is provided in the
 * <code>getIdentifier()</code> method of 
 * {@link TaxonomyServiceAPI}.
 * 
 * 
 * 
 * 
 * 
 * 
 * However, given the taxonomies that the
 * RIF would likely use, it seemed unnecessary.  
 * 
 * <p>
 * 
 * </p>
 *
 * <hr>
 * The Rapid Inquiry Facility (RIF) is an automated tool devised by SAHSU 
 * that rapidly addresses epidemiological and public health questions using 
 * routinely collected health and population data and generates standardised 
 * rates and relative risks for any given health outcome, for specified age 
 * and year ranges, for any given geographical area.
 *
 * <p>
 * Copyright 2017 Imperial College London, developed by the Small Area
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

final public class TaxonomyTerm {

	// ==========================================
	// Section Constants
	// ==========================================
	
	// ==========================================
	// Section Properties
	// ==========================================
	/** The label. */
	private String label;
	
	/** The name space. */
	private String nameSpace;
	
	/** The description. */
	private String description;
	
	/** The parent term. */
	private TaxonomyTerm parentTerm;
	
	/** The sub terms. */
	private ArrayList<TaxonomyTerm> childTerms;
	
	// ==========================================
	// Section Construction
	// ==========================================

	/**
	 * Instantiates a new taxonomy term.
	 */
	private TaxonomyTerm() {	
		
		childTerms = new ArrayList<TaxonomyTerm>();
		parentTerm = null;
	}

	/**
	 * New instance.
	 *
	 * @return the taxonomy term
	 */
	public static TaxonomyTerm newInstance() {
		
		TaxonomyTerm taxonomyTerm = new TaxonomyTerm();
		return taxonomyTerm;
	}
	
	/**
	 * Creates the shallow copy.
	 *
	 * @param originalTerm the original term
	 * @return the taxonomy term
	 */
	public static TaxonomyTerm createShallowCopy(
		final TaxonomyTerm originalTerm) {

		TaxonomyTerm copyTerm = new TaxonomyTerm();
		
		copyTerm.setLabel(originalTerm.getLabel());
		copyTerm.setDescription(originalTerm.getDescription());
		copyTerm.setNameSpace(originalTerm.getNameSpace());
		
		return copyTerm;
	}
	
	
	// ==========================================
	// Section Accessors and Mutators
	// ==========================================

	public boolean hasMatchingLabel(final String targetLabel) {
		return label.equals(targetLabel);		
	}
	
	public static boolean hasTermMatchingLabel(
		final ArrayList<TaxonomyTerm> taxonomyTerms,
		final String targetLabel) {
		
		for (TaxonomyTerm taxonomyTerm : taxonomyTerms) {
			if (taxonomyTerm.getLabel().equals(targetLabel)) {
				return true;
			}
		}
		
		return false;
		
	}
	
	/**
	 * Adds the sub term.
	 *
	 * @param subTerm the sub term
	 */
	public void addChildTerm(
		final TaxonomyTerm childTerm) {

		childTerms.add(childTerm);
	}

	/**
	 * Adds the sub terms.
	 *
	 * @param childTerms the sub terms
	 */
	public void addChildTerms(
		final ArrayList<TaxonomyTerm> childTerms) {

		this.childTerms.addAll(childTerms);
	}
	
	/**
	 * Gets the sub terms.
	 *
	 * @return the sub terms
	 */
	public ArrayList<TaxonomyTerm> getChildTerms() {
		
		return childTerms;
	}
	
	/**
	 * Returns the number of immediate child terms, so it doesn't count the whole
	 * tree of terms, just the number of nodes in the next tier of the taxonomy.
	 * @return
	 */
	public int getNumberOfChildTerms() {
		return childTerms.size();
	}
	
	public boolean isRootTerm() {
		if (parentTerm == null) {
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Gets the label.
	 *
	 * @return the label
	 */
	public String getLabel() {
		
		return label;
	}

	/**
	 * Sets the label.
	 *
	 * @param label the new label
	 */
	public void setLabel(
		final String label) {
		
		this.label = label;
	}

	/**
	 * Gets the name space.
	 *
	 * @return the name space
	 */
	public String getNameSpace() {
		
		return nameSpace;
	}

	/**
	 * Sets the name space.
	 *
	 * @param nameSpace the new name space
	 */
	public void setNameSpace(
		final String nameSpace) {

		this.nameSpace = nameSpace;
	}

	/**
	 * Gets the description.
	 *
	 * @return the description
	 */
	public String getDescription() {
		
		return description;
	}

	/**
	 * Sets the description.
	 *
	 * @param description the new description
	 */
	public void setDescription(final String description) {
		
		this.description = description;
	}
	
	/**
	 * Gets the parent term.
	 *
	 * @return the parent term
	 */
	public TaxonomyTerm getParentTerm() {
		return parentTerm;
	}
	
	/**
	 * Sets the parent term.
	 *
	 * @param parentTerm the new parent term
	 */
	public void setParentTerm(
		final TaxonomyTerm parentTerm) {
		
		this.parentTerm = parentTerm;
	}
	
	public String getIdentifier() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(label);
		buffer.append("-");
		buffer.append(nameSpace);
		return buffer.toString();
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
