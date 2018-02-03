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
 *
 * SQL statement name: 	denominatorReport.sql
 * Type:				Postgres SQL
 * Parameters (preceded by %):
 *						1: Extract table name; e.g. s367_extract
 *						2: Min year
 *						3: Max year
 *						4: Min gender
 *						5: Max gender
 *
 * Description:			Denominator SQL report. Handles gendes: males, females or both
 *						Missing years of data appear as blanks
 * Note:				NO SUPPORT FOR ESCAPING!
 *						Dependency is generate_series()
 */
WITH x AS (
	SELECT generate_series(%2, %3) AS year
), y AS (
	SELECT generate_series(%4, %5) AS sex
), a AS (
	SELECT x.year, 
	       y.sex,
		   SUM(c.total_pop) AS c_total_pop,
		   SUM(s.total_pop) AS s_total_pop
	  FROM x CROSS JOIN y
				FULL OUTER JOIN %1 c ON (
					c.sex = y.sex AND c.year = x.year AND c.study_or_comparison = 'C')
				FULL OUTER JOIN %1 s ON (
					y.sex = s.sex AND x.year = s.year AND s.study_or_comparison = 'S')
	 GROUP BY x.year, y.sex
) 
SELECT a.year, 
       a.c_total_pop comparison_males, 
       b.c_total_pop comparison_females, 
	   a.c_total_pop + b.c_total_pop AS comparison_both,
	   a.s_total_pop AS study_males, 
	   b.s_total_pop AS study_females,
	   a.s_total_pop + b.s_total_pop AS study_both
  FROM a a
			FULL OUTER JOIN a b ON (
				a.year = b.year AND b.sex = 2 /* Females */) 
 WHERE a.sex = 1 /* Males */
 ORDER BY year;
 