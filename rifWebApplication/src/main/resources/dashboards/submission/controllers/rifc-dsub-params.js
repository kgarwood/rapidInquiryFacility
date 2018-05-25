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
 * CONTROLLER for disease submission investigation parameters modal
 */
angular.module("RIF")
        .controller('ModalParametersCtrl', ['$timeout', '$q', '$scope', '$uibModal', 'SubmissionStateService', 'ParameterStateService', 'user', 'StudyAreaStateService',
            function ($timeout, $q, $scope, $uibModal, SubmissionStateService, ParameterStateService, user, StudyAreaStateService) {

                $scope.tree = SubmissionStateService.getState().investigationTree;
                $scope.animationsEnabled = true;

                /*
                 * TABLE SET UP (1)
                 * ICD code list
                 */
                $scope.gridOptionsICD = {
                    enableHorizontalScrollbar: 0,
                    enableColumnResizing: true,
                    rowTemplate: rowTemplate(),
                    columnDefs: [
                        {name: 'term_name', enableHiding: false, width: 100},
                        {name: 'term_description', enableHiding: false}
                    ],
                    onRegisterApi: function (gridApi) {
                        $scope.gridApi1 = gridApi;
                    }
                };
                function rowTemplate() {
                    return  '<div id="testdiv">' +
                            '<div style="height: 100%" ng-class="{ ' +
                            'ICDNotSelected: row.entity.selected===0,' +
                            'ICDSelected: row.entity.selected===1' +
                            '}">' +
                            '<div ng-click="grid.appScope.rowClickICD(row)">' +
                            '<div ng-repeat="(colRenderIndex, col) in colContainer.renderedColumns track by col.colDef.name" class="ui-grid-cell" ui-grid-cell></div>' +
                            '</div>';
                }

                $scope.rowClickICD = function (row) {
                    var idToCheck = [];
                    for (var i = 0; i < $scope.thisICDselection.length; i++) {
                        idToCheck.push($scope.thisICDselection[i][0]);
                    }
                    var thisIndex = idToCheck.indexOf(row.entity.identifier);
                    if (thisIndex === -1) {
                        $scope.thisICDselection.push([row.entity.identifier, row.entity.term_description]);
                    } else {
                        $scope.thisICDselection.splice(thisIndex, 1);
                    }
                };

                /*
                 * TABLE SET UP (2)
                 * table setups recap table
                 * this may be converted into an 'investigation list' table when we have >1 investigation
                 */
                $scope.thisICDselection = ParameterStateService.getState().terms;

                $scope.gridOptionsRecap = {
                    enableColumnMenus: false,
                    enableHorizontalScrollbar: 0,
                    enableColumnResizing: true,
                    enableSorting: false,
                    enableFiltering: false,
                    rowTemplate: rowTemplateRecap(),
                    columnDefs: [
                        {name: 'identifier', enableHiding: false, width: "20%"},
                        {name: 'description', enableHiding: false, width: "80%"}

                    ],
                    onRegisterApi: function (gridApi) {
                        $scope.gridApi2 = gridApi;
                    }
                };
                function rowTemplateRecap() {
                    return  '<div id="testdiv">' +
                            '<div style="height: 100%">' +
                            '<div ng-click="grid.appScope.rowClickRecap(row)">' +
                            '<div ng-repeat="(colRenderIndex, col) in colContainer.renderedColumns track by col.colDef.name" class="ui-grid-cell" ui-grid-cell></div>' +
                            '</div>';
                }
                $timeout(function () {
                    fillRecapTable($scope.thisICDselection);
                }, 1000);

                /*
                 * FILL PARAMETER DROP-DOWNS
                 */

                //Check geography and level selected
                var thisGeography;
                var thisNumerator;
                var thisGeoLevel;

                $scope.fillContents = function () {
                    thisGeography = SubmissionStateService.getState().geography;
                    thisNumerator = SubmissionStateService.getState().numerator.numeratorTableName;
                    thisGeoLevel = StudyAreaStateService.getState().studyResolution;

                    //taxonomy services
                    user.getTaxonomyServiceProviders().then(handleTaxonomyServiceProviders, handleParameterError);

                    //defaults
                    $scope.years = [];
                    $scope.sexes = [];
                    $scope.covariates = [];
                    $scope.selectedAges = [];
                    $scope.ages = [];
                    $scope.taxonomyServices = [];
                    $scope.covariate = ParameterStateService.getState().covariate;
                    if ($scope.covariate === "") {
                        $scope.covariate = "NONE";
                    }
                    $scope.title = ParameterStateService.getState().title;
                    $scope.sex = ParameterStateService.getState().sex;
                    $scope.startYear = Number(ParameterStateService.getState().start);
                    $scope.endYear = Number(ParameterStateService.getState().end);
                    $scope.yearInterval = Number(ParameterStateService.getState().interval);
                    $scope.lowerAge = ParameterStateService.getState().lowerAge;
                    $scope.upperAge = ParameterStateService.getState().upperAge;

                    //covariates
                    var d1 = $q.defer();
                    var p1 = d1.promise;
                    d1.resolve(fillCovariates());
                    p1.then(function (value) {
                        return value;
                    }, handleParameterError);

                    //years
                    var d2 = $q.defer();
                    var p2 = d2.promise;
                    d2.resolve(fillYears());
                    p2.then(function (value) {
                        return value;
                    }, handleParameterError);


                    $q.all([p1, p2]).then(function (result) {
                        for (var i = 0; i < result.length; i++) {
                            if (result[i] !== true) {
                                $scope.showWarning(result[i]);
                            }
                        }
                    });
                };

                function handleParameterError(res) {
                    $scope.consoleLog("parameter error");
                }

                //handle taxonomy services
                function handleTaxonomyServiceProviders(res) {
                    $scope.taxonomyServices.length = 0;
                    for (var i = 0; i < res.data.length; i++) {
                        $scope.taxonomyServices.push(res.data[i].identifier);
                    }
                    $scope.taxonomyScheme = $scope.taxonomyServices[0];
                }

                //handle fill covariates box
                function fillCovariates() {
                    $scope.covariates.length = 0;
                    $scope.covariates.push("NONE");
                    var tmp = [];
                    if (thisGeoLevel !== "") {
                        user.getCovariates(user.currentUser, thisGeography, thisGeoLevel).then(fillHandleCovariates, handleParameterError);
                    } else {
                        return "Cannot display available covariates until you select a study resolution in Study Area";
                    }
                    function fillHandleCovariates(res) {
                        if (!angular.isUndefined(res.data)) {
                            for (var i = 0; i < res.data.length; i++) {
                                $scope.covariates.push(res.data[i].name);
                                tmp.push({name: res.data[i].name, minimum_value: res.data[i].minimumValue,
                                    maximum_value: res.data[i].maximumValue, covariate_type: res.data[i].covariateType});
                            }
                        }
                        ParameterStateService.getState().possibleCovariates = tmp;
                    }
                    return true;
                }

                //handle years etc
                function fillYears() {
                    $scope.ages.length = 0;
                    $scope.agesSelected = 0;
                    $scope.years.length = 0;

                    if (thisGeography !== "") {
                        user.getAgeGroups(user.currentUser, thisGeography, thisNumerator).then(fillHandleAgeGroups, handleParameterError);
                        user.getYearRange(user.currentUser, thisGeography, thisNumerator).then(fillHandleYears, handleParameterError);
                    } else {
                        return "Cannot display available age groups and year ranges until you select a valid geography";
                    }
                    user.getSexes(user.currentUser).then(fillHandleSexes, handleParameterError);

                    function fillHandleAgeGroups(res) {
                        if (!angular.isUndefined(res.data)) {
                            var tmp = [];
                            for (var i = 0; i < res.data[0].name.length; i++) {
                                $scope.ages.push(res.data[0].name[i]);
                                tmp.push({id: 0, name: res.data[0].name[i], lower_limit: res.data[1].lowerAgeLimit[i], upper_limit: res.data[2].upperAgeLimit[i]});
                            }
                            ParameterStateService.getState().possibleAges = tmp;
                            if ($scope.lowerAge === "") {
                                $scope.lowerAge = $scope.ages[0];
                            }
                            if ($scope.upperAge === "") {
                                $scope.upperAge = $scope.ages[$scope.ages.length - 1];
                            }
                        }
                    }
                    function fillHandleYears(res) {
                        if (!angular.isUndefined(res.data)) {
                            for (var i = res.data[0].lowerBound; i <= res.data[0].upperBound; i++) {
                                $scope.years.push(Number(i));
                            }
                            if ($scope.startYear === 1) {
                                $scope.startYear = Number(res.data[0].lowerBound);
                            }
                            if ($scope.endYear === 1) {
                                $scope.endYear = Number(res.data[0].upperBound);
                            }
                            $scope.yearsChanged();
                        }
                    }
                    function fillHandleSexes(res) {
                        $scope.sexes.length = 0;
                        if (!angular.isUndefined(res.data)) {
                            for (var i = 0; i < res.data[0].names.length; i++) {
                                $scope.sexes.push(res.data[0].names[i]);
                            }
                            if ($scope.sex === "") {
                                if (res.data[0].names.indexOf("Both") !== -1) {
                                    $scope.sex = "Both";
                                } else {
                                    $scope.sex = res.data[0].names[0];
                                }                               
                            }
                        }
                    }
                    return true;
                }

                //$scope.intervals = [];
                $scope.yearsChanged = function () {

                    //ensure that start year is before end year
                    if ($scope.endYear < $scope.startYear) {
                        $scope.endYear = [$scope.startYear, $scope.startYear = $scope.endYear][0];
                    }

                    /* CURRENTLY NOT IMPLEMENTED
                     //calculate possible intervals, would need to add another drop-down to UI
                     $scope.intervals.length = 0;
                     var diff = $scope.endYear - $scope.startYear + 1;
                     for (var i = 1; i <= diff; i++) {
                     if (diff % i === 0) {
                     $scope.intervals.push(i);
                     }
                     }
                     $scope.yearInterval = $scope.intervals[0];
                     */
                    $scope.yearInterval = 1; //FIXED TO BE ONE YEAR INTERVALS
                };
                //make sure age bands are ascending
                $scope.agesChanged = function () {
                    if ($scope.upperAge < $scope.lowerAge) {
                        $scope.upperAge = [$scope.lowerAge, $scope.lowerAge = $scope.upperAge][0];
                    }
                };

                /*
                 * ICD SEARCH
                 */
                $scope.searchText = "";
                $scope.hitsCount = "";

                //search on return key from text box
                $scope.textKeyPress = function (event) {
                    if (event.charCode === 13) {
                        $scope.searchDescription();
                    }
                };
                //Get search terms
                $scope.searchDescription = function () {
                    user.getMatchingTerms($scope.taxonomyScheme.toLowerCase(), $scope.searchText).then(handleTextSearch, handleTextSearch);
                };
                function handleTextSearch(res) {
                    var myICD = [];
                    if (res.data.length !== 0) {
                        for (var i = 0; i < res.data.length; i++) {
                            myICD.push({term_name: res.data[i].label, term_description: res.data[i].description, identifier: res.data[i].identifier, selected: 0});
                            //if selected already, make them green in ICD table
                            for (var j = 0; j < $scope.thisICDselection.length; j++) {
                                if (res.data[i].identifier === $scope.thisICDselection[j][0]) {
                                    myICD[i].selected = 1;
                                    break;
                                }
                            }
                        }
                    }
                    if (res.data.length === 1) {
                        $scope.hitsCount = "1 term returned";
                    } else {
                        $scope.hitsCount = res.data.length + " terms returned";
                    }
                    $scope.gridOptionsICD.data = myICD;
                }
                $scope.resetTextSearch = function () {
                    $scope.searchText = "";
                    $scope.hitsCount = "";
                };

                /*
                 * TABLE SYNCHRONISATION
                 */
                $scope.$watchCollection('thisICDselection', function (newNames, oldNames) {
                    if (newNames === oldNames) {
                        return;
                    }
                    var idToCheck = [];
                    for (var i = 0; i < newNames.length; i++) {
                        idToCheck.push(newNames[i][0]);
                    }
                    if (!angular.isUndefined($scope.gridOptionsICD.data)) {
                        for (var i = 0; i < $scope.gridOptionsICD.data.length; i++) {
                            if (idToCheck.indexOf($scope.gridOptionsICD.data[i].identifier) !== -1) {
                                $scope.gridOptionsICD.data[i].selected = 1;
                            } else {
                                $scope.gridOptionsICD.data[i].selected = 0;
                            }
                        }
                    }
                    fillRecapTable($scope.thisICDselection);
                });

                function fillRecapTable(ICD) {
                    var rowCollection = [];
                    for (var i = 0; i < ICD.length; i++) {
                        var obj = {
                            identifier: ICD[i][0],
                            description: ICD[i][1]
                        };
                        rowCollection.push(obj);
                    }
                    $scope.gridOptionsRecap.data = rowCollection;
                }

                //sync the recap table with the ICD table to remove terms
                $scope.rowClickRecap = function (row) {
                    var idToCheck = [];
                    for (var i = 0; i < $scope.thisICDselection.length; i++) {
                        idToCheck.push($scope.thisICDselection[i][0]);
                    }
                    var thisIndex = idToCheck.indexOf(row.entity.identifier);
                    if (thisIndex !== -1) {
                        $scope.thisICDselection.splice(thisIndex, 1);
                    }
                };

                //yes-no modal for $scope.clearInvestigations
                $scope.openClearAll = function () {
                    $scope.modalHeader = "Clear investigations";
                    $scope.modalBody = "Clear all rows. Are you sure?";
                    var modalInstance = $uibModal.open({
                        animation: true,
                        templateUrl: 'utils/partials/rifp-util-yesno.html',
                        controller: 'ModalClearAllYesNoInstanceCtrl',
                        windowClass: 'stats-Modal',
                        backdrop: 'static',
                        scope: $scope,
                        keyboard: false
                    });
                };

                //Clear all from recap table
                $scope.clearInvestigations = function () {
                    $scope.openClearAll();
                };

                $scope.doClearAll = function () {
                    resetTable();
                    fillRecapTable($scope.thisICDselection);
                };

                resetTable = function () {
                    // $scope.thisICDselection.length = 0; // This causes a leak!
					$scope.thisICDselection.splice(0, $scope.thisICDselection.length); // Fix
                };

                //submit and save the results
                $scope.addInvestigation = function () {
                    ParameterStateService.getState().terms = $scope.thisICDselection;
                    ParameterStateService.getState().title = $scope.title;
                    ParameterStateService.getState().start = $scope.startYear;
                    ParameterStateService.getState().end = $scope.endYear;
                    ParameterStateService.getState().interval = $scope.yearInterval;
                    ParameterStateService.getState().sex = $scope.sex;
                    ParameterStateService.getState().covariate = $scope.covariate;
                    ParameterStateService.getState().lowerAge = $scope.lowerAge;
                    ParameterStateService.getState().upperAge = $scope.upperAge;
                    $scope.submit();
                };

                $scope.open = function () {
                    var modalInstance = $uibModal.open({
                        animation: false, //$scope.animationsEnabled,
                        templateUrl: 'dashboards/submission/partials/rifp-dsub-params.html',
                        controller: 'ModalParametersInstanceCtrl',
                        windowClass: 'modal-fit',
                        backdrop: 'static',
                        scope: $scope,
                        keyboard: false
                    });
                    modalInstance.opened.then(function () {
                        //fill all user controls
                        $scope.fillContents();
                    });
                    modalInstance.result.then(function () {
                        //Change tree icon colour
                        
                        //ParameterStateService.getState().terms
                         //if ($scope.thisICDselection.length !== 0 & 
                        
                        if (ParameterStateService.getState().terms.length !== 0 & 
                                ParameterStateService.getState().title !== "" &
                                ParameterStateService.getState().start !== "" & 
                                ParameterStateService.getState().end !== "" & 
                                ParameterStateService.getState().interval !== "" &
                                ParameterStateService.getState().upperAge !== "" & 
                                ParameterStateService.getState().lowerAge !== "") 
                        {   
                            SubmissionStateService.getState().investigationTree = true;
                            $scope.tree = true;
                        } else {
                            SubmissionStateService.getState().investigationTree = false;
                            $scope.tree = false;
                        }
                    });
                };
            }
        ])
        .controller('ModalParametersInstanceCtrl', function ($scope, $uibModalInstance) {
            $scope.close = function () {
                $uibModalInstance.dismiss();
            };
            $scope.submit = function () {
                $uibModalInstance.close();
            };
        })
        .controller('ModalRemoveYesNoInstanceCtrl', function ($scope, $uibModalInstance) {
            $scope.close = function () {
                $uibModalInstance.dismiss();
            };
            $scope.submit = function () {
                $scope.removeThisRow();
                $uibModalInstance.close();
            };
        })
        .controller('ModalClearAllYesNoInstanceCtrl', function ($scope, $uibModalInstance) {
            $scope.close = function () {
                $uibModalInstance.dismiss();
            };
            $scope.submit = function () {
                $scope.doClearAll();
                $uibModalInstance.close();
            };
        });