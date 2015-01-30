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
-- Rapid Enquiry Facility (RIF) - Populate tile lookup table 
--							      T_RIF40_<GEOGRAPHY>_MAPTILES from simplified geometry
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
-- Error codes assignment (see PLpgsql\Error_codes.txt):
--
-- rif40_xml_pkg:
--
-- rif40_GetMapAreas: 		52051 to 52099
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
		RAISE EXCEPTION 'C20900: User check failed: % is not rif40', user;	
	END IF;
END;
$$;

DROP FUNCTION IF EXISTS rif40_geo_pkg.populate_rif40_tiles(VARCHAR);
	
CREATE OR REPLACE FUNCTION rif40_geo_pkg.populate_rif40_tiles(
	l_geography 		VARCHAR)
RETURNS VOID
SECURITY INVOKER
AS $body$
/*
Function: 		populate_rif40_tiles()
Parameters:		Geography
Returns:		Nothing
Description:	Populate tile lookup table T_RIF40_<GEOGRAPHY>_MAPTILES from simplified geometry

Error range: populate_rif40_tiles:								60100 to 60149

 */
DECLARE
 	c1pop_tiles CURSOR(l_geography VARCHAR) FOR
		SELECT *
		  FROM rif40_geographies
		 WHERE geography = l_geography;
--
	c1_rec 			RECORD;
--
	sql_stmt 		VARCHAR;
--
	num_rows		INTEGER;
--
	stp 			TIMESTAMP WITH TIME ZONE:=clock_timestamp();
	stp2 			TIMESTAMP WITH TIME ZONE:=clock_timestamp();
	etp 			TIMESTAMP WITH TIME ZONE;
	took 			INTERVAL;
--
	zoomlevel 		INTEGER;
	max_zoomlevel 	INTEGER:=11;
--
	error_message 	VARCHAR;
	v_detail 		VARCHAR:='(Not supported until 9.2; type SQL statement into psql to see remote error)';	
	v_context		VARCHAR;
 BEGIN
--
-- Must be rif40 or have rif_user or rif_manager role
--
	IF NOT rif40_sql_pkg.is_rif40_user_manager_or_schema() THEN
		PERFORM rif40_log_pkg.rif40_error(-60100, 'populate_rif40_tiles', 'User % must be rif40 or have rif_user or rif_manager role', 
			USER::VARCHAR	/* Username */);
	END IF;
	--
-- Test geography
--
	IF l_geography IS NULL THEN
		PERFORM rif40_log_pkg.rif40_error(-60101, 'populate_rif40_tiles', 'NULL geography parameter');
	END IF;	
--
	OPEN c1pop_tiles(l_geography);
	FETCH c1pop_tiles INTO c1_rec;
	CLOSE c1pop_tiles;
--
	IF c1_rec.geography IS NULL THEN
		PERFORM rif40_log_pkg.rif40_error(-60103, 'populate_rif40_tiles', 'geography: % not found', 
			l_geography::VARCHAR	/* Geography */);
	END IF;	
--
	sql_stmt:='DELETE FROM '||quote_ident('t_rif40_'||LOWER(c1_rec.geography)||'_maptiles');
	PERFORM rif40_sql_pkg.rif40_ddl(sql_stmt);
