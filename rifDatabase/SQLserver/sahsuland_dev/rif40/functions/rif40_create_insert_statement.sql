
-- ************************************************************************
--
-- Description:
--
-- Rapid Enquiry Facility (RIF) - RIF40 run study - create insert statement
--
-- Copyright:
--
-- The Rapid Inquiry Facility (RIF) is an automated tool devised by SAHSU 
-- that rapidly addresses epidemiological and public health questions using 
-- routinely collected health and population data and generates standardised 
-- rates and relative risks for any given health outcome, for specified age 
-- and year ranges, for any given geographical area.
--
-- Copyright 2014 Imperial College London, developed by the Small Area
-- Health Statistics Unit. The work of the Small Area Health Statistics Unit 
-- is funded by the Public Health England as part of the MRC-PHE Centre for 
-- Environment and Health. Funding for this project has also been received 
-- from the Centers for Disease Control and Prevention.  
--
-- This file is part of the Rapid Inquiry Facility (RIF) project.
-- RIF is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Lesser General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.
--
-- RIF is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
-- GNU Lesser General Public License for more details.
--
-- You should have received a copy of the GNU Lesser General Public License
-- along with RIF. If not, see <http://www.gnu.org/licenses/>; or write 
-- to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, 
-- Boston, MA 02110-1301 USA
--
-- Author:
--
-- Peter Hambly, SAHSU
--
-- Error codes:  ..\..\error_handling\rif40_custom_error_messages.sql
--
IF EXISTS (SELECT *
           FROM   sys.objects
           WHERE  object_id = OBJECT_ID(N'[rif40].[rif40_create_insert_statement]')
                  AND type IN ( N'P' ))
	DROP PROCEDURE [rif40].[rif40_create_insert_statement]
GO 

CREATE PROCEDURE [rif40].[rif40_create_insert_statement](@rval INT OUTPUT, @study_id INT, 
	@study_or_comparison VARCHAR(1), @yearno INT, @debug INT=0, @year_start INTEGER=NULL, @year_stop INTEGER=NULL)
AS
BEGIN
/*
Function:	rif40_create_insert_statement()
Parameter:	Success or failure [INTEGER], Study ID, study or comparison (S/C), year_start, year_stop
Returns:	Success or failure [INTEGER], as  first parameter
Description:	Create AND EXECUTE INSERT SQL statement
 */

