-- ************************************************************************
-- *
-- * THIS SCRIPT MAY BE EDITED - NO NEED TO USE ALTER SCRIPTS
-- *
-- ************************************************************************
--
-- ************************************************************************
--
-- GIT Header
--
-- $Format:Git ID: (%h) %ci$
-- $Id$
-- Version hash: $Format:%H$
--
-- Description:
--
-- Rapid Enquiry Facility (RIF) - PG psql code (state machine and extract SQL generation)
--								  Create disease mapping example
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
\set ECHO all
\set ON_ERROR_STOP ON
\timing

--
-- Check user is rif40
--
DO LANGUAGE plpgsql $$
BEGIN
	IF user = 'rif40' THEN
		RAISE INFO 'User check: %', user;	
	ELSE
		RAISE EXCEPTION 'C209xx: User check failed: % is not rif40', user;	
	END IF;
END;
$$;

DROP FUNCTION IF EXISTS rif40_sm_pkg.rif40_create_disease_mapping_example(
	VARCHAR,VARCHAR,VARCHAR,VARCHAR,VARCHAR,VARCHAR[],VARCHAR,VARCHAR,VARCHAR,VARCHAR,INTEGER,INTEGER,VARCHAR[][],VARCHAR[],VARCHAR[], VARCHAR, VARCHAR); 
DROP FUNCTION IF EXISTS rif40_sm_pkg._rif40_create_disease_mapping_example(VARCHAR, VARCHAR, INTEGER, VARCHAR, Text[]);

-- Old
DROP FUNCTION IF EXISTS rif40_sm_pkg.rif40_create_disease_mapping_example(
	VARCHAR,VARCHAR,VARCHAR,VARCHAR,VARCHAR,VARCHAR[],VARCHAR,VARCHAR,VARCHAR,VARCHAR,INTEGER,INTEGER,VARCHAR[][],VARCHAR[],VARCHAR[], VARCHAR); 
DROP FUNCTION IF EXISTS rif40_sm_pkg._rif40_create_disease_mapping_example(VARCHAR, VARCHAR, INTEGER, Text[]);	
DROP FUNCTION IF EXISTS rif40_sm_pkg._rif40_create_disease_mapping_example(VARCHAR, VARCHAR, INTEGER, BOOLEAN);
DROP FUNCTION IF EXISTS rif40_sm_pkg._rif40_create_disease_mapping_example(VARCHAR, VARCHAR, INTEGER);
DROP FUNCTION IF EXISTS rif40_sm_pkg.rif40_create_disease_mapping_example(
	VARCHAR,VARCHAR,VARCHAR,VARCHAR,VARCHAR,VARCHAR[],VARCHAR,VARCHAR,VARCHAR,VARCHAR,INTEGER,INTEGER,VARCHAR[],VARCHAR[],VARCHAR[]); 
DROP FUNCTION IF EXISTS rif40_sm_pkg.rif40_create_disease_mapping_example(
	VARCHAR,VARCHAR,VARCHAR,VARCHAR,VARCHAR,VARCHAR[],VARCHAR,VARCHAR,VARCHAR,VARCHAR,INTEGER,INTEGER,VARCHAR[][],VARCHAR[],VARCHAR[]);	
DROP FUNCTION IF EXISTS rif40_sm_pkg.rif40_create_disease_mapping_example(
	VARCHAR,VARCHAR,VARCHAR,VARCHAR,VARCHAR,VARCHAR[],VARCHAR,VARCHAR,VARCHAR,VARCHAR,INTEGER,INTEGER,VARCHAR[][],VARCHAR[][],VARCHAR[],VARCHAR[]);
DROP FUNCTION IF EXISTS rif40_sm_pkg._rif40_create_disease_mapping_example(VARCHAR, VARCHAR, INTEGER, BOOLEAN, Text[]);

--
-- Error codes
--
-- rif40_create_disease_mapping_example:	56200 to 56399
--
CREATE OR REPLACE FUNCTION rif40_sm_pkg.rif40_create_disease_mapping_example(
	geography					VARCHAR,
	geolevel_view				VARCHAR,
	geolevel_area				VARCHAR,
	geolevel_map				VARCHAR,
	geolevel_select				VARCHAR,
	geolevel_selection			VARCHAR[],
	project						VARCHAR,
	study_name					VARCHAR,
	denom_tab					VARCHAR,
	numer_tab					VARCHAR,
	year_start					INTEGER,
	year_stop					INTEGER,
	condition_array				VARCHAR[][],
	investigation_desc_array 	VARCHAR[],
	covariate_array				VARCHAR[],
	stop_after_table			VARCHAR DEFAULT NULL,
	test_run_class 				VARCHAR DEFAULT 'rif40_create_disease_mapping_example'
	)