--
	sql_stmt:='INSERT INTO '||quote_ident('t_rif40_'||LOWER(c1_rec.geography)||'_maptiles')||E'\n'||
			'(geography, geolevel_name, tile_id, x_tile_number, y_tile_number, zoomlevel, optimised_geojson, optimised_topojson, gid)'||E'\n'||
			'WITH a AS ( /* level geolevel */'||E'\n'||
			'	SELECT a1.geography, a1.geolevel_name,'||E'\n'||
			'	       MIN(geolevel_id) AS min_geolevel_id,'||E'\n'||
			'		   $1::INTEGER AS zoomlevel,'||E'\n'||
			'		   a2.max_geolevel_id'||E'\n'||
			'	  FROM rif40_geolevels a1, ('||E'\n'||
			'			SELECT geography, MAX(geolevel_id) AS max_geolevel_id FROM rif40_geolevels GROUP BY geography) a2'||E'\n'||
			'	 WHERE a1.geography = $2'||E'\n'||
			'	   AND a1.geography = a2.geography'||E'\n'||
			'	 GROUP BY a1.geography, a1.geolevel_name, a2.max_geolevel_id'||E'\n'||
			'	HAVING MIN(geolevel_id) = 1'||E'\n'||
			'), b AS ( /* Get bounds of geography */'||E'\n'||
			'	SELECT b.geography,'||E'\n'||
			'	       a.min_geolevel_id,'||E'\n'||
			'	       a.max_geolevel_id,'||E'\n'||
			'	       a.zoomlevel,'||E'\n'||
			'          CASE '||E'\n'||
			'				WHEN a.zoomlevel <= 6 THEN ST_XMax(b.optimised_geometry) 				/* Optimised for zoom level 6 */'||E'\n'||
			'				WHEN a.zoomlevel IN (7, 8) THEN ST_XMax(b.optimised_geometry_2) 		/* Optimised for zoom level 8 */'||E'\n'||
			'				WHEN a.zoomlevel IN (9, 10, 11) THEN ST_XMax(b.optimised_geometry_3) 	/* Optimised for zoom level 11 */'||E'\n'||
			'				ELSE NULL'||E'\n'||
			'		   END AS Xmax,'||E'\n'||
			'          CASE '||E'\n'||
			'				WHEN a.zoomlevel <= 6 THEN ST_XMin(b.optimised_geometry) 				/* Optimised for zoom level 6 */'||E'\n'||
			'				WHEN a.zoomlevel IN (7, 8) THEN ST_XMin(b.optimised_geometry_2) 		/* Optimised for zoom level 8 */'||E'\n'||
			'				WHEN a.zoomlevel IN (9, 10, 11) THEN ST_XMin(b.optimised_geometry_3) 	/* Optimised for zoom level 11 */'||E'\n'||
			'				ELSE NULL'||E'\n'||
			'		   END AS Xmin,'||E'\n'||
			'          CASE '||E'\n'||
			'				WHEN a.zoomlevel <= 6 THEN ST_YMax(b.optimised_geometry) 				/* Optimised for zoom level 6 */'||E'\n'||
			'				WHEN a.zoomlevel IN (7, 8) THEN ST_YMax(b.optimised_geometry_2) 		/* Optimised for zoom level 8 */'||E'\n'||
			'				WHEN a.zoomlevel IN (9, 10, 11) THEN ST_YMax(b.optimised_geometry_3) 	/* Optimised for zoom level 11 */'||E'\n'||
			'				ELSE NULL'||E'\n'||
			'		   END AS Ymax,'||E'\n'||
			'          CASE '||E'\n'||
			'				WHEN a.zoomlevel <= 6 THEN ST_YMin(b.optimised_geometry) 				/* Optimised for zoom level 6 */'||E'\n'||
			'				WHEN a.zoomlevel IN (7, 8) THEN ST_YMin(b.optimised_geometry_2) 		/* Optimised for zoom level 8 */'||E'\n'||
			'				WHEN a.zoomlevel IN (9, 10, 11) THEN ST_YMin(b.optimised_geometry_3) 	/* Optimised for zoom level 11 */'||E'\n'||
			'				ELSE NULL'||E'\n'||
			'		   END AS Ymin'||E'\n'||			
			'      FROM '||quote_ident('t_rif40_'||LOWER(c1_rec.geography)||'_geometry')||' b, a'||E'\n'||
 			'    WHERE a.geography = b.geography'||E'\n'||
			'	   AND a.geolevel_name = b.geolevel_name'||E'\n'||
			'), d AS ( /* Convert XY bounds to tile numbers */'||E'\n'||
			'	SELECT geography, min_geolevel_id, max_geolevel_id, zoomlevel,'||E'\n'|| 
			'		   Xmin AS area_Xmin, Xmax AS area_Xmax, Ymin AS area_Ymin, Ymax AS area_Ymax,'||E'\n'||
			'           rif40_geo_pkg.latitude2tile(Xmin, zoomlevel) AS X_mintile,'||E'\n'||
			'           rif40_geo_pkg.latitude2tile(Xmax, zoomlevel) AS X_maxtile,'||E'\n'||
			'           rif40_geo_pkg.longitude2tile(Ymin, zoomlevel) AS Y_mintile,'||E'\n'||
			'           rif40_geo_pkg.longitude2tile(Ymax, zoomlevel) AS Y_maxtile'||E'\n'||
			'      FROM b'||E'\n'||
			'), e AS ( /* Generate latitude tile series */'||E'\n'||
			'	SELECT min_geolevel_id,'||E'\n'|| 
			'		   CASE WHEN X_mintile > X_maxtile THEN generate_series(X_mintile, X_maxtile, -1)'||E'\n'||
			'				ELSE generate_series(X_mintile, X_maxtile) END AS x_series'||E'\n'||
			'	  FROM d'||E'\n'||
			'), f AS ( /* Generate longitude tile series */'||E'\n'||
			'	SELECT min_geolevel_id,'||E'\n'||
			'		   CASE WHEN Y_mintile > Y_maxtile THEN generate_series(Y_mintile, Y_maxtile, -1)'||E'\n'||
			'				ELSE generate_series(Y_mintile, Y_maxtile) END AS y_series'||E'\n'|| 
			'	  FROM d'||E'\n'||
			'), g AS ( /* Generate GEOLEVEL_ID series */'||E'\n'||
			'	SELECT geography, min_geolevel_id, zoomlevel,'||E'\n'|| 
			'		   area_Xmin, area_Xmax, area_Ymin, area_Ymax,'||E'\n'|| 
			'		   generate_series(min_geolevel_id::int, max_geolevel_id::int) AS geolevel_series'||E'\n'||
			'	  FROM d'||E'\n'||
			'), h AS ( /* For each tile generated by the three series build bounding box */'||E'\n'||
			'	SELECT g.geography, g.geolevel_series AS geolevel_id, zoomlevel,'||E'\n'|| 
			'		   area_Xmin, area_Xmax, area_Ymin, area_Ymax,'||E'\n'|| 
			'		   x_series, y_series, h.geolevel_name,'||E'\n'||
			'		   g.geography||''_''||g.geolevel_series::Text||''_''||h.geolevel_name||''_''||'||E'\n'||
			'				zoomlevel::Text||''_''||x_series::Text||''_''||y_series::Text AS tile_id,'||E'\n'||
			'	       rif40_geo_pkg.tile2latitude(x_series::INTEGER, zoomlevel) AS tile_Xmin,'||E'\n'||
			'	       rif40_geo_pkg.tile2latitude((x_series+1)::INTEGER, zoomlevel) AS tile_Xmax,'||E'\n'||
			'	       rif40_geo_pkg.tile2longitude(y_series::INTEGER, zoomlevel) AS tile_Ymin,'||E'\n'||
			'	       rif40_geo_pkg.tile2longitude((y_series+1)::INTEGER, zoomlevel) AS tile_Ymax'||E'\n'||
			'      FROM rif40_geolevels h, g, /* Twin full joins */'||E'\n'||
			'      		e FULL JOIN f ON (e.min_geolevel_id = f.min_geolevel_id)'||E'\n'||
			'     WHERE g.min_geolevel_id = e.min_geolevel_id'||E'\n'||
			'       AND h.geography       = g.geography'||E'\n'||
 			'       AND g.geolevel_series = h.geolevel_id'||E'\n'||
			'), i AS ( /* Intersect bound with geolevel geometry to build list of area_ids */'||E'\n'||
			'	SELECT h.geography,'||E'\n'||
			'           h.geolevel_name,'||E'\n'||
			'           h.tile_id,'||E'\n'||
			'           h.x_series AS x_tile_number,'||E'\n'||
			'           h.y_series AS y_tile_number,'||E'\n'||
			'           h.zoomlevel,'||E'\n'||
			'	        i.area_id,'||E'\n'||
			'           ST_MakeEnvelope(h.tile_Xmin, h.tile_Ymin, h.tile_Xmax, h.tile_Ymax) AS geom /* Bound */'||E'\n'||
			'      FROM t_rif40_sahsu_geometry i, h'||E'\n'||
			'     WHERE optimised_geometry_3 && ST_MakeEnvelope(h.tile_Xmin, h.tile_Ymin, h.tile_Xmax, h.tile_Ymax)'||E'\n'||
 			'      AND h.geolevel_name = i.geolevel_name    /* Partition eliminate */'||E'\n'||
			'												/* Intersect bound with geolevel geometry */'||E'\n'||
			'), j AS ( /* Build array of area_ids for _rif40_get_geojson_as_js() */'||E'\n'||
			'	SELECT i.geography,'||E'\n'||
			'           i.geolevel_name,'||E'\n'||
			'           i.tile_id,'||E'\n'||
			'           i.x_tile_number,'||E'\n'||
			'           i.y_tile_number,'||E'\n'||
			'           i.zoomlevel,'||E'\n'||
			'		    COUNT(DISTINCT(i.area_id)) AS total  	/* Total area IDs */,'||E'\n'||
			'           ARRAY_AGG(i.area_id) AS area_id_list 	/* Array of area IDs */,'||E'\n'||
			'           ST_IsValid(i.geom) AS is_valid   		/* Test bound */,'||E'\n'||
			'           ST_Area(i.geom) AS area         		/* Area of bound */'||E'\n'||
			'	FROM i'||E'\n'||
			'	GROUP BY i.geography,'||E'\n'||
			'           i.geolevel_name,'||E'\n'||
			'           i.tile_id,'||E'\n'||
			'           i.x_tile_number,'||E'\n'||
			'           i.y_tile_number,'||E'\n'||
			'           i.zoomlevel,'||E'\n'||
			'		   ST_IsValid(i.geom),'||E'\n'||
			'		   ST_Area(i.geom)'||E'\n'||
			'), k AS ( /* Convert area_ids array to GeoJSON using _rif40_get_geojson_as_js() */'||E'\n'||
			'	SELECT j.geography,'||E'\n'||
			'		   j.geolevel_name,'||E'\n'||
			'          j.tile_id,'||E'\n'||
			'          j.x_tile_number,'||E'\n'||
			'          j.y_tile_number,'||E'\n'||
			'          j.zoomlevel,'||E'\n'||
			'		   j.total,'||E'\n'||
			'		   rif40_xml_pkg._rif40_get_geojson_as_js('||E'\n'||
			'					j.geography, '||E'\n'||
			'					j.geolevel_name, '||E'\n'||
			'					j.area_id_list, '||E'\n'||
			'					(j.total+2)::INTEGER	/* Add 2 for header and footer */,'||E'\n'||
			'					TRUE 					/* Produce JSON not JS */, '||E'\n'||
			'					j.zoomlevel::INTEGER)::JSON AS optimised_geojson'||E'\n'||
			'	FROM j'||E'\n'||
			')'||E'\n'||
			'SELECT geography,'||E'\n'||
			'       geolevel_name,'||E'\n'||
			'       tile_id,'||E'\n'||
			'       x_tile_number,'||E'\n'||
			'       y_tile_number,'||E'\n'||
			'       zoomlevel,'||E'\n'||
			'       optimised_geojson,'||E'\n'||				
	 		'       to_json(''X''::Text)::JSON AS optimised_topojson /* Dummy value */,'||E'\n'||			
			'       ROW_NUMBER() OVER() AS gid'||E'\n'||
			'  FROM k'||E'\n'||
			' WHERE k.total > 0'||E'\n'||
			' ORDER BY 1';
