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
 * DIRECTIVE for map and table linked area selections
 * TODO: This prob needs refactoring / overhauling to fit in with the mapping controllers
 * although it does work fine as it is
 */

/* global L, d3, key, topojson */
angular.module("RIF")
        .directive('submissionMapTable', ['ModalAreaService', 'LeafletDrawService', '$uibModal', 'JSONService', 'mapTools',
            'GISService', 'LeafletBaseMapService', '$timeout', 'user', 'SubmissionStateService', 
			'SelectStateService', 'ParametersService',
            function (ModalAreaService, LeafletDrawService, $uibModal, JSONService, mapTools,
                    GISService, LeafletBaseMapService, $timeout, user, SubmissionStateService,
					SelectStateService, ParametersService) {
                return {
                    templateUrl: 'dashboards/submission/partials/rifp-dsub-maptable.html',
                    restrict: 'AE',
                    link: function ($scope) {
						var parameters=ParametersService.getParameters();
					    var selectorBands = { // Study and comparison are selectors
								weight: 3,
								opacity: 0.8,
								fillOpacity: 0,
								bandColours: ['#e41a1c', '#377eb8', '#4daf4a', '#984ea3', '#ff7f00', '#ffff33']
							};
						if (parameters && parameters.selectorBands) {
							selectorBands=parameters.selectorBands
						}		
						$scope.centroid_type="UNKNOWN";		
						
                        $scope.areamap = L.map('areamap', {condensedAttributionControl: false}).setView([0, 0], 1);		
						$scope.areamap.createPane('shapes');
						$scope.areamap.getPane('shapes').style.zIndex = 650; // set shapes to show on top of markers but below pop-ups
						
						SubmissionStateService.setAreaMap($scope.areamap);
						
                        $scope.thisLayer = LeafletBaseMapService.setBaseMap(LeafletBaseMapService.getCurrentBaseMapInUse("areamap"));

                        //Reference the child scope
                        //will be from the comparison area or study area controller
                        $scope.child = {};
                        var alertScope = $scope.$parent.$$childHead.$parent.$parent.$$childHead;
						$scope.areamap.on('remove', function(e) {
                            alertScope.consoleDebug("[rifd-dsub-maptable.js] removed shared areamap");
						});
//						$scope.areamap.on('layeradd', function(layerEvent){
//                            alertScope.consoleDebug("[rifd-dsub-maptable.js] added layer to areamap" +
//								"; layerId: " + (layerEvent.layer._leaflet_id || "(no _leaflet_id)"));
//						});
						$scope.areamap.on('error', function(errorEvent){
                            alertScope.consoleError("[rifd-dsub-maptable.js] error in areamap" +
								(errorEvent.message || "(no message)"));
						});	
						
                        ///Called on DOM render completion to ensure basemap is rendered
                        $timeout(function () {
                            //add baselayer
                            $scope.renderMap("areamap");

                            //Store the current zoom and view on map changes
                            $scope.areamap.on('zoomend', function (e) {
                                $scope.input.center.zoom = $scope.areamap.getZoom();
                            });
                            $scope.areamap.on('moveend', function (e) {
                                $scope.input.center.lng = $scope.areamap.getCenter().lng;
                                $scope.input.center.lat = $scope.areamap.getCenter().lat;
                            });

                            //slider
                            var slider = L.control.slider(function (v) {
                                $scope.changeOpacity(v);
                            }, {
                                id: slider,
                                position: 'topleft',
                                orientation: 'horizontal',
                                min: 0,
                                max: 1,
                                step: 0.01,
                                value: $scope.transparency,
                                title: 'Transparency',
                                logo: '',
                                syncSlider: true
                            }).addTo($scope.areamap);

                            //Custom toolbar
                            var tools = mapTools.getSelectionTools($scope);
                            for (var i = 0; i < tools.length; i++) {
                                new tools[i]().addTo($scope.areamap);
                            }

                            //scalebar and fullscreen
                            L.control.scale({position: 'bottomleft', imperial: false}).addTo($scope.areamap);
                            $scope.areamap.addControl(new L.Control.Fullscreen());

                            //drop down for bands
                            var dropDown = mapTools.getBandDropDown($scope);
                            new dropDown().addTo($scope.areamap);

                            //Set initial map extents
                            $scope.center = $scope.input.center;
//
// TO STOP LEAFLET NOT DISPLAYING SELECTED AREAS (experimental)
//                            $scope.areamap.setView([$scope.center.lat, $scope.center.lng], $scope.center.zoom);

                            //Attributions to open in new window
                            L.control.condensedAttribution({
                                prefix: '<a href="http://leafletjs.com" target="_blank">Leaflet</a>'
                            }).addTo($scope.areamap);

                            $scope.areamap.doubleClickZoom.disable();
                            $scope.areamap.band = Math.max.apply(null, $scope.possibleBands);
                        });

                        /*
                         * LOCAL VARIABLES
                         */
                        //map max bounds from topojson layer
                        var maxbounds;
                        //If geog changed then clear selected
                        var thisGeography = SubmissionStateService.getState().geography;
                        if (thisGeography !== $scope.input.geography) {
                            $scope.input.selectedPolygon.length = 0;
                            $scope.input.selectAt = "";
                            $scope.input.studyResolution = "";
                            $scope.input.geography = thisGeography;
                        }
						
						// Also defined in rifs-util-leafletdraw.js

                        //selectedPolygon array synchronises the map <-> table selections  
                        $scope.selectedPolygon = $scope.input.selectedPolygon;
                        //total for display
                        $scope.selectedPolygonCount = $scope.selectedPolygon.length;
                        //band colour look-up for selected districts
                        $scope.possibleBands = $scope.input.bands;
                        $scope.currentBand = 1; //from dropdown
                        //d3 polygon rendering, changed by slider
                        $scope.transparency = $scope.input.transparency;

                        /*
                         * TOOL STRIP 
                         * These repeat stuff in the leafletTools directive - possible refactor
                         */
                        //Clear all selection from map and table
                        $scope.clear = function () {
                            $scope.selectedPolygon.length = 0;
                            $scope.input.selectedPolygon.length = 0;
//                            $scope.clearAOI();
							if ($scope.input.type === "Risk Analysis") {
								SelectStateService.initialiseRiskAnalysis();
							}
							else {			
								SelectStateService.resetState();
							}
							
                            if ($scope.areamap.hasLayer($scope.shapes)) {
                                $scope.areamap.removeLayer($scope.shapes);
								$scope.shapes = new L.layerGroup();
								$scope.areamap.addLayer($scope.shapes);
								
								$scope.info.update();
                            }
							
							if (maxbounds) { //  Zoom back to maximum extent of geolevel
								$scope.areamap.fitBounds(maxbounds);
							}
                        };
						
						// Bring shapes to front by descending band order; lowest in front (so dblclick works!)
						$scope.bringShapesToFront = function() {
							var layerCount=0;
							var maxBands=0;
							var shapeLayerOptionsBanderror=0;
							var shapeLayerBringToFrontError=0;
							
							if ($scope.shapes) {
								var shapesLayerList=$scope.shapes.getLayers();
								var shapesLayerBands = {};
								var shapesLayerAreas = {};
								var useBands=false;
								
								for (var i=0; i<shapesLayerList.length; i++) {
									var shapeLayer=shapesLayerList[i];
									if (shapeLayer.options.icon) { // Factory icon - ignore
									}										
									else if (shapeLayer.options.band == undefined) {	
										alertScope.consoleLog("[rifd-dsub-maptable.js] cannot resolve shapesLayerList[" + i + 
											 "].options.band/area; options: " + JSON.stringify(shapeLayer.options));
										shapeLayerOptionsBanderror++;
									}
									else {
										if (shapesLayerBands[shapeLayer.options.band] == undefined) {
											shapesLayerBands[shapeLayer.options.band] = [];
										}
										
										if (shapeLayer.options.area) {
											if (shapesLayerAreas[shapeLayer.options.area] == undefined) {
												shapesLayerAreas[shapeLayer.options.area] = [];
											}
											shapesLayerAreas[shapeLayer.options.area].push($scope.shapes.getLayerId(shapeLayer));
										}
										else {
											useBands=true;
										}
										shapesLayerBands[shapeLayer.options.band].push($scope.shapes.getLayerId(shapeLayer));
										if (maxBands < shapeLayer.options.band) {
											maxBands=shapeLayer.options.band;
										}
										layerCount++;
									}
								}

								if (!useBands) { // Use areas - all present
	
									shapesLayerAreaList=Object.keys(shapesLayerAreas); 
									shapesLayerAreaList.sort(function(a, b){return b - a}); 
									// Sort into descended list so the smallest areas are in front
									alertScope.consoleDebug("[rifd-dsub-maptable.js] sorted areas: " + shapesLayerAreaList.length + 
										"; " + JSON.stringify(shapesLayerAreaList));
									for (var k=0; k<shapesLayerAreaList.length; k++) {
											
										for (var area in shapesLayerAreas) {
											if (area == shapesLayerAreaList[k]) {
												var areaIdList=shapesLayerAreas[area];
												for (var l=0; l<areaIdList.length; l++) {
													var shapeLayer=$scope.shapes.getLayer(areaIdList[l]);
													if (shapeLayer && typeof shapeLayer.bringToFront === "function") { 
														alertScope.consoleDebug("[rifd-dsub-maptable.js] bring layer: " + areaIdList[l] + " to front" +
															"; band: " + shapeLayer.options.band +
															"; area: " + shapeLayer.options.area);
														shapeLayer.bringToFront();
													}
													else {		
														shapeLayerBringToFrontError++;
														alertScope.consoleLog("[rifd-dsub-maptable.js] cannot resolve shapesLayerAreas[" + area + 
															"][" + l + "].bringToFront()");
													}
												}
											}
										}
									}
								}
								else { // Use bands
									
									for (var j=maxBands; j>0; j--) { 
										alertScope.consoleDebug("[rifd-dsub-maptable.js] band: " + j + "/" + maxBands + 
											"; areas: "  + Object.keys(shapesLayerAreas).length +
											"; bands: " + Object.keys(shapesLayerBands).length + 
											"; layers: " + shapesLayerBands[j].length + "; ids: " + JSON.stringify(shapesLayerBands[j]));
										for (var k=0; k<shapesLayerBands[j].length; k++) {
											var shapeLayer=$scope.shapes.getLayer(shapesLayerBands[j][k]);
											if (shapeLayer && typeof shapeLayer.bringToFront === "function") { 
												shapeLayer.bringToFront();
											}
											else {		
												shapeLayerBringToFrontError++;
												alertScope.consoleLog("[rifd-dsub-maptable.js] cannot resolve shapesLayerBands[" + j + 
													"][" + k + "].bringToFront()");
											}
										}
									}
								} 
								
								alertScope.consoleDebug("[rifd-dsub-maptable.js] brought " + layerCount + " shapes in " + 
									maxBands + " layer(s) to the front");
								if (shapeLayerOptionsBanderror > 0) {	
									alertScope.showError("[rifd-dsub-maptable.js] no band set in shapeLayer options (" + 
										shapeLayerOptionsBanderror + ")");
								}
								if (shapeLayerBringToFrontError > 0) {
									alertScope.showError("[rifd-dsub-maptable.js] shapeLayer bingToFront() error (" + 
										shapeLayerBringToFrontError + ")");
								}
							}
						}; 
						
                        //remove AOI layer
                        $scope.clearAOI = function () {
//                            if ($scope.areamap.hasLayer($scope.shpfile)) {
//                               $scope.areamap.removeLayer($scope.shpfile);
//                               $scope.shpfile = new L.layerGroup();
//                            }
                        };
                        //Select all in map and table
                        $scope.selectAll = function () {
                            $scope.selectedPolygon.length = 0;
                            for (var i = 0; i < $scope.gridOptions.data.length; i++) {
                                $scope.selectedPolygon.push({
									id: $scope.gridOptions.data[i].area_id, 
									gid: $scope.gridOptions.data[i].area_id, 
									label: $scope.gridOptions.data[i].label, 
									band: $scope.currentBand});
                            }
                        };
                        $scope.changeOpacity = function (v) {
                            $scope.transparency = v;
                            $scope.input.transparency = $scope.transparency;
                            if ($scope.geoJSON) {
                                $scope.geoJSON._geojsons.default.eachLayer(handleLayer);
                            }

                        };
                        //Reset only the selected band back to 0
                        $scope.clearBand = function () {
                            var i = $scope.selectedPolygon.length;
                            while (i--) {
                                if ($scope.selectedPolygon[i].band === $scope.currentBand) {
                                    $scope.selectedPolygon.splice(i, 1);
                                }
                            }
                        };
                        //Zoom to layer
                        $scope.zoomToExtent = function () {
                            $scope.areamap.fitBounds(maxbounds);
                        };
                        //Zoom to selection
                        $scope.zoomToSelection = function () {
                            var studyBounds = new L.LatLngBounds();
                            if (angular.isDefined($scope.geoJSON && $scope.geoJSON._geojsons && $scope.geoJSON._geojsons.default)) {
                                $scope.geoJSON._geojsons.default.eachLayer(function (layer) {
                                    for (var i = 0; i < $scope.selectedPolygon.length; i++) {
                                        if ($scope.selectedPolygon[i].id === layer.feature.properties.area_id) {
                                            studyBounds.extend(layer.getBounds());
                                        }
                                    }
                                });
                                if (studyBounds.isValid()) {
                                    $scope.areamap.fitBounds(studyBounds);
                                }
                            }
                        };
                        //Show-hide centroids
                        $scope.showCentroids = function () {
                            if ($scope.areamap.hasLayer(centroidMarkers)) {
                                $scope.areamap.removeLayer(centroidMarkers);
                            } else {
                                $scope.areamap.addLayer(centroidMarkers);
                            }
                        }; 
						
                        // Show-hide shapes and associated info
						$scope.showShapes = function () {
                            if ($scope.shapes == undefined) {
								alertScope.showError("[rifd-dsub-maptable.js] no shapes layerGroup");
							}
							else if ($scope.areamap.hasLayer($scope.shapes)) {
                                $scope.areamap.removeLayer($scope.shapes);
								alertScope.consoleDebug("[rifd-dsub-maptable.js] remove shapes layerGroup");
								if ($scope.info._map) { // Remove info control
									$scope.info.remove();
									alertScope.consoleDebug("[rifd-dsub-maptable.js] remove info control");
								}
                            } 
							else {
                                $scope.areamap.addLayer($scope.shapes);
								alertScope.consoleDebug("[rifd-dsub-maptable.js] add shapes layerGroup");
								if ($scope.info._map == undefined) { // Add back info control
									$scope.info.addTo($scope.areamap);
									alertScope.consoleDebug("[rifd-dsub-maptable.js] add info control");
								}
								
								$scope.bringShapesToFront();
                            }
                        };

                        /*
                         * DISEASE MAPPING OR RISK MAPPING
                         */
                        $scope.studyTypeChanged = function () {
                            //clear selection
                            $scope.clear();
                            //offer the correct number of bands
                            SubmissionStateService.getState().studyType = $scope.input.type;
                            if ($scope.input.type === "Risk Analysis") {
                                $scope.possibleBands = [1, 2, 3, 4, 5, 6];
                                $scope.areamap.band = 6;
								
								SelectStateService.initialiseRiskAnalysis();
                            } else {
                                $scope.possibleBands = [1];
                                $scope.currentBand = 1;
                                $scope.areamap.band = 1;
								
								SelectStateService.resetState();
                            }
                        };

                        /*
                         * RENDER THE MAP AND THE TABLE
                         */
                        getMyMap = function () {

                            if ($scope.areamap.hasLayer($scope.geoJSON)) {
                                $scope.areamap.removeLayer($scope.geoJSON);
                            }

                            var topojsonURL = user.getTileMakerTiles(user.currentUser, thisGeography, $scope.input.selectAt);
                            latlngList = []; // centroids!
                            latlngListById = []; // centroids!
                            centroidMarkers = new L.layerGroup();

                            //Get the centroids from DB
                            var bWeightedCentres = true;
                            user.getTileMakerCentroids(user.currentUser, thisGeography, $scope.input.selectAt).then(function (res) {
								
								
								if (res.data.smoothed_results[0] && res.data.smoothed_results[0].pop_x && res.data.smoothed_results[0].pop_y) {
									$scope.centroid_type="population weighted";
								}
								else if (res.data.smoothed_results[0] && res.data.smoothed_results[0].x && res.data.smoothed_results[0].y) {
									$scope.centroid_type="database geographic";
								}
								
								var latlngListDups=0;
                                for (var i = 0; i < res.data.smoothed_results.length; i++) {
									
                                    var p = res.data.smoothed_results[i];
									latlngList.push({
										latLng: L.latLng([p.y, p.x]), 
										name: p.name, 
										id: p.id,
										band: -1
									});
									
                                    var circle = new L.CircleMarker([p.y, p.x], {
                                        radius: 2,
                                        fillColor: "blue",
                                        color: "#000",
                                        weight: 1,
                                        opacity: 1,
                                        fillOpacity: 0.8
                                    });
									
                                    centroidMarkers.addLayer(circle);

									if (latlngListById[p.id]) {
										latlngListDups++;
									}
									else {
										latlngListById[p.id] = {
											latLng: L.latLng([p.y, p.x]), 
											name: p.name,
											circleId: centroidMarkers.getLayerId(circle)
										}
									}
									
									if (latlngListDups > 0) {
										alertScope.showWarning("Duplicate IDs in centroid list");
									}
                                }
                            }, function () {
                                //couldn't get weighted centres so generate geographic with leaflet
                                alertScope.showWarning("Could not find (weighted) centroids stored in database - calculating geographic centroids on the fly");
                                bWeightedCentres = false;
								$scope.centroid_type="Leaflet calculated geographic";
                            }).then(function () {
								var latlngListDups=0;
                                $scope.geoJSON = new L.topoJsonGridLayer(topojsonURL, {
                                    attribution: 'Polygons &copy; <a href="http://www.sahsu.org/content/rapid-inquiry-facility" target="_blank">Imperial College London</a>',
                                    layers: {
                                        default: {
                                            renderer: L.canvas(),
                                            style: style,
                                            onEachFeature: function (feature, layer) {
                                                //get as centroid marker layer. 
                                                if (!bWeightedCentres) {
                                                    var p = layer.getBounds().getCenter();
                                                    latlngList.push({
														latLng: L.latLng([p.lat, p.lng]), 
														name: feature.properties.name, 
														id: feature.properties.area_id,
														bnand: -1
													});
													feature.properties.latLng = L.latLng([p.lat, p.lng]);
                                                    var circle = new L.CircleMarker([p.lat, p.lng], {
                                                        radius: 2,
                                                        fillColor: "red",
                                                        color: "#000",
                                                        weight: 1,
                                                        opacity: 1,
                                                        fillOpacity: 0.8
                                                    });
													
                                                    centroidMarkers.addLayer(circle);
													
													if (latlngListById[feature.properties.area_id]) {
														latlngListDups++;
													}
													else {
														latlngListById[feature.properties.area_id] = {
															latLng: L.latLng([p.lat, p.lng]), 
															name: p.name,
															circleId: centroidMarkers.getLayerId(circle)
														}
													}
                                                }
												else { // Using database centroids
													feature.properties.latLng = latlngListById[feature.properties.area_id].latLng;
												}
												feature.properties.circleId = latlngListById[feature.properties.area_id].circleId;
												
                                                layer.on('mouseover', function (e) {
                                                    //if drawing then return
                                                    if ($scope.input.bDrawing) {
                                                        return;
                                                    }
                                                    this.setStyle({
                                                        color: 'gray',
                                                        weight: 1.5,
                                                        fillOpacity: function () {
                                                            //set tranparency from slider
                                                            return($scope.transparency - 0.3 > 0 ? $scope.transparency - 0.3 : 0.1);
                                                        }()
                                                    });
                                                    $scope.thisPolygon = feature.properties.name;
													// Centroids: feature.properties.latLng [app] and 
													// feature.properties.geographic_centroid{} [tilemaker]
													
													if (feature.properties.circleId) {
														$scope.highLightedCircleId=feature.properties.circleId;
														var circle=centroidMarkers.getLayer(feature.properties.circleId);
														circle.setStyle({
															radius: 3,
															weight: 2
														});
													}
                                                    $scope.$digest();
                                                });
                                                layer.on('mouseout', function (e) {
                                                    $scope.geoJSON._geojsons.default.resetStyle(e.target);
                                                    $scope.thisPolygon = "";
													if ($scope.highLightedCircleId) {
														var circle=centroidMarkers.getLayer(feature.properties.circleId);
														circle.setStyle({
															radius: 2,
															weight: 1
														});
														$scope.highLightedCircleId=undefined;
													}
                                                    $scope.$digest();
                                                });
                                                layer.on('click', function (e) {
                                                    //if drawing then return
                                                    if ($scope.input.bDrawing) {
                                                        return;
                                                    }
                                                    var thisPoly = e.target.feature.properties.area_id;
                                                    var bFound = false;
                                                    for (var i = 0; i < $scope.selectedPolygon.length; i++) {
                                                        if ($scope.selectedPolygon[i].id === thisPoly) {
                                                            bFound = true;
                                                            $scope.selectedPolygon.splice(i, 1);  // delete
                                                            break;
                                                        }
                                                    }
                                                    if (!bFound) {
                                                        $scope.selectedPolygon.push({
															id: feature.properties.area_id, 
															gid: feature.properties.gid, label: 
															feature.properties.name, 
															band: $scope.currentBand});
                                                    }
                                                    $scope.$digest(); // Force $watch sync
                                                });
                                            }
                                        }
                                    }
                                });
                                $scope.areamap.addLayer($scope.geoJSON);

                                //Get max bounds
                                user.getGeoLevelSelectValues(user.currentUser, thisGeography).then(function (res) {
                                    var lowestLevel = res.data[0].names[0];
                                    user.getTileMakerTilesAttributes(user.currentUser, thisGeography, lowestLevel).then(function (res) {
                                        maxbounds = L.latLngBounds([res.data.bbox[1], res.data.bbox[2]], [res.data.bbox[3], res.data.bbox[0]]);
                                        if (Math.abs($scope.input.center.lng) < 1 && Math.abs($scope.input.center.lat < 1)) {
                                            $scope.areamap.fitBounds(maxbounds);
                                        }
                                    });
                                });

                                //Get overall layer properties
                                user.getTileMakerTilesAttributes(user.currentUser, thisGeography, $scope.input.selectAt).then(function (res) {
                                    if (angular.isUndefined(res.data.objects)) {
                                        alertScope.showError("Could not get district polygons from database");
                                        return;
                                    }                                  
                                    //populate the table
                                    for (var i = 0; i < res.data.objects.collection.geometries.length; i++) {
                                        var thisPoly = res.data.objects.collection.geometries[i];
                                        var bFound = false;
                                        for (var j = 0; j < $scope.selectedPolygon.length; j++) {
                                            if ($scope.selectedPolygon[j].id === thisPoly.properties.area_id) {
                                                res.data.objects.collection.geometries[i].properties.band = $scope.selectedPolygon[j].band;
                                                bFound = true;
                                                break;
                                            }
                                        }
                                        if (!bFound) {
                                            res.data.objects.collection.geometries[i].properties.band = 0;
                                        }
                                    }
                                    $scope.gridOptions.data = ModalAreaService.fillTable(res.data);
                                    $scope.totalPolygonCount = res.data.objects.collection.geometries.length;
                                });
                            }).then(function () {
								// Add back selected shapes
								addSelectedShapes();	
							});
                        };

                        /*
                         * GET THE SELECT AND VIEW RESOLUTIONS
                         */
                        $scope.geoLevels = [];
                        $scope.geoLevelsViews = [];
                        user.getGeoLevelSelectValues(user.currentUser, thisGeography).then(handleGeoLevelSelect, handleGeographyError);

                        $scope.geoLevelChange = function () {
                            //Clear the map
                            $scope.selectedPolygon.length = 0;
//                            $scope.clearAOI();
                            if ($scope.areamap.hasLayer(centroidMarkers)) {
                                $scope.areamap.removeLayer(centroidMarkers);
                            }
                            user.getGeoLevelViews(user.currentUser, thisGeography, $scope.input.selectAt).then(handleGeoLevelViews, handleGeographyError);
                        };
						
						function addSelectedShapes() {
							var selectedShapes=undefined;
							// Add back selected shapes
							if ($scope.input.name == "ComparisionAreaMap") {
								selectedShapes=SelectStateService.getState().studySelection.comparisonShapes;
							}
							else {
								selectedShapes=SelectStateService.getState().studySelection.studyShapes;
							}
							if (selectedShapes) {
								alertScope.consoleDebug("[rifd-dsub-maptable.js] addSelectedShapes() selectedShapes " + 
									$scope.input.name + ": " + 
									selectedShapes.length + " shape");
								for (var i=0; i<selectedShapes.length; i++) {
									var points=0;
									if (selectedShapes[i].geojson &&
									    selectedShapes[i].geojson.geometry.coordinates[0]) {
										points=selectedShapes[i].geojson.geometry.coordinates[0].length;
									}
									alertScope.consoleDebug("[rifd-dsub-maptable.js] selectedShape[" + i + "] " +
										"band: " + selectedShapes[i].band +
										"; color[" + (selectedShapes[i].band-1) + "]: " + selectorBands.bandColours[selectedShapes[i].band-1] +
										"; circle: " + selectedShapes[i].circle +
										"; freehand: " + selectedShapes[i].freehand +
										"; points: " + points);
								}
								
								if ($scope.info._map == undefined) { // Add back info control
									$scope.info.addTo($scope.areamap);
								}
								
								if (!$scope.areamap.hasLayer($scope.shapes)) {
									alertScope.consoleDebug("[rifd-dsub-maptable.js] addSelectedShapes(): add shapes layerGroup");
									$scope.shapes = new L.layerGroup();
									$scope.areamap.addLayer($scope.shapes);
								}
								else {
									if ($scope.shapes.getLayers().length == 0) {
										alertScope.consoleDebug("[rifd-dsub-maptable.js] start addSelectedShapes(): shapes layerGroup has no layers");				
//										$scope.areamap.removeLayer($scope.shapes);
//										$scope.shapes = new L.layerGroup();
//										$scope.areamap.addLayer($scope.shapes);
									}
									else {
										alertScope.consoleDebug("[rifd-dsub-maptable.js] start addSelectedShapes(): shapes layerGroup has " +
											$scope.shapes.getLayers().length + " layers");
									}
								}
								
								for (var i = 0; i < selectedShapes.length; i++) {
									var selectedShape=selectedShapes[i];
									function selectedShapesHighLightFeature(e, selectedShape) {
										alertScope.consoleDebug("[rifd-dsub-maptable.js] selectedShapesHighLightFeature " + 
											"(" + e.target._leaflet_id + "; " + JSON.stringify(e.target._latlng) + "): " +
											(JSON.stringify(selectedShape.properties) || "no properties"));
										$scope.info.update(selectedShape, e.target._latlng);
									}									
									function selectedShapesResetFeature(e) {
										alertScope.consoleDebug("[rifd-dsub-maptable.js] selectedShapesResetFeature " +  
											"(" + e.target._leaflet_id + "; " + JSON.stringify(e.target._latlng) + "): " +
											(JSON.stringify(selectedShape.properties) || "no properties"));
										$scope.info.update(undefined, e.target._latlng);
									}		
									
									if (selectedShape.circle) { // Represent circles as a point and a radius
									
										if ((selectedShape.band == 1) || (selectedShape.band > 1 && !selectedShape.finalCircleBand)) {
											// basic shape to map shapes layer group
											var circle = new L.Circle([selectedShape.latLng.lat, selectedShape.latLng.lng], {
													pane: 'shapes', 
													band: selectedShape.band,
													area: selectedShape.area,
													radius: selectedShape.radius,
													color: (selectorBands.bandColours[selectedShapes[i].band-1] || 'blue'),
													weight: (selectorBands.weight || 3),
													opacity: (selectorBands.opacity || 0.8),
													fillOpacity: (selectorBands.fillOpacity || 0),
													selectedShape: selectedShape
												});										
											circle.on({
//												dblclick : function(e) {
//													selectedShapesHighLightFeature(e, this.options.selectedShape);
//												}, 
												mouseover : function(e) {
													selectedShapesHighLightFeature(e, this.options.selectedShape);
												}, 
												mouseout : function(e) {
													selectedShapesResetFeature(e, this.options.selectedShape);
												} 
											}); 
											
											if (circle) {
												$scope.shapes.addLayer(circle);
												alertScope.consoleDebug("[rifd-dsub-maptable.js] addSelectedShapes(): " +
													"adding circle: " + JSON.stringify(selectedShape.latLng) + 
													"; color[" + (selectedShapes[i].band-1) + "]: " + (selectorBands.bandColours[selectedShapes[i].band-1] || 'blue') + 
													"; radius: " + selectedShape.radius + 
													"; band: " + selectedShape.band +
													"; area: " + selectedShape.area);
											}
											else {
												alertScope.showError("Could not restore circle");
											}
											
											if (selectedShape.band == 1) {
											   var factory = L.icon({
													iconUrl: 'images/factory.png',
													iconSize: 15
												});
												var marker = new L.marker([selectedShape.latLng.lat, selectedShape.latLng.lng], {
													pane: 'shapes',
													icon: factory
												});
												$scope.shapes.addLayer(marker);
											}
										}
									}
									else { // Use L.polygon(), L.geoJSON needs a GeoJSON layer
										var polygon; 
										var coordinates=selectedShape.geojson.geometry.coordinates[0];												
										if (selectedShape.freehand) { // Shapefile		
											coordinates=selectedShape.coordinates;	
										}		
																		
										polygon=L.polygon(coordinates, {
												pane: 'shapes', 
												band: selectedShape.band,
												area: selectedShape.area,
												color: (selectorBands.bandColours[selectedShapes[i].band-1] || 'blue'),
												weight: (selectorBands.weight || 3),
												opacity: (selectorBands.opacity || 0.8),
												fillOpacity: (selectorBands.fillOpacity || 0),
												selectedShape: selectedShape
											});		
										if (polygon && polygon._latlngs.length > 0) {										
											polygon.on({
//												dblclick : function(e) {
//													selectedShapesHighLightFeature(e, this.options.selectedShape);
//												}, 
												mouseover : function(e) {
													selectedShapesHighLightFeature(e, this.options.selectedShape);
												}, 
												mouseout : function(e) {
													selectedShapesResetFeature(e, this.options.selectedShape);
												} 
											}); 
											$scope.shapes.addLayer(polygon);
											alertScope.consoleDebug("[rifd-dsub-maptable.js] addSelectedShapes(): adding polygon" + 
												"; band: " + selectedShape.band +
												"; area: " + selectedShape.area +
												"; freehand: " + selectedShape.freehand +
												"; " + coordinates.length + " coordinates; " +
												JSON.stringify(coordinates).substring(0,100) + "...");							
										}
										else {
											alertScope.consoleDebug("[rifd-dsub-maptable.js] addSelectedShapes(): L.Polygon is undefined" +
												"; geoJSON: " + JSON.stringify(selectedShape.geojson, null, 1));
											if (selectedShape.freehand) {	
												alertScope.showError("Could not restore freehand Polygon shape");
											}
											else {
												alertScope.showError("Could not restore shapefile Polygon shape");
											}
										}
										
									}
								}
								
								$scope.areamap.whenReady(function() {
									alertScope.consoleDebug("[rifd-dsub-maptable.js] end addSelectedShapes(): shapes layerGroup has " +
										$scope.shapes.getLayers().length + " layers" +
										"; centered: " + JSON.stringify($scope.center));
										
									$timeout(function() {

										$scope.zoomToSelection(); // Zoom to selection	
										$timeout(function() {								
											$scope.redrawMap();
										}, 100);			
									}, 100);			
								});
							}
						}

                        function handleGeoLevelSelect(res) {
                            $scope.geoLevels.length = 0;
                            for (var i = 0; i < res.data[0].names.length; i++) {
                                $scope.geoLevels.push(res.data[0].names[i]);
                            }
                            //To check that comparison study area not greater than study area
                            //Assumes that geoLevels is ordered array
                            $scope.input.geoLevels = $scope.geoLevels;
                            //Only get default if pristine
                            if ($scope.input.selectAt === "" & $scope.input.studyResolution === "") {
                                user.getDefaultGeoLevelSelectValue(user.currentUser, thisGeography).then(handleDefaultGeoLevels, handleGeographyError);
                            } else {
                                user.getGeoLevelViews(user.currentUser, thisGeography, $scope.input.selectAt).then(handleGeoLevelViews, handleGeographyError);
                            }
                        }
                        function handleDefaultGeoLevels(res) {
                            //get the select levels
                            $scope.input.selectAt = res.data[0].names[0];
                            $scope.input.studyResolution = res.data[0].names[0];
                            $scope.geoLevelChange();
                        }
                        function handleGeoLevelViews(res) {
                            $scope.geoLevelsViews.length = 0;
                            for (var i = 0; i < res.data[0].names.length; i++) {
                                $scope.geoLevelsViews.push(res.data[0].names[i]);
                            }
                            //if not in list then match (because result res cannot be lower than select res)
                            if ($scope.geoLevelsViews.indexOf($scope.input.studyResolution) === -1) {
                                $scope.input.studyResolution = $scope.input.selectAt;
                            }
                            //get table
                            getMyMap();
							
                        }

                        function handleGeographyError() {
                            $scope.close();
                        }

                        /*
                         * MAP SETUP
                         */
                        //district centres for rubberband selection
                        var latlngList = [];
                        var centroidMarkers = new L.layerGroup();
						$scope.shapes = new L.layerGroup();
                        $scope.areamap.addLayer($scope.shapes);
						
                        //shapefile AOI, used in directive
//                        $scope.shpfile = new L.layerGroup();

                        //Set up table (UI-grid)
                        $scope.gridOptions = ModalAreaService.getAreaTableOptions();
                        $scope.gridOptions.columnDefs = ModalAreaService.getAreaTableColumnDefs();
                        //Enable row selections
                        $scope.gridOptions.onRegisterApi = function (gridApi) {
                            $scope.gridApi = gridApi;
                        };
						
                        //Set the user defined basemap
                        $scope.renderMap = function (mapID) {
                            $scope.areamap.removeLayer($scope.thisLayer);
                            if (!LeafletBaseMapService.getNoBaseMap("areamap")) {
                                $scope.thisLayer = LeafletBaseMapService.setBaseMap(LeafletBaseMapService.getCurrentBaseMapInUse("areamap"));
                                $scope.thisLayer.addTo($scope.areamap);
                            }
                            //hack to refresh map
                            setTimeout(function () {
                                $scope.areamap.invalidateSize();
                            }, 50);
                        };

                        function renderFeature(feature) {
                            for (var i = 0; i < $scope.selectedPolygon.length; i++) {
                                if ($scope.selectedPolygon[i].id === feature) {
                                    bFound = true;
                                    //max possible is six bands according to specs
//                                    var cb = ['#e41a1c', '#377eb8', '#4daf4a', '#984ea3', '#ff7f00', '#ffff33'];
//                                    return cb[$scope.selectedPolygon[i].band - 1];
                                    return selectorBands.bandColours[$scope.selectedPolygon[i].band - 1];
                                }
                            }
                            return '#F5F5F5'; //whitesmoke
                        }

                        //Functions to style topoJson on selection changes
                        function style(feature) {
                            return {
                                fillColor: renderFeature(feature.properties.area_id),
                                weight: 1,
                                opacity: 1,
                                color: 'gray',
                                fillOpacity: $scope.transparency
                            };
                        }
                        function handleLayer(layer) {
                            layer.setStyle({
                                fillColor: renderFeature(layer.feature.properties.area_id),
                                fillOpacity: $scope.transparency
                            });
                        }

                        //********************************************************************************************************
                        //Watch selectedPolygon array for any changes
                        $scope.$watchCollection('selectedPolygon', function (newNames, oldNames) {
                            if (newNames === oldNames) {
                                return;
                            }
                            //Update table selection
                            $scope.gridApi.selection.clearSelectedRows();
                            for (var i = 0; i < $scope.gridOptions.data.length; i++) {
                                $scope.gridOptions.data[i].band = 0;
                                for (var j = 0; j < $scope.selectedPolygon.length; j++) {
                                    if ($scope.gridOptions.data[i].area_id === $scope.selectedPolygon[j].id) {
                                        $scope.gridOptions.data[i].band = $scope.selectedPolygon[j].band;
                                    }
                                }
                            }
                            //Update the area counter
                            $scope.selectedPolygonCount = newNames.length;

                            if (!$scope.geoJSON) {
                                return;
                            } else {
                                //Update map selection    
                                $scope.geoJSON._geojsons.default.eachLayer(handleLayer);
                            }

                        });
                        //*********************************************************************************************************************
                        //SELECTION METHODS

                        /*
                         * SELECT AREAS USING LEAFLETDRAW
                         */
                        //Add Leaflet.Draw capabilities
                        var drawnItems;
                        LeafletDrawService.getCircleCapability();
                        LeafletDrawService.getPolygonCapability();
                        
                        //Add Leaflet.Draw toolbar
                        L.drawLocal.draw.toolbar.buttons.circle = "Select by concentric bands";
                        L.drawLocal.draw.toolbar.buttons.polygon = "Select by freehand polygons";
                        drawnItems = new L.FeatureGroup();
                        $scope.areamap.addLayer(drawnItems);
                        var drawControl = new L.Control.Draw({
                            draw: {
                                polygon: {
                                    shapeOptions: {
                                        color: '#0099cc',
                                        weight: 4,
                                        opacity: 1,
                                        fillOpacity: 0.2
                                    }
                                },
                                marker: false,
                                polyline: false,
                                rectangle: false
                            },
                            edit: {
                                remove: false,
                                edit: false,
                                featureGroup: drawnItems
                            }
                        });
                        $scope.areamap.addControl(drawControl);
                        new L.Control.GeoSearch({
                            provider: new L.GeoSearch.Provider.OpenStreetMap()
                        }).addTo($scope.areamap);
                        //add the circle to the map
                        $scope.areamap.on('draw:created', function (e) {
                            drawnItems.addLayer(e.layer);
                        });
                        //override other map mouse events
                        $scope.areamap.on('draw:drawstart', function (e) {
                            $scope.input.bDrawing = true;
                        });

						// To trigger the watchers
						$scope.safeApply = function(count, fn) {
							var phase = this.$root.$$phase;
							if (phase == '$apply' || phase == '$digest') {
								if (fn && (typeof(fn) === 'function')) {
									fn();
								}
								alertScope.consoleLog("[rifd-dsub-maptable.js] (" + count + ") No need to apply(), in progress");
								if (count <= 10) { // try again up to 10 times
									$timeout(function() {
										$scope.safeApply(count++, fn);
										}, 1000);
								}
							} else {
								alertScope.consoleLog("[rifd-dsub-maptable.js] (" + count + ") Call apply() to trigger watchers");
								this.$apply(fn);
							}
						};
						
						// Map redraw function with slight delay for leaflet
						$scope.redrawMap = function() {
							$scope.bringShapesToFront();
									
							$scope.areamap.whenReady(function() {
								$timeout(function() {										
										
										alertScope.consoleLog("[rifd-dsub-maptable.js] redraw map");
										$scope.areamap.fitBounds($scope.areamap.getBounds()); // Force map to redraw after 0.5s delay
									}, 500);	
							});									
						};
						
                        // completed selection event fired from service
						$scope.$on('completedDrawSelection', function (event, data) {
							
							alertScope.consoleLog("[rifd-dsub-maptable.js] completed Draw Selection");
							if ($scope.info._map == undefined) { // Add back info control
								$scope.info.addTo($scope.areamap);
							}
							$scope.zoomToSelection(); // Zoom to selection
							$scope.safeApply(0, function() {
								$scope.redrawMap();
							});					
                        });
						
                        // selection event fired from service
                        $scope.$on('makeDrawSelection', function (event, data) {
                            $scope.makeDrawSelection(data);
                        });
                        $scope.makeDrawSelection = function (shape) {
							
							// Create savedShape for SelectStateService
							var savedShape = {
								circle: shape.circle,
								freehand: shape.freehand,
								band: shape.band,
								area: shape.area, 
								properties: shape.properties,
								radius: undefined,
								latLng: undefined,
								geojson: undefined,
								finalCircleBand: (shape.finalCircleBand || false),
								style: undefined
							}
							
							if (savedShape.freehand) {
								if (shape.band == -1) {
									shape.band=1;
									savedShape.band=1;
								}
								
								if (shape.data._latlngs && shape.data._latlngs.length > 1) { // Fix freehand polygons
									if (shape.data._latlngs[0].lat == shape.data._latlngs[shape.data._latlngs.length-1].lat &&
									   shape.data._latlngs[0].lng == shape.data._latlngs[shape.data._latlngs.length-1].lng) { // OK
									} 
									else { // Make it a polygon
										shape.data._latlngs.push({
											lat: shape.data._latlngs[0].lat,
											lng: shape.data._latlngs[0].lng
										});
										alertScope.consoleDebug("[rifd-dsub-maptable.js] Fix freehand polygon; " +
										shape.data._latlngs.length + " points: " + 
											JSON.stringify(shape.data._latlngs));
									}
								}
							}
							
							savedShape.style={
										color: (selectorBands.bandColours[savedShape.band-1] || 'blue'),
										weight: (selectorBands.weight || 3),
										opacity: (selectorBands.opacity || 0.8),
										fillOpacity: (selectorBands.fillOpacity || 0)
									};
		
							function highLightFeature(e) {
								alertScope.consoleDebug("[rifd-dsub-maptable.js] makeDrawSelection highLightFeature " +  
										"(" + this._leaflet_id + "; " + JSON.stringify(this._latlng) + "): " +
										(JSON.stringify(savedShape.properties) || "no properties"));
								$scope.info.update(savedShape, this._latlng); 
							}									
							function resetFeature(e) {
								alertScope.consoleDebug("[rifd-dsub-maptable.js] makeDrawSelection resetFeature " +  
										"(" + this._leaflet_id + "; " + JSON.stringify(this._latlng) + "): " +
										(JSON.stringify(savedShape.properties) || "no properties"));
								$scope.info.update(undefined, this._latlng);
							}		
							
							if (shape.circle) { // Represent circles as a point and a radius
								savedShape.radius=shape.data.getRadius();
								savedShape.latLng=shape.data.getLatLng();
								if (savedShape.area == undefined || savedShape.area == 0) {
									savedShape.area = Math.round((Math.PI*Math.pow(shape.data.getRadius(), 2)*100)/1000000)/100 // Square km to 2dp
									
									if (savedShape.area == undefined || savedShape.area == 0) {
										alertScope.consoleDebug("[rifd-dsub-maptable.js] makeDrawSelection(): Cannot determine area" +
											"; geoJSON: " + JSON.stringify(savedShape.geojson, null, 1));
										alertScope.showError("Could not create circle shape");
									}
								}
								if ((shape.band == 1) || (shape.band > 1 && !savedShape.finalCircleBand)) {
									// basic shape to map shapes layer group
									var circle = new L.Circle([savedShape.latLng.lat, savedShape.latLng.lng], {
											pane: 'shapes', 
											band: savedShape.band,
											area: savedShape.area,
											radius: savedShape.radius,
											color: (savedShape.style.color || selectorBands.bandColours[savedShape.band-1] || 'blue'),
											weight: (savedShape.style.weight || selectorBands.weight || 3),
											opacity: (savedShape.style.opacity || selectorBands.opacity || 0.8),
											fillOpacity: (savedShape.style.fillOpacity || selectorBands.fillOpacity || 0)
										});										
									circle.on({
//										dblclick: highLightFeature,
										mouseover: highLightFeature,
										mouseout: resetFeature
									});
									
									$scope.shapes.addLayer(circle);
									alertScope.consoleLog("[rifd-dsub-maptable.js] makeDrawSelection() added circle" +
										"; color: " + selectorBands.bandColours[savedShape.band-1] +
										"; savedShape: " + JSON.stringify(savedShape, null, 1));
									if (shape.band == 1) {
					                   var factory = L.icon({
											iconUrl: 'images/factory.png',
											iconSize: 15
										});
										var marker = new L.marker([savedShape.latLng.lat, savedShape.latLng.lng], {
											pane: 'shapes',
											icon: factory
										});
										$scope.shapes.addLayer(marker);
									}
								}
								else {
									alertScope.consoleLog("[rifd-dsub-maptable.js] makeDrawSelection() suppressed circle" +
										"; savedShape: " + JSON.stringify(savedShape, null, 1));
								}
							}
							else { // Use geoJSON								
								
								var coordinates;
								
								if (shape.data._latlngs && shape.data._latlngs.length > 1) {
									coordinates=shape.data._latlngs;
								}
								else {
									coordinates=shape.data._latlngs[0];
								}
							
								var polygon;
								
								if (savedShape.freehand) {
									polygon=L.polygon(coordinates, {
											pane: 'shapes', 
											band: savedShape.band,
											area: savedShape.area,
											color: (savedShape.style.color || selectorBands.bandColours[savedShape.band-1] || 'blue'),
											weight: (savedShape.style.weight || selectorBands.weight || 3),
											opacity: (savedShape.style.opacity || selectorBands.opacity || 0.8),
											fillOpacity: (savedShape.style.fillOpacity || selectorBands.fillOpacity || 0)
										});		
									savedShape.coordinates=coordinates; // L.Polygon()	- now fixed; was a lineString
									savedShape.geojson=angular.copy(polygon.toGeoJSON());								
								}
								else { // Shapefile
									savedShape.geojson=angular.copy(shape.data.toGeoJSON());	
									savedShape.data=shape.data;									
									polygon=L.polygon(savedShape.geojson.geometry.coordinates[0], {
											pane: 'shapes', 
											band: savedShape.band,
											area: savedShape.area,
											color: (savedShape.style.color || selectorBands.bandColours[savedShape.band-1] || 'blue'),
											weight: (savedShape.style.weight || selectorBands.weight || 3),
											opacity: (savedShape.style.opacity || selectorBands.opacity || 0.8),
											fillOpacity: (savedShape.style.fillOpacity || selectorBands.fillOpacity || 0)
										});
								}
							
								if (polygon && polygon._latlngs.length > 0) {	
									
									if (savedShape.area == undefined || savedShape.area == 0) {
										if (savedShape.geojson) {
											savedShape.area = Math.round((turf.area(savedShape.geojson)*100)/1000000)/100; // Square km to 2dp
										}
										else {
											alertScope.consoleLog("[rifd-dsub-maptable.js] makeDrawSelection(): savedShape.area could not be set: " + 
												JSON.stringify(savedShape));
										}
									
										if (savedShape.area == undefined || savedShape.area == 0) {
											alertScope.consoleDebug("[rifd-dsub-maptable.js] makeDrawSelection(): Cannot determine area" +
												"; geoJSON: " + JSON.stringify(savedShape.geojson, null, 1));
											if (savedShape.freehand) {	
												alertScope.showError("Could not create freehand Polygon shape");
											}
											else {
												alertScope.showError("Could not create shapefile Polygon shape");
											}
										}
										polygon.options.area=savedShape.area;
									}
								
									polygon.on({
//										dblclick: highLightFeature,
										mouseover: highLightFeature,
										mouseout: resetFeature
									}); 
									$scope.shapes.addLayer(polygon);
										
									alertScope.consoleDebug("[rifd-dsub-maptable.js] makeDrawSelection(): added Polygon" + 
										"; band: " + savedShape.band +
										"; area: " + savedShape.area +
										"; freehand: " + savedShape.freehand +
										"; style: " + JSON.stringify(savedShape.style) +
										"; " + coordinates.length + " coordinates; " +
												JSON.stringify(coordinates).substring(0,100) + "..." +
										"; properties: " + (JSON.stringify(savedShape.properties) || "None"));			
								}
								else {
									alertScope.consoleDebug("[rifd-dsub-maptable.js] makeDrawSelection(): L.Polygon is undefined" +
										"; geoJSON: " + JSON.stringify(savedShape.geojson, null, 1));
									if (savedShape.freehand) {	
										alertScope.showError("Could not create freehand Polygon shape");
									}
									else {
										alertScope.showError("Could not create shapefile Polygon shape");
									}
								}
							}	
		
							// Save to SelectStateService
							if ($scope.input.name == "ComparisionAreaMap") { 
								SelectStateService.getState().studySelection.comparisonShapes.push(savedShape);
								alertScope.consoleDebug("[rifd-dsub-maptable.js] Save to ComparisionAreaMap SelectStateService " +
									SelectStateService.getState().studySelection.comparisonShapes.length);
							}
							else {
								SelectStateService.getState().studySelection.studyShapes.push(savedShape);
								alertScope.consoleDebug("[rifd-dsub-maptable.js] Save to StudyAreaMap SelectStateService " +
									SelectStateService.getState().studySelection.studyShapes.length);
							}
							
							function latlngListCallback () { 
								var areaNameList = {};
								var areaCheck = {};
								var duplicateAreaCheckIds = [];
								
								// Check for duplicate selectedPolygons 
								for (var j = 0; j < $scope.selectedPolygon.length; j++) {
									var thisPolyID = $scope.selectedPolygon[j].id;
									if (areaCheck[thisPolyID]) {
										areaCheck[thisPolyID].count++;
									}
									else {
										areaCheck[thisPolyID] = { 
											count: 1,
											index: []
										};
									}
									areaCheck[thisPolyID].index.push(j);
								}
								for (var id in areaCheck) {
									if (areaCheck[id].count > 1) {
										duplicateAreaCheckIds.push(id);
									}
									
/*									if (duplicateAreaCheckIds.length < 10) {
										alertScope.consoleDebug("[rifd-dsub-maptable.js] " + 
											"duplicateAreaCheckIds[" + duplicateAreaCheckIds.length + "] " +
											id + "; duplicates: " +
											JSON.stringify(areaCheck[id].index));
									} */
								}
								if (duplicateAreaCheckIds.length > 0) {
									alertScope.consoleDebug("[rifd-dsub-maptable.js] " + 
										duplicateAreaCheckIds.length +
										" duplicateAreaCheckIds: " + JSON.stringify(duplicateAreaCheckIds));
									alertScope.showError("Duplicate area IDs detected in selected polygon list");
								}
								
								for (var i = 0; i < latlngList.length; i++) {
									var thisPolyID = latlngList[i].id;
									
									var bFound = false;
									// Update band
									for (var j = 0; j < $scope.selectedPolygon.length; j++) {
										if ($scope.selectedPolygon[j].id === thisPolyID) {
											latlngList[i].band=$scope.selectedPolygon[j].band;
											bFound=true;
											break;
										}
									} 	
									// Sync table - done by $scope.$watchCollection() above
                            									
									// Update areaNameList for debug
									if (latlngList[i].band && latlngList[i].band != -1) {
										if (areaNameList[latlngList[i].band]) {
											areaNameList[latlngList[i].band].push(latlngList[i].name);
										}
										else {
											areaNameList[latlngList[i].band] = [];
											areaNameList[latlngList[i].band].push(latlngList[i].name);
										}
									}
								}
										
								alertScope.consoleDebug("[rifd-dsub-maptable.js] $scope.selectedPolygon.length: " + $scope.selectedPolygon.length);
								
								for (var band in areaNameList) {
									alertScope.consoleDebug("[rifd-dsub-maptable.js] areaNameList band: " + 
									band + "; " + areaNameList[band].length);
//										": " + JSON.stringify(areaNameList[band]));	
								}

								if (!shape.circle && !shape.shapefile) {
									removeMapDrawItems();
									//auto increase band dropdown
									if ($scope.currentBand < Math.max.apply(null, $scope.possibleBands)) {
										$scope.currentBand++;
									}
								}												
							}

							var itemsProcessed = 0;
                            latlngList.forEach(
								function asyncFunction(latLng) { // Check centroids of areas lie within the shape
									//is point in defined polygon?
									var test;
									if (shape.circle) {
										test = GISService.getPointincircle(latLng.latLng, shape);
									} else {
										test = GISService.getPointinpolygon(latLng.latLng, shape);
									}
									if (test) { // Intersects
										var thisLatLng = latLng.latLng;
										var thisPoly = latLng.name;
										var thisPolyID = latLng.id;
										var bFound = false;
										
										// Selects the correct polygons
										for (var i = 0; i < $scope.selectedPolygon.length; i++) {
											if ($scope.selectedPolygon[i].id === thisPolyID) { // Found
												if ($scope.selectedPolygon[i].band == undefined ||
													$scope.selectedPolygon[i].band === -1) { 
															// If not set in concentric shapes
													if (latlngList[itemsProcessed].band == undefined ||
														latlngList[itemsProcessed].band === -1) { 
															// If not set on map
														if (shape.band === -1) {  // Set band
															$scope.selectedPolygon[i].band=$scope.currentBand;
														}
														else {
															$scope.selectedPolygon[i].band=shape.band;
														}		
													}
												}
												bFound = true;
												break;
											}
										}
										
										if (!bFound) {
											if (shape.band === -1) {
												$scope.selectedPolygon.push({
													id: thisPolyID, 
													gid: thisPolyID, 
													label: thisPoly, 
													band: $scope.currentBand, 
													centroid: thisLatLng
												});
												latlngList[itemsProcessed].band=$scope.currentBand;
											} else {
												$scope.selectedPolygon.push({
													id: thisPolyID, 
													gid: thisPolyID, 
													label: thisPoly, 
													band: shape.band, 
													centroid: thisLatLng
												});
												latlngList[itemsProcessed].band=shape.band;
											}
											if (SelectStateService.getState().studyType == "risk_analysis_study") {
																
	//											SelectStateService.getState().studySelection.points.pushIfNotExist(thisLatLng, function(e) { 
	//												return e.lat === thisLatLng.lat && e.lng === thisLatLng.lng; 
	//											});
											}
										}
									}
									itemsProcessed++;
									if (itemsProcessed === latlngList.length) {
										latlngListCallback();
									}
								}
							);
                        }; // End of makeDrawSelection()
                        //remove drawn items event fired from service
                        $scope.$on('removeDrawnItems', function (event, data) {
                            removeMapDrawItems();
                        });
                        function removeMapDrawItems() {
                            drawnItems.clearLayers();
                            $scope.areamap.addLayer(drawnItems);
                            $scope.input.bDrawing = false; //re-enable layer events
                        }
						
						$scope.info = L.control();
						$scope.info.onAdd = function(map) {
							this._div = L.DomUtil.create('div', 'info');
							this.update();
							return this._div;
						};

						// method that we will use to update the control based on feature properties passed
						$scope.info.update = function (savedShape, latLng /* Of shape, not mouse! */) {
							
							if (this._div) {
								if (savedShape) {
									if (savedShape.circle) {
										this._div.innerHTML = '<h4>Circle;</h4><b>Radius: ' + Math.round(savedShape.radius * 10) / 10 + 'm</b></br>' +
											"<b>Lat: " + Math.round(savedShape.latLng.lat * 1000) / 1000 + // 100m precision
											"; long: " +  Math.round(savedShape.latLng.lng * 1000) / 1000 +'</b></br>';
									}
									else {
										var coordinates=savedShape.geojson.geometry.coordinates[0];												
										if (savedShape.freehand) { // Shapefile		
											savedShape=savedShape.coordinates;	
										}	
										
										if (savedShape.freehand) {
											this._div.innerHTML = '<h4>Freehand polygon</h4>';
										}
										else  {
											this._div.innerHTML = '<h4>Shapefile polygon</h4>';
										}
										if (coordinates) {
											this._div.innerHTML+='<b>' + coordinates.length + ' points</b></br>';
										}
									}
									
									if (savedShape.area) {
										this._div.innerHTML+= '<b>area: ' + savedShape.area + ' square km</b><br />'
									}
										
									for (var property in savedShape.properties) {
										if (property == 'area') {
											if (savedShape.area === undefined) {
												this._div.innerHTML+= '<b>' + property + ': ' + savedShape.properties[property] + ' square km</b><br />'
											}
										}
										else if (property != '$$hashKey') {
											this._div.innerHTML+= '<b>' + property + ': ' + savedShape.properties[property] + '</b><br />'
										}
									}
								}
								else if ($scope.shapes.getLayers().length > 0) {
									this._div.innerHTML = '<h4>Mouse over selection shapes to show properties</br>' +
										'Hide selection shapes to mouse over area names</h4>';
								}
								else {
									this._div.innerHTML = '<h4>Mouse over area names</h4>';
								}
								this._div.innerHTML += '<b>Centroids: ' + $scope.centroid_type + '</b>';
							}
							
/* The aim of this bit of code was to display the area. However "layer.fireEvent('mouseover');" breaks the selection and
   the latLng is the shape, not the position of the mouse. Encourage user to use show/hide selection instead
   
                            if (!$scope.input.bDrawing && 
							    angular.isDefined($scope.geoJSON && $scope.geoJSON._geojsons && $scope.geoJSON._geojsons.default)) {
							
								$scope.geoJSON._geojsons.default.eachLayer(function (layer) {	
									if (savedShape) {
										layer.fireEvent('mouseover'); // Breaks selection		
									}
									else {
										layer.fireEvent('mouseout');  	
									}
								});
							} */
						};
						
                        /*
                         * SELECT AREAS FROM A LIST, CSV
                         */
                        $scope.openFromList = function () {
                            $scope.modalHeader = "Upload ID file";
                            $scope.accept = ".csv";
                            $scope.showContent = function ($fileContent) {
                                $scope.content = $fileContent.toString();
                            };
                            $scope.uploadFile = function () {
								/* Upload CSV file. Required fields: ID,Band. Name is 
								   included to make the file more understandable. Ideally
								   this function should be made capitalisation insensitive
								   and more flexible in the names, i.e. ID/areaId/area_id
								   and Band/bandId/band_id
								
								e.g.
								ID,NAME,Band
								01779778,California,1
								01779780,Connecticut,1
								01705317,Georgia,1
								01779785,Iowa,1
								01779786,Kentucky,1
								01629543,Louisiana,1
								01779789,Michigan,1
								01779795,New Jersey,1
								00897535,New Mexico,1
								01455989,Utah,1
								01779804,Washington,1
								
								Structure of parsed JSON:
								
								listOfIDs=[
								  {
									"ID": "01785533",
									"NAME": "Alaska",
									"Band": "1"
								  },
								  ...
								  {
									"ID": "01779804",
									"NAME": "Washington",
									"Band": "1"
								  }
								]; 
								 */
                                try {
                                    //parse the csv file
                                    var listOfIDs = JSON.parse(JSONService.getCSV2JSON($scope.content));
                                    //attempt to fill 'selectedPolygon' with valid entries
                                    $scope.clear();
									
									if ($scope.input.type === "Risk Analysis") {
										SelectStateService.initialiseRiskAnalysis();
									}
									else {			
										SelectStateService.resetState();
									}
									
                                    var bPushed = false;
                                    var bInvalid = false;
                                    for (var i = 0; i < listOfIDs.length; i++) {
                                        for (var j = 0; j < $scope.gridOptions.data.length; j++) {
                                            if ($scope.gridOptions.data[j].area_id === listOfIDs[i].ID) {
                                                var thisBand = Number(listOfIDs[i].Band);
//												alertScope.consoleLog("[rifd-dsub-maptable.js] [" + i + "," + j + "] MATCH area_id: " + $scope.gridOptions.data[j].area_id + 
//													"; ID: " + listOfIDs[i].ID +
//													"; thisBand: " + thisBand);
                                                if ($scope.possibleBands.indexOf(thisBand) !== -1) {
                                                    bPushed = true;
                                                    $scope.selectedPolygon.push({
														id: $scope.gridOptions.data[j].area_id, 
														gid: $scope.gridOptions.data[j].area_id,
                                                        label: $scope.gridOptions.data[j].label, 
														band: Number(listOfIDs[i].Band)});
                                                    break;
                                                } else {
                                                    bInvalid = true;
                                                }
                                            }
                                        }
                                    }
                                    if (!bPushed) {
                                        alertScope.showWarning("No valid 'ID' fields or 'Band' numbers found in your list");
//										alertScope.consoleDebug("[rifd-dsub-maptable.js] " + JSON.stringify(listOfIDs, null, 2));
                                    } else if (!bInvalid) {
                                        alertScope.showSuccess("List uploaded sucessfully");
                                    } else {
                                        alertScope.showSuccess("List uploaded sucessfully, but some 'ID' fields or 'Band' numbers were not valid");
                                    }
                                } catch (e) {
                                    alertScope.showError("Could not read or process the file: Please check formatting");
                                }
                            };
							
                            var modalInstance = $uibModal.open({
                                animation: true,
                                templateUrl: 'dashboards/submission/partials/rifp-dsub-fromfile.html',
                                controller: 'ModalFileListInstanceCtrl',
                                windowClass: 'stats-Modal',
                                backdrop: 'static',
                                scope: $scope,
                                keyboard: false
                            });
                        };
                    }
                };
            }]);