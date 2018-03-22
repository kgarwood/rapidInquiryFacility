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
 
 * David Morley
 * @author dmorley
 */

/*
 * CONTROLLER to handle map panels
 */

/* global L */
angular.module("RIF")
        .controller('leafletLayersCtrl', ['$scope', 'user', 'LeafletBaseMapService', 'ChoroService', 'ColorBrewerService', 
            'MappingStateService', 'ViewerStateService', 'MappingService', 'ParametersService',
            function ($scope, user, LeafletBaseMapService, ChoroService, ColorBrewerService,
                    MappingStateService, ViewerStateService, MappingService, ParametersService) {

                //Reference the parent scope, viewer or disease mapping
                var parentScope = $scope.$parent;
                parentScope.child = $scope;

                //Reference the state service
                $scope.myService = MappingStateService;
                if (parentScope.myMaps[0] === "viewermap") {
                    $scope.myService = ViewerStateService;
					ViewerStateService.setRemoveMap(function() { 
						$scope.removeMap("viewermap"); 
					});
                }
                else {
					MappingStateService.setRemoveMap(function() { 
						$scope.removeMap("diseasemap1");
						$scope.removeMap("diseasemap2");
					});
                }

                //Handle UI-Layout resize events
                $scope.$on('ui.layout.loaded', function () {
                    $scope.getD3Frames();
                });
                $scope.$on('ui.layout.resize', function (e, beforeContainer, afterContainer) {
                    $scope.getD3FramesOnResize(beforeContainer, afterContainer);
                    for (var i in parentScope.myMaps) {
                        $scope.map[parentScope.myMaps[i]].invalidateSize();
                    }
                });
				
				$scope.parameters=ParametersService.getParameters()||{
						usePouchDBCache: false,		// DO NOT Use PouchDB caching in TopoJSONGridLayer.js; it interacts with the diseasemap sync;
						disableMapLocking: false,	// Disable front end debugging
						mapLockingOptions: {},		// Map locking options (for Leaflet.Sync())
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
									intervals: 	3,
									invert:		false,
									brewerName:	"Constant"
							},
							'viewermap': {
									method: 	'quantile', 
									feature:	'relative_risk',
									intervals: 	9,
									invert:		true,
									brewerName:	"PuOr"
							}
						},
						userMethods: {
							'AtlasRelativeRisk': {
									description: 'Atlas Relative Risk',
									breaks:		[-Infinity, 0.68, 0.76, 0.86, 0.96, 1.07, 1.2, 1.35, 1.51, Infinity],
									invert:		true,
									brewerName: "PuOr"
							},
							'AtlasProbability': {
									description: 'Atlas Probability',
									breaks: 	[0.0, 0.20, 0.81, 1.0],	
									invert:		false,
									brewerName:	"RdYlGn"
							}
						}
					};	
					// DO NOT Use PouchDB caching in TopoJSONGridLayer.js; it interacts with the diseasemap sync;	

				$scope.disableMapLocking=$scope.parameters.disableMapLocking||false;	
					// Disable disease map initial sync [for leak testing]
				$scope.layerStats = {
					layerAdds: 0,
					subLayerAdds: 0,
					layerUpdates: 0,
					subLayerUpdates: 0,
					subLayerRefreshes: 0,
					layerRemoves: 0,
					subLayerRemoves: 0,
					Layerwarnings: 0,
					errors: 0
				};
				
				$scope.cacheStats={
					hits: 0,
					misses: 0,
					errors: 0,
					tiles: 0,
					size: 0
				};					
                //Leaflet maps
                $scope.map = ({
                    'diseasemap1': {},
                    'diseasemap2': {},
                    'viewermap': {}
                });			
				
                //Polygons
                $scope.geoJSON = ({
                    'diseasemap1': {},
                    'diseasemap2': {},
                    'viewermap': {}
                });

                //Legends and Infoboxes
                $scope.legend = {
                    'diseasemap1': L.control({position: 'topright'}),
                    'diseasemap2': L.control({position: 'topright'}),
                    'viewermap': L.control({position: 'topright'})
                };
                $scope.infoBox = {
                    'diseasemap1': L.control({position: 'bottomright'}),
                    'diseasemap2': L.control({position: 'bottomright'}),
                    'viewermap': L.control({position: 'bottomright'})
                };
                $scope.infoBox2 = {
                    'diseasemap1': L.control({position: 'bottomright'}),
                    'diseasemap2': L.control({position: 'bottomright'}),
                    'viewermap': null
                };

                //the default basemap              
                $scope.thisLayer = {
                    "diseasemap1": LeafletBaseMapService.setBaseMap(LeafletBaseMapService.getCurrentBaseMapInUse("diseasemap1")),
                    "diseasemap2": LeafletBaseMapService.setBaseMap(LeafletBaseMapService.getCurrentBaseMapInUse("diseasemap2")),
                    "viewermap": LeafletBaseMapService.setBaseMap(LeafletBaseMapService.getCurrentBaseMapInUse("viewermap"))
                };
                //metadata of TopoJSON being mapped
                $scope.tileInfo = {
                    "diseasemap1": {'geography': null, 'level': null},
                    "diseasemap2": {'geography': null, 'level': null},
                    "viewermap": {'geography': null, 'level': null}
                };
                //attribute being mapped
                $scope.attr = {
                    "diseasemap1": "",
                    "diseasemap2": "",
                    "viewermap": ""
                };
                //renderers defined by choropleth map service             
                var thisMap = {
                    "diseasemap1": [],
                    "diseasemap2": [],
                    "viewermap": []
                };
                $scope.tableData = {
                    "diseasemap1": [],
                    "diseasemap2": [],
                    "viewermap": []
                };
                $scope.sexes = {
                    "diseasemap1": [],
                    "diseasemap2": [],
                    "viewermap": []
                };
                $scope.initialRefresh = {
                    "diseasemap1": false,
                    "diseasemap2": false,
                    "viewermap": false
                };
                $scope.attributeDataLoaded = {
                    "diseasemap1": false,
                    "diseasemap2": false,
                    "viewermap": false
                };
                $scope.checkAttributeDataLoadedTimer = {
                    "diseasemap1": undefined,
                    "diseasemap2": undefined,
                    "viewermap": undefined
                };
				
                $scope.studyIDs = [];

				$scope.removeMap = function(mapID) {
					$scope.consoleLog("[rifc-util-mapping.js] remove map: " + mapID);
					if ($scope.map[mapID].hasLayer($scope.geoJSON[mapID])) {
						$scope.geoJSON[mapID]._geojsons.default.eachLayer($scope.removeSubLayer);
						$scope.map[mapID].removeLayer($scope.geoJSON[mapID]);
						$scope.geoJSON[mapID]={};
                    }
				}

				
                /*
                 * Tidy up on error
                 */
                function clearTheMapOnError(mapID) {
					$scope.consoleLog("[rifc-util-mapping.js] clearTheMapOnError: " + mapID);
                    //on error, remove polygon, legends and D3 charts
                    if (mapID === "viewermap") {
                        //datatable
                        $scope.viewerTableOptions.data.length = 0;
                        //pyramid
                        $scope.yearPop = null;
                        $scope.yearsPop.length = 0;
                        $scope.populationData["viewermap"].length = 0;
                        //histogram                                                                   
                        $scope.histoData["viewermap"].length = 0;
                    } else {
                        $scope.thisPoly[mapID] = null;
                        if (!angular.isUndefined($scope.infoBox2[mapID].update)) {
                            $scope.infoBox2[mapID].update(null);
                        }
                        $scope.rrChartData[mapID].length = 0;
                    }

                    if ($scope.map[mapID].hasLayer($scope.geoJSON[mapID])) {
                        $scope.map[mapID].removeLayer($scope.geoJSON[mapID]);
                        if ($scope.legend[mapID]._map) {
                            $scope.map[mapID].removeControl($scope.legend[mapID]);
                        }
                    }
                }

                /*.
                 * Fill the study drop-downs
                 */
                //update study list if new study processed, but do not update maps
                $scope.$on('updateStudyDropDown', function (event, thisStudy) {
                    $scope.studyIDs.push(thisStudy);
                });

				$scope.$on('rrZoomStatus', function (event, rrZoomStatus) { // For trace from rifd-dmap-d3rrzoom.js
					if (rrZoomStatus.level = "DEBUG") {
						$scope.consoleDebug("[rifc-util-mapping.js] rrZoomStatus: " + rrZoomStatus.msg);
					}
					else if (rrZoomStatus.level = "WARNING") {
						$scope.showWarning("[rifc-util-mapping.js] rrZoomStatus: " + rrZoomStatus.msg);
					}
					else if (rrZoomStatus.level = "ERROR") {
						$scope.showError("[rifc-util-mapping.js] rrZoomStatus: " + rrZoomStatus.msg);
					}
					else if (rrZoomStatus.level = "INFO") {
						$scope.consoleLog("[rifc-util-mapping.js] rrZoomStatus: " + rrZoomStatus.msg);
					}
					else {
						$scope.consoleDebug("[rifc-util-mapping.js] rrZoomStatus: " + JSON.stringify(rrZoomStatus, null, 0));
					}
				});
													
				 /*
					C: created, not verified; 
					V: verified, but no other work done; [NOT USED BY MIDDLEWARE]
					E: extracted imported or created, but no results or maps created; 
					G: Extract failure, extract, results or maps not created;
					R: initial results population, create map table; [NOT USED BY MIDDLEWARE]
					S: R success;
					F: R failure, R has caught one or more exceptions [depends on the exception handler design]
					W: R warning. [NOT USED BY MIDDLEWARE]
				 */
                //Get the possible studies initially
                $scope.getStudies = function () {
                    user.getCurrentStatusAllStudies(user.currentUser).then(function (res) {
                        for (var i = 0; i < res.data.smoothed_results.length; i++) {
							if (res.data.smoothed_results[i].study_state === "S") { // New success
                                var thisStudy = {
                                    "study_id": res.data.smoothed_results[i].study_id,
                                    "name": res.data.smoothed_results[i].study_name
                                };
                                $scope.studyIDs.push(thisStudy);
                            }
/*							
							else if (res.data.smoothed_results[i].study_state === "W") { // R warning. [NOT USED BY MIDDLEWARE]
                                var thisStudy = {
                                    "study_id": res.data.smoothed_results[i].study_id,
                                    "name": res.data.smoothed_results[i].study_name
                                };
                                $scope.studyIDs.push(thisStudy);
                            } */
                        }
                        //sort array on ID with most recent first
                        $scope.studyIDs.sort(function (a, b) {
                            return parseFloat(a.study_id) - parseFloat(b.study_id);
                        }).reverse();
                        //Remember defaults
                        for (var j = 0; j < parentScope.myMaps.length; j++) {
                            var s = $scope.myService.getState().study[parentScope.myMaps[j]].study_id;
                            if (s !== null) {
                                for (var i = 0; i < $scope.studyIDs.length; i++) {
                                    if ($scope.studyIDs[i].study_id === s) {
                                        $scope.$parent.studyID[parentScope.myMaps[j]] = $scope.studyIDs[i];
                                    }
                                }
                            } else {
                                $scope.$parent.studyID[parentScope.myMaps[j]] = $scope.studyIDs[0];
                            }
                        }
                        //update sex drop-down
                        for (var j = 0; j < parentScope.myMaps.length; j++) {
                            $scope.updateSex(parentScope.myMaps[j]);
                        }
                    }, function (e) {
                        $scope.showError("Unable to retrieve study status");
						$scope.consoleError("[rifc-util-mapping.js] Unable to retrieve study status: " + 
							JSON.stringify(e));
                    });
                };

                $scope.updateSex = function (mapID) {
                    if ($scope.studyID[mapID] !== null && angular.isDefined($scope.studyID[mapID])) {
                        //Store this study selection
                        $scope.myService.getState().study[mapID] = $scope.studyID[mapID];
                        //Get the sexes for this study
						if ($scope.studyID[mapID].study_id) {
							user.getSexesForStudy(user.currentUser, $scope.studyID[mapID].study_id, mapID)
									.then(handleSexes, function getSexesForStudy(e) {
										$scope.showWarning("Error " + e + "; getting sexes for study: " + $scope.studyID[mapID].study_id + "; map: " + mapID);
										clearTheMapOnError(mapID);
									});
						}
						else {
							$scope.showWarning("No study ID defined for map: " + mapID);
						}

                        function handleSexes(res) {
							$scope.consoleDebug("[rifc-util-mapping.js] handleSexes: " + JSON.stringify(res, null, 0));
                            $scope.sexes[res.config.leaflet].length = 0;
                            if (!angular.isUndefined(res.data[0].names)) {
                                for (var i = 0; i < res.data[0].names.length; i++) {
                                    $scope.sexes[res.config.leaflet].push(res.data[0].names[i]);
                                }
                            }
                            //if no or invalid preselection, then set dropdown to last one in list     
                            if ($scope.sexes[res.config.leaflet].indexOf($scope.sex[res.config.leaflet]) === -1 | $scope.sex[res.config.leaflet] === null) {
                                $scope.sex[res.config.leaflet] = $scope.sexes[res.config.leaflet][$scope.sexes[res.config.leaflet].length - 1];
                            }
                            //dashboard specific
                            if (mapID === "viewermap") {
                                //update pyramid if in viewer
                                $scope.child.fillPyramidData();
                            } else {
                                //check selection link is possible
                                if ($scope.myService.getState().selectionLock) {
                                    var g1 = $scope.tileInfo["diseasemap1"];
                                    var g2 = $scope.tileInfo["diseasemap2"];
                                    if (g1.geography !== null && g2.geography !== null) {
                                        if (g1.geography !== g2.geography) {
                                            //different geographies     
                                            $scope.showWarning("Cannot link selections for different geographies: " + g1.geography + " & " + g2.geography);
                                            $scope.myService.getState().selectionLock = false;
                                            $scope.$parent.bLockSelect = false;
                                        } else {
                                            if (g1.level !== g2.level) {
                                                //different levels
                                                $scope.showWarning("Cannot link selections for different geolevels: " + g1.level + " & " + g2.level);
                                            }
                                        }
                                    }
                                }
                            }
                            $scope.updateStudy(res.config.leaflet);
                        }
                    }
                };

                /*
                 * Map rendering
                 */
                //change the basemaps 
                $scope.renderMap = function (mapID) {
					var getCurrentBaseMap=LeafletBaseMapService.getCurrentBaseMapInUse(mapID);
					if ($scope.studyID[mapID].study_id) {
						$scope.consoleDebug("[rifc-util-mapping.js] renderMap (basemap) for mapID: " + mapID + 
							"; study: " + $scope.studyID[mapID].study_id + 
							"; sex: " + $scope.sex[mapID] + 
							"; getCurrentBaseMap: " + getCurrentBaseMap);			
					}
					else {
						$scope.consoleDebug("[rifc-util-mapping.js] renderMap (basemap) for mapID: " + mapID + 
							"; study: not set; sex: not set ; getCurrentBaseMap: " + getCurrentBaseMap);	
					}
					$scope.map[mapID].removeLayer($scope.thisLayer[mapID]);
					//add new baselayer if requested
					if (!LeafletBaseMapService.getNoBaseMap(mapID)) {
						$scope.thisLayer[mapID] = LeafletBaseMapService.setBaseMap(getCurrentBaseMap);
						$scope.thisLayer[mapID].addTo($scope.map[mapID]);
					}	
                };

                //Draw the map
                $scope.refresh = function (mapID) {
					if (!$scope.initialRefresh[mapID]) {
						$scope.consoleDebug("[rifc-util-mapping.js] initial refresh start for mapID: " + mapID);
						$scope.initialRefresh[mapID]=true;
					}
					else {
						$scope.consoleDebug("[rifc-util-mapping.js] refresh start for mapID: " + mapID);
					}					
                    //get choropleth map renderer
                    $scope.attr[mapID] = ChoroService.getMaps(mapID).feature;
                    thisMap[mapID] = ChoroService.getMaps(mapID).renderer;
					
                    //not a choropleth, but single colour
                    if (thisMap[mapID].range && thisMap[mapID].range.length === 1) {
                        //remove existing legend
                        if ($scope.legend[mapID]._map) {
                            $scope.map[mapID].removeControl($scope.legend[mapID]);
                        }
                        if (angular.isDefined($scope.geoJSON[mapID]._geojsons.default)) {
                            $scope.geoJSON[mapID]._geojsons.default.eachLayer($scope.handleLayer);
                        }
                    }
					else {
						//remove old legend and add new
						$scope.legend[mapID].onAdd = ChoroService.getMakeLegend(thisMap[mapID], $scope.attr[mapID]);
						if ($scope.legend[mapID]._map) { //This may break in future leaflet versions
							$scope.map[mapID].removeControl($scope.legend[mapID]);
						}
						$scope.legend[mapID].addTo($scope.map[mapID]);
						//force a redraw
						if (angular.isDefined($scope.geoJSON[mapID]._geojsons.default)) {
							$scope.geoJSON[mapID]._geojsons.default.eachLayer($scope.handleLayer);
						}
					}
					
                    //draw histogram [IT MUST BW HERE OR D3 GETS CONFUSED!]
					callGetD3chart = function(mapID) {
						$scope.$broadcast('rrZoomReset', {msg: "watchCall reset: " + mapID});
						$scope.getD3chart(mapID, $scope.attr[mapID]); // Crashes firefox	
						
						$scope.consoleDebug("[rifc-util-mapping.js] refresh completed for mapID: " + mapID);	
					}
					setTimeout(callGetD3chart, 500, mapID);		 
                }; //End of refresh(0)

                //remove rsub layer
				$scope.removeSubLayer  = function (layer) {
                    var mapID = layer.options.mapID;
					$scope.layerStats.subLayerRemoves++;
//					layer.remove(); 	// Remove [should be done by TopoJSONGridLayer.js]
//					layer=undefined;
				}
				
                //apply relevent renderer to layer
                $scope.handleLayer = function (layer) {
                    var mapID = layer.options.mapID;
					$scope.layerStats.subLayerRefreshes++;
                    if (mapID === "viewermap") {
                        //Join geography and results table
                        var thisAttr;
                        for (var i = 0; i < $scope.viewerTableOptions.data.length; i++) {
                            if ($scope.viewerTableOptions.data[i].area_id === layer.feature.properties.area_id) {
                                thisAttr = $scope.viewerTableOptions.data[i][ChoroService.getMaps("viewermap").feature];
                                break;
                            }
                        }
                        //is selected?
                        var selected = false;
                        if (angular.isArray($scope.thisPoly) && $scope.thisPoly.indexOf(layer.feature.properties.area_id) !== -1) {
                            selected = true;
                        }
                        var polyStyle = ChoroService.getRenderFeatureMapping(undefined, thisAttr, selected);
						if (thisMap["viewermap"].scale) {
							polyStyle = ChoroService.getRenderFeatureMapping(thisMap["viewermap"].scale, thisAttr, selected);
						}
                        layer.setStyle({
                            weight: 1,
                            color: "gray",
                            fillColor: polyStyle,
                            fillOpacity: $scope.child.transparency[mapID]
                        });
                    } else {
						if (mapID === undefined) { // Occurs only on SQL Server!
//							Do nothing!						
							$scope.consoleError("[rifc-util-mapping.js] Null mapID; layer options: " + JSON.stringify(layer.options, null, 2));
							if (layer !== undefined) {
								$scope.layerStats.LayerRemoves++;
								layer.remove(); 	// Remove 
								layer=undefined;
							}
						}
						else if ($scope.tableData[mapID] === undefined) {
							$scope.showError("Invalid table data for mapID: " + mapID);
							clearTheMapOnError(mapID);
						}
                        else if ($scope.tableData[mapID].length !== 0) {
                            var thisAttr;
                            for (var i = 0; i < $scope.tableData[mapID].length; i++) {
                                if ($scope.tableData[mapID][i].area_id === layer.feature.properties.area_id) {
                                    thisAttr = $scope.tableData[mapID][i][ChoroService.getMaps(mapID).feature];
                                    break;
                                }
                            }
                            var polyStyle = ChoroService.getRenderFeatureViewer(thisMap[mapID].scale, layer.feature, thisAttr, $scope.thisPoly[mapID]);
                            layer.setStyle({
                                weight: polyStyle[2],
                                color: polyStyle[1],
                                fillColor: polyStyle[0],
                                fillOpacity: $scope.child.transparency[mapID]
                            });
                        }
						else {
							$scope.layerStats.LayerWarnings++;
							if ($scope.layerStats.Layerwarnings < 20) {
								$scope.consoleDebug("[rifc-util-mapping.js] No table data for mapID: " + mapID + " [20 warnings max.]"); // You will get 1000's of these!!	
							}
						}
                    }
                };
				
				$scope.defaultRenderMap = function (mapID) {
					
					var choroScope = {
						input: {},
						mapID: mapID,
						options: [],
						domain: [],
						tableData: {}
					}
					choroScope.input.isDefault = ChoroService.getMaps(mapID).isDefault;
					choroScope.input.checkboxInvert = ChoroService.getMaps(mapID).invert;
					choroScope.input.selectedSchemeName = ChoroService.getMaps(mapID).brewerName;
					choroScope.input.intervalRange = ColorBrewerService.getSchemeIntervals(choroScope.selectedSchemeName);
					choroScope.input.selectedN = ChoroService.getMaps(mapID).intervals;
					choroScope.input.method = ChoroService.getMaps(mapID).method;
                    var colorBrewerList = ColorBrewerService.getSchemeList();
                    for (var j in colorBrewerList) {
                        choroScope.options.push({name: colorBrewerList[j], image: 'images/colorBrewer/' + colorBrewerList[j] + '.png'});
                    }
					//set saved swatch selection
					var cb = ChoroService.getMaps(mapID).brewerName;
					for (var i = 0; i < choroScope.options.length; i++) {
						if (choroScope.options[i].name === cb) {
							choroScope.input.currOption = choroScope.options[i];
						}
					}

					//list of attributes
					choroScope.input.features = ChoroService.getMaps(mapID).features;
					if (choroScope.input.features.indexOf(ChoroService.getMaps(mapID).feature) === -1) {
						choroScope.input.selectedFeature = choroScope.input.features[0];
					} else {
						choroScope.input.selectedFeature = ChoroService.getMaps(mapID).feature;
					}
				
					choroScope.brewerName = ChoroService.getMaps(mapID).brewerName;
					choroScope.invert = ChoroService.getMaps(mapID).invert;
					choroScope.brewer = ChoroService.getMaps(mapID).brewer;
					choroScope.intervals = ChoroService.getMaps(mapID).intervals;
					choroScope.feature = ChoroService.getMaps(mapID).feature;
					choroScope.method = ChoroService.getMaps(mapID).method;
					choroScope.renderer = ChoroService.getMaps(mapID).renderer;
					
//					$scope.consoleDebug("[rifc-util-mapping.js] defaultRenderMap() mapID: " + mapID + "; choroScope: " + JSON.stringify(choroScope, null, 2)); 
					choroScope.tableData[mapID]=$scope.tableData[mapID];	
					ChoroService.doRenderSwatch(true /* Called on modal open */, true /* Secret field, always true */, choroScope, ColorBrewerService);
					
					$scope.input=choroScope.input;
					$scope.domain=choroScope.domain;
					
					var savedMapState = {
						input: choroScope.input,
//						domain:	choroScope.domain,  // Contains functions - will cause XML parse errors!
						maps: {					// Probably not needed apart from the initial state
							features: [],
							brewerName: choroScope.brewerName,
							intervals: choroScope.intervals,
							feature: choroScope.feature,
							invert: choroScope.invert,
							method: choroScope.method,
							isDefault: false,
							renderer: { //  May need more here
								scale: null,
								breaks: [],
								range: ["#9BCD9B"],
								mn: null,
								mx: null
							},
							init: false
						}
					};
					savedMapState.maps=ChoroService.getMaps(mapID);
//					$scope.consoleDebug("[rifc-util-mapping.js] defaultRenderMap() mapID: " + mapID + 
//						"; data rows: " + choroScope.tableData[mapID].length +
//						"; Saved map state: " + JSON.stringify(savedMapState, null, 2));
					
				}
				
				$scope.createTopoJSONLayer = function (mapID) {
					var topojsonURL = user.getTileMakerTiles(user.currentUser, $scope.tileInfo[mapID].geography, $scope.tileInfo[mapID].level);
					
					if (!$scope.geoJSON[mapID]) { // Created in: mapping\controllers\rifc-dmap-main.js, viewer\controllers\rifc-view-viewer.js:
						$scope.consoleError("[rifc-util-mapping.js] Unable to create topoJsonGridLayer for mapID: " + mapID + 
							"; no map");
						return;
					}
								
					$scope.consoleDebug("[rifc-util-mapping.js] create topoJsonGridLayer for mapID: " + mapID + 
						"; Geography: " + $scope.tileInfo[mapID].geography +
						"; Geolevel: " + $scope.tileInfo[mapID].level +
						"; URL: " + topojsonURL +
						"; study: " + $scope.studyID[mapID].study_id + 
						"; sex: " + $scope.sex[mapID]);
					$scope.geoJSON[mapID] = new L.topoJsonGridLayer(topojsonURL, {
						attribution: 'Polygons &copy; <a href="http://www.sahsu.org/content/rapid-inquiry-facility" target="_blank">Imperial College London</a>',
						// Options
						consoleDebug: $scope.consoleDebug,
						consoleError: $scope.consoleError,
						name: mapID + "." + $scope.tileInfo[mapID].geography + "." + $scope.tileInfo[mapID].level, 
													// Should be unique (includes mapID, geography and geolevel name)
//										maxZoom: maxzoomlevel,
						useCache: $scope.parameters.usePouchDBCache,
						auto_compaction: true,
						layers: {
							default: {
								mapID: mapID,
								renderer: L.canvas(),
								style: function (feature) {
									return({
										weight: 1,
										opacity: 1,
										color: "gray",
										fillColor: "transparent"
									});
								},
								onEachFeature: function (feature, layer) {
									layer.on('mouseover', function (e) {
										this.setStyle({
											color: 'gray',
											weight: 1.5,
											fillOpacity: function () {
												return($scope.child.transparency[mapID] - 0.3 > 0 ? $scope.child.transparency[mapID] - 0.3 : 0.1);
											}()
										});
										$scope.infoBox[mapID].update(layer.feature.properties.area_id, 
											layer.feature.properties.name);
									});
									layer.on('mouseout', function (e) {
										$scope.geoJSON[mapID]._geojsons.default.eachLayer($scope.handleLayer);
										$scope.infoBox[mapID].update(false);
									});
									layer.on('click', function (e) {
										if (mapID === "viewermap") {
											//Multiple selections
											var thisPoly = e.target.feature.properties.area_id;
											var bFound = false;
											for (var i = 0; i < $scope.thisPoly.length; i++) {
												if ($scope.thisPoly[i] === thisPoly) {
													bFound = true;
													$scope.thisPoly.splice(i, 1);
													break;
												}
											}
											if (!bFound) {
												$scope.thisPoly.push(thisPoly);
											}
										} else {
											//Single selections
											$scope.thisPoly[mapID] = e.target.feature.properties.area_id;
											$scope.myService.getState().area_id[mapID] = e.target.feature.properties.area_id;
											$scope.infoBox2[mapID].update($scope.thisPoly[mapID]);
											$scope.$parent.updateMapSelection($scope.thisPoly[mapID], mapID);
											if ($scope.bLockSelect) {
												var otherMap = MappingService.getOtherMap(mapID);
												$scope.thisPoly[otherMap] = e.target.feature.properties.area_id;
												$scope.myService.getState().area_id[otherMap] = e.target.feature.properties.area_id;
												$scope.$parent.updateMapSelection(e.target.feature.properties.area_id, otherMap);
											}
										}
									});
								}
							}
						}
					}); // End of new L.topoJsonGridLayer()
					
					//force re-render of new tiles								
					$scope.geoJSON[mapID].on('load', function (e) {
						$scope.map[mapID].whenReady(function(e) {			
							if ($scope.geoJSON[mapID]._tiles) {												
								$scope.consoleDebug("[rifc-util-mapping.js] load event for mapID: " + mapID + 
									"; layer stats " + JSON.stringify($scope.layerStats, null, 2) + 
									"; cache stats " + JSON.stringify($scope.cacheStats, null, 2) + 
									"; study: " + $scope.studyID[mapID].study_id + 
									"; sex: " + $scope.sex[mapID] +
									"; tiles: " + Object.keys($scope.geoJSON[mapID]._tiles).length +
									"; zoomlevel: " + $scope.map[mapID].getZoom() +
									"; areas: " + $scope.geoJSON[mapID]._geojsons.default.getLayers().length);	
							}
							else {
								$scope.consoleDebug("[rifc-util-mapping.js] load event for mapID: " + mapID + 
									"; layer stats " + JSON.stringify($scope.layerStats, null, 2) + 
									"; cache stats " + JSON.stringify($scope.cacheStats, null, 2) + 	
									"; study: " + $scope.studyID[mapID].study_id + 
									"; sex: " + $scope.sex[mapID] +
									"; tiles: UNKNOWN" +
									"; zoomlevel: " + $scope.map[mapID].getZoom() +
									"; areas: " + $scope.geoJSON[mapID]._geojsons.default.getLayers().length);
							}
						
							doLoadWork = function(mapID) {							
								$scope.defaultRenderMap(mapID);
								$scope.refresh(mapID);	
								
								if (mapID !== "viewermap") { 
									if ($scope.disableMapLocking) {
										$scope.consoleDebug("[rifc-util-mapping.js] map locking disabled for mapID: " + mapID +
											"; disableMapLocking: " + $scope.disableMapLocking);
									}
									else {			
										$scope.consoleDebug("[rifc-util-mapping.js] map locking enabled for mapID: " + mapID +
											"; disableMapLocking: " + $scope.disableMapLocking);
										$scope.mapLocking();								
									}
								}									
							};
							checkAttributeDataLoaded = function(mapID) {
								if ($scope.initialRefresh[mapID]) {
									$scope.consoleDebug("[rifc-util-mapping.js] attribute data wait already refeshed for mapID: " + mapID +
										"; attribute data loaded: " + $scope.attributeDataLoaded[mapID] + 
										"; clear interval timer: " + $scope.checkAttributeDataLoadedTimer[mapID]);
									clearInterval($scope.checkAttributeDataLoadedTimer[mapID]);
								}
								else if ($scope.attributeDataLoaded[mapID]) {
									$scope.consoleDebug("[rifc-util-mapping.js] attribute data ready for mapID: " + mapID +
										"; clear interval timer: " + $scope.checkAttributeDataLoadedTimer[mapID]);
									clearInterval($scope.checkAttributeDataLoadedTimer[mapID]);
//									$scope.checkAttributeDataLoadedTimer[mapID] = undefined;

//									if ($scope.geoJSON[mapID]._geojsons.default) {
//										$scope.geoJSON[mapID]._geojsons.default.eachLayer($scope.handleLayer);
//									}
//									else {							
//										$scope.consoleError("[rifc-util-mapping.js] geoJSON not yet set up: $scope.geoJSON[mapID]._geojsons.default is NULL");
//									}
									doLoadWork(mapID);
								}
								else {
									$scope.consoleDebug("[rifc-util-mapping.js] attribute data wait for mapID: " + mapID);
								}
							};
							
							if (!$scope.initialRefresh[mapID] && !$scope.attributeDataLoaded[mapID] && $scope.checkAttributeDataLoadedTimer[mapID] == undefined) {
								$scope.checkAttributeDataLoadedTimer[mapID] = setInterval(checkAttributeDataLoaded, 1000, mapID);	
							}	
							else {
								doLoadWork(mapID);
							}																			
						});
					});
								
					$scope.geoJSON[mapID].on('remove', function (e) {
						$scope.layerStats.layerRemoves++;
					});		
					$scope.geoJSON[mapID].on('add', function (e) {
						$scope.layerStats.layerAdds++;
					});	
					$scope.geoJSON[mapID].on('addsublayer', function (stats) {
						$scope.layerStats.subLayerAdds+=stats.subLayerAdds;
						$scope.layerStats.subLayerUpdates+=stats.subLayerUpdates;
					});
					$scope.geoJSON[mapID].on('tileerror', function(error, tile) {
						if ($scope.cacheStats) {
							$scope.layerStats.errors++;
						}
						var msg="";
						if (error && error.message) {
							msg+=error.message;
						}
						if (tile) {
							$scope.consoleError("[rifc-util-mapping.js] Error: loading topoJSON tile: " + 
								(JSON.stringify(tile.coords)||"UNK"));		
						}
					});
					$scope.geoJSON[mapID].on('tilecacheerror', function tileCacheErrorHandler(ev) {
						if ($scope.cacheStats) {
							$scope.cacheStats.errors++;
						}
					});
					$scope.geoJSON[mapID].on('tilecachemiss', function tileCacheErrorHandler(ev) {
						if ($scope.cacheStats) {
							$scope.cacheStats.misses++;
						}
					});
					$scope.geoJSON[mapID].on('tilecachehit', function tileCacheErrorHandler(ev) {
						if ($scope.cacheStats) {
							$scope.cacheStats.hits++;
						}
					});
														
				} // End of createTopoJSONLayer()
				
                $scope.updateStudy = function (mapID) {
                    //Check inputs are valid
                    if ($scope.studyID[mapID] === null || $scope.sex[mapID] === null) {
                        $scope.showError("Invalid study or sex code");
                        clearTheMapOnError(mapID);
                    } else {
						$scope.initialRefresh[mapID]=false;
						$scope.attributeDataLoaded[mapID]=false;
						$scope.consoleDebug("[rifc-util-mapping.js] updateStudy for mapID: " + mapID + "; study: " + $scope.studyID[mapID].study_id + 
							"; sex: " + $scope.sex[mapID]);
                        //Reset all renderers, but only if not called from state change
                        if (!$scope.myService.getState().initial) {
                            thisMap[mapID] = ChoroService.getMaps(mapID).renderer;
                        }
                        $scope.myService.getState().initial = false;
                        //Remove RR chart
                        if (mapID !== "viewermap" && !angular.isUndefined($scope.rrChartData[mapID])) {
                            $scope.rrChartData[mapID].length = 0;
                        }

                        //Remove any existing geography
                        if ($scope.map[mapID].hasLayer($scope.geoJSON[mapID])) {
							$scope.geoJSON[mapID]._geojsons.default.eachLayer($scope.removeSubLayer);
							$scope.map[mapID].removeLayer($scope.geoJSON[mapID]);
							$scope.geoJSON[mapID]={};
                        }

                        //save study, sex selection
                        $scope.myService.getState().sex[mapID] = $scope.sex[mapID];
                        $scope.myService.getState().study[mapID] = $scope.$parent.studyID[mapID];
                        //add the requested geography
                        user.getGeographyAndLevelForStudy(user.currentUser, $scope.studyID[mapID].study_id).then(
							function (res) {
								$scope.tileInfo[mapID].geography = res.data[0][0]; //e.g. SAHSU
								$scope.tileInfo[mapID].level = res.data[0][1]; //e.g. LEVEL3
										
								// Synchronised in on load
								
								getAttributeTable(mapID, // Needs study_id, sex
									function getAttributeTableCallBack(msg) {
										$scope.consoleDebug(msg);


									},
									function getAttributeTableError(e) {
										$scope.showError("Error fetching table data for mapID: " + mapID + "; " + e);
									});
									
								$scope.createTopoJSONLayer(mapID);
								$scope.consoleDebug("[rifc-util-mapping.js] completed topoJsonGridLayer for mapID: " + mapID + 
									"; study: " + $scope.studyID[mapID].study_id);												
								$scope.map[mapID].addLayer($scope.geoJSON[mapID]); // Add layer to map
											
								$scope.map[mapID].whenReady(function(e) {	
									//pan events                            
										$scope.map[mapID].on('zoomend', function (e) {
											$scope.myService.getState().center[mapID].zoom = $scope.map[mapID].getZoom();
										});
										$scope.map[mapID].on('moveend', function (e) {
											$scope.myService.getState().center[mapID].lng = $scope.map[mapID].getCenter().lng;
											$scope.myService.getState().center[mapID].lat = $scope.map[mapID].getCenter().lat;
										});
									});
								// End of create grid layer											

							}, function (e) { //getGeographyAndLevelForStudy error handler
								$scope.consoleError("[rifc-util-mapping.js] Unable to getGeographyAndLevelForStudy(): " + 
									JSON.stringify(e));
							}

						// End of user.getGeographyAndLevelForStudy()
                        ).then(function () { 
							$scope.map[mapID].whenReady(function(e) { // BG tiles set

								setMapCentreAndBounds(mapID,
									function setMapCentreAndBoundsCallback(msg) {	// setMapCentreAndBoundsCallback
										$scope.consoleDebug(msg); 
										
										$scope.myService.getState().center[mapID].zoom = $scope.map[mapID].getZoom();
										$scope.myService.getState().center[mapID].lng = $scope.map[mapID].getCenter().lng;
										$scope.myService.getState().center[mapID].lat = $scope.map[mapID].getCenter().lat;
										$scope.consoleDebug("[rifc-util-mapping.js] add topoJsonGridLayer for mapID: " + mapID + 
											"; study: " + $scope.studyID[mapID].study_id);		
										$scope.consoleDebug("[rifc-util-mapping.js] initial setView for mapID: " + mapID + 
												"; centre: " + JSON.stringify($scope.myService.getState().center[mapID]));		
 
									}, function setMapCentreAndBoundsError(e) {	// setMapCentreAndBoundsError
										$scope.consoleError(e); 
									}
								);
							});
							
                        });
                    }

                    //Sync or unsync map extents using https://github.com/jieter/Leaflet.Sync ONCE!
                    $scope.mapLocking = function () {
                        if ($scope.$parent.bLockCenters) {
							if (!$scope.map["diseasemap1"].isSynced($scope.map["diseasemap2"])) { // sync interactions on diseasemap1 with diseasemap2.
								$scope.consoleDebug("[rifc-util-mapping.js] mapLocking: sync interactions on diseasemap1 with diseasemap2");
								$scope.map["diseasemap1"].sync($scope.map["diseasemap2"], $scope.parameters.mapLockingOptions);
							}	
							if (!$scope.map["diseasemap2"].isSynced($scope.map["diseasemap1"])) {
								$scope.consoleDebug("[rifc-util-mapping.js] mapLocking: sync interactions on diseasemap2 with diseasemap1");
								$scope.map["diseasemap2"].sync($scope.map["diseasemap1"], $scope.parameters.mapLockingOptions);
							}
                        } 
						else {
							if ($scope.map["diseasemap1"].isSynced($scope.map["diseasemap2"])) {
								$scope.consoleDebug("[rifc-util-mapping.js] mapLocking: unsync diseasemap1 with diseasemap2");
								$scope.map["diseasemap1"].unsync($scope.map["diseasemap2"]);
							}
							if ($scope.map["diseasemap2"].isSynced($scope.map["diseasemap1"])) {
								$scope.consoleDebug("[rifc-util-mapping.js] mapLocking: unnsync diseasemap2 with diseasemap1");
								$scope.map["diseasemap2"].unsync($scope.map["diseasemap1"]);
							}
                        }
                    };
					
					function setMapCentreAndBounds(mapID, setMapCentreAndBoundsCallback, setMapCentreAndBoundsError) {
						var promise=new Promise(function(resolve, reject) {
								user.getGeoLevelSelectValues(user.currentUser, $scope.tileInfo[mapID].geography).then(function (res) {
								var lowestLevel = res.data[0].names[0];
								user.getTileMakerTilesAttributes(user.currentUser, $scope.tileInfo[mapID].geography, lowestLevel).then(function (res) {
									$scope.maxbounds = L.latLngBounds([res.data.bbox[1], res.data.bbox[2]], [res.data.bbox[3], res.data.bbox[0]]);
									if (mapID !== "diseasemap2" || $scope.disableMapLocking) {
										
										//do not get maxbounds for diseasemap2
										if ($scope.myService.getState().center[mapID].lat === 0) {
											$scope.map[mapID].fitBounds($scope.maxbounds);
											$scope.map[mapID].whenReady(function(e) {	
												var centre = $scope.myService.getState().center[mapID];
												resolve("[rifc-util-mapping.js] set fitBounds (1) for mapID: " + mapID + 
																"; disableMapLocking: " + $scope.disableMapLocking +
																"; lowestLevel: " + lowestLevel +
																"; maxbounds: " + JSON.stringify($scope.maxbounds) +
																"; centre: " + JSON.stringify(centre));
											});					
										} 
										else {
											var centre = $scope.myService.getState().center[mapID];
											$scope.map[mapID].setView([centre.lat, centre.lng], centre.zoom);
											$scope.map[mapID].whenReady(function(e) {	
												resolve("[rifc-util-mapping.js] set setView (2) for mapID: " + mapID + 
															"; disableMapLocking: " + $scope.disableMapLocking +
															"; lowestLevel: " + lowestLevel +
															"; maxbounds: " + JSON.stringify($scope.maxbounds) +
															"; centre: " + JSON.stringify(centre));
											});
										}
									} 
									else { // diseasemap2
										var centre = $scope.myService.getState().center[mapID];
										$scope.map[mapID].setView([centre.lat, centre.lng], centre.zoom);
										$scope.map[mapID].whenReady(function(e) {	
											resolve("[rifc-util-mapping.js] set setView (3) for mapID: " + mapID + 
														"; disableMapLocking: " + $scope.disableMapLocking +
														"; lowestLevel: " + lowestLevel +
														"; maxbounds: " + JSON.stringify($scope.maxbounds) +
														"; centre: " + JSON.stringify(centre));
										});
									}
								}, function (e) {
									reject("[rifc-util-mapping.js] Unable to getTileMakerTilesAttributes(): " + 
										JSON.stringify(e));
								});
							}, function (e) {
								reject("[rifc-util-mapping.js] Unable to getGeoLevelSelectValues(): " + 
									JSON.stringify(e));
							});											
						});
						
						promise.then(function(result) {
							setMapCentreAndBoundsCallback(result); 
						}, function(err) {
							setMapCentreAndBoundsError(err); 
						});

					}; // End of setMapCentreAndBounds()

                    function getAttributeTable(mapID, getAttributeTableCallBack, getAttributeTableError) {
                        user.getSmoothedResults(user.currentUser, $scope.studyID[mapID].study_id, MappingService.getSexCode($scope.sex[mapID]))
                                .then(function (res) {
                                    //variables possible to map
                                    var attrs = ["smoothed_smr", "relative_risk", "posterior_probability"];
                                    ChoroService.getMaps(mapID).features = attrs;
                                    //make array for choropleth
                                    $scope.tableData[mapID].length = 0;
                                    for (var i = 0; i < res.data.smoothed_results.length; i++) {
                                        $scope.tableData[mapID].push(res.data.smoothed_results[i]);
                                    }

                                    //supress some pre-selected columns (see the service)   
                                    if (mapID === "viewermap") {
                                        //fill results table for data viewer only
                                        var colDef = [];
                                        var attrs = [];
                                        //Add column for selected
                                        for (var i = 0; i < res.data.smoothed_results.length; i++) {
                                            res.data.smoothed_results[i]._selected = 0;
                                        }
/*
 * Valid data viewer columns: "area_id", "band_id", "observed", "expected", "population", "adjusted", "inv_id", "posterior_probability",
 *                            "lower95", "upper95", "relative_risk", "smoothed_smr", "smoothed_smr_lower95", "smoothed_smr_upper95"
 */
                                        for (var i in res.data.smoothed_results[0]) {
                                            if (ViewerStateService.getValidColumn(i)) {
                                                if (i !== "area_id" && // Valid Choropleth map features
												    i !== "band_id" && 
												    i !== "inv_id" && 
												    i !== "adjusted" && 
												    i !== "_selected") {
                                                    attrs.push(i);
                                                }
                                                colDef.push({
                                                    name: i,
                                                    width: 100
                                                });
                                            }
                                        }
                                        //draw and refresh histogram and table
                                        ChoroService.getMaps("viewermap").features = attrs;
                                        $scope.viewerTableOptions.columnDefs = colDef;
                                        $scope.viewerTableOptions.data = $scope.tableData.viewermap;
                                        $scope.updateTable();
                                    }
									getAttributeTableCallBack("[rifc-util-mapping.js] " + $scope.tableData[mapID].length + 
										" rows of attribute data fetched for map: " + mapID +
										"; study : " + $scope.studyID[mapID].study_id);		
									$scope.attributeDataLoaded[mapID]=true;
                                }, function (e) {
                                    clearTheMapOnError(mapID);
									getAttributeTableError(e);
                                });
                    }

                    /*
                     * INFO BOXES AND LEGEND
                     */
                    //An empty control on map
                    function closureAddControl(m) {
                        return function () {
                            this._div = L.DomUtil.create('div', 'info');
                            this.update();
                            return this._div;
                        };
                    }
                    //The hover box update
                    function closureInfoBoxUpdate(m) {
                        return function (poly, name) {
                            if (poly) {
                                this._div.style["display"] = "inline";
                                this._div.innerHTML =
                                        function () {
                                            var feature = ChoroService.getMaps(m).feature;
                                            var tmp;
                                            var inner = '<h5>ID: ' + poly + '</br>Name: ' + name + '</h5>';
                                            if ($scope.attr[m] !== "") {
                                                for (var i = 0; i < $scope.tableData[m].length; i++) {
                                                    if ($scope.tableData[m][i].area_id === poly) {
                                                        tmp = $scope.tableData[m][i][$scope.attr[m]];
														$scope.tableData[m][i].name = name;
                                                        break;
                                                    }
                                                }
                                                if (feature !== "" && !isNaN(Number(tmp))) {
                                                    inner = '<h5>ID: ' + poly + '</br>Name: ' + name + '</br>' + feature.toUpperCase().replace("_", " ") + ": " + Number(tmp).toFixed(3) + '</h5>';
                                                }
                                            }
                                            return inner;
                                        }();
                            } else {
                                this._div.innerHTML = '';
                                this._div.style["display"] = "none";
                            }
                        };
                    }
                    //Area info box update
                    function closureInfoBox2Update(m) {
                        return function (poly) {
                            if (poly === null) {
                                this._div.innerHTML = "";
                                this._div.style["display"] = "none";
                            } else {
                                var results = null;
                                for (var i = 0; i < $scope.tableData[m].length; i++) {
                                    if ($scope.tableData[m][i].area_id === poly) {
                                        results = $scope.tableData[m][i];
                                    }
                                }
                                if (results !== null) {
                                    this._div.style["display"] = "inline";
                                    this._div.innerHTML =
                                            '<h5>ID: ' + poly + '</br>' +
                                            'Name: ' + (results.name||'N/A') + '</br>' +
                                            'Population: ' + results.population + '</br>' +
                                            'Observed: ' + results.observed + '</br>' +
                                            'Expected: ' + Number(results.expected).toFixed(2) + '</br>' + '</h5>';
                                } else {
                                    this._div.innerHTML = "";
                                    this._div.style["display"] = "none";
                                }
                            }
                        };
                    }

                    //Add the controls
                    for (var i = 0; i < parentScope.myMaps.length; i++) {
                        var m = parentScope.myMaps[i];
                        $scope.infoBox[m].onAdd = closureAddControl(m);
                        $scope.infoBox[m].update = closureInfoBoxUpdate(m);
                        if (m !== "viewermap") {
                            $scope.infoBox2[m].onAdd = closureAddControl(m);
                            $scope.infoBox2[m].update = closureInfoBox2Update(m);
                        }
                    }
                    for (var i = 0; i < parentScope.myMaps.length; i++) {
                        if (parentScope.myMaps.indexOf("diseasemap1") !== -1) {
                            $scope.infoBox2["diseasemap1"].addTo($scope.map["diseasemap1"]);
                            $scope.infoBox["diseasemap1"].addTo($scope.map["diseasemap1"]);
                        }
                        if (parentScope.myMaps.indexOf("diseasemap2") !== -1) {
                            $scope.infoBox2["diseasemap2"].addTo($scope.map["diseasemap2"]);
                            $scope.infoBox["diseasemap2"].addTo($scope.map["diseasemap2"]);
                        }
                        if (parentScope.myMaps.indexOf("viewermap") !== -1) {
                            $scope.infoBox["viewermap"].addTo($scope.map["viewermap"]);
                        }
                    }
                };
            }]);