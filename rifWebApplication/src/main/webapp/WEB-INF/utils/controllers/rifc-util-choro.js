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
 * CONTROLLER for choropleth map symbology modal used by viewer and mapper
 */
/* global d3 */

angular.module("RIF")
        .controller('ChoroplethModalCtrl', ['$scope', '$uibModal', 'ChoroService', 'ColorBrewerService',
            function ($scope, $uibModal, ChoroService, ColorBrewerService) {

                $scope.open = function (map) {
                    //Brewer swatches obtained from https://github.com/timothyrenner/ColorBrewer.jl
                    $scope.options = [];
                    var colorBrewerList = ColorBrewerService.getSchemeList();
                    for (var j in colorBrewerList) {
                        $scope.options.push({name: colorBrewerList[j], image: 'images/colorBrewer/' + colorBrewerList[j] + '.png'});
                    }

                    $scope.map = map;
                    var modalInstance = $uibModal.open({
                        animation: true,
                        templateUrl: 'utils/partials/rifp-util-choro.html',
                        controller: 'ChoroplethModalInstanceCtrl',
                        windowClass: 'mapping-Modal',
                        scope: $scope
                    });
                    modalInstance.opened.then(function () {
                        $scope.$$childHead.renderSwatch(true, true);                        
                    });
                    modalInstance.result.then(function (modal) {
                        ChoroService.getMaps(map).brewerName = modal.currOption.name;
                        ChoroService.getMaps(map).invert = modal.checkboxInvert;
                        ChoroService.getMaps(map).brewer = ColorBrewerService.getColorbrewer(modal.currOption.name, modal.selectedN);
                        ChoroService.getMaps(map).intervals = modal.selectedN;
                        ChoroService.getMaps(map).feature = modal.selectedFeature;
                        ChoroService.getMaps(map).method = modal.method;
                        ChoroService.getMaps(map).renderer = modal.thisMap;
                        $scope.refresh(map);
                    });
                };
            }])
        .controller('ChoroplethModalInstanceCtrl', function ($scope, $uibModalInstance, ColorBrewerService, ChoroService) {
            $scope.input = {};
            $scope.input.checkboxInvert = ChoroService.getMaps($scope.map).invert;
            $scope.input.selectedSchemeName = ChoroService.getMaps($scope.map).brewerName;
            $scope.input.intervalRange = ColorBrewerService.getSchemeIntervals($scope.input.selectedSchemeName);
            $scope.input.selectedN = ChoroService.getMaps($scope.map).intervals;
            $scope.input.method = ChoroService.getMaps($scope.map).method;

            //set saved swatch selection
            var cb = ChoroService.getMaps($scope.map).brewerName;
            for (var i = 0; i < $scope.options.length; i++) {
                if ($scope.options[i].name === cb) {
                    $scope.input.currOption = $scope.options[i];
                }
            }

            //list of attributes
            $scope.input.features = ChoroService.getMaps($scope.map).features;
            if ($scope.input.features.indexOf(ChoroService.getMaps($scope.map).feature) === -1) {
                $scope.input.selectedFeature = $scope.input.features[0];
            } else {
                $scope.input.selectedFeature = ChoroService.getMaps($scope.map).feature;
            }
            
            //Map renderer on opening
            var onXRenderRestore = angular.copy(ChoroService.getMaps($scope.map));

            $scope.domain = [];

            $scope.renderSwatch = function (bOnOpen, bCalc) {
                //ensure that the colour scheme allows the selected number of classes
                var n = angular.copy($scope.input.selectedN);
                $scope.input.intervalRange = ColorBrewerService.getSchemeIntervals($scope.input.currOption.name);
                if ($scope.input.selectedN > Math.max.apply(Math, $scope.input.intervalRange)) {
                    $scope.input.selectedN = Math.max.apply(Math, $scope.input.intervalRange);
                } else if ($scope.input.selectedN < Math.min.apply(Math, $scope.input.intervalRange)) {
                    $scope.input.selectedN = Math.min.apply(Math, $scope.input.intervalRange);
                }

                //get the domain 
                $scope.domain.length = 0;
                for (var i = 0; i < $scope.tableData[$scope.map].length; i++) {
                    $scope.domain.push(Number($scope.tableData[$scope.map][i][$scope.input.selectedFeature]));
                }
                
                //save the selected brewer
                ChoroService.getMaps($scope.map).brewerName = $scope.input.currOption.name;

                if (bOnOpen) {
                    //if called on modal open
                    if (!ChoroService.getMaps($scope.map).init) {
                        //initialise basic renderer
                        ChoroService.getMaps($scope.map).init = true;
                        $scope.input.thisMap = ChoroService.getChoroScale($scope.input.method, $scope.domain, ColorBrewerService.getColorbrewer($scope.input.currOption.name,
                                $scope.input.selectedN), $scope.input.checkboxInvert, $scope.map);
                        ChoroService.getMaps($scope.map).renderer = $scope.input.thisMap;
                    } else {
                        //restore previous renderer
                        $scope.input.thisMap = ChoroService.getMaps($scope.map).renderer;
                    }
                } else {
                    //update current renderer
                    if (!bCalc) {
                        if (n !== $scope.input.selectedN) {
                            //reset as class number requested not possible
                            $scope.input.thisMap = ChoroService.getChoroScale($scope.input.method, $scope.domain, ColorBrewerService.getColorbrewer($scope.input.currOption.name,
                                    $scope.input.selectedN), $scope.input.checkboxInvert, $scope.map);
                        } else {
                            var tempRenderer = ChoroService.getChoroScale($scope.input.method, $scope.domain, ColorBrewerService.getColorbrewer($scope.input.currOption.name,
                                    $scope.input.selectedN), $scope.input.checkboxInvert, $scope.map);
                            $scope.input.thisMap.range = tempRenderer.range;
                        }
                    } else {
                        $scope.input.thisMap = ChoroService.getChoroScale($scope.input.method, $scope.domain, ColorBrewerService.getColorbrewer($scope.input.currOption.name,
                                $scope.input.selectedN), $scope.input.checkboxInvert, $scope.map);
                    }
                }
            };

            $scope.close = function () {
                //reset to what was there on modal open  
                ChoroService.getMaps($scope.map).renderer = onXRenderRestore.renderer;
                ChoroService.getMaps($scope.map).brewerName = onXRenderRestore.brewerName;
                $uibModalInstance.dismiss();
            };

            $scope.apply = function () {
                //check breaks are numeric               
                for (var i = 0; i < $scope.input.thisMap.breaks.length; i++) {
                    var thisBreak = Number($scope.input.thisMap.breaks[i]);
                    if (!isNaN(thisBreak)) {
                        $scope.input.thisMap.breaks[i] = thisBreak;
                    } else {
                        $scope.showWarning("Non-numeric break value entered");
                        return;
                    }
                }

                //check breaks are sequential
                var tmp = angular.copy($scope.input.thisMap.breaks);
                tmp.push($scope.input.thisMap.mx);
                tmp.unshift($scope.input.thisMap.mn);
                for (var i = 0; i < tmp.length - 1; i++) {
                    if (tmp[i] > tmp[i + 1]) {
                        $scope.showWarning("Breaks are not in ascending order");
                        return;
                    }
                }

                //apply any user made changes to breaks
                $scope.input.thisMap.scale = d3.scaleThreshold()
                        .domain($scope.input.thisMap.breaks)
                        .range($scope.input.thisMap.range);

                $uibModalInstance.close($scope.input);
            };
        });