--
-- Defaults if set to NULL
--
	IF @debug IS NULL SET @debug=0;
	
	SET @rval=1; 	-- Success
	
	DECLARE c1insext CURSOR FOR
		SELECT study_id, extract_table, comparison_geolevel_name, study_geolevel_name, min_age_group, max_age_group
		  FROM rif40_studies a
		 WHERE a.study_id = @study_id;
	DECLARE @c1_rec_study_id 					INTEGER;
	DECLARE @c1_rec_extract_table 				VARCHAR(30);
	DECLARE @c1_rec_comparison_geolevel_name 	VARCHAR(30);
	DECLARE @c1_rec_study_geolevel_name 		VARCHAR(30);
	DECLARE @c1_rec_min_age_group 				INTEGER;
	DECLARE @c1_rec_max_age_group 				INTEGER;
	
	DECLARE c3insext CURSOR FOR
		SELECT COUNT(DISTINCT(numer_tab)) AS distinct_numerators
		  FROM rif40_investigations a
		 WHERE a.study_id = @study_id;
	DECLARE @c3_rec_distinct_numerators 		INTEGER;
	DECLARE c4insext CURSOR FOR
		WITH b AS (
			SELECT DISTINCT(a.numer_tab) AS numer_tab
			  FROM rif40_investigations a
		 	 WHERE a.study_id   = @study_id
		)
		SELECT b.numer_tab,
 		       t.description, t.age_sex_group_field_name, t.year_start, t.year_stop, t.total_field /*, 
   		       t.sex_field_name, t.age_group_field_name, t.age_group_id */
		  FROM b, rif40_tables t 
		 WHERE t.table_name = b.numer_tab;
	DECLARE @c4_rec_numer_tab 					VARCHAR(30);
	DECLARE @c4_rec_description 				VARCHAR(2000);
	DECLARE @c4_rec_age_sex_group_field_name 	VARCHAR(30);
	DECLARE @c4_rec_year_start					INTEGER;
	DECLARE	@c4_rec_year_stop 					INTEGER;
	DECLARE @c4_rec_total_field				 	VARCHAR(30);
		
		 /*
	c7insext CURSOR(l_study_id INTEGER) FOR
		SELECT DISTINCT a.covariate_name AS covariate_name, b.covariate_table AS covariate_table_name
		  FROM rif40_inv_covariates a
				LEFT OUTER JOIN rif40_geolevels b ON (a.study_geolevel_name = b.geolevel_name)
		 WHERE a.study_id = l_study_id
 		 ORDER BY a.covariate_name;
*/
	DECLARE c8insext CURSOR FOR
		SELECT b.denom_tab,
 		       t.description, t.total_field, t.year_start, t.year_stop,
   		       t.sex_field_name, t.age_group_field_name, t.age_sex_group_field_name, t.age_group_id, 
		       MIN(a.offset) AS min_age_group, MAX(a.offset) AS max_age_group
		  FROM rif40_studies b, rif40_tables t, rif40_age_groups a
		 WHERE t.table_name   = b.denom_tab
		   AND t.age_group_id = a.age_group_id
		   AND b.study_id     = @study_id
		 GROUP BY b.denom_tab,
 		       t.description, t.total_field, t.year_start, t.year_stop,
   		       t.sex_field_name, t.age_group_field_name, t.age_sex_group_field_name, t.age_group_id;
	DECLARE @c8_rec_denom_tab					VARCHAR(30);
	DECLARE @c8_rec_description					VARCHAR(2000);
	DECLARE @c8_rec_total_field					INTEGER;
	DECLARE @c8_rec_year_start					INTEGER;
	DECLARE @c8_rec_year_stop					INTEGER;
	DECLARE @c8_rec_sex_field_name				VARCHAR(30);
	DECLARE @c8_rec_age_group_field_name		VARCHAR(30);
	DECLARE @c8_rec_age_sex_group_field_name	VARCHAR(30);
	DECLARE @c8_rec_age_group_id				INTEGER;		
	DECLARE @c8_rec_min_age_group				INTEGER;		
	DECLARE @c8_rec_max_age_group				INTEGER;		   
	
	/*
	c7_rec RECORD;
--
	covariate_table_name	VARCHAR;
-- */

	DECLARE @inv_array 		TABLE (inv VARCHAR(MAX));
	DECLARE @inv_join_array TABLE (outer_join VARCHAR(MAX));
	DECLARE @i			INTEGER=0;
	DECLARE @j			INTEGER=0;
	DECLARE @k			INTEGER=0;
--
	DECLARE @sql_stmt		NVARCHAR(MAX);
	DECLARE @ddl_stmts 	Sql_stmt_table;
	DECLARE @t_ddl		INTEGER=0;
--
	DECLARE @crlf  		VARCHAR(2)=CHAR(10)+CHAR(13);
	DECLARE @tab		VARCHAR(1)=CHAR(9);
	DECLARE @err_msg 	VARCHAR(MAX);
	DECLARE @msg	 	VARCHAR(MAX);
	
	DECLARE @areas_table	VARCHAR(30)='g_rif40_study_areas';
--
-- Use different areas_table for comparison (it has no band_id)
--	
	IF @study_or_comparison = 'C' SET @areas_table='g_rif40_comparison_areas';
--
	OPEN c1insext;	
	FETCH NEXT FROM c1insext INTO @c1_rec_study_id, @c1_rec_extract_table, 
		@c1_rec_comparison_geolevel_name, @c1_rec_study_geolevel_name, @c1_rec_min_age_group, @c1_rec_max_age_group;
	IF @@CURSOR_ROWS = 0 BEGIN
		CLOSE c1insext;
		DEALLOCATE c1insext;
		SET @err_msg = formatmessage(56000, @study_id); -- Study ID %i not found
		THROW 56000, @err_msg, 1;
	END;
	CLOSE c1insext;
	DEALLOCATE c1insext;

--
-- Create INSERT statement
-- 
	SET @sql_stmt='INSERT INTO ' + LOWER(@c1_rec_extract_table) + ' (' + @crlf;	