--
	PERFORM rif40_log_pkg.rif40_log('DEBUG1', 'populate_rif40_tiles', 
		'[60104] Populating RIF tiles for geography: %; % zoomlevels; SQL>'||E'\n'||'%;',
		c1_rec.geography::VARCHAR		/* Geography */,
		max_zoomlevel::VARCHAR			/* Max zoom level */,
		sql_stmt::VARCHAR				/* SQL */);
--		
	FOR zoomlevel IN 0 .. max_zoomlevel LOOP

		stp2:=clock_timestamp();

		BEGIN
			EXECUTE sql_stmt USING zoomlevel, c1_rec.geography;		
			GET DIAGNOSTICS num_rows = ROW_COUNT;
		EXCEPTION
			WHEN others THEN
--
-- Print exception to INFO, re-raise
--
				GET STACKED DIAGNOSTICS v_detail = PG_EXCEPTION_DETAIL,
										v_context = PG_EXCEPTION_CONTEXT;
				error_message:='populate_rif40_tiles('||c1_rec.geography||'); zoomlevel: '||zoomlevel||'; caught: '||E'\n'||
					SQLERRM::VARCHAR||' in SQL> '||E'\n'||sql_stmt||E'\n'||
							'Detail: '||v_detail::VARCHAR||E'\n'||
							'Context: '||v_context::VARCHAR;
				RAISE NOTICE '60105: %', error_message;
