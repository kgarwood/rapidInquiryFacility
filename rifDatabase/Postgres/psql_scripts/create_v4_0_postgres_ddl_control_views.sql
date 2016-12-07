\echo * Script: ../postgres/create_v4_0_postgres_ddl_control_views.sql autogenerated using: $Id$ by: RIF40 on: 03-MAR-2014
\echo * Create RIF40 Postgres DDL control views (used to check the setup is complete)
-- ************************************************************************
-- *
-- * DO NOT EDIT THIS SCRIPT OR MODIFY THE RIF SCHEMA - USE ALTER SCRIPTS
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
-- Rapid Enquiry Facility (RIF) - Create RIF40 Postgres DDL control views (used to check the setup is complete)
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
-- Check database is sahsuland_dev or sahsuland_empty
--
DO LANGUAGE plpgsql $$
BEGIN
	IF current_database() = 'sahsuland_dev' THEN
		RAISE INFO 'Database check: %', current_database();	
	ELSIF current_database() = 'sahsuland_empty' THEN
		RAISE INFO 'Database check: %', current_database();	
	ELSE
		RAISE EXCEPTION 'C20901: Database check failed: % is not sahsuland_dev or sahsuland_empty', current_database();	
	END IF;
END;
$$;

CREATE TABLE "rif40_columns" (
table_or_view_name_hide varchar(40)
, column_name_hide varchar(40)
, table_or_view_name_href varchar(522)
, column_name_href varchar(847)
, nullable varchar(8)
, oracle_data_type varchar(189)
, comments varchar(4000)
);
GRANT SELECT ON rif40_columns TO rif_user;
CREATE TABLE "rif40_tables_and_views" (
class varchar(13)
, table_or_view varchar(178)
, table_or_view_name_href varchar(413)
, table_or_view_name_hide varchar(40)
, comments varchar(4000)
);
GRANT SELECT ON rif40_tables_and_views TO rif_user;
CREATE TABLE "rif40_triggers" (
table_name varchar(40)
, column_name varchar(4000)
, trigger_name varchar(40)
, trigger_type varchar(16)
, triggering_event varchar(227)
, when_clause varchar(4000)
, action_type varchar(11)
, comments varchar(4000)
);
GRANT SELECT ON rif40_triggers TO rif_user;