--
-- Add columns
-- 
	DECLARE c2insext CURSOR FOR
		SELECT column_name
		  FROM information_schema.columns a
		 WHERE a.table_schema = 'rif_studies'
		   AND a.table_name   = LOWER(@c1_rec_extract_table)
		 ORDER BY a.ordinal_position;			    
	DECLARE @c2_rec_column_name 	VARCHAR(30);
	SET @i=0;
	OPEN c2insext;
	FETCH NEXT FROM c2insext INTO @c2_rec_column_name;
	WHILE @@FETCH_STATUS = 0
	BEGIN
		SET @i=@i+1;
		IF @i = 1 SET @sql_stmt=@sql_stmt + @tab + @c2_rec_column_name
		ELSE SET @sql_stmt=@sql_stmt + ',' + @c2_rec_column_name;
--
		FETCH NEXT FROM c2insext INTO @c2_rec_column_name;
	END;
	CLOSE c2insext;
	DEALLOCATE c2insext;
--
	IF @i = 0 BEGIN
		SET @err_msg = formatmessage(56001, @study_id, @c1_rec_extract_table); -- Study ID %i no columns found for extract table: %s
		THROW 56001, @err_msg, 1;
	END;

--
-- Get number of distinct numerators
--
	OPEN c3insext;
	FETCH NEXT FROM c3insext INTO @c3_rec_distinct_numerators;
	IF @@CURSOR_ROWS = 0 BEGIN
		CLOSE c3insext;
		DEALLOCATE c3insext;
		SET @err_msg = formatmessage(56000, @study_id); -- Study ID %i not found
		THROW 56000, @err_msg, 1;
	END;
	CLOSE c3insext;
	DEALLOCATE c3insext;
	SET @sql_stmt=@sql_stmt + ') /* '+ CAST(@c3_rec_distinct_numerators AS VARCHAR) + ' numerator(s) */' + @crlf;
	
--
-- Get denominator setup
--
	OPEN c8insext;
	FETCH NEXT FROM c8insext INTO @c8_rec_denom_tab, @c8_rec_description, @c8_rec_total_field, @c8_rec_year_start, @c8_rec_year_stop,
   		       @c8_rec_sex_field_name, @c8_rec_age_group_field_name, @c8_rec_age_sex_group_field_name, @c8_rec_age_group_id,
			   @c8_rec_min_age_group, @c8_rec_max_age_group;
	IF @@CURSOR_ROWS = 0 BEGIN
		CLOSE c8insext;
		DEALLOCATE c8insext;
		SET @err_msg = formatmessage(56000, @study_id); -- Study ID %i not found
		THROW 56000, @err_msg, 1;
	END;
	CLOSE c8insext;
	DEALLOCATE c8insext;

--
-- Loop through distinct numerators
--
	SET @i=0;
	OPEN c4insext;
	FETCH NEXT FROM c4insext INTO @c4_rec_numer_tab, @c4_rec_description, @c4_rec_age_sex_group_field_name, 
		@c4_rec_year_start, @c4_rec_year_stop, @c4_rec_total_field;
	WHILE @@FETCH_STATUS = 0
	BEGIN
		SET @i=@i+1;
		
--
-- Open WITH clause (common table expression)
-- 
		IF @i = 1 SET @sql_stmt=@sql_stmt + 'WITH n' + CAST(@i AS VARCHAR) + ' AS (' + @tab + 
			'/* ' + @c4_rec_numer_tab + ' - ' + @c4_rec_description + ' */' + @crlf
		ELSE SET @sql_stmt=@sql_stmt + ', n' + CAST(@i AS VARCHAR) + ' AS (' + @tab + 
			'/* ' + @c4_rec_numer_tab + ' - ' + @c4_rec_description + ' */' + @crlf;
			
--
-- Numerator JOINS
--
		INSERT INTO @inv_join_array(outer_join) VALUES (@tab + 'LEFT OUTER JOIN n' + CAST(@i AS VARCHAR) + ' ON ( ' + @crlf +
			@tab + '/* ' + @c4_rec_numer_tab + ' - ' + @c4_rec_description + ' */' + @crlf +
			@tab + @tab + '    d.area_id'+ @tab + @tab + ' = n' + CAST(@i AS VARCHAR) + '.area_id' + @crlf +
			@tab + @tab + 'AND d.year' + @tab + @tab + ' = n' + CAST(@i AS VARCHAR) + '.year' + @crlf +
--
-- [Add conversion support for differing age/sex/group names; convert to AGE_SEX_GROUP]
--
			@tab + @tab + 'AND d.' + LOWER(@c8_rec_age_sex_group_field_name) + @tab + ' = n' + CAST(@i AS VARCHAR) + '.n_age_sex_group)');
				/* List of numerator joins (for use in FROM clause) */;

		SET @sql_stmt=@sql_stmt + @tab + 'SELECT s.area_id' + @tab + @tab + '/* Study or comparision resolution */,' + @crlf +
			@tab + '       c.year,' + @crlf;
