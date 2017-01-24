/* 
 * SERVICE $httpInterceptor to handle in and outgoing requests
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
                                scope.$root.$$childHead.showError(res.data[0].errorMessages[0]);                            
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
        .config(function ($httpProvider) {
            $httpProvider.interceptors.push('authInterceptor');
        });