RETURNS INTEGER
SECURITY INVOKER
AS $func$
/*
Function: 	rif40_create_disease_mapping_example()
Parameters:	Geography, geolevel view, geolevel area, geolevel map, geolevel select, 
			geolevel selection array, project, study name, denominator table, numerator table,
			year_start, year_stop,
			investigation ICD conditions 2 dimensional array (i.e. matrix); 4 columnsxN rows:
				outcome_group_name, min_condition, max_condition, predefined_group_name
			investigation descriptions array, 
			covariate array,
			stop after table [Halts creation after  this table - for test harness],
			test run class
Returns:	Nothing
Description:	Create disease mapping exmaple

Example of condition_array data:

WITH data AS (
    SELECT '{{"SAHSULAND_ICD", "C34", NULL, NULL}, {"SAHSULAND_ICD", "162", "1629", NULL}}'::text[] AS arr
)
SELECT arr[i][1] AS outcome_group_name, 
       arr[i][2] AS min_condition, 
       arr[i][3] AS max_condition, 
       arr[i][4] AS predefined_group_name
  FROM data, generate_subscripts((SELECT arr FROM data), 1) i;
 outcome_group_name | min_condition | max_condition | predefined_group_name
--------------------+---------------+---------------+-----------------------
 SAHSULAND_ICD      | C34           |               |
 SAHSULAND_ICD      | 162           | 1629          |
(2 rows)

Setup a disease mapping example.

WARNING: This function does not test the validity of the inputs, it relies on the trigger functions.
This allows it to be used for test putposes

1. Setup geographic area to be studied

View the geolevel <geolevel_view> of <geolevel_area> and select at <geolevel_select> geolevel and map at <geolevel_map>. 
Provide list of areas selected <geolevel_selection>

E.g. [Values required in ()'s]

Geography: 		England and Wales 2001 (EW01)
Geolevel view: 		2001 Government office region (GOR2001)
Geolevel area: 		London (H)
Geolevel map: 		2001 Census statistical ward (WARD2001)
Gelevel select: 	2001 local area district/unitary authority (LADUA2001)
Geolevel selection:	Array of LADUA2001

Geolevel view and area define the geolevel and area to be mapped (so the user can select geolevel section)
Gelevel select defines the geoelevel the user will select at
Geolevel map define the geolevel the RIF will map at
Geolevel selection is an array of <geolevel_select> that the user selected. The forms the study area.
The comparison geolevel is the default set in rif40_geographies
The comparison area is the RIF default (the array produced by rif40_geo_pkg.get_default_comparison_area())

2. INSERT INTO rif40_studies

RIF40_STUDIES defaults (schema):

username		USER
study_id 		(nextval('rif40_study_id_seq'::regclass))::integer
study_date		LOCALIMESTAMP
study_state		C
audsid			sys_context('USERENV'::character varying, 'SESSIONID'::character varying)
stats_method	HET

RIF40_STUDIES defaults (from trigger):

extract_table		S_<study_id>_EXTRACT
map_table		S_<study_id>_MAP

RIF40_STUDIES defaults (this function):

study_type		1		[disease mapping]
direct_stand_tab	NULL
suppression_value	parameter "SuppressionValue"
extract_permitted	1 for SAHSU geogrpahy, 0 otherwise
transfer_permitted	1 for SAHSU geogrpahy, 0 otherwise

RIF40_STUDIES values  (where different from parameter names):
 
comparison_geolevel_name	<geolevel_view>
study_geolevel_name		<geolevel_map>
min_age_group, max_age_group	MIN/MAX defined for denominator

3. INSERT INTO rif40_investigations

RIF40_INVESTIGATIONS defaults (schema)

username		USER
study_id 		(currval('rif40_study_id_seq'::regclass))::integer
inv_id			(nextval('rif40_inv_id_seq'::regclass))::integer
classifier		QUANTILE
classifier_bands	5
investigation_state	C
mh_test_type		No test

RIF40_INVESTIGATIONS defaults (this function):

genders			3 [both]
min_age_group, max_age_group	MIN/MAX defined for denominator
inv_name		INV_<n> [index in investigation ICD conditions array]

4. INSERT INTO rif40_inv_conditions

RIF40_INV_CONDITIONS defaults (schema)

username		USER
study_id 		(currval('rif40_study_id_seq'::regclass))::integer
inv_id			(nextval('rif40_inv_id_seq'::regclass))::integer
line_number		1

5. INSERT INTO rif40_study_areas

RIF40_STUDY_AREAS defaults (schema)

username		USER
study_id 		(currval('rif40_study_id_seq'::regclass))::integer

RIF40_STUDY_AREAS defaults (this function)

band_id			<n> [index in geolevel selection array]	

6. INSERT INTO rif40_comparison_areas

RIF40_COMPARISON_AREAS defaults (schema)

username		USER
study_id 		(currval('rif40_study_id_seq'::regclass))::integer

The actual comparison area comes from rif40_geo_pkg.rif40_get_default_comparison_area().
This returns all areas at the default comparison area level covered by the users selected geolevels.

7. INSERT INTO rif40_inv_covariates

RIF40_INV_COVARIATES defaults (schema)

username		USER
study_id 		(currval('rif40_study_id_seq'::regclass))::integer
inv_id			(nextval('rif40_inv_id_seq'::regclass))::integer


RIF40_INV_COVARIATES defaults (this function)

min, max		MIN/MAX for covariate
study_geolevel_name	study_geolevel_name FROM rif40_studies

8. INSERT INTO rif40_study_shares if any RIF DB role is a RIF manager and extraction is permitted

RIF40_STUDY_SHARES defaults (schema)

grantor			USER
study_id 		(currval('rif40_study_id_seq'::regclass))::integer

 */