--
-- [Add support for differing age/sex/group names; convert to AGE_SEX_GROUP]
--
		SET @sql_stmt=@sql_stmt + @tab + '       c.' + LOWER(@c4_rec_age_sex_group_field_name) + ' AS n_age_sex_group,' + @crlf;				

--
-- Individual investigations [add age group/sex/year filters]
-- 
		DECLARE c5insext CURSOR FOR
			SELECT inv_id, inv_name, year_start, year_stop, genders, min_age_group, max_age_group, inv_description
			  FROM rif40_investigations a
			 WHERE a.study_id  = @study_id
			   AND a.numer_tab = @c4_rec_numer_tab
			 ORDER BY inv_id;
		DECLARE @c5_rec_inv_id			INTEGER;
		DECLARE @c5_rec_year_start		INTEGER;
		DECLARE	@c5_rec_year_stop 		INTEGER;
		DECLARE @c5_rec_inv_name		VARCHAR(20);
		DECLARE @c5_rec_genders			INTEGER;
		DECLARE @c5_rec_min_age_group	INTEGER;
		DECLARE @c5_rec_max_age_group	INTEGER;
		DECLARE @c5_rec_inv_description VARCHAR(2000);
--	
		OPEN c5insext;
		FETCH NEXT FROM c5insext INTO @c5_rec_inv_id, @c5_rec_inv_name, @c5_rec_year_start, @c5_rec_year_stop, @c5_rec_genders,
			@c5_rec_min_age_group, @c5_rec_max_age_group, @c5_rec_inv_description;
		WHILE @@FETCH_STATUS = 0
		BEGIN	
			SET @j=@j+1;
			INSERT INTO @inv_array(inv) VALUES('       COALESCE(' + 
				'n' + CAST(@i AS VARCHAR) + '.inv_' + CAST(@c5_rec_inv_id AS VARCHAR) + '_' + LOWER(@c5_rec_inv_name) +
				', 0) AS inv_' + CAST(@c5_rec_inv_id AS VARCHAR) + '_' + LOWER(@c5_rec_inv_name)); 
				/* List of investigations (for use in final SELECT) */
			IF @j > 1 SET @sql_stmt=@sql_stmt + ',' + @crlf;
			SET @sql_stmt=@sql_stmt + @tab + '       SUM(CASE ' + @tab + @tab + '/* Numerators - can overlap */' + @crlf +
				@tab + @tab + @tab + 'WHEN ((' + @tab + @tab + '/* Investigation ' + CAST(@j AS VARCHAR) + ' ICD filters */' + @crlf;

--
-- Add conditions
--
			DECLARE c6insext CURSOR FOR
				SELECT condition
				  FROM rif40_inv_conditions a
				 WHERE a.study_id = @study_id
				   AND a.inv_id   = @c5_rec_inv_id
				 ORDER BY inv_id, line_number;	
			DECLARE @c6_rec_condition	VARCHAR(2000);
--	
			OPEN c6insext;
			FETCH NEXT FROM c6insext INTO @c6_rec_condition;
			WHILE @@FETCH_STATUS = 0
			BEGIN	
				SET @k=@k+1;
				IF @k = 1 SET @sql_stmt=@sql_stmt + @tab + @tab + @tab + '    ' + @c6_rec_condition +
					' /* Filter ' + CAST(@k AS VARCHAR) + ' */' + @crlf
				ELSE SET @sql_stmt=@sql_stmt + @tab + @tab + @tab + ' OR ' + @c6_rec_condition +
					' /* Filter ' + CAST(@k AS VARCHAR) + ' */'  + @crlf;
--
				FETCH NEXT FROM c6insext INTO @c6_rec_condition;
			END; /* Loop k: c6insext */
			CLOSE c6insext;
			DEALLOCATE c6insext;	
			SET @sql_stmt=@sql_stmt + @tab + @tab + @tab + ') /* ' + CAST(@k AS VARCHAR) + ' lines of conditions: study: ' +
				CAST(@study_id AS VARCHAR) + ', inv: ' + CAST(@c5_rec_inv_id AS VARCHAR) + ' */' + @crlf +
				@tab + @tab + @tab + 'AND (1=1' + @crlf;	
				
