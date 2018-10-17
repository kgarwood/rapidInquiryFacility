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
 * SERVICE for stored map state: see also rifs-dmap-mappingstate.js which will be merged in time
 */

angular.module("RIF")
        .factory('CommonMappingStateService', ['AlertService', 'SelectStateService',
                function (AlertService, SelectStateService) {
                    
					var mapNames = {
						"areamap": "Study and comparison area map",
						"diseasemap1": "Disease mapping left hand map",
						"diseasemap2": "Disease mapping right hand map",
						"viewer": "Viewer map"
					};
					
					function checkAreaType(areaType) { // Check: $scope.input.name
						if (areaType == "ComparisionAreaMap" || areaType == "StudyAreaMap") { // OK
						}
						else {
							throw new Error("checkAreaType() invalid areaType: " + areaType);
						}
					}
					
					var s = {};
					var t = { // All possible mapping elements
						map: undefined,
						shapes: undefined,
						drawnItems: undefined,
						info: undefined,
						studyArea: {
							selectedPolygon: [],				
							selectedPolygonObj: {},
							areaNameList: {}
						},
						comparisonArea: {
							selectedPolygon: [],				
							selectedPolygonObj: {},
							areaNameList: {}
						},
						maxbounds: undefined,						
						currentBand: undefined,
						possibleBands: undefined,
						description: undefined,
						getSelectedPolygon: function(areaType) { // Get selectedPolygon list
							checkAreaType(areaType);
							return this.areaType.selectedPolygon;
						},
						getSelectedPolygonObj: function(areaType, thisPolyId) { // Get selectedPolygon list
							checkAreaType(areaType);
							return this.areaType.selectedPolygonObj[thisPolyId];
						},
						getAreaNameList: function(areaType) { // Get areaNameList list 
							checkAreaType(areaType);
							
							var areaNameList=this.areaType.areaNameList;
//							AlertService.consoleLog("[rifs-util-mapstate.js] getAreaNameList() for: " + areaType + 
//								"; areaNameList: " + (areaNameList ? Object.keys(areaNameList).length : "0"));
							return this.areaType.areaNameList;
						},
						setAreaNameList: function(areaType) { // Set areaNameList list
							checkAreaType(areaType);
							var oldAreaNameList = this.areaType.areaNameList;
							if (areaType == "StudyAreaMap" &&
								SelectStateService.getState().studySelection && 
								SelectStateService.getState().studySelection.studySelectedAreas) {
								var newStudyAreaNameList = {};
								var studySelectedAreas=angular.copy(
									SelectStateService.getState().studySelection.studySelectedAreas);
								
								for (var i = 0; i < studySelectedAreas.length; i++) {              									
									// Update areaNameList for debug
									if (studySelectedAreas[i].band && studySelectedAreas[i].band != -1) {
										if (newStudyAreaNameList[studySelectedAreas[i].band]) {
											newStudyAreaNameList[studySelectedAreas[i].band].push(studySelectedAreas[i].label);
										}
										else {
											newStudyAreaNameList[studySelectedAreas[i].band] = [];
											newStudyAreaNameList[studySelectedAreas[i].band].push(studySelectedAreas[i].label);
										}
									}
								}
								if (Object.keys(newStudyAreaNameList).length > 0) {
									AlertService.consoleError("[rifs-util-mapstate.js] setAreaNameList() for: " + areaType + 
										"; studySelectedAreas: " + (studySelectedAreas ? studySelectedAreas.length : "0") +
										"; old areaNameList: " + (oldAreaNameList ? Object.keys(oldAreaNameList).length : "0") +
										"; newStudyAreaNameList: " + (newStudyAreaNameList ? Object.keys(newStudyAreaNameList).length : "0"),
										new Error("Dummy"));
									this.areaType.areaNameList = {};
									this.areaType.areaNameList = angular.copy(newStudyAreaNameList);
								}
							}
							else if (areaType == "ComparisionAreaMap" &&
								SelectStateService.getState().studySelection && 
								SelectStateService.getState().studySelection.comparisonSelectedAreas) {
								var newComparisonAreaNameList = {};
								var comparisonSelectedAreas=angular.copy(
									SelectStateService.getState().studySelection.comparisonSelectedAreas);
								
								for (var i = 0; i < comparisonSelectedAreas.length; i++) {              									
									// Update areaNameList for debug
									if (comparisonSelectedAreas[i].band && comparisonSelectedAreas[i].band != -1) {
										if (newComparisonAreaNameList[comparisonSelectedAreas[i].band]) {
											newComparisonAreaNameList[comparisonSelectedAreas[i].band].push(
												comparisonSelectedAreas[i].label);
										}
										else {
											newComparisonAreaNameList[comparisonSelectedAreas[i].band] = [];
											newComparisonAreaNameList[comparisonSelectedAreas[i].band].push(
												comparisonSelectedAreas[i].label);
										}
									}
								}
								
								if (Object.keys(newComparisonAreaNameList).length > 0) {
									AlertService.consoleError("[rifs-util-mapstate.js] setAreaNameList() for: " + areaType + 
										"; comparisonSelectedAreas: " + (comparisonSelectedAreas ? comparisonSelectedAreas.length : "0") +
										"; old areaNameList: " + (oldAreaNameList ? Object.keys(oldAreaNameList).length : "0") +
										"; newComparisonAreaNameList: " + 
											(newComparisonAreaNameList ? Object.keys(newComparisonAreaNameList).length : "0"));
									this.areaType.areaNameList = {};
									this.areaType.areaNameList = angular.copy(newComparisonAreaNameList);
								}
							}
							
							return this.areaType.areaNameList;
						},
						getAllSelectedPolygonObj: function(areaType) { // Get selectedPolygon list
							checkAreaType(areaType);
							return this.areaType.selectedPolygonObj;
						},
						clearSelectedPolygon: function(areaType) { // Clear selectedPolygon list
							checkAreaType(areaType);
							this.areaType.selectedPolygon.length = 0;
							this.areaType.selectedPolygonObj = {};
							AlertService.consoleDebug("[rifs-util-mapstate.js] clearSselectedPolygon(" + areaType + "): " + 
								this.areaType.selectedPolygon.length);
							return this.areaType.selectedPolygon;
						},
						initialiseSelectedPolygon: function(areaType, arr) {	// Initialise selectedPolygon from an array arr of items
							checkAreaType(areaType);	
							var oldLength=((this.areaType && this.areaType.selectedPolygon) ? this.areaType.selectedPolygon.length : undefined);
							
							if (this.areaType == undefined) {
								this.areaType = {
									selectedPolygon: [],
									selectedPolygonObj: {},
									areaNameList: {}
								};		
							}
							
							var areaNameList = {};
							var oldAreaNameList = this.areaType.areaNameList;
							if (arr && arr.length > 0) {
								this.areaType.selectedPolygon.length = 0;					
								for (var i = 0; i < arr.length; i++) { // Maintain keyed list for faster checking
									this.areaType.selectedPolygonObj[arr[i].id] = angular.copy(arr[i]);								
									// Update areaNameList for debug
									if (arr[i].band && arr[i].band != -1) {
										if (areaNameList[arr[i].band]) {
											areaNameList[arr[i].band].push(
												arr[i].label);
										}
										else {
											areaNameList[arr[i].band] = [];
											areaNameList[arr[i].band].push(
												arr[i].label);
										}
									}
								}
								this.areaType.selectedPolygon = angular.copy(arr);
							}
										
							if (Object.keys(areaNameList).length > 0) {
								AlertService.consoleError("[rifs-util-mapstate.js] setAreaNameList() for: " + areaType + 
									"; arr: " + (arr ? arr.length : "0") +
									"; old areaNameList: " + (oldAreaNameList ? Object.keys(oldAreaNameList).length : "0") +
									"; areaNameList: " + 
										(areaNameList ? Object.keys(areaNameList).length : "0"));
								this.areaType.areaNameList = {};
								this.areaType.areaNameList = angular.copy(areaNameList);
							}
								
							AlertService.consoleDebug("[rifs-util-mapstate.js] initialiseSelectedPolygon(" + areaType+ ") from: " + 
								oldLength + "; to: " + this.areaType.selectedPolygon.length);
							return this.areaType.selectedPolygon;
						},
						sortSelectedPolygon: function(areaType) { // Sort selectedPolygon list alphabetically by id 
							checkAreaType(areaType);	
							this.areaType.selectedPolygon.sort(function(a, b) {
								if (a.id < b.id) {
									return -1;
								}
								else if (a.id > b.id) {
									return 1;
								}
								else { // Same
									return 0;
								}
							}); // Alphabetically by id!
							return this.areaType.selectedPolygon;
						},
						addToSelectedPolygon: function(areaType, item) {	// Add item to selectedPolygon
							checkAreaType(areaType);	
							if (item && item.id) {
								if (this.areaType.selectedPolygonObj[item.id]) {
									throw new Error("Duplicate items: " + item.id + " in selectedPolygon " + areaType + " list");
								}
								this.areaType.selectedPolygonObj[item.id] = angular.copy(item);
								this.areaType.selectedPolygon.push(angular.copy(item));
//								AlertService.consoleDebug("[rifs-util-mapstate.js] addToSelectedPolygon(" + areaType + ", " + 
//									JSON.stringify(item) + "): " + 
//									this.areaType.selectedPolygon.length);
							}
							else {
								throw new Error("Null item/id: " + JSON.stringify(item) + "; areaType: " + areaType);
							}
							return this.areaType.selectedPolygon;
						},
						removeFromSselectedPolygon: function(areaType, id) { // Remove item from selectedPolygon
							checkAreaType(areaType);	
							if (id) {
								if (this.areaType.selectedPolygonObj[id]) {
									var found=false;
									for (var i = 0; i < this.areaType.selectedPolygon.length; i++) { 
										if (this.areaType.selectedPolygon[i].id == id) {
											found=true;
											AlertService.consoleDebug("[rifs-util-mapstate.js] removeFromSselectedPolygon(" + areaType + ", " +
												JSON.stringify(this.areaType.selectedPolygonObj[id]) + "): " + 
												(this.areaType.selectedPolygon.length-1));
											this.areaType.selectedPolygon.splice(i, 1);
											delete this.areaType.selectedPolygonObj[id];
											break;
										}
									}	
									if (!found) {
										throw new Error("Cannot find item: " + id + " in selectedPolygon " + areaType + " list");
									}	
								}
								else {
									throw new Error("Cannot find item: " + id + " in selectedPolygon " + areaType + " object");
								}
							}
							else {
								throw new Error("removeFromSselectedPolygon() Null id: " + areaType);
							}
							return this.areaType.selectedPolygon;
						}
					};
									
                    return {
                        getState: function (mapName) {
							var found=false;
							for (var key in mapNames) {
								if (key == mapName) {
									found=true;
								}
								
								if (s[key] == undefined) { // Initialise
									s[key] = t;
									s[key].description=mapNames[key];
								}								
							}
							if (!found) {
								throw new Error("[rifs-util-mapstate.js] invalid map name: " + mapName);
							}
							return s[mapName];
						},
						resetState: function (mapName) {
							if (mapName) {
								var found=false;
								for (var key in mapNames) {
									if (key == mapName) {
										found=true;
										s[key] = t; // Re-Initialise
										s[key].description=mapNames[key];
										AlertService.consoleDebug("[rifs-util-mapstate.js] resetState(" + key + "): " + 
											s[key].description);
									}							
								}
								if (!found) {
									throw new Error("[rifs-util-mapstate.js] invalid map name: " + mapName);
								}
								return s[mapName];		
							}
							else {
								for (var key in mapNames) {
									s[key] = t; // Re-Initialise
									s[key].description=mapNames[key];
									AlertService.consoleDebug("[rifs-util-mapstate.js] resetState(" + key + "): " + 
										s[key].description);
								}	
								return undefined;								
							}
						}
                    };
                }]);