DECLARE
	c1cdm CURSOR(l_table_name VARCHAR) FOR  
		WITH a AS (
			SELECT a.age_group_id, MIN(a.offset) AS min_age_group, MAX(a.offset) AS max_age_group
			  FROM rif40_age_groups a, rif40_tables b
			 WHERE a.age_group_id = b.age_group_id
			   AND b.table_name   = l_table_name
			 GROUP BY a.age_group_id
		)
		SELECT min_age_group, max_age_group, 
		       b.fieldname AS min_age_group_name,
		       c.fieldname AS max_age_group_name
		  FROM a
			LEFT OUTER JOIN rif40_age_groups b ON (a.age_group_id = b.age_group_id AND a.min_age_group = b.offset) 
			LEFT OUTER JOIN rif40_age_groups c ON (a.age_group_id = c.age_group_id AND a.max_age_group = c.offset); 
	c1cdm_rec RECORD;
	c2cdm CURSOR FOR
		SELECT p2.param_name, 
		       CASE WHEN pg_has_role(USER, 'rif_no_suppression', 'USAGE') THEN 1 /* Not Suppressed */ ELSE p2.param_value::INTEGER END suppression_value
		  FROM rif40.rif40_parameters p2
		 WHERE p2.param_name = 'SuppressionValue';
	c2cdm_rec RECORD;
	c3cdm CURSOR(l_geography VARCHAR) FOR
		SELECT defaultcomparea, hierarchytable
		  FROM rif40_geographies a
		 WHERE a.geography = l_geography;
	c3cdm_rec RECORD;
	c4cdm CURSOR FOR /* Valid RIF users */
		SELECT rolname, pg_has_role(rolname, 'rif_manager', 'USAGE') AS is_rif_manager
		  FROM pg_roles r, pg_namespace n
		 WHERE (pg_has_role(rolname, 'rif_user', 'USAGE') OR pg_has_role(rolname, 'rif_manager', 'USAGE'))
		   AND n.nspowner = r.oid
		   AND nspname = rolname;
	c4cdm_rec RECORD;
	c5cdm CURSOR FOR /* Check if rif40_investigations.geography column still exists (pre alter 2) */
		SELECT column_name
		  FROM information_schema.columns
		 WHERE table_name   = 'rif40_investigations'
		   AND column_name  = 'geography'
		   AND table_schema = 'rif40';
	c5cdm_rec RECORD;
--
	i 			INTEGER:=0;
	l_inv_name	 	VARCHAR;
	l_area_id 		VARCHAR;
	icd 			VARCHAR;
	comparision_area	VARCHAR[];
	study_area_count	INTEGER;
	comparison_area_count	INTEGER;
	covariate_count		INTEGER;
--
	parent_test_id		INTEGER:=NULL;
-- 
	sql_stmt		VARCHAR;
	sql_stmt2		VARCHAR;	
--
	l_extract_permitted 	INTEGER:=0;
	l_transfer_permitted 	INTEGER:=0;
--
	v_detail		VARCHAR;
	error_message	VARCHAR;
	inv_cond_rec	RECORD;
BEGIN
--
-- Check INVESTIGATION array are the same length
--
--	IF array_ndims(condition_array) != array_length(investigation_desc_array, 1) THEN
--		PERFORM rif40_log_pkg.rif40_error(-56200, 'rif40_create_disease_mapping_example', 
--			'icd condition array dimension (%) != description array length (%)',
--			array_ndims(condition_array)::VARCHAR,
--			array_length(investigation_desc_array, 1)::VARCHAR);
--	END IF;
--
-- Get MIN/MAX age groups. Check table exists
--
	OPEN c1cdm(numer_tab);
	FETCH c1cdm INTO c1cdm_rec;
	IF NOT FOUND THEN
		PERFORM rif40_log_pkg.rif40_error(-56201, 'rif40_create_disease_mapping_example', 
			'Cannot find numerator table: %',
			numer_tab::VARCHAR);
	END IF;
	CLOSE c1cdm;
--
-- Get suppression_value
--
	OPEN c2cdm;
	FETCH c2cdm INTO c2cdm_rec;
	IF NOT FOUND THEN
		PERFORM rif40_log_pkg.rif40_error(-56202, 'rif40_create_disease_mapping_example', 
			'Cannot find SuppressionValue parameter');
	END IF;
	CLOSE c2cdm;