--
				RAISE;
		END;
--
-- Instrument
--
		etp:=clock_timestamp();
		took:=age(etp, stp2);
		PERFORM rif40_log_pkg.rif40_log('DEBUG1', 'populate_rif40_tiles', 
			'[60104] Populated RIF tiles for geography: %; zoomlevel %/%, rows: %, time taken: %',
			c1_rec.geography::VARCHAR		/* Geography */,
			zoomlevel::VARCHAR				/* Zoom level */,
			max_zoomlevel::VARCHAR			/* Max zoom level */,
			num_rows::VARCHAR				/* Rows inserted */,
			took::VARCHAR					/* Time taken */);
	END LOOP;
--
-- Instrument
--
	etp:=clock_timestamp();
	took:=age(etp, stp);
	PERFORM rif40_log_pkg.rif40_log('DEBUG1', 'populate_rif40_tiles', 
		'[60106] Populated RIF tiles for geography: %, overall time taken: %',
		c1_rec.geography::VARCHAR	/* Geography */,
		took::VARCHAR				/* Time taken */);
--
	sql_stmt:='ANALYZE VERBOSE '||quote_ident('t_rif40_'||LOWER(c1_rec.geography)||'_maptiles');
	PERFORM rif40_sql_pkg.rif40_ddl(sql_stmt);