--
-- Processing years filter [commented out in Postgres]
--
/*			IF year_start = year_stop THEN
				sql_stmt:=sql_stmt||E'\t'||E'\t'||E'\t'||'   AND (c.year = $2'||E'\t'||'-* Denominator (INSERT) year filter *-'||E'\n';
			ELSE
				sql_stmt:=sql_stmt||E'\t'||E'\t'||E'\t'||'   AND (c.year BETWEEN $2 AND $3'||E'\t'||'-* Denominator (INSERT) year filter *-'||E'\n';
			END IF; */		

--
-- Investigation filters: year, age group, genders
--
			IF @c5_rec_year_start = @c5_rec_year_stop SET @sql_stmt=@sql_stmt + @tab + @tab + @tab +
				'   AND  c.year = ' + CAST(@c5_rec_year_start AS VARCHAR) + @crlf
			ELSE IF @c4_rec_year_start = @c5_rec_year_start AND @c4_rec_year_stop = @c5_rec_year_stop SET @sql_stmt=@sql_stmt +
				@tab + @tab + @tab + @tab + '        /* No year filter required for investigation ' + CAST(@j AS VARCHAR) + ' */' + @crlf
			ELSE SET @sql_stmt=@sql_stmt + @tab + @tab + @tab + '   AND  c.year BETWEEN ' + CAST(@c5_rec_year_start AS VARCHAR) +
					' AND ' + CAST(@c5_rec_year_stop AS VARCHAR) + @crlf;
		
			IF @c5_rec_genders = 3 SET @sql_stmt=@sql_stmt + @tab + @tab + @tab + @tab + 
				'        /* No genders filter required for investigation  ' + CAST(@j AS VARCHAR) + ' */' + @crlf
			ELSE SET @sql_stmt=@sql_stmt + @tab + @tab + @tab + 
				'   AND  TRUNC(c.' + LOWER(@c4_rec_age_sex_group_field_name) + '/100) = ' + CAST(@c5_rec_genders AS VARCHAR) + @crlf;
		
			IF @c8_rec_min_age_group = @c5_rec_min_age_group AND @c8_rec_max_age_group = @c5_rec_max_age_group SET @sql_stmt=@sql_stmt +
				@tab + @tab + @tab + @tab + 
				'        /* No age group filter required for investigation ' + CAST(@j AS VARCHAR) + ' */)' + @crlf
			ELSE SET @sql_stmt=@sql_stmt + @tab + @tab + @tab + @tab + 
				'   AND  MOD(c.' + LOWER(@c4_rec_age_sex_group_field_name) + ', 100) BETWEEN ' + 
				CAST(@c5_rec_min_age_group AS VARCHAR) + ' AND ' + CAST(@c5_rec_max_age_group AS VARCHAR) +
				' /* Investigation ' + CAST(@j AS VARCHAR) + ' year, age group filter */)' + @crlf;
--
			IF @c4_rec_total_field IS NULL /* Handle total fields */ SET @sql_stmt=@sql_stmt + @tab + @tab + @tab + ') THEN 1' + @crlf
			ELSE SET @sql_stmt=@sql_stmt + @tab + @tab + @tab + ') THEN ' + LOWER(@c4_rec_total_field) + @crlf;
--
			SET @sql_stmt=@sql_stmt + @tab + @tab + @tab + 'ELSE 0' + @crlf +
				@tab + '       END) inv_' + CAST(@c5_rec_inv_id AS VARCHAR) + '_' + LOWER(@c5_rec_inv_name) +
				@tab + '/* Investigation ' + CAST(@j AS VARCHAR) + ' - ' + @c5_rec_inv_description + ' */ ';			
--
			FETCH NEXT FROM c5insext INTO @c5_rec_inv_id, @c5_rec_inv_name, @c5_rec_year_start, @c5_rec_year_stop, @c5_rec_genders,
				@c5_rec_min_age_group, @c5_rec_max_age_group, @c5_rec_inv_description;
		END; /* Loop j: c5insext */
		CLOSE c5insext;
		DEALLOCATE c5insext;
		