--
-- Get comparison geolevel - is the default set in rif40_geographies
--
	OPEN c3cdm(geography);
	FETCH c3cdm INTO c3cdm_rec;
	IF NOT FOUND THEN
		PERFORM rif40_log_pkg.rif40_error(-56203, 'rif40_create_disease_mapping_example', 
			'Cannot find rif40_geographies geography: %',
			geography::VARCHAR);
	END IF;
	CLOSE c3cdm;
--
-- Set up IG - SAHSU no restrictions; otherwise full restrictions
--
	IF geography = 'SAHSU' THEN
		l_extract_permitted:=1;
		l_transfer_permitted:=1;
	END IF;
--
-- 2. INSERT INTO rif40_studies
--
	sql_stmt:='INSERT /* 1 */ INTO rif40_studies ('||E'\n'||
' 		geography, project, study_name, study_type,'||E'\n'||
' 		comparison_geolevel_name, study_geolevel_name, denom_tab,'||E'\n'||            
' 		year_start, year_stop, max_age_group, min_age_group,'||E'\n'|| 
' 		suppression_value, extract_permitted, transfer_permitted, stats_method)'||E'\n'||
'	VALUES ('||E'\n'||
'		 '''||geography||''' 					/* geography */,'||E'\n'||
'		 '''||project||''' 						/* project */,'||E'\n'|| 
'		 '''||study_name||''' 					/* study_name */,'||E'\n'||
'		 1 									/* study_type [disease mapping] */,'||E'\n'||
'		 '''||c3cdm_rec.defaultcomparea||'''	/* comparison_geolevel_name */,'||E'\n'||
'		 '''||geolevel_map||''' 				/* study_geolevel_name */,'||E'\n'||   
'		 '''||denom_tab||''' 					/* denom_tab */,'||E'\n'||            
'		 '||year_start||'					/* year_start */,'||E'\n'||       
'		 '||year_stop||' 					/* year_stop */,'||E'\n'||      
'		 '||c1cdm_rec.max_age_group||' 	/* max_age_group */,'||E'\n'|| 
'		 '||c1cdm_rec.min_age_group||' 	/* min_age_group */,'||E'\n'|| 
'		 '||c2cdm_rec.suppression_value||' /* suppression_value */,'||E'\n'|| 
'		 '||l_extract_permitted||' 		/* extract_permitted */,'||E'\n'|| 
'		 '||l_transfer_permitted||'		/* transfer_permitted */,'||E'\n'|| 
'		 ''HET''						/* stats_method */)';
	parent_test_id:=rif40_sm_pkg._rif40_create_disease_mapping_example(sql_stmt, study_name, 
		parent_test_id, test_run_class,
		ARRAY['trigger_fct_t_rif40_studies_checks']);
	PERFORM rif40_log_pkg.rif40_log('INFO', 'rif40_create_disease_mapping_example', 
		'[56204] Created study: % % for project %',  
		currval('rif40_study_id_seq'::regclass)::VARCHAR,
		study_name::VARCHAR,
		project::VARCHAR);
	PERFORM rif40_log_pkg.rif40_log('INFO', 'rif40_create_disease_mapping_example', 
		'[56205] View the geolevel % of "%" and select at % geolevel and map at %; default comparison area %',
		geolevel_view::VARCHAR,
		geolevel_area::VARCHAR, 
		geolevel_select::VARCHAR,
		geolevel_map::VARCHAR,
		c3cdm_rec.defaultcomparea::VARCHAR);
	PERFORM rif40_log_pkg.rif40_log('DEBUG3', 'rif40_create_disease_mapping_example', 
		'[56206] List of areas selected: "%"', 
		array_to_string(geolevel_selection, '","')::VARCHAR);
	PERFORM rif40_log_pkg.rif40_log('INFO', 'rif40_create_disease_mapping_example', 
		'[56207] Denominator: %; period: %-%; age groups % to %',
		denom_tab::VARCHAR,
		year_start::VARCHAR,
		year_stop::VARCHAR,
		c1cdm_rec.min_age_group_name::VARCHAR,
		c1cdm_rec.max_age_group_name::VARCHAR);
	PERFORM rif40_log_pkg.rif40_log('INFO', 'rif40_create_disease_mapping_example', 
		'[56208] Suppression value: %; extract permitted: %; transfer permitted %',
		c2cdm_rec.suppression_value::VARCHAR,
		l_extract_permitted::VARCHAR,
		l_transfer_permitted::VARCHAR);
	IF stop_after_table = 'rif40_studies' /* 1 */ THEN
		RETURN parent_test_id;
	END IF;	
--	
	OPEN c5cdm;
	FETCH c5cdm INTO c5cdm_rec;
	CLOSE c5cdm;
--
-- 3. INSERT INTO rif40_investigations
--
-- Process investigations array
--
	FOREACH icd IN ARRAY investigation_desc_array LOOP
		i:=i+1;
		l_inv_name:='T_INV_'||i::VARCHAR;
		sql_stmt='INSERT /* 2 */ INTO rif40_investigations('||E'\n';
--
-- Check if rif40_investigations.geography column still exists (pre alter 2)
--
		IF 	c5cdm_rec.column_name = 'geography' THEN
 			sql_stmt=sql_stmt||
				'	geography,'||E'\n'; 
		END IF;
		sql_stmt=sql_stmt||
			'	inv_name,'||E'\n'||
			'	inv_description,'||E'\n'||
			'	genders,'||E'\n'||
			'	numer_tab,'||E'\n'|| 
			'	year_start,'||E'\n'||
			'	year_stop,'||E'\n'|| 
			'	max_age_group,'||E'\n'||
			'	min_age_group'||E'\n'||
			')'||E'\n'||
			'VALUES ('||E'\n'; 
		IF 	c5cdm_rec.column_name = 'geography' THEN
 			sql_stmt=sql_stmt||
				'	'||geography||' 		/* geography */,'||E'\n'; 
		END IF;
		sql_stmt=sql_stmt||
			'	'''||l_inv_name||''' 		/* inv_name */,'||E'\n'||  
			'	'''||investigation_desc_array[i]||'''	/* inv_description */,'||E'\n'||
			'	3			/* genders [both] */,'||E'\n'||
			'	'''||numer_tab||'''		/* numer_tab */,'||E'\n'||
			'	'||year_start||'		/* year_start */,'||E'\n'||       
			'	'||year_stop||' 		/* year_stop */,'||E'\n'||      
			'	'||c1cdm_rec.max_age_group||' /* max_age_group */,'||E'\n'|| 
			'	'||c1cdm_rec.min_age_group||' /* min_age_group */)';
		parent_test_id:=rif40_sm_pkg._rif40_create_disease_mapping_example(sql_stmt, study_name, 
			parent_test_id, test_run_class,
			ARRAY['trigger_fct_t_rif40_investigations_checks']);
	
--
-- 4. INSERT INTO rif40_inv_conditions
--
		IF stop_after_table IS NULL OR NOT stop_after_table = 'rif40_investigations' /* 2 */ THEN
			sql_stmt2:='WITH data AS ('||E'\n'||
	'				SELECT '''||condition_array::Text||'''::Text[][] AS arr'||E'\n'||
	'			), b AS ('||E'\n'||
	'				SELECT arr[i][1] AS outcome_group_name,'||E'\n'|| 
	'					   arr[i][2] AS min_condition, '||E'\n'||
	'					   arr[i][3] AS max_condition, '||E'\n'||
	'					   arr[i][4] AS predefined_group_name, '||E'\n'||
	'       	           ROW_NUMBER() OVER() AS line_number'||E'\n'||
	'			      FROM data, generate_subscripts((SELECT arr FROM data), 1) i'||E'\n'||
	'			)'||E'\n'||
	'		SELECT outcome_group_name, min_condition, max_condition, predefined_group_name, line_number'||E'\n'||
	'		  FROM b';
			sql_stmt:='INSERT /* 3 */ INTO rif40_inv_conditions('||E'\n'||
	'			outcome_group_name, min_condition, max_condition, predefined_group_name, line_number)'||E'\n'||
				sql_stmt2||E'\n'||
	'		RETURNING outcome_group_name, min_condition, max_condition, predefined_group_name';
			parent_test_id:=rif40_sm_pkg._rif40_create_disease_mapping_example(sql_stmt, study_name, 
				parent_test_id, test_run_class,
				ARRAY['trigger_fct_t_rif40_inv_conditions_checks']);
			FOR inv_cond_rec IN EXECUTE sql_stmt2 LOOP
	--
				PERFORM rif40_log_pkg.rif40_log('INFO', 'rif40_create_disease_mapping_example', 	
					'[56209] Created investigation: % (%): %; numerator: %; outcome_group_names: %, min/max/predefined group conditions: "%/%/%"',  
					currval('rif40_inv_id_seq'::regclass)::VARCHAR,
					l_inv_name::VARCHAR,
					investigation_desc_array[i]::VARCHAR,
					numer_tab::VARCHAR,
					inv_cond_rec.outcome_group_name::VARCHAR,
					inv_cond_rec.min_condition::VARCHAR,
					inv_cond_rec.max_condition::VARCHAR,
					inv_cond_rec.predefined_group_name::VARCHAR);
			END LOOP;
		END IF;		
	END LOOP;
	IF stop_after_table IN ('rif40_investigations', 'rif40_inv_conditions') /* 2,3 */ THEN
		RETURN parent_test_id;
	END IF;
--
-- 5. INSERT INTO rif40_study_areas
--
-- Process geolevel_selection array, populate study areas
--
	IF geolevel_map = geolevel_select THEN
		sql_stmt:='INSERT /* 4 */ INTO rif40_study_areas(area_id, band_id)'||E'\n'|| 
		'SELECT unnest('||E'\n'|| 
				''''||geolevel_selection::Text||'''::Text[]) /* at Geolevel select */ AS study_area, ROW_NUMBER() OVER() AS band_id';
