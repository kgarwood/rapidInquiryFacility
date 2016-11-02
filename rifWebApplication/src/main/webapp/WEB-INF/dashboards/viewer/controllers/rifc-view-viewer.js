/* global L, key, topojson, d3, ss, values */


//TODO:
// how do we know that the geography is SAHSU? from the study ID?

angular.module("RIF")

        .controller('ViewerCtrl2', ['$scope',
            function ($scope) {

                $scope.settings = function () {
                    console.log("settings button clicked");
                };

            }])
        .controller('ViewerCtrl', ['$scope', 'user', 'leafletData', 'LeafletBaseMapService', '$timeout', 'ViewerStateService', 'ChoroService',
            function ($scope, user, leafletData, LeafletBaseMapService, $timeout, ViewerStateService, ChoroService) {

                //data for top-left histogram panel
                $scope.histoData = [];
                function getHistoData() {
                    $scope.histoData.length = 0;
                    for (var i = 0; i < $scope.viewerTableOptions.data.length; i++) {
                        $scope.histoData.push($scope.viewerTableOptions.data[i][ChoroService.getMaps(1).feature]);
                    }
                }

                //ui-container sizes
                $scope.distHistoCurrentHeight = 200;
                $scope.distHistoCurrentWidth = 200;
                $scope.pyramidCurrentHeight = 200;
                $scope.pyramidCurrentWidth = 200;
                $scope.vSplit1 = ViewerStateService.getState().vSplit1;
                $scope.hSplit1 = ViewerStateService.getState().hSplit1;
                $scope.hSplit2 = ViewerStateService.getState().hSplit2;

                //TODO: if browser window resized - see mapping controller

                $scope.$on('ui.layout.loaded', function () {
                    $scope.distHistoCurrentHeight = d3.select("#hSplit1").node().getBoundingClientRect().height;
                    $scope.distHistoCurrentWidth = d3.select("#hSplit1").node().getBoundingClientRect().width;
                    $scope.pyramidCurrentHeight = d3.select("#hSplit2").node().getBoundingClientRect().height;
                    $scope.pyramidCurrentWidth = d3.select("#hSplit1").node().getBoundingClientRect().width;
                });

                $scope.$on('ui.layout.resize', function (e, beforeContainer, afterContainer) {
                    //Monitor split sizes                  
                    if (beforeContainer.id === "vSplit1") {
                        ViewerStateService.getState().vSplit1 = (beforeContainer.size / beforeContainer.maxSize) * 100;
                        $scope.distHistoCurrentWidth = beforeContainer.size;
                        $scope.pyramidCurrentWidth = beforeContainer.size;
                    }
                    if (beforeContainer.id === "hSplit1") {
                        ViewerStateService.getState().hSplit1 = (beforeContainer.size / beforeContainer.maxSize) * 100;
                        $scope.distHistoCurrentHeight = beforeContainer.size;
                        $scope.pyramidCurrentHeight = afterContainer.size;
                    }
                    if (beforeContainer.id === "hSplit2") {
                        ViewerStateService.getState().hSplit2 = (beforeContainer.size / beforeContainer.maxSize) * 100;
                    }

                    if (beforeContainer.id === "hSplit3") {
                        //       $scope.pyramidCurrentHeight = beforeContainer.size;
                    }

                    //Rescale leaflet container        
                    leafletData.getMap("viewermap").then(function (map) {
                        setTimeout(function () {
                            map.invalidateSize();
                        }, 50);
                    });
                });

                //Drop-downs
                $scope.studyIDs = [1];
                $scope.studyID = $scope.studyIDs[0];
                $scope.years = [1990, 1991, 1992, 1993];
                $scope.year = $scope.years[0];
                $scope.sexes = ["Male", "Female", "Both"];
                $scope.sex = $scope.sexes[0];

                //Once drop-downs filled then render initial state

                /*
                 $scope.attributes = ["lower95", "upper95"];
                 $scope.attribute;
                 user.getSmoothedResultAttributes(user.currentUser, $scope.studyID).then(function (res) {
                 $scope.attributes = res.data;
                 $scope.attribute = $scope.attributes[0];
                 }, handleAttributeError);
                 function handleAttributeError(e) {
                 console.log("attribute error");
                 }
                 */

                //TODO: Will be called from options dropdowns
                //draw relevant geography for this study
                //  $scope.renderGeography = function () {
                user.getTiles(user.currentUser, "SAHSU", "LEVEL4").then(handleTopoJSON, handleTopoJSON);
                //   };
                //  $scope.renderGeography();

                //leaflet render
                $scope.transparency = 0.7;
                $scope.selectedPolygon = [];
                var maxbounds;
                var thisMap = [];
                $scope.domain = [];
                var attr;
                $scope.populationData = [];

                //get the user defined basemap
                $scope.parent = {};
                $scope.parent.thisLayer = LeafletBaseMapService.setBaseMap(LeafletBaseMapService.getCurrentBaseMapInUse("viewermap"));
                //called on bootstrap and on modal submit
                $scope.parent.renderMap = function (mapID) {
                    leafletData.getMap(mapID).then(function (map) {
                        map.removeLayer($scope.parent.thisLayer);
                        if (!LeafletBaseMapService.getNoBaseMap("viewermap")) {
                            $scope.parent.thisLayer = LeafletBaseMapService.setBaseMap(LeafletBaseMapService.getCurrentBaseMapInUse("viewermap"));
                            map.addLayer($scope.parent.thisLayer);
                        }
                        //restore setView
                        if (maxbounds && ViewerStateService.getState().zoomLevel === -1) {
                            map.fitBounds(maxbounds);
                        } else {
                            map.setView(ViewerStateService.getState().view, ViewerStateService.getState().zoomLevel);
                        }
                        //hack to refresh map
                        setTimeout(function () {
                            map.invalidateSize();
                        }, 50);
                    });
                };
                $scope.parent.renderMap("viewermap");

                $timeout(function () {
                    leafletData.getMap("viewermap").then(function (map) {
                        map.on('zoomend', function (e) {
                            ViewerStateService.getState().zoomLevel = map.getZoom();
                        });
                        map.on('moveend', function (e) {
                            ViewerStateService.getState().view = map.getCenter();
                        });
                        new L.Control.GeoSearch({
                            provider: new L.GeoSearch.Provider.OpenStreetMap()
                        }).addTo(map);
                    });
                });

                //Clear all selection from map and table
                $scope.clear = function () {
                    $scope.selectedPolygon.length = 0;
                };

                //Zoom to layer
                $scope.zoomToExtent = function () {
                    leafletData.getMap("viewermap").then(function (map) {
                        map.fitBounds(maxbounds);
                    });
                };

                //UI-Grid setup options
                $scope.viewerTableOptions = {
                    enableFiltering: true,
                    enableRowSelection: true,
                    enableColumnResizing: true,
                    enableRowHeaderSelection: false,
                    enableHorizontalScrollbar: 1,
                    rowHeight: 25,
                    multiSelect: true,
                    rowTemplate: rowTemplate(),
                    onRegisterApi: function (gridApi) {
                        $scope.gridApi = gridApi;
                    }
                };
                function rowTemplate() {
                    return  '<div id="testdiv" tabindex="0" ng-keydown="grid.appScope.keyDown($event)" ng-keyup="grid.appScope.keyUp($event);">' +
                            '<div style="height: 100%" ng-class="{ ' +
                            'viewerSelected: row.entity._selected === 1' +
                            '}">' +
                            '<div ng-click="grid.appScope.rowClick(row)">' +
                            '<div ng-repeat="(colRenderIndex, col) in colContainer.renderedColumns track by col.colDef.name" class="ui-grid-cell" ui-grid-cell></div>' +
                            '</div>';
                }

                //Render map functions
                function style(feature) {
                    return {
                        fillColor: ChoroService.getRenderFeature(thisMap.scale, attr, false),
                        weight: 1,
                        opacity: 1,
                        color: 'gray',
                        dashArray: '3',
                        fillOpacity: $scope.transparency
                    };
                }

                function handleLayer(layer) {
                    //Join geography and results table
                    var thisAttr;
                    for (var i = 0; i < $scope.viewerTableOptions.data.length; i++) {
                        if ($scope.viewerTableOptions.data[i].area_id === layer.feature.properties.area_id) {
                            thisAttr = $scope.viewerTableOptions.data[i][ChoroService.getMaps(1).feature];
                            break;
                        }
                    }
                    //is selected?
                    var selected = false;
                    if ($scope.selectedPolygon.indexOf(layer.feature.properties.area_id) !== -1) {
                        selected = true;
                    }
                    var polyStyle = ChoroService.getRenderFeature(thisMap.scale, thisAttr, selected);
                    layer.setStyle({
                        fillColor: polyStyle,
                        fillOpacity: $scope.transparency
                    });
                }

                $scope.changeOpacity = function () {
                    $scope.topoLayer.eachLayer(handleLayer);
                };

                //Hover box and Legend
                var infoBox = L.control({position: 'bottomleft'});
                var legend = L.control({position: 'topright'});
                infoBox.onAdd = function () {
                    this._div = L.DomUtil.create('div', 'info');
                    this.update();
                    return this._div;
                };
                infoBox.update = function (poly) {
                    if (poly) {
                        var thisAttr;
                        for (var i = 0; i < $scope.viewerTableOptions.data.length; i++) {
                            if ($scope.viewerTableOptions.data[i].area_id === poly) {
                                thisAttr = $scope.viewerTableOptions.data[i][ChoroService.getMaps(1).feature];
                                break;
                            }
                        }
                        if (ChoroService.getMaps(1).feature !== "") {
                            this._div.innerHTML = '<h4>ID: ' + poly + '</br>' + ChoroService.getMaps(1).feature.toUpperCase() + ": " + Number(thisAttr).toFixed(3) + '</h4>';
                        } else {
                            this._div.innerHTML = '<h4>ID: ' + poly + '</h4>';
                        }
                    }
                };

                //information from choropleth modal to colour map                             
                $scope.parent.refresh = function () {
                    //get selected colour ramp
                    var rangeIn = ChoroService.getMaps(1).brewer;
                    $scope.distHistoName = ChoroService.getMaps(1).feature;
                    attr = ChoroService.getMaps(1).feature;

                    //not a choropleth, but single colour
                    if (rangeIn.length === 1) {
                        attr = "";
                        leafletData.getMap("viewermap").then(function (map) {
                            //remove existing legend
                            if (legend._map) {
                                map.removeControl(legend);
                            }
                        });
                        $scope.topoLayer.eachLayer(handleLayer);
                        return;
                    }

                    thisMap = ChoroService.getMaps(1).renderer;

                    //remove old legend and add new
                    legend.onAdd = ChoroService.getMakeLegend(thisMap, attr);
                    leafletData.getMap("viewermap").then(function (map) {
                        if (legend._map) { //This may break in future leaflet versions
                            map.removeControl(legend);
                        }
                        legend.addTo(map);
                    });

                    //force a redraw
                    $scope.topoLayer.eachLayer(handleLayer);

                    //Histogram
                    getHistoData();
                };

                function handleTopoJSON(res) {
                    leafletData.getMap("viewermap").then(function (map) {
                        $scope.topoLayer = new L.TopoJSON(res.data, {
                            style: style,
                            onEachFeature: function (feature, layer) {
                                layer.on('mouseover', function (e) {
                                    this.setStyle({
                                        color: 'gray',
                                        weight: 1.5,
                                        fillOpacity: function () {
                                            return($scope.transparency - 0.3 > 0 ? $scope.transparency - 0.3 : 0.1);
                                        }()
                                    });
                                    infoBox.addTo(map);
                                    infoBox.update(layer.feature.properties.area_id);
                                });
                                layer.on('click', function (e) {
                                    var thisPoly = e.target.feature.properties.area_id;
                                    var bFound = false;
                                    for (var i = 0; i < $scope.selectedPolygon.length; i++) {
                                        if ($scope.selectedPolygon[i] === thisPoly) {
                                            bFound = true;
                                            $scope.selectedPolygon.splice(i, 1);
                                            break;
                                        }
                                    }
                                    if (!bFound) {
                                        $scope.selectedPolygon.push(thisPoly);
                                    }
                                });
                                layer.on('mouseout', function (e) {
                                    $scope.topoLayer.eachLayer(handleLayer);
                                    map.removeControl(infoBox);
                                });
                            }
                        });
                        $scope.topoLayer.addTo(map);
                        maxbounds = $scope.topoLayer.getBounds();
                    }).then(function () {
                        $scope.getAttributeTable();
                    }).then(function () {
                        $scope.parent.renderMap("viewermap");
                        $scope.parent.refresh();
                    });
                }

                $scope.getAttributeTable = function () {
                    //All results in table
                    user.getSmoothedResults(user.currentUser, 1, 1, 1990)
                            .then(handleSmoothedResults, attributeError);

                    //Population pyramid data
                    user.getAllPopulationPyramidData(user.currentUser, 1, 1990)
                            .then(handlePopulation, attributeError);

                    function handleSmoothedResults(res) {
                        //fill results table
                        var colDef = [];
                        var attrs = [];
                        $scope.tableData = [];
                        for (var i = 0; i < res.data.smoothed_results.length; i++) {
                            res.data.smoothed_results[i]._selected = 0;
                            $scope.tableData.push(res.data.smoothed_results[i]);
                        }
                        for (var i in res.data.smoothed_results[0]) {
                            var testCast = Number(res.data.smoothed_results[0][i]);
                            if (angular.isNumber(testCast) & isFinite(testCast)) {
                                if (i !== "_selected") {
                                    attrs.push(i); //Numeric attributes possible to map 
                                }
                            }
                            colDef.push({
                                name: i,
                                width: 100
                            });
                        }

                        if (ChoroService.getMaps(1).feature === "") {
                            ChoroService.getMaps(1).feature = attrs[0];
                            $scope.distHistoName = attrs[0];
                        }
                        else {
                            $scope.distHistoName = ChoroService.getMaps(1).feature;
                        }

                        ChoroService.setFeaturesToMap(attrs);
                        $scope.viewerTableOptions.columnDefs = colDef;
                        $scope.viewerTableOptions.data = $scope.tableData;

                        getHistoData();
                    }

                    function handlePopulation(res) {
                        $scope.populationData = res.data.smoothed_results;
                    }

                    function attributeError(e) {
                        console.log(e);
                    }
                };

                //Multiple select with shift
                //detect shift key (16) down
                var bShift = false;
                var multiStart = -1;
                var multiStop = -1;
                $scope.keyDown = function ($event) {
                    if (!bShift && $event.keyCode === 16) {
                        bShift = true;
                    }
                };
                //detect shift key (16) up
                $scope.keyUp = function ($event) {
                    if (bShift && $event.keyCode === 16) {
                        bShift = false;
                        multiStop = -1;
                    }
                };
                $scope.rowClick = function (row) {
                    var myVisibleRows = $scope.gridApi.core.getVisibleRows();
                    if (!bShift) {
                        //We are doing a single click select on the table
                        var thisPoly = row.entity.area_id;
                        var bFound = false;
                        for (var i = 0; i < $scope.selectedPolygon.length; i++) {
                            if ($scope.selectedPolygon[i] === thisPoly) {
                                bFound = true;
                                $scope.selectedPolygon.splice(i, 1);
                                break;
                            }
                        }
                        if (!bFound) {
                            $scope.selectedPolygon.push(thisPoly);
                        }
                    } else {
                        //We are doing a multiple select on the table, shift key is down
                        multiStop = matchRowNumber(myVisibleRows, row.entity.area_id);
                        for (var i = Math.min(multiStop, multiStart);
                                i <= Math.min(multiStop, multiStart) + (Math.abs(multiStop - multiStart)); i++) {
                            var thisPoly = myVisibleRows[i].entity.area_id;
                            var bFound = false;
                            for (var j = 0; j < $scope.selectedPolygon.length; j++) {
                                if ($scope.selectedPolygon[j] === thisPoly) {
                                    bFound = true;
                                    break;
                                }
                            }
                            if (!bFound) {
                                $scope.selectedPolygon.push(thisPoly);
                            }
                        }
                    }
                    multiStart = matchRowNumber(myVisibleRows, row.entity.area_id);
                };
                function matchRowNumber(visible, id) {
                    for (var i = 0; i < visible.length; i++) {
                        if (visible[i].entity.area_id === id) {
                            return(i);
                        }
                    }
                }

                //Watch selectedPolygon array for any changes
                $scope.$watchCollection('selectedPolygon', function (newNames, oldNames) {
                    if (newNames === oldNames) {
                        return;
                    }
                    //Update table selection
                    $scope.gridApi.selection.clearSelectedRows();
                    for (var i = 0; i < $scope.viewerTableOptions.data.length; i++) {
                        $scope.viewerTableOptions.data[i]._selected = 0;
                        for (var j = 0; j < $scope.selectedPolygon.length; j++) {
                            if ($scope.viewerTableOptions.data[i].area_id === $scope.selectedPolygon[j]) {
                                $scope.viewerTableOptions.data[i]._selected = 1;
                            }
                        }
                    }
                    //Update map selection
                    $scope.topoLayer.eachLayer(handleLayer);
                });
            }]);