--
-- Check at least one investigation
--
		IF @j = 0 BEGIN
			CLOSE c4insext;
			DEALLOCATE c4insext;
			SET @err_msg = formatmessage(56004, @study_id, @c4_rec_numer_tab); 
				-- Study ID %i no investigations created: distinct numerator: %s
			THROW 56004, @err_msg, 1;
		END;
		SET @sql_stmt=@sql_stmt + @crlf;

--
-- From clause
--
		SET @sql_stmt=@sql_stmt + @tab + '  FROM ' + LOWER(@c4_rec_numer_tab) + ' c, ' + @tab +'/* ' + @c4_rec_description + ' */' + @crlf +
			@tab + '       ' + @areas_table + ' s ' + @tab + '/* Study or comparision area to be extracted */' + @crlf;
		IF @study_or_comparison = 'C' SET @sql_stmt=@sql_stmt + @tab + 
			' WHERE c.' + LOWER(@c1_rec_comparison_geolevel_name) + ' = s.area_id ' + @tab + '/* Comparison selection */' + @crlf
		ELSE SET @sql_stmt=@sql_stmt + @tab + ' WHERE c.' + LOWER(@c1_rec_study_geolevel_name) + ' = s.area_id ' + 
			@tab + '/* Study selection */' + @crlf;
--
-- [Add correct age_sex_group limits]
--
		IF @c8_rec_min_age_group = @c1_rec_min_age_group AND @c8_rec_max_age_group = @c1_rec_max_age_group SET @sql_stmt=@sql_stmt + 
			@tab + '       /* No age group filter required for denominator */' + @crlf
		ELSE SET @sql_stmt=@sql_stmt + @tab + '   AND MOD(c.' + LOWER(@c8_rec_age_sex_group_field_name) + ', 100) BETWEEN ' +
				CAST(@c1_rec_min_age_group AS VARCHAR) + ' AND ' + CAST(@c1_rec_max_age_group AS VARCHAR) +
				' /* All valid age groups for denominator I */' + @crlf;
		SET @sql_stmt=@sql_stmt + @tab + '   AND s.study_id = $1' + @tab + @tab + '/* Current study ID */' + @crlf;

--
-- Processing years filter
--
		IF @year_start = @year_stop SET @sql_stmt=@sql_stmt + @tab + '   AND c.year = $2' + @tab + @tab + 
			'/* Denominator (INSERT) year filter */' + @crlf
		ELSE SET @sql_stmt=@sql_stmt + @tab + '   AND c.year BETWEEN $2 AND $3' + @tab + 
			'/* Denominator (INSERT) year filter */' + @crlf;

--
-- Group by clause
-- [Add support for differing age/sex/group names]
--
		SET @sql_stmt=@sql_stmt + @tab + ' GROUP BY c.year, s.area_id, c.' + LOWER(@c4_rec_age_sex_group_field_name) + @crlf;

--
-- Close WITH clause (common table expression)
-- 
		SET @sql_stmt=@sql_stmt + @tab + ') /* ' + @c4_rec_numer_tab + ' - ' + @c4_rec_description + ' */' + @crlf;
		
--
		FETCH NEXT FROM c4insext INTO @c4_rec_numer_tab, @c4_rec_description, @c4_rec_age_sex_group_field_name, 
			@c4_rec_year_start, @c4_rec_year_stop, @c4_rec_total_field;
	END; /* Loop i: c4insext */
	CLOSE c4insext;
	DEALLOCATE c4insext;

--
-- Denominator CTE with covariates joined at study geolevel
--
	
	PRINT @sql_stmt;	