--
-- User selection carried out at different level to mapping
--
	ELSE  
		sql_stmt:='INSERT /* 4 */ INTO rif40_study_areas(area_id, band_id)'||E'\n'||
			'SELECT DISTINCT '||LOWER(geolevel_map)||', ROW_NUMBER() OVER() AS band_id'||E'\n'||
			E'\t'||'  FROM '||LOWER(c3cdm_rec.hierarchytable)||E'\n'||
			E'\t'||' WHERE '||LOWER(geolevel_select)||' IN ('||E'\n'||
			E'\t'||'SELECT unnest('||E'\n'|| 
				''''||geolevel_selection::Text||'''::Text[]) /* at Geolevel select */ AS study_area)';
	END IF;
	parent_test_id:=rif40_sm_pkg._rif40_create_disease_mapping_example(sql_stmt, study_name, 
		parent_test_id, test_run_class, 
		ARRAY['trigger_fct_t_rif40_study_areas_checks', 'trigger_fct_t_rif40_study_areas_checks2']);
	GET DIAGNOSTICS study_area_count = ROW_COUNT;
	IF stop_after_table = 'rif40_study_areas' /* 4 */ THEN
		RETURN parent_test_id;
	END IF;
	
--
-- Get default comparison area, populate comparison areas [INSERT/trigger]
--
	comparision_area:=rif40_geo_pkg.rif40_get_default_comparison_area(geography, geolevel_select, geolevel_selection);
	IF comparision_area IS NULL THEN
		PERFORM rif40_log_pkg.rif40_error(-56212, 'rif40_create_disease_mapping_example', 
			'rif40_geo_pkg.rif40_get_default_comparison_area(%, %, <geolevel_selection>) returned NULL',
			geography::VARCHAR,
			geolevel_select::VARCHAR);
	END IF;
