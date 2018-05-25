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
 * SERVICE $httpInterceptor to handle in and outgoing requests
 * This is just a generic interceptor found on the web somewhere
 */
angular.module("RIF")
        .factory('authInterceptor', ['$q', '$injector',
            function ($q, $injector) {
                return {
                    request: function (config) {
                        var AuthService = $injector.get('user');
                        //login check here on all outgoing RIF requests to middleware
                        if (!angular.isUndefined(config.headers.rifUser)) {
                            try {
                                AuthService.isLoggedIn(AuthService.currentUser).then(loggedIn, loggedIn);
                                function loggedIn(res) {
                                    if (angular.isUndefined(res) || angular.isUndefined(res.data) || res.data[0].result === "false") {
                                        //Redirect to login screen
                                        $injector.get('$state').transitionTo('state0');
                                    }
                                }
                            } catch (e) {
                                $injector.get('$state').transitionTo('state0');
                            }
                        }
                        return config;
                    },
                    response: function (res) {
                        //called with the response object from the server
                        if (!angular.isUndefined(res.data[0])) {
                            if (!angular.isUndefined(res.data[0].errorMessages)) {
                                var scope = $injector.get('$rootScope');
								/* Trap:
								 *
								 * API method "isLoggedIn" has a null "userID" parameter.
								 * Record "User" field "User ID" cannot be empty.
								 */
								if (res.data[0].errorMessages[0] ==
									'API method "isLoggedIn" has a null "userID" parameter.') {
									scope.$root.$$childHead.consoleError(res.data[0].errorMessages[0]);
								}
								else if (res.data[0].errorMessages[0] ==
									'Record "User" field "User ID" cannot be empty.') {
									scope.$root.$$childHead.consoleError(res.data[0].errorMessages[0]);
								}
								else if (res.data[0].errorMessages[0] ==
									'Unable to roll back database transaction.') {
									scope.$root.$$childHead.consoleError(res.data[0].errorMessages[0]);
								}
								else {
									scope.$root.$$childHead.showError(res.data[0].errorMessages[0]);
								}
                                return $q.reject();
                            }
                        }
                        return res;
                    },
                    requestError: function (rejection) {
                        return $q.reject(rejection);
                    },
                    responseError: function (rejection) {
                        //for non-200 status
                        return $q.reject(rejection);
                    }
                };
            }])
        .config(["$httpProvider", function ($httpProvider) {
            $httpProvider.interceptors.push('authInterceptor');
			
			// https://stackoverflow.com/questions/1043339/javascript-for-detecting-browser-language-preference
			var getFirstBrowserLanguage = function () {
				var language = navigator.languages && navigator.languages[0] || // Chrome / Firefox
					navigator.language ||   // All browsers
					navigator.userLanguage; // IE <= 10
			   
				return language;
			 };
			 
			$httpProvider.defaults.headers.common["Accept-Language"] = getFirstBrowserLanguage() || "en-US";
        }]);