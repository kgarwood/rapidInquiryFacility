package rifServices.graphics;
/**
 *
 * <hr>
 * The Rapid Inquiry Facility (RIF) is an automated tool devised by SAHSU 
 * that rapidly addresses epidemiological and public health questions using 
 * routinely collected health and population data and generates standardised 
 * rates and relative risks for any given health outcome, for specified age 
 * and year ranges, for any given geographical area.
 *
 * Copyright 2017 Imperial College London, developed by the Small Area
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
 * Peter Hambly
 * @author phambly
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

public enum RIFGraphicsOutputType {
	RIFGRAPHICS_JPEG(1, true, false) {
		public String getRIFGraphicsOutputTypeDescription() {
			return "Joint Photographic Experts Group";
		}
		public String getGraphicsExtentsion() {
			return "jpg";
		}
	},
	RIFGRAPHICS_PNG(2, true, false) {
		public String getRIFGraphicsOutputTypeDescription() {
			return "Portable Network Graphics";
		}
		public String getGraphicsExtentsion() {
			return "png";
		}
	},
	RIFGRAPHICS_TIFF(3, false, false) {
		public String getRIFGraphicsOutputTypeDescription() {
			return "Tagged Image File Format";
		}
		public String getGraphicsExtentsion() {
			return "tif";
		}
	},
	RIFGRAPHICS_SVG(4, true, false) {
		public String getRIFGraphicsOutputTypeDescription() {
			return "Scalable Vector Graphics";
		}
		public String getGraphicsExtentsion() {
			return "svg";
		}
	},
	RIFGRAPHICS_EPS(5, true, true) {
		public String getRIFGraphicsOutputTypeDescription() {
			return "Encapsulated Postscript";
		}
		public String getGraphicsExtentsion() {
			return "eps";
		}
	},
	RIFGRAPHICS_PS(6, true, true) {
		public String getRIFGraphicsOutputTypeDescription() {
			return "Postscript";
		}
		public String getGraphicsExtentsion() {
			return "ps";
		}
	};
	
	private final int outputType;
	private final boolean enabled;
	private final boolean usesFop;
	
	RIFGraphicsOutputType(int outputType, boolean enabled, boolean usesFop) { // Constructor
		this.outputType=outputType;
		this.enabled=enabled;
		this.usesFop=usesFop;
	}
	
	public int getRIFGraphicsOutputType() { // Get method
		return outputType;
	}	
	
	public boolean isRIFGraphicsOutputTypeEnabled() { // Get method
		return enabled;
	}	
	
	public boolean doesRIFGraphicsOutputTypeUseFop() { // Get method
		return usesFop;
	}
	
	public String getRIFGraphicsOutputTypeShortName() {
		return name().replace("RIFGRAPHICS_", "");
	}
	
	public abstract String getGraphicsExtentsion();	
	public abstract String getRIFGraphicsOutputTypeDescription(); // ToString replacement

}