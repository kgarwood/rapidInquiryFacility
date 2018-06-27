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
 * DIRECTIVE for risk analysis area selection using shapefiles
 */

/* global L */
angular.module("RIF")
        //Open a shapefile for risk analysis
        .controller('ModalAOIShapefileInstanceCtrl', function ($scope, $uibModalInstance) {
            $scope.close = function () {
                $uibModalInstance.dismiss();
            };
            $scope.submit = function () {
                var bOk = $scope.displayShapeFile();
                if (bOk) {
                    $uibModalInstance.close();
                }
            };
        })
        .directive('riskAnalysis', ['$rootScope', '$uibModal', '$q', 'ParametersService',
			// SelectStateService is not need as makeDrawSelection() in rifd-dsub-maptable.js is called to update
            function ($rootScope, $uibModal, $q, ParametersService) {
                return {
                    restrict: 'A', //added as attribute to in to selectionMapTools > btn-addAOI in rifs-utils-mapTools
                    link: function (scope, element, attr) {

                        var alertScope = scope.$parent.$$childHead.$parent.$parent.$$childHead;
                        var poly; //polygon shapefile
                        var buffers; //concentric buffers around points					
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
					
						// Also defined in rifs-util-leafletdraw.js
                        var factory = L.icon({
                            iconUrl: 'images/factory.png',
                            iconAnchor: [16, 16]
                        });
                        //user input boxes
                        scope.bandAttr = [];
                        element.on('click', function (event) {
                            scope.modalHeader = "Select with a shapefile";
                            scope.accept = ".zip";
                            var modalInstance = $uibModal.open({
                                animation: true,
                                templateUrl: 'dashboards/submission/partials/rifp-dsub-fromshp.html',
                                windowClass: 'shapefile-modal',
                                controller: 'ModalAOIShapefileInstanceCtrl',
                                backdrop: 'static',
                                scope: scope,
                                keyboard: false
                            });
                            //ng-show which options to display
                            //depending on the type of shapefile uploaded
                            scope.selectionMethod = 1;
                            scope.bProgress = false;
                            scope.isPolygon = false;
                            scope.isPoint = false;
                            scope.isTable = false;
							scope.hasBandAttribute = false;
							scope.hasExposureAttributes = false;
                            scope.bandAttr.length = 0;
                            //remove any existing AOI layer
                            poly = null;
                            buffers = null;
                            if (scope.areamap.hasLayer(scope.shpfile)) {
                                scope.areamap.removeLayer(scope.shpfile);
                                scope.shpfile = new L.layerGroup();
                            }
                        });
                        scope.radioChange = function (selectionMethod) {
                            scope.selectionMethod = selectionMethod;
                            if (selectionMethod === 3) { // make selection by attribute value in file
                                scope.isTable = true;
                            } else {
                                scope.isTable = false;
                            }
                        };
						
						scope.changedValue = function(attr) {
							scope.selectedAttr=attr;
						}
						
						function getSelectionMethodAsString(attributeName) {
							if (scope.selectionMethod === 1) { // Single boundary; already set
								return "selection by single boundary";
							}
							else if (scope.selectionMethod === 2) { // make selection by band attribute in file
								return "selection by band attribute";
							}
							else if (scope.selectionMethod === 3) { // make selection by attribute value in file
								return "selection by attribute value: " + (attributeName||"(unknown attribute)");
							}
							else {
								return "unknown selection method " + scope.selectionMethod;
							}
						};
						
                        function readShpFile(file) {
                            try {
                                if (file.name.slice(-3) !== 'zip') {
                                    //not a zip file
                                    alertScope.showError("All parts of the shapefile expected in one zipped file");
                                    return;
                                } else {

                                    //type of study
                                    if (scope.possibleBands.length === 1) {
                                        scope.isRiskMapping = false;
                                    } else {
                                        scope.isRiskMapping = true;
                                    }

                                    var reader = new FileReader();
                                    var deferred = $q.defer();
                                    //http://jsfiddle.net/ashalota/ov0p4ajh/10/
                                    //http://leaflet.calvinmetcalf.com/#3/31.88/10.63
                                    reader.onload = function () {
                                        var bAttr = false;
                                        scope.attrs = [];
                                        poly = new L.Shapefile(this.result, {
                                            style: function (feature) {
                                                if (feature.geometry.type === "Point") {
                                                    scope.isPolygon = false;
                                                    scope.isPoint = true;
                                                    scope.isTable = true;
                                                } else if (feature.geometry.type === "Polygon") {
                                                    if (!bAttr) {
														var exposureAttributesCount = 0;
                                                        for (var property in feature.properties) {
                                                            scope.attrs.push(property);
															if (property == "band") {
																scope.hasBandAttribute = true;
															}
															else {
																exposureAttributesCount++;
															}
                                                        }
														if (exposureAttributesCount > 0) {
															scope.hasExposureAttributes = true;														}
														
							
                                                        bAttr = true;
                                                        scope.selectedAttr = scope.attrs[scope.attrs.length - 1];
															// Set default
                                                    }
                                                    scope.isPolygon = true;
                                                    scope.isPoint = false;
                                                    scope.isTable = false;
                                                    return {
                                                        fillColor: 'none',
                                                        weight: 2,
                                                        color: 'blue'
                                                    };
                                                }
                                            },
                                            onEachFeature: function (feature, layer) {
                                                //add markers with pop-ups if points
                                                if (feature.geometry.type === "Point") {
                                                    layer.setIcon(factory);
                                                    var popupContent = "";
                                                    for (var property in feature.properties) {
                                                        popupContent = popupContent + property.toUpperCase() +
                                                                ":\t" + feature.properties[property] + "</br>";
                                                    }
                                                    layer.bindPopup(popupContent);
                                                }
                                            }
                                        });
                                        deferred.resolve(poly);
                                    };
                                    reader.readAsArrayBuffer(file);
                                    return deferred.promise;
                                }
                            } catch (err) {
                                alertScope.showError("Could not open Shapefile: " + err.message);
                                scope.bProgress = false;
                            }
                        }

                        scope.screenShapeFile = function () {
                            scope.bProgress = true;
                            var files = document.getElementById('setUpFile').files;
                            if (files.length === 0) {
                                return;
                            }
                            var file = files[0];
                            //clear existing layers
                            if (scope.shpfile.hasLayer(buffers)) {
                                scope.shpfile.removeLayer(buffers);
                            }
                            if (scope.shpfile.hasLayer(poly)) {
                                scope.shpfile.removeLayer(poly);
                            }
                            poly = null;
                            buffers = null;
                            //async for progress bar
                            readShpFile(file).then(function () {
                                //switch off progress bar
                                scope.bProgress = false;
                                if (!scope.isPolygon & !scope.isPoint) {
                                    alertScope.showError("This is not a valid point or polygon zipped shapefile");
                                }
                            });
                        };
                        scope.displayShapeFile = function () {
                            //exit if there is no shapefile
                            if (!scope.isPolygon && !scope.isPoint) {
                                alertScope.showError("File is not a shapefile");
                                return false;
                            }

                            //check user input on bands
                            if (scope.selectionMethod === 3 || scope.isPoint) {
                                //trim any trailing zeros
                                //check numeric
                                var bZero = [];
                                for (var i = 0; i < scope.bandAttr.length; i++) {
                                    var thisBreak = Number(scope.bandAttr[i]);
                                    if (!isNaN(thisBreak)) {
                                        if (thisBreak !== 0) {
                                            bZero.push(1);
                                        } else {
                                            bZero.push(0);
                                        }
                                    } else {
                                        alertScope.showError("Non-numeric band value entered");
                                        return false; //and only display the points
                                    }
                                }
                                var total = 0;
                                for (var i in bZero) {
                                    total += bZero[i];
                                }
                                if (total !== scope.bandAttr.length) {
                                    var tmp = angular.copy(scope.bandAttr);
                                    //there are zero values, are they at the end?
                                    for (var i = scope.bandAttr.length - 1; i >= 0; i--) {
                                        if (scope.bandAttr[i] === '') {
                                            tmp.pop();
                                        }
                                        else {
                                            break;
                                        }
                                    }
                                    scope.bandAttr = angular.copy(tmp);
                                }
                                if (scope.isPoint) {
                                    //check ascending and sequential for radii
                                    for (var i = 0; i < scope.bandAttr.length - 1; i++) {
										var a = parseInt(scope.bandAttr[i]);
										var b = parseInt(scope.bandAttr[i+1]);
										
										if (a.toString() != scope.bandAttr[i] || 
										    b.toString() != scope.bandAttr[i+1]) {
											alertScope.consoleLog("[rifd-dsub-risk.js] scope.bandAttr[" + i + "]: " + 
												JSON.stringify(scope.bandAttr));
                                            alertScope.showError("Distance band values are not integers");
                                            return false;
										}
                                        else if (a > b) {
											alertScope.consoleLog("[rifd-dsub-risk.js] scope.bandAttr[" + i + "]: " + 
												JSON.stringify(scope.bandAttr));
                                            alertScope.showError("Distance band values are not in ascending order");
                                            return false;
                                        }
                                    }
                                } else {
                                    //check descending and sequential for exposures
                                    for (var i = 0; i < scope.bandAttr.length - 1; i++) {
										var a = parseInt(scope.bandAttr[i]);
										var b = parseInt(scope.bandAttr[i+1]);
                                        
										if (a.toString() != scope.bandAttr[i] || 
										    b.toString() != scope.bandAttr[i+1]) {
											alertScope.consoleLog("[rifd-dsub-risk.js] scope.bandAttr[" + i + "]: " + 
												JSON.stringify(scope.bandAttr));
                                            alertScope.showError("Distance band values are not integers");
                                            return false;
										}
                                        else if (a < b) {
											alertScope.consoleLog("[rifd-dsub-risk.js] scope.bandAttr[" + i + "]: " + 
												JSON.stringify(scope.bandAttr));
                                            alertScope.showError("Exposure band values are not in descending order");
                                            return false;
                                        }
                                    }
                                }
                            }

                            //make bands around points
                            if (scope.isPoint) { 
                                //make polygons and apply selection
                                buffers = new L.layerGroup();
								var i = 0;
								var points = 0;
                                for (; i < scope.bandAttr.length; i++) {
                                    for (var j in poly._layers) {
                                        //Shp Library inverts lat, lngs for some reason (Bug?) - switch back
                                        var polygon = L.circle(
											[poly._layers[j].feature.geometry.coordinates[1],
                                             poly._layers[j].feature.geometry.coordinates[0]],
                                                {
                                                    radius: scope.bandAttr[i],
                                                    fillColor: 'none',
                                                    weight: (selectorBands.weight || 3),
													opacity: (selectorBands.opacity || 0.8),
													fillOpacity: (selectorBands.fillOpacity || 0),
                                                    color: selectorBands.bandColours[i] // Band i+1
                                                });
                                        buffers.addLayer(polygon);
                                        $rootScope.$broadcast('makeDrawSelection', {
                                            data: polygon,
                                            circle: true,
                                            freehand: false,
                                            band: i + 1,
											area: Math.PI*Math.pow(scope.bandAttr[i], 2)
                                        });
										points += Object.keys(poly._layers).length;
                                    }
                                }
								
								alertScope.showSuccess("Added " + (i+1) + " band(s) from  " + 
									points + " points using " +
									getSelectionMethodAsString());
								try {	
									scope.shpfile.addLayer(buffers);
								} catch (err) {
									alertScope.showError("Could not open Shapefile, no valid features");
									return false;
								}	
								
                                $rootScope.$broadcast('completedDrawSelection', {});
                            } else if (scope.isPolygon) {
                                if (scope.selectionMethod === 2) { // make selection by band attribute in file
                                    for (var i in poly._layers) {
                                        //check these are valid bands
										if (poly._layers[i].feature.properties.band == undefined) {                                            //band number not recognized
                                            alertScope.showError("Invalid band descriptor: (no band field)");
                                            return false;
										}
                                        if (scope.possibleBands.indexOf(poly._layers[i].feature.properties.band) === -1) {
                                            //band number not recognized
                                            alertScope.showError("Invalid band descriptor: " + poly._layers[i].feature.properties.band);
                                            return false;
                                        }
                                    }
                                } else if (scope.selectionMethod === 3) {
                                    //check the attribute is numeric etc
                                    for (var i in poly._layers) {
                                        //check these are valid exposure values
                                        if (!angular.isNumber(poly._layers[i].feature.properties[scope.selectedAttr])) {
											var a=parseFloat(poly._layers[i].feature.properties[scope.selectedAttr]);
											if (!isNaN(a)) {
												poly._layers[i].feature.properties[scope.selectedAttr]=a;
											}
											else {
												//number not recognized 
												alertScope.showError("Non-numeric value in file: " + 
													poly._layers[i].feature.properties[scope.selectedAttr]);
												return false;
											}
                                        }
                                    }
                                }
                                //make the selection for each polygon
								
								var maxBand=0;
								var attributeName=undefined;
								var bandValues={};
								var shapeList = []
                                for (var i in poly._layers) {
                                    var polygon = L.polygon(poly._layers[i].feature.geometry.coordinates[0], {});
                                    var shape = {
                                        data: angular.copy(polygon),
										circle: false,
										freehand: false,
										shapefile: true,
										area: undefined,
										index: i,
										selectionMethod: scope.selectionMethod
                                    };
									shape.area = turf.area(polygon.toGeoJSON());
									shapeList.push(shape);
								}
								shapeList.sort(function(a, b){return a.area - b.area}); 
									// Sort into ascending order by area
                                for (var i =0; i< shapeList.length; i++) {
									var shape = shapeList[i];
                                    if (scope.selectionMethod === 1) { // Single boundary; already set
                                        shape.band = 1;
										maxBand=1;
										
										if (bandValues[i]) {
											bandValues[i].band = shape.band;
										}
										else {
											bandValues[i] = {
												band: shape.band,
												value: undefined
											};
										}
                                    } 
									else if (scope.selectionMethod === 2) { // make selection by band attribute in file
                                        shape.band = poly._layers[shapeList[i].index].feature.properties.band;
										attributeName="band";
										
										if (bandValues[i]) {
											bandValues[i].band = shape.band;
										}
										else {
											bandValues[i]={
												band: shape.band,
												value: undefined
											};
										}
                                    } 
									else if (scope.selectionMethod === 3) { // make selection by attribute value in file
                                        var attr = poly._layers[shapeList[i].index].feature.properties[scope.selectedAttr];
										attributeName=scope.selectedAttr;
                                        shape.band = -1;
                                        for (var k = 0; k < scope.bandAttr.length; k++) { // In descending order
                                            if (shape.band == -1 && attr >= scope.bandAttr[k]) {
                                                shape.band = k  + 1;
												alertScope.consoleDebug("[rifd-dsub-risk.js] selection by attribute value shape.band[" + i + "]: " + shape.band +
													"; attr value: " + attr +
													"; k: " + k + 
													">= scope.bandAttr[k] " + scope.bandAttr[k]);
                                            } 
											else {
												alertScope.consoleDebug("[rifd-dsub-risk.js] No selection by attribute value shape.band[" + i + "]: " + shape.band +
													"; attr value: " + attr + 
													"; k: " + k + 
													"; NOT >= scope.bandAttr[k] " + scope.bandAttr[k]);
											}
											
											if (bandValues[i]) {
												bandValues[i].band = shape.band;
												bandValues[i].value = attr;
											}
											else {
												bandValues[i]={
													band: shape.band,
													value: attr
												};
											}
											
                                        }
										
                                    }
									
									if (shape.band > maxBand) {
										maxBand = shape.band;
									}
                                    //make the selection
                                    scope.makeDrawSelection(shape);
                                } // End of shapefile polygon processing for loop
								
								var bandsUsed={};
								var noBandsUsed=0;
								for (var k in bandValues) {
									if (bandValues[k].band > 0) {
										if (bandsUsed[bandValues[k].band]) {
											bandsUsed[bandValues[k].band]++;
										}
										else {
											bandsUsed[bandValues[k].band]=1;
										}
									}
								}								
								alertScope.consoleDebug("[rifd-dsub-risk.js] " + getSelectionMethodAsString(attributeName) +	
									"; maxBand: " + maxBand +
									"; scope.attrs: " + (scope.attrs||"(no attributes in shapefile)") +
									"; scope.bandAttr (user supplied band values): " + JSON.stringify(scope.bandAttr) +
									"; bandsUsed: " + JSON.stringify(bandsUsed, null , 1) +
									"; bandValues: " + JSON.stringify(bandValues, null , 1));
								if (maxBand > 0) {
									alertScope.showSuccess("Added " + 
										Object.keys(bandsUsed).length + "/" + maxBand + " band(s) from " + 
										Object.keys(poly._layers).length + " polygons using " +
										getSelectionMethodAsString(attributeName));
								}
								else {
									alertScope.showError("Added no bands from " + 
										Object.keys(poly._layers).length + " polygons using " +
										getSelectionMethodAsString(attributeName));
									return false;			
								}
														
								try {
									scope.shpfile.addLayer(poly); // Add poly to layerGroup
								} catch (err) {
									alertScope.showError("Could not open Shapefile, no valid features");
									return false;
								} 
								
                                $rootScope.$broadcast('completedDrawSelection', {});
                            } // End of isPolygon()

                            //add AOI layer to map on modal close                            
                            try {
                                scope.shpfile.addTo(scope.areamap);
                            } catch (err) {
                                alertScope.showError("Could not open Shapefile, no valid features");
                                return false;
                            }
                            return true;
                        };
                    }
                };
            }]);