END;
$body$
LANGUAGE PLPGSQL;

COMMENT ON FUNCTION rif40_geo_pkg.populate_rif40_tiles(VARCHAR) IS 'Function: 		populate_rif40_tiles()
Parameters:		Geography
Returns:		Nothing
Description:	Populate tile lookup table T_RIF40_<GEOGRAPHY>_MAPTILES from simplified geometry

SQL>
INSERT INTO t_rif40_sahsu_maptiles
(geography, geolevel_name, tile_id, x_tile_number, y_tile_number, zoomlevel, optimised_geojson, optimised_topojson, gid)
WITH a AS ( /* level geolevel */
        SELECT a1.geography, a1.geolevel_name,
               MIN(geolevel_id) AS min_geolevel_id,
                   $1::INTEGER AS zoomlevel,
                   a2.max_geolevel_id
          FROM rif40_geolevels a1, (
                        SELECT geography, MAX(geolevel_id) AS max_geolevel_id FROM rif40_geolevels GROUP BY geography) a2
         WHERE a1.geography = $2
           AND a1.geography = a2.geography
         GROUP BY a1.geography, a1.geolevel_name, a2.max_geolevel_id
        HAVING MIN(geolevel_id) = 1
), b AS ( /* Get bounds of geography */
        SELECT b.geography,
               a.min_geolevel_id,
               a.max_geolevel_id,
               a.zoomlevel,
          CASE
                                WHEN a.zoomlevel <= 6 THEN ST_XMax(b.optimised_geometry)                /* Optimised for zoom level 6 */
                                WHEN a.zoomlevel IN (7, 8) THEN ST_XMax(b.optimised_geometry_2)         /* Optimised for zoom level 8 */
                                WHEN a.zoomlevel IN (9, 10, 11) THEN ST_XMax(b.optimised_geometry_3)    /* Optimised for zoom level 11 */
                                ELSE NULL
                   END AS Xmax,
          CASE
                                WHEN a.zoomlevel <= 6 THEN ST_XMin(b.optimised_geometry)                /* Optimised for zoom level 6 */
                                WHEN a.zoomlevel IN (7, 8) THEN ST_XMin(b.optimised_geometry_2)         /* Optimised for zoom level 8 */
                                WHEN a.zoomlevel IN (9, 10, 11) THEN ST_XMin(b.optimised_geometry_3)    /* Optimised for zoom level 11 */
                                ELSE NULL
                   END AS Xmin,
          CASE
                                WHEN a.zoomlevel <= 6 THEN ST_YMax(b.optimised_geometry)                /* Optimised for zoom level 6 */
                                WHEN a.zoomlevel IN (7, 8) THEN ST_YMax(b.optimised_geometry_2)         /* Optimised for zoom level 8 */
                                WHEN a.zoomlevel IN (9, 10, 11) THEN ST_YMax(b.optimised_geometry_3)    /* Optimised for zoom level 11 */
                                ELSE NULL
                   END AS Ymax,
          CASE
                                WHEN a.zoomlevel <= 6 THEN ST_YMin(b.optimised_geometry)                /* Optimised for zoom level 6 */
                                WHEN a.zoomlevel IN (7, 8) THEN ST_YMin(b.optimised_geometry_2)         /* Optimised for zoom level 8 */
                                WHEN a.zoomlevel IN (9, 10, 11) THEN ST_YMin(b.optimised_geometry_3)    /* Optimised for zoom level 11 */
                                ELSE NULL
                   END AS Ymin
      FROM t_rif40_sahsu_geometry b, a
    WHERE a.geography = b.geography
           AND a.geolevel_name = b.geolevel_name
), d AS ( /* Convert XY bounds to tile numbers */
        SELECT geography, min_geolevel_id, max_geolevel_id, zoomlevel,
                   Xmin AS area_Xmin, Xmax AS area_Xmax, Ymin AS area_Ymin, Ymax AS area_Ymax,
           rif40_geo_pkg.latitude2tile(Xmin, zoomlevel) AS X_mintile,
           rif40_geo_pkg.latitude2tile(Xmax, zoomlevel) AS X_maxtile,
           rif40_geo_pkg.longitude2tile(Ymin, zoomlevel) AS Y_mintile,
           rif40_geo_pkg.longitude2tile(Ymax, zoomlevel) AS Y_maxtile
      FROM b
), e AS ( /* Generate latitude tile series */
        SELECT min_geolevel_id,
                   CASE WHEN X_mintile > X_maxtile THEN generate_series(X_mintile, X_maxtile, -1)
                                ELSE generate_series(X_mintile, X_maxtile) END AS x_series
          FROM d
), f AS ( /* Generate longitude tile series */
        SELECT min_geolevel_id,
                   CASE WHEN Y_mintile > Y_maxtile THEN generate_series(Y_mintile, Y_maxtile, -1)
                                ELSE generate_series(Y_mintile, Y_maxtile) END AS y_series
          FROM d
), g AS ( /* Generate GEOLEVEL_ID series */
        SELECT geography, min_geolevel_id, zoomlevel,
                   area_Xmin, area_Xmax, area_Ymin, area_Ymax,
                   generate_series(min_geolevel_id::int, max_geolevel_id::int) AS geolevel_series
          FROM d
), h AS ( /* For each tile generated by the three series build bounding box */
        SELECT g.geography, g.geolevel_series AS geolevel_id, zoomlevel,
                   area_Xmin, area_Xmax, area_Ymin, area_Ymax,
                   x_series, y_series, h.geolevel_name,
                   g.geography||'_'||g.geolevel_series::Text||'_'||h.geolevel_name||'_'||
                                zoomlevel::Text||'_'||x_series::Text||'_'||y_series::Text AS tile_id,
               rif40_geo_pkg.tile2latitude(x_series::INTEGER, zoomlevel) AS tile_Xmin,
               rif40_geo_pkg.tile2latitude((x_series+1)::INTEGER, zoomlevel) AS tile_Xmax,
               rif40_geo_pkg.tile2longitude(y_series::INTEGER, zoomlevel) AS tile_Ymin,
               rif40_geo_pkg.tile2longitude((y_series+1)::INTEGER, zoomlevel) AS tile_Ymax
      FROM rif40_geolevels h, g, /* Twin full joins */
                e FULL JOIN f ON (e.min_geolevel_id = f.min_geolevel_id)
     WHERE g.min_geolevel_id = e.min_geolevel_id
       AND h.geography       = g.geography
       AND g.geolevel_series = h.geolevel_id
), i AS ( /* Intersect bound with geolevel geometry to build list of area_ids */
        SELECT h.geography,
           h.geolevel_name,
           h.tile_id,
           h.x_series AS x_tile_number,
           h.y_series AS y_tile_number,
           h.zoomlevel,
                i.area_id,
           ST_MakeEnvelope(h.tile_Xmin, h.tile_Ymin, h.tile_Xmax, h.tile_Ymax) AS geom /* Bound */
      FROM t_rif40_sahsu_geometry i, h
     WHERE optimised_geometry_3 && ST_MakeEnvelope(h.tile_Xmin, h.tile_Ymin, h.tile_Xmax, h.tile_Ymax)
      AND h.geolevel_name = i.geolevel_name    /* Partition eliminate */
                                                                                                /* Intersect bound with geolevel geometry */
), j AS ( /* Build array of area_ids for _rif40_get_geojson_as_js() */
        SELECT i.geography,
           i.geolevel_name,
           i.tile_id,
           i.x_tile_number,
           i.y_tile_number,
           i.zoomlevel,
                    COUNT(DISTINCT(i.area_id)) AS total /* Total area IDs */,
           ARRAY_AGG(i.area_id) AS area_id_list         /* Array of area IDs */,
           ST_IsValid(i.geom) AS is_valid               /* Test bound */,
           ST_Area(i.geom) AS area                      /* Area of bound */
        FROM i
        GROUP BY i.geography,
           i.geolevel_name,
           i.tile_id,
           i.x_tile_number,
           i.y_tile_number,
           i.zoomlevel,
                   ST_IsValid(i.geom),
                   ST_Area(i.geom)
), k AS ( /* Convert area_ids array to GeoJSON using _rif40_get_geojson_as_js() */
        SELECT j.geography,
                   j.geolevel_name,
          j.tile_id,
          j.x_tile_number,
          j.y_tile_number,
          j.zoomlevel,
                   j.total,
                   rif40_xml_pkg._rif40_get_geojson_as_js(
                                        j.geography,
                                        j.geolevel_name,
                                        j.area_id_list,
                                        (j.total+2)::INTEGER    /* Add 2 for header and footer */,
                                        TRUE                    /* Produce JSON not JS */,
                                        j.zoomlevel::INTEGER)::JSON AS optimised_geojson
        FROM j
)
SELECT geography,
       geolevel_name,
       tile_id,
       x_tile_number,
       y_tile_number,
       zoomlevel,
       optimised_geojson,
       to_json(''X''::Text)::JSON AS optimised_topojson /* Dummy value */,
       ROW_NUMBER() OVER() AS gid
  FROM k
 WHERE k.total > 0
 ORDER BY 1;
 
