/**
 * The Rapid Inquiry Facility (RIF) is an automated tool devised by SAHSU 
 * that rapidly addresses epidemiological and public health questions using 
 * routinely collected health and population data and generates standardised 
 * rates and relative risks for any given health outcome, for specified age 
 * and year ranges, for any given geographical area.
 *
 * Copyright 2016 Imperial College London, developed by the Small Area
 * Health Statistics Unit. The work of the Small Area Health Statistics Unit 
 * is funded by the Public Health England as part of the MRC-PHE Centre for 
 * Environment and Health. Funding for this project has also been received 
 * from the United States Centers for Disease Control and Prevention.  
 *
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

 * Peter Hambly
 * @author phambly
 */

/* 
 * SERVICE to store frnt end parameters (so they can be set in the middleware)
 */
angular.module("RIF")
        .factory('ParametersService',
                function () {
                    //this is used for all front end parameters. They originate from the Middle ware (when implemented)
                    var defaultParameters = {
                        usePouchDBCache: 	false,			// DO NOT Use PouchDB caching in TopoJSONGridLayer.js; it interacts with the diseasemap sync;
                        debugEnabled:		false,			// Disable front end debugging
                        disableMapLocking:	false,			// Disable disease map initial sync [You can re-enable it!]
                        disableSelectionLocking: false,		// Disable selection locking [You can re-enable it!]
                        
                        syncMapping2EventsDisabled: false,	// Disable syncMapping2Events handler [for leak testing]
                        rrDropLineRedrawDisabled: false,	// Disable rrDropLineRedraw handler [for leak testing]
                        rrchartWatchDisabled: false,		// Disable Angular $watch on rrchart<mapID> [for leak testing]
                        
                        mapLockingOptions: {},				// Map locking options (for Leaflet.Sync())
                        
                        /*
                         * For the Color Brewer names see: https://github.com/timothyrenner/ColorBrewer.jl
                         * Derived from: http://colorbrewer2.org/
                         */
                        mappingDefaults: {				
                            'diseasemap1': {
                                    method: 	'quantile', 
                                    feature:	'smoothed_smr',
                                    intervals: 	9,
                                    invert:		true,
                                    brewerName:	"PuOr"
                            },
                            'diseasemap2': {
                                    method: 	'AtlasProbability', 
                                    feature:	'posterior_probability',
                                    breaks: 	[0.0, 0.20, 0.81, 1.0],	
                                    invert:		false,
                                    brewerName:	"RdYlGn"
                            },
                            'viewermap': {
                                    method: 	'AtlasRelativeRisk', 
                                    feature:	'relative_risk',
                                    breaks:		[-Infinity, 0.68, 0.76, 0.86, 0.96, 1.07, 1.2, 1.35, 1.51, Infinity],
                                    invert:		true,
                                    brewerName: "PuOr"
                            }
                        },
                        defaultLogin: {						// DO NOT SET in a production environment; for use on single user tests system only!
                            username: 	"",
                            password:	""
                        },
                        userMethods: {
                            'AtlasRelativeRisk': {
                                    description: 'Atlas Relative Risk',
                                    breaks:		[-Infinity, 0.68, 0.76, 0.86, 0.96, 1.07, 1.2, 1.35, 1.51, Infinity],
                                    invert:		true,
                                    brewerName: "PuOr",
                                    invalidScales: ["Constant", "Dark2", "Accent", "Pastel2", "Set2"]
                            },
                            'AtlasProbability': {
                                    description: 'Atlas Probability',
                                    feature:	'posterior_probability',
                                    breaks: 	[0.0, 0.20, 0.81, 1.0],	
                                    invert:		false,
                                    brewerName:	"RdYlGn",
                                    invalidScales: ["Constant"]
                            }
                        },
                        selectorBands: { // Study and comparison are selectors
                            weight: 3,
                            opacity: 0.8,
                            fillOpacity: 0,
                            bandColours: ['#e41a1c', '#377eb8', '#4daf4a', '#984ea3', '#ff7f00', '#ffff33']
                        },
                        disableMouseClicksAt: 5000, // Areas
                        // Additional modules support. Not enabled by default until in production
                        additionalModules: {
                            multipleCovariates: {
                                enabled: false,
                                description: "Multiple study and additional extract covariates",
                                status: "alpha" // notYetImplemented/inDevelopment/alpha/beta/production
                            },
                            pooledAnalysis: {
                                enabled: false,
                                description: "Individual site and pooled analysis (1 or more groups of sites). Groups are defined from a categorisation variable in the shapefile or from regional area identifiers",
                                status: "inDevelopment" // notYetImplemented/inDevelopment/alpha/beta/production
                            },
                            priorSensitivityAnalysis: {
                                enabled: false,
                                description: "Prior sensitivity analysis",
                                status: "notYetImplemented" // notYetImplemented/inDevelopment/alpha/beta/production
                            },
                            informationGovernance: {
                                enabled: false,
                                description: "Information governance module",
                                status: "notYetImplemented" // notYetImplemented/inDevelopment/alpha/beta/production
                            },
                            dataLoading: {
                                enabled: false,
                                description: "Integrated data loading module",
                                status: "notYetImplemented" // notYetImplemented/inDevelopment/alpha/beta/production
                            },
                            geospatialDataLoading: {
                                enabled: false,
                                description: "Integrated geospatial data loading module",
                                status: "notYetImplemented" // notYetImplemented/inDevelopment/alpha/beta/production
                            }
                        }
					};      
                    var parameters=angular.copy(defaultParameters);                    
                    return {
                        getParameters: function () {
                            return parameters;
                        },
                        setParameters: function (params) {
							if (params) {                                
								var newParameters = params;
                                var keys=Object.keys(defaultParameters);
                                for (var i=0; i<keys.length; i++) {
                                    if (newParameters[keys[i]] == undefined) {
                                        newParameters[keys[i]] = defaultParameters[keys[i]];
                                    }
                                }
                                parameters=angular.copy(newParameters);
							}
                        },
                        setLoginParameters: function (params) {
							if (params.defaultLogin) {
								parameters.defaultLogin = params.defaultLogin;
							}
							parameters.debugEnabled=params.debugEnabled||true;
                        },
                        isModuleEnabled(moduleName) {
                            if (parameters && parameters.additionalModules && parameters.additionalModules[moduleName] &&
                                parameters.additionalModules[moduleName].enabled &&
                                parameters.additionalModules[moduleName].enabled == true) {
                                return parameters.additionalModules[moduleName].enabled;
                            }
                            else {
                                return false;
                            }
                        },
                        getModuleDescription(moduleName) {
                            if (parameters && parameters.additionalModules && parameters.additionalModules[moduleName]) {
                                return (parameters.additionalModules[moduleName].description ||
                                        "No description for module: " + moduleName);
                            }
                            else {
                                throw new Error("Invalid module: " + moduleName);
                            }
                        },
                        getModuleStatus(moduleName) {
                            if (parameters && parameters.additionalModules && parameters.additionalModules[moduleName]) {
                                if (parameters.additionalModules[moduleName].status) {
                                    switch (parameters.additionalModules[moduleName].status) {
                                        case "notYetImplemented":
                                        case "inDevelopment":
                                        case "alpha":
                                        case "beta": 
                                        case "production":
                                            break;
                                        default:
                                            throw new Error("Invalid status: " + 
                                                parameters.additionalModules[moduleName].status + " for module: " + moduleName);
                                            break;
                                    }
                                    return parameters.additionalModules[moduleName].status;
                                }
                                else {
                                    throw new Error("No status for module: " + moduleName);
                                }
                            }
                            else {
                                throw new Error("Invalid module: " + moduleName);
                            }
                        }
                    };
                });