--
-- 6. INSERT INTO rif40_comparison_areas
--
	sql_stmt:='INSERT /* 5 */ INTO rif40_comparison_areas(area_id)'||E'\n'|| 
		'SELECT unnest('||E'\n'|| 
				''''||comparision_area::Text||'''::Text[]) AS comparision_area';
	parent_test_id:=rif40_sm_pkg._rif40_create_disease_mapping_example(sql_stmt, study_name, 
		parent_test_id, test_run_class, 
		ARRAY['trigger_fct_t_rif40_comp_areas_checks', 'trigger_fct_t_rif40_comp_areas_checks2']);
	GET DIAGNOSTICS comparison_area_count = ROW_COUNT;
	IF stop_after_table = 'rif40_comparison_areas' /* 5 */ THEN
		RETURN parent_test_id;
	END IF;
--
-- 7. INSERT INTO rif40_inv_covariates
--
	sql_stmt:='INSERT /* 6 */ INTO rif40_inv_covariates(geography, covariate_name, study_geolevel_name, min, max)'||E'\n'|| 
		'WITH a AS ('||E'\n'||
		'	SELECT unnest('||E'\n'|| 
		'         '''||covariate_array::Text||'''::Text[])::Text AS covariate_name,'||E'\n'||
		'	      '''||geolevel_map||'''::Text AS study_geolevel_name,'||E'\n'||
		'	      '''||geography||'''::Text AS geography'||E'\n'|| 
		')'||E'\n'||
		'SELECT a.geography, a.covariate_name, a.study_geolevel_name, b.min, b.max'||E'\n'||
		'  FROM a'||E'\n'||
		'	LEFT OUTER JOIN rif40_covariates b ON'||E'\n'|| 
		'		(a.covariate_name = b.covariate_name AND a.study_geolevel_name = b.geolevel_name AND a.geography = b.geography)';
	parent_test_id:=rif40_sm_pkg._rif40_create_disease_mapping_example(sql_stmt, study_name, 
		parent_test_id, test_run_class, 
		ARRAY['trigger_fct_t_rif40_inv_covariates_checks']);		
	GET DIAGNOSTICS covariate_count = ROW_COUNT;
--
	PERFORM rif40_log_pkg.rif40_log('INFO', 'rif40_create_disease_mapping_example', 	
		'[56213] Inserted % study area(s); % comparision area(s) [default comparison area geolevel: %]; % covariate(s)',
		study_area_count::VARCHAR,
		comparison_area_count::VARCHAR,
		c3cdm_rec.defaultcomparea::VARCHAR,
		covariate_count::VARCHAR);
	IF stop_after_table = 'rif40_covariates' /* 6 */ THEN
		RETURN parent_test_id;
	END IF;
--
-- 8. INSERT INTO rif40_study_shares if any RIF DB role is a RIF manager and extraction is permitted
--
	FOR c4cdm_rec IN c4cdm LOOP
		IF c4cdm_rec.is_rif_manager AND l_extract_permitted = 1 THEN
			sql_stmt:='INSERT /* 7 */ INTO rif40_study_shares(grantee_username) VALUES ('''||c4cdm_rec.rolname||''')';
			parent_test_id:=rif40_sm_pkg._rif40_create_disease_mapping_example(sql_stmt, study_name, 
				parent_test_id, test_run_class, 
				ARRAY['trigger_fct_rif40_study_shares_checks']);			
			PERFORM rif40_log_pkg.rif40_log('INFO', 'rif40_create_disease_mapping_example', 
				'[56214] Shared study % to %',
				currval('rif40_study_id_seq'::regclass)::VARCHAR,
				c4cdm_rec.rolname::VARCHAR);	
		END IF;
	END LOOP;
--
	RETURN parent_test_id;
END;
$func$ LANGUAGE plpgsql;

COMMENT ON FUNCTION rif40_sm_pkg.rif40_create_disease_mapping_example(
	VARCHAR,VARCHAR,VARCHAR,VARCHAR,VARCHAR,VARCHAR[],VARCHAR,VARCHAR,VARCHAR,VARCHAR,INTEGER,INTEGER,VARCHAR[][],VARCHAR[],VARCHAR[], VARCHAR, VARCHAR) 
IS 'Function: 	rif40_create_disease_mapping_example()
Parameters:	Geography, geolevel view, geolevel area, geolevel map, geolevel select, 
			geolevel selection array, project, study name, denominator table, numerator table,
			year_start, year_stop,
			investigation ICD conditions 2 dimensional array (i.e. matrix); 4 columnsxN rows:
			outcome_group_name, min_condition, max_condition, predefined_group_name, 
			investigation descriptions array, covariate array,
			stop after table [Halts creation after  this table - for test harness],
			test run class
Returns:	Nothing
Description:	Create disease mapping exmaple

Example of condition_array data:

WITH data AS (
    SELECT ''{{"SAHSULAND_ICD", "C34", NULL, NULL}, {"SAHSULAND_ICD", "162", "1629", NULL}}''::text[] AS arr
)
SELECT arr[i][1] AS outcome_group_name, 
       arr[i][2] AS min_condition, 
       arr[i][3] AS max_condition, 
       arr[i][4] AS predefined_group_name
  FROM data, generate_subscripts((SELECT arr FROM data), 1) i;
 outcome_group_name | min_condition | max_condition | predefined_group_name
--------------------+---------------+---------------+-----------------------
 SAHSULAND_ICD      | C34           |               |
 SAHSULAND_ICD      | 162           | 1629          |
(2 rows)

Setup a disease mapping example.

WARNING: This function does not test the validity of the inputs, it relies on the trigger functions.
This allows it to be used for test putposes

1. Setup geographic area to be studied

View the geolevel <geolevel_view> of <geolevel_area> and select at <geolevel_select> geolevel and map at <geolevel_map>. 
Provide list of areas selected <geolevel_selection>

E.g. [Values required in ()''s]

Geography: 		England and Wales 2001 (EW01)
Geolevel view: 		2001 Government office region (GOR2001)
Geolevel area: 		London (H)
Geolevel map: 		2001 Census statistical ward (WARD2001)
Gelevel select: 	2001 local area district/unitary authority (LADUA2001)
Geolevel selection:	Array of LADUA2001

Geolevel view and area define the geolevel and area to be mapped (so the user can select geolevel section)
Gelevel select defines the geoelevel the user will select at
Geolevel map define the geolevel the RIF will map at
Geolevel selection is an array of <geolevel_select> that the user selected. The forms the study area.
The comparison geolevel is the default set in rif40_geographies
The comparison area is the RIF default (the array produced by rif40_geo_pkg.get_default_comparison_area())

2. INSERT INTO rif40_studies

RIF40_STUDIES defaults (schema):

username		USER
study_id 		(nextval(''rif40_study_id_seq''::regclass))::integer
study_date		LOCALIMESTAMP
study_state		C
audsid			sys_context(''USERENV''::character varying, ''SESSIONID''::character varying)

RIF40_STUDIES defaults (from trigger):

extract_table		S_<study_id>_EXTRACT
map_table		S_<study_id>_MAP

RIF40_STUDIES defaults (this function):

study_type		1		[disease mapping]
direct_stand_tab	NULL
suppression_value	parameter "SuppressionValue"
extract_permitted	1 for SAHSU geogrpahy, 0 otherwise
transfer_permitted	1 for SAHSU geogrpahy, 0 otherwise

RIF40_STUDIES values  (where different from parameter names):
 
comparison_geolevel_name	<geolevel_view>
study_geolevel_name		<geolevel_map>
min_age_group, max_age_group	MIN/MAX defined for denominator

3. INSERT INTO rif40_investigations

RIF40_INVESTIGATIONS defaults (schema)

username		USER
study_id 		(currval(''rif40_study_id_seq''::regclass))::integer
inv_id			(nextval(''rif40_inv_id_seq''::regclass))::integer
classifier		QUANTILE
classifier_bands	5
investigation_state	C
mh_test_type		No test

RIF40_INVESTIGATIONS defaults (this function):

genders			3 [both]
min_age_group, max_age_group	MIN/MAX defined for denominator
inv_name		INV_<n> [index in investigation ICD conditions array]

4. INSERT INTO rif40_inv_conditions

RIF40_INV_CONDITIONS defaults (schema)

username		USER
study_id 		(currval(''rif40_study_id_seq''::regclass))::integer
inv_id			(nextval(''rif40_inv_id_seq''::regclass))::integer
line_number		1

5. INSERT INTO rif40_study_areas

RIF40_STUDY_AREAS defaults (schema)

username		USER
study_id 		(currval(''rif40_study_id_seq''::regclass))::integer

RIF40_STUDY_AREAS defaults (this function)

band_id			<n> [index in geolevel selection array]	

6. INSERT INTO rif40_comparison_areas

RIF40_COMPARISON_AREAS defaults (schema)

username		USER
study_id 		(currval(''rif40_study_id_seq''::regclass))::integer

The actual comparison area comes from rif40_geo_pkg.rif40_get_default_comparison_area().
This returns all areas at the default comparison area level covered by the users selected geolevels.

7. INSERT INTO rif40_inv_covariates

RIF40_INV_COVARIATES defaults (schema)

username		USER
study_id 		(currval(''rif40_study_id_seq''::regclass))::integer
inv_id			(nextval(''rif40_inv_id_seq''::regclass))::integer


RIF40_INV_COVARIATES defaults (this function)

min, max		MIN/MAX for covariate
study_geolevel_name	study_geolevel_name FROM rif40_studies

8. INSERT INTO rif40_study_shares if any RIF DB role is a RIF manager and extraction is permitted

RIF40_STUDY_SHARES defaults (schema)

grantor			USER
study_id 		(currval(''rif40_study_id_seq''::regclass))::integer
';

GRANT EXECUTE ON FUNCTION rif40_sm_pkg.rif40_create_disease_mapping_example(
	VARCHAR,VARCHAR,VARCHAR,VARCHAR,VARCHAR,VARCHAR[],VARCHAR,VARCHAR,VARCHAR,VARCHAR,INTEGER,INTEGER,VARCHAR[][],VARCHAR[],VARCHAR[], VARCHAR, VARCHAR) 
	TO rif_manager;
GRANT EXECUTE ON FUNCTION rif40_sm_pkg.rif40_create_disease_mapping_example(
	VARCHAR,VARCHAR,VARCHAR,VARCHAR,VARCHAR,VARCHAR[],VARCHAR,VARCHAR,VARCHAR,VARCHAR,INTEGER,INTEGER,VARCHAR[][],VARCHAR[],VARCHAR[], VARCHAR, VARCHAR) 
 	TO rif40;
	
CREATE OR REPLACE FUNCTION rif40_sm_pkg._rif40_create_disease_mapping_example(	
	sql_stmt				VARCHAR,
	study_name				VARCHAR,
	parent_test_id			INTEGER,
	test_run_class			VARCHAR,
	pg_debug_functions		Text[] DEFAULT NULL)
RETURNS INTEGER
SECURITY INVOKER
AS $func$
/*
Function: 	_rif40_create_disease_mapping_example()
Parameters:	SQL statement, study name, parent_test_id,
			test run class,
			Array of Postgres functions for test harness to enable debug on
Returns:	New parent_test_id
Description:	Execute disease mapping exmaple	SQL statement, register with test harness,
			
 */
DECLARE
	f_test_id 		INTEGER;
BEGIN
--
-- Check for NULLs
--
	IF study_name IS NULL THEN
		PERFORM rif40_log_pkg.rif40_error(-56215, '_rif40_create_disease_mapping_example', 
			'NULL study name');
	ELSIF sql_stmt IS NULL THEN
		PERFORM rif40_log_pkg.rif40_error(-56216, '_rif40_create_disease_mapping_example', 
			'NULL SQL statement for study: %',
			study_name::VARCHAR);
	END IF;
--
-- Run SQL statement
--
	PERFORM rif40_sql_pkg.rif40_ddl(sql_stmt);
	
--
-- Register with test harness
--
	f_test_id:=rif40_sql_pkg._rif40_sql_test_register(
			sql_stmt 		/* test_stmt */, 
			test_run_class  /* test_run_class */, 
			study_name 	    /* test_case_title */, 
			NULL::Text[][] 	/* results */, 
			NULL::XML 		/* results_xml */,
			NULL			/* pg_error_code_expected */, 
			FALSE			/* raise_exception_on_failure */, 
			TRUE			/* expected_result */, 
			parent_test_id	/* parent_test_id */,
			pg_debug_functions /* Array of Postgres functions for test harness to enable debug on */);	

--
-- Return new parent_test_id
--			
	RETURN f_test_id;
END;
$func$ LANGUAGE plpgsql;
 
COMMENT ON FUNCTION rif40_sm_pkg._rif40_create_disease_mapping_example(VARCHAR, VARCHAR, INTEGER, VARCHAR, Text[]) IS 'Function: 	_rif40_create_disease_mapping_example()
Parameters:	SQL statement, study name, parent_test_id,
			test run class,
			Array of Postgres functions for test harness to enable debug on
Returns:	New parent_test_id
Description:	Execute disease mapping exmaple	SQL statement, register with test harness';

--
-- Eof