psql:alter_scripts/v4_0_alter_5.sql:542: INFO:  [DEBUG1] populate_rif40_tiles(): [60104] Populated RIF tiles for geography: SAHSU; zoomlevel 0/11, rows: 0, time taken: 00:00:05.998
psql:alter_scripts/v4_0_alter_5.sql:542: INFO:  [DEBUG1] populate_rif40_tiles(): [60104] Populated RIF tiles for geography: SAHSU; zoomlevel 1/11, rows: 0, time taken: 00:00:05.93
psql:alter_scripts/v4_0_alter_5.sql:542: INFO:  [DEBUG1] populate_rif40_tiles(): [60104] Populated RIF tiles for geography: SAHSU; zoomlevel 2/11, rows: 0, time taken: 00:00:06.335
psql:alter_scripts/v4_0_alter_5.sql:542: INFO:  [DEBUG1] populate_rif40_tiles(): [60104] Populated RIF tiles for geography: SAHSU; zoomlevel 3/11, rows: 0, time taken: 00:00:05.911
psql:alter_scripts/v4_0_alter_5.sql:542: INFO:  [DEBUG1] populate_rif40_tiles(): [60104] Populated RIF tiles for geography: SAHSU; zoomlevel 4/11, rows: 0, time taken: 00:00:05.852
psql:alter_scripts/v4_0_alter_5.sql:542: INFO:  [DEBUG1] populate_rif40_tiles(): [60104] Populated RIF tiles for geography: SAHSU; zoomlevel 5/11, rows: 0, time taken: 00:00:05.861
psql:alter_scripts/v4_0_alter_5.sql:542: INFO:  [DEBUG1] populate_rif40_tiles(): [60104] Populated RIF tiles for geography: SAHSU; zoomlevel 6/11, rows: 0, time taken: 00:00:09.295
psql:alter_scripts/v4_0_alter_5.sql:542: INFO:  [DEBUG1] populate_rif40_tiles(): [60104] Populated RIF tiles for geography: SAHSU; zoomlevel 7/11, rows: 0, time taken: 00:00:16.339
psql:alter_scripts/v4_0_alter_5.sql:542: INFO:  [DEBUG1] populate_rif40_tiles(): [60104] Populated RIF tiles for geography: SAHSU; zoomlevel 8/11, rows: 0, time taken: 00:00:34.829
psql:alter_scripts/v4_0_alter_5.sql:542: INFO:  [DEBUG1] populate_rif40_tiles(): [60104] Populated RIF tiles for geography: SAHSU; zoomlevel 9/11, rows: 0, time taken: 00:01:18.221
psql:alter_scripts/v4_0_alter_5.sql:542: INFO:  [DEBUG1] populate_rif40_tiles(): [60104] Populated RIF tiles for geography: SAHSU; zoomlevel 10/11, rows: 0, time taken: 00:04:02.419
psql:alter_scripts/v4_0_alter_5.sql:542: INFO:  [DEBUG1] populate_rif40_tiles(): [60104] Populated RIF tiles for geography: SAHSU; zoomlevel 11/11, rows: 0, time taken: 00:14:03.615
psql:alter_scripts/v4_0_alter_5.sql:542: INFO:  [DEBUG1] populate_rif40_tiles(): [60106] Populated RIF tiles for geography: SAHSU, overall time taken: 00:21:02.701

';
 
 --
 -- Eof