/*

--
-- Denominator CTE with covariates joined at study geolevel
--
-* /e.g. 
, d AS (
        SELECT d1.year, s.area_id, NULL::INTEGER AS band_id, d1.age_sex_group, 
	       c.ses, 
	       SUM(COALESCE(d1.total, 0)) AS total_pop
          FROM g_rif40_comparison_areas s, sahsuland_pop d1     /- Study or comparison area to be extracted -/
  	      LEFT OUTER JOIN sahsuland_covariates_level4 c ON (        /- Covariates -/
     	               d1.level2 = c.level2				/- Join at study geolevel -/
     	           AND c.year    = $2)
         WHERE d1.year    = $2          /- Denominator (INSERT) year filter -/
           AND s.area_id  = d1.level2   /- Comparison geolevel join -/
           AND s.area_id  IS NOT NULL   /- Exclude NULL geolevel -/
           AND s.study_id = $1          /- Current study ID -/
               /- No age group filter required for denominator -/
         GROUP BY d1.year, s.area_id, d1.age_sex_group, c.ses
)
 *-
	sql_stmt:=sql_stmt||', d AS ('||E'\n';
	IF study_or_comparison = 'C' THEN
		sql_stmt:=sql_stmt||E'\t'||'SELECT d1.year, s.area_id, NULL::INTEGER AS band_id, d1.'||
			quote_ident(LOWER(c8_rec.age_sex_group_field_name))||','||E'\n';
	ELSE
		sql_stmt:=sql_stmt||E'\t'||'SELECT d1.year, s.area_id, s.band_id, d1.'||
			quote_ident(LOWER(c8_rec.age_sex_group_field_name))||','||E'\n';
	END IF;
	FOR c7_rec IN c7insext(study_id) LOOP
		IF c7_rec.covariate_table_name IS NULL THEN
			PERFORM rif40_log_pkg.rif40_error(-56006, 'rif40_create_insert_statement', 
				'Study ID % NULL covariate table: %',
				study_id::VARCHAR					/- Study ID -/,				
				c7_rec.covariate_table_name::VARCHAR 	/- covariate_table_name 2 -/);		
		ELSIF covariate_table_name IS NULL THEN /- Only one coaviate table is supported -/
			covariate_table_name:=c7_rec.covariate_table_name;
		ELSIF covariate_table_name != c7_rec.covariate_table_name THEN
			PERFORM rif40_log_pkg.rif40_error(-56007, 'rif40_create_insert_statement', 
				'Study ID % multiple covariate tables: %, %',
				study_id::VARCHAR					/- Study ID -/,		
				covariate_table_name::VARCHAR 		/- covariate_table_name 1 -/,		
				c7_rec.covariate_table_name::VARCHAR 	/- covariate_table_name 2 -/);		
		END IF;
		k:=k+1;
		sql_stmt:=sql_stmt||E'\t'||'       c.'||quote_ident(LOWER(c7_rec.covariate_name))||','||E'\n';
	END LOOP;
	sql_stmt:=sql_stmt||E'\t'||'       SUM(COALESCE(d1.'||coalesce(quote_ident(LOWER(c8_rec.total_field)), 'total')||
			', 0)) AS total_pop'||E'\n';
	sql_stmt:=sql_stmt||E'\t'||'  FROM '||quote_ident(areas_table)||' s, '||
			quote_ident(LOWER(c1_rec.denom_tab))||' d1 '||
			E'\t'||'/- Study or comparison area to be extracted -/'||E'\n';
	IF covariate_table_name IS NOT NULL THEN
		sql_stmt:=sql_stmt||E'\t'||E'\t'||'LEFT OUTER JOIN '||quote_ident(LOWER(covariate_table_name))||' c ON ('||E'\t'||'/- Covariates -/'||E'\n';
--
-- This is joining at the study geolevel. For comparison areas this needs to be aggregated to the comparison area
--
		sql_stmt:=sql_stmt||E'\t'||E'\t'||E'\t'||'    d1.'||quote_ident(LOWER(c1_rec.study_geolevel_name))||
			' = c.'||quote_ident(LOWER(c1_rec.study_geolevel_name))||E'\t'||E'\t'||'/- Join at study geolevel -/'||E'\n';
		sql_stmt:=sql_stmt||E'\t'||E'\t'||E'\t'||'AND c.year = $2)'||E'\n';
	END IF;
	sql_stmt:=sql_stmt||E'\t'||' WHERE d1.year = $2'||E'\t'||E'\t'||'/- Denominator (INSERT) year filter -/'||E'\n';
	IF study_or_comparison = 'C' THEN
		sql_stmt:=sql_stmt||E'\t'||'   AND s.area_id  = d1.'||quote_ident(LOWER(c1_rec.comparison_geolevel_name))||E'\t'||'/- Comparison geolevel join -/'||E'\n';
	ELSE
		sql_stmt:=sql_stmt||E'\t'||'   AND s.area_id  = d1.'||quote_ident(LOWER(c1_rec.study_geolevel_name))||E'\t'||'/- Study geolevel join -/'||E'\n';
	END IF;
	sql_stmt:=sql_stmt||E'\t'||'   AND s.area_id  IS NOT NULL'||E'\t'||'/- Exclude NULL geolevel -/'||E'\n';
	sql_stmt:=sql_stmt||E'\t'||'   AND s.study_id = $1'||E'\t'||E'\t'||'/- Current study ID -/'||E'\n';
--
-- [Add correct age_sex_group limits]
--
	IF c8_rec.min_age_group = c1_rec.min_age_group AND c8_rec.max_age_group = c1_rec.max_age_group THEN
		sql_stmt:=sql_stmt||E'\t'||'       /- No age group filter required for denominator -/'||E'\n';
	ELSE 
		sql_stmt:=sql_stmt||E'\t'||'   AND MOD(d1.'||quote_ident(LOWER(c8_rec.age_sex_group_field_name))||', 100) BETWEEN '||
			c1_rec.min_age_group::VARCHAR||' AND '||c1_rec.max_age_group::VARCHAR||
			' /- All valid age groups for denominator II -/'||E'\n';
	END IF;
--
-- [Add gender filter]
--

--
-- Add GROUP BY clause
--
	IF study_or_comparison = 'C' THEN
		sql_stmt:=sql_stmt||E'\t'||' GROUP BY d1.year, s.area_id, '||quote_ident(LOWER(c8_rec.age_sex_group_field_name));
	ELSE
		sql_stmt:=sql_stmt||E'\t'||' GROUP BY d1.year, s.area_id, s.band_id, d1.'||quote_ident(LOWER(c8_rec.age_sex_group_field_name));
	END IF;
	FOR c7_rec IN c7insext(study_id) LOOP
		k:=k+1;
		sql_stmt:=sql_stmt||', c.'||quote_ident(LOWER(c7_rec.covariate_name));
	END LOOP;
	sql_stmt:=sql_stmt||E'\n'||') /- End of denominator -/'||E'\n';
--
-- Main SQL statement
--
	sql_stmt:=sql_stmt||'SELECT d.year,'||E'\n';
	sql_stmt:=sql_stmt||'       '''||study_or_comparison||''' AS study_or_comparison,'||E'\n';
	sql_stmt:=sql_stmt||'       $1 AS study_id,'||E'\n';
	sql_stmt:=sql_stmt||'       d.area_id,'||E'\n';
	sql_stmt:=sql_stmt||'       d.band_id,'||E'\n';
--
-- [Add support for differing age/sex/group names]
--
	sql_stmt:=sql_stmt||'       TRUNC(d.'||LOWER(c8_rec.age_sex_group_field_name)||'/100) AS sex,'||E'\n';
	sql_stmt:=sql_stmt||'       MOD(d.'||LOWER(c8_rec.age_sex_group_field_name)||', 100) AS age_group,'||E'\n';
--
-- Add covariate names (Assumes 1 covariate table)
--
	k:=0;
	FOR c7_rec IN c7insext(study_id) LOOP
		k:=k+1;
--		IF study_or_comparison = 'C' THEN
--			sql_stmt:=sql_stmt||'       NULL::INTEGER AS '||LOWER(c7_rec.covariate_name)||','||E'\n';
--		ELSE
			sql_stmt:=sql_stmt||'       d.'||LOWER(c7_rec.covariate_name)||','||E'\n';
--		END IF;
	END LOOP;
--
-- Add investigations 
--
	sql_stmt:=sql_stmt||array_to_string(inv_array, ','||E'\n')||', '||E'\n';
--
-- Add denominator
--
	sql_stmt:=sql_stmt||'       d.total_pop'||E'\n';
--
-- FROM clause
--
	sql_stmt:=sql_stmt||'  FROM d'||E'\t'||E'\t'||E'\t'||'/- Denominator - '||c8_rec.description||' -/'||E'\n';
	sql_stmt:=sql_stmt||array_to_string(inv_join_array, E'\n')||E'\n';

--
-- ORDER BY clause
--
	sql_stmt:=sql_stmt||' ORDER BY 1, 2, 3, 4, 5, 6, 7';
--
	PERFORM rif40_log_pkg.rif40_log('DEBUG1', 'rif40_create_insert_statement', 
		'[56005] SQL> %;', sql_stmt::VARCHAR);
 */
--
	RETURN @rval;
END;
GO

--
-- Eof