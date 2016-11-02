/* 
 * CONTROLLER for disease submission run study from file modal
 */
angular.module("RIF")
        .controller('ModalRunFileCtrl', ['$q', 'user', '$scope', '$uibModal',
            'StudyAreaStateService', 'CompAreaStateService', 'SubmissionStateService', 'StatsStateService', 'ParameterStateService',
            function ($q, user, $scope, $uibModal,
                    StudyAreaStateService, CompAreaStateService, SubmissionStateService, StatsStateService, ParameterStateService) {

                var rifJob;
                var tmpProjects;
                var tmpGeography;
                var tmpGeoLevel;
                var tmpHealthThemeName;
                var tmpHealthThemeDescription;
                var tmpNumeratorName;
                var tmpDenominatorName;
                var tmpMethod;
                var tmpChecked = -2;
                var tmpFullICDselection = [];
                var tmpTitle;
                var tmpStart;
                var tmpEnd;
                var tmpInterval;
                var tmpSex;
                var tmpCovariate;

                /*
                 * THE FUNCIONS FOR CHECKING RIFJOB
                 */
                //checking this is a valid RIF study object
                function uploadCheckStructure() {
                    try {
                        rifJob = JSON.parse($scope.$$childHead.$$childTail.content).rif_job_submission;
                    } catch (e) {
                        return "Could not read file";
                    }
                    //has a non-empty object been uploaded?
                    if (angular.isUndefined(rifJob) || rifJob === null) {
                        return "Not a valid or recognised RIF job";
                    }
                    //Expected headers present for RIF study
                    var expectedHeaders = ['submitted_by', 'job_submission_date', 'project', 'disease_mapping_study', 'calculation_methods', 'rif_output_options'];
                    var thisHeaders = [];
                    for (var i in rifJob) {
                        thisHeaders.push(rifJob[i]);
                    }
                    if (expectedHeaders.length !== thisHeaders.length) {
                        return "Not a recognised RIF job, expected headers not found";
                    } else {
                        for (var i = 0; i < rifJob.length; i++) {
                            if (thisHeaders[i] !== expectedHeaders[i]) {
                                return "Expected header not found " + expectedHeaders[i];
                            }
                        }
                    }
                    return true;
                }

                //checking if the Health theme exists and matches geography
                function uploadHealthThemes() {
                    tmpHealthThemeName = rifJob.disease_mapping_study.investigations.investigation[0].health_theme.name;
                    tmpHealthThemeDescription = rifJob.disease_mapping_study.investigations.investigation[0].health_theme.description;
                    var themeErr = user.getHealthThemes(user.currentUser, rifJob.disease_mapping_study.geography.name).then(uploadHandleHealthThemes, fromFileError);

                    function uploadHandleHealthThemes(res) {
                        var bFound = false;
                        for (var i = 0; i < res.data.length; i++) {
                            if (res.data[i].name === tmpHealthThemeName & res.data[i].description === tmpHealthThemeDescription) {
                                bFound = true;
                                break;
                            }
                        }
                        if (!bFound) {
                            return "Health Theme '" + tmpHealthThemeName + "' not found in database";
                        } else {
                            return true;
                        }
                    }
                    return themeErr;
                }

                //Checking if the geography exists in the user database
                function uploadCheckGeography() {
                    tmpGeography = rifJob.disease_mapping_study.geography.name;
                    tmpGeoLevel = rifJob.disease_mapping_study.disease_mapping_study_area.geo_levels.geolevel_select.name;
                    var bFound = false;
                    for (var i = 0; i < $scope.$parent.geographies.length; i++) {
                        if ($scope.$parent.geographies[i] === tmpGeography) {
                            bFound = true;
                            break;
                        }
                    }
                    if (!bFound) {
                        return "Geography '" + tmpGeography + "' not found in database";
                    } else {
                        return true;
                    }
                }

                function uploadFractions() {
                    tmpNumeratorName = rifJob.disease_mapping_study.investigations.investigation[0].numerator_denominator_pair.numerator_table_name;
                    tmpDenominatorName = rifJob.disease_mapping_study.investigations.investigation[0].numerator_denominator_pair.denominator_table_name;
                    var fractionErr = user.getFractions(user.currentUser, tmpGeography, tmpHealthThemeDescription).then(uploadHandleFractions, fromFileError);

                    function uploadHandleFractions(res) {
                        var bFound = false;
                        for (var i = 0; i < res.data.length; i++) {
                            if (res.data[i].numeratorTableName === tmpNumeratorName & res.data[i].denominatorTableName === tmpDenominatorName) {
                                bFound = true;
                                break;
                            }
                        }
                        if (!bFound) {
                            return "Numerator-Denominator Pair '" + tmpNumeratorName + " - " + tmpDenominatorName + "' not found in database";
                        } else {
                            return true;
                        }
                    }
                    return fractionErr;
                }

                function uploadStats() {
                    tmpMethod = rifJob.calculation_methods.calculation_method;
                    if (angular.isUndefined(tmpMethod.code_routine_name)) {
                        //method not yet selected by the user
                        return true;
                    }
                    //check method is actually availble to user
                    var statErr = user.getAvailableCalculationMethods(user.currentUser).then(uploadHandleAvailableCalculationMethods, fromFileError);

                    function uploadHandleAvailableCalculationMethods(res) {
                        var bFound = false;
                        var pCount = 0;
                        for (var i = 0; i < res.data.length; i++) {
                            if (tmpMethod.code_routine_name === res.data[i].codeRoutineName) {
                                if (tmpMethod.description === res.data[i].description) {
                                    for (var j = 0; j < tmpMethod.parameters.parameter.length; j++) {
                                        for (var k = 0; k < res.data[i].parameterProxies.length; k++) {
                                            if (tmpMethod.parameters.parameter[j].name === res.data[i].parameterProxies[k].name) {
                                                pCount++;
                                            }
                                        }
                                    }
                                    if (pCount === res.data[i].parameterProxies.length) {
                                        tmpChecked = i;
                                        bFound = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (!bFound) {
                            return "Statistical Method '" + tmpMethod.description + "' not found in database, or has incomplete description";
                        } else {
                            return true;
                        }
                    }
                    return statErr;
                }

                function uploadProjects() {
                    tmpProjects = rifJob.project;
                    var projectErr = user.getProjects(user.currentUser).then(uploadHandleProjects, fromFileError);

                    function uploadHandleProjects(res) {
                        var bFound = false;
                        for (var i = 0; i < res.data.length; i++) {
                            if (res.data[i].name === tmpProjects.name && res.data[i].description === tmpProjects.description) {
                                bFound = true;
                                break;
                            }
                        }
                        if (!bFound) {
                            return "Project '" + tmpProjects.name + "' not found in database";
                        } else {
                            return true;
                        }
                    }
                    return projectErr;
                }

                function uploadInvestigations() {
                    //terms
                    var inv = rifJob.disease_mapping_study.investigations.investigation;
                    for (var j = 0; j < inv[0].health_codes.health_code.length; j++) {
                        tmpFullICDselection.push([inv[0].health_codes.health_code[j].code + '-' + inv[0].health_codes.health_code[j].name_space,
                            inv[0].health_codes.health_code[j].description]);
                    }
                    //parameters
                    tmpTitle = inv[0].title;
                    tmpStart = inv[0].year_range.lower_bound;
                    tmpEnd = inv[0].year_range.upper_bound;
                    tmpInterval = inv[0].years_per_interval;
                    tmpSex = inv[0].sex;
                    var cv = "";
                    if (!angular.isUndefined(inv[0].covariates[0])) {
                        cv = inv[0].covariates[0].adjustable_covariate.name;
                    }
                    tmpCovariate = cv;
                    return true;
                }

                /*
                 * All tests passed so commit changes to states
                 */
                function confirmStateChanges() {
                    //general
                    SubmissionStateService.getState().studyName = rifJob.disease_mapping_study.name;
                    SubmissionStateService.getState().geography = rifJob.disease_mapping_study.geography.name;
                    SubmissionStateService.getState().numerator = rifJob.disease_mapping_study.investigations.investigation[0].numerator_denominator_pair.numerator_table_name;
                    SubmissionStateService.getState().denominator = rifJob.disease_mapping_study.investigations.investigation[0].numerator_denominator_pair.denominator_table_name;
                    SubmissionStateService.getState().studyDescription = rifJob.disease_mapping_study.description;
                    SubmissionStateService.getState().healthTheme = rifJob.disease_mapping_study.investigations.investigation[0].health_theme.name;

                    //Study area
                    StudyAreaStateService.getState().selectAt = rifJob.disease_mapping_study.disease_mapping_study_area.geo_levels.geolevel_select.name;
                    StudyAreaStateService.getState().studyResolution = rifJob.disease_mapping_study.disease_mapping_study_area.geo_levels.geolevel_to_map.name;
                    StudyAreaStateService.getState().polygonIDs = rifJob.disease_mapping_study.disease_mapping_study_area.map_areas.map_area;
                    StudyAreaStateService.getState().geography = rifJob.disease_mapping_study.geography.name;
                    if (StudyAreaStateService.getState().polygonIDs.length !== 0) {
                        SubmissionStateService.getState().studyTree = true;
                    }
                    //Comparison area
                    CompAreaStateService.getState().selectAt = rifJob.disease_mapping_study.comparison_area.geo_levels.geolevel_select.name;
                    CompAreaStateService.getState().studyResolution = rifJob.disease_mapping_study.comparison_area.geo_levels.geolevel_to_map.name;
                    CompAreaStateService.getState().polygonIDs = rifJob.disease_mapping_study.comparison_area.map_areas.map_area;
                    CompAreaStateService.getState().geography = rifJob.disease_mapping_study.geography.name;
                    if (CompAreaStateService.getState().polygonIDs.length !== 0) {
                        SubmissionStateService.getState().comparisonTree = true;
                    }

                    //Parameters
                    var inv = rifJob.disease_mapping_study.investigations.investigation;
                    ParameterStateService.getState().title = inv[0].title;
                    ParameterStateService.getState().start = inv[0].year_range.lower_bound;
                    ParameterStateService.getState().end = inv[0].year_range.upper_bound;
                    ParameterStateService.getState().interval = inv[0].years_per_interval;
                    ParameterStateService.getState().sex = inv[0].sex;
                    ParameterStateService.getState().covariate = tmpCovariate;

                    ParameterStateService.getState().activeHealthTheme = rifJob.disease_mapping_study.investigations.investigation[0].health_theme.name;
                    ParameterStateService.getState().terms = tmpFullICDselection;
                    if (tmpFullICDselection.length !== 0) {
                        SubmissionStateService.getState().investigationTree = true;
                    }

                    //Stats
                    StatsStateService.getState().checked = tmpChecked;
                    if (tmpChecked >= 0) {
                        for (var i = 0; i < tmpMethod.parameters.parameter.length; i++) {
                            StatsStateService.getState().model[tmpChecked][i] = tmpMethod.parameters.parameter[i].value;
                        }
                        SubmissionStateService.getState().statsTree = true;
                    }
                }

                function fromFileError() {
                    $scope.showError("Could not upload the file");
                }


                $scope.open = function () {
                    $scope.modalHeader = "Open study from file";
                    $scope.accept = ".json";

                    $scope.showContent = function ($fileContent) {
                        $scope.content = $fileContent.toString();
                    };

                    $scope.uploadFile = function () {

                        //check initial file structure
                        var d1 = $q.defer();
                        var p1 = d1.promise;
                        d1.resolve(uploadCheckStructure());
                        p1.then(function (value) {
                            return value;
                        }, fromFileError);

                        //check geography exists
                        var p2 = uploadCheckGeography();

                        //check health theme
                        var d3 = $q.defer();
                        var p3 = d3.promise;
                        d3.resolve(uploadHealthThemes());
                        p3.then(function (value) {
                            return value;
                        }, fromFileError);

                        //check numerator-denominator match
                        var d4 = $q.defer();
                        var p4 = d4.promise;
                        d4.resolve(uploadFractions());
                        p4.then(function (value) {
                            return value;
                        }, fromFileError);

                        //check stats and parameter match
                        var d5 = $q.defer();
                        var p5 = d5.promise;
                        d5.resolve(uploadStats());
                        p5.then(function (value) {
                            return value;
                        }, fromFileError);

                        //check project matches
                        var d6 = $q.defer();
                        var p6 = d6.promise;
                        d6.resolve(uploadProjects());
                        p6.then(function (value) {
                            return value;
                        }, fromFileError);

                        //check investigations
                        var d7 = $q.defer();
                        var p7 = d7.promise;
                        d7.resolve(uploadInvestigations());
                        p7.then(function (value) {
                            return value;
                        }, fromFileError);

                        //resolve all the promises
                        $q.all([p1, p2, p3, p4, p5, p6, p7]).then(function (result) {
                            var bPass = true;
                            for (var i = 0; i < result.length; i++) {
                                if (result[i] !== true) {
                                    bPass = false;
                                    $scope.showError(result[i]);
                                    break;
                                }
                            }
                            if (bPass) {
                                //All tests passed
                                confirmStateChanges();
                                $scope.showSuccess("RIF study opened from file");
                                $scope.$parent.resetState();
                            }
                        });
                    };

                    var modalInstance = $uibModal.open({
                        animation: true,
                        templateUrl: 'dashboards/submission/partials/rifp-dsub-fromfile.html',
                        controller: 'ModalRunFileInstanceCtrl',
                        windowClass: 'stats-Modal',
                        backdrop: 'static',
                        scope: $scope,
                        keyboard: false
                    });
                };
            }])
        .controller('ModalRunFileInstanceCtrl', function ($scope, $uibModalInstance) {
            $scope.close = function () {
                $uibModalInstance.dismiss();
            };
            $scope.submit = function () {
                $scope.uploadFile();
                $uibModalInstance.close();
            };
        });