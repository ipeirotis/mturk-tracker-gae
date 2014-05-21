angular.module('mturk', ['ngRoute', 'ngSanitize', 'ui.bootstrap', 'googlechart'])
.config(['$routeProvider', 'views', function($routeProvider, views) {
    $routeProvider
    .when('/api', {templateUrl: views.api, controller: 'ApiController'})
    .when('/arrivals', {templateUrl: views.arrivals, controller: 'ArrivalsController'})
    .when('/completions', {templateUrl: views.completions, controller: 'CompletionsController'})
    .when('/general', {templateUrl: views.general, controller: 'GeneralController'})
    .when('/requesters', {templateUrl: views.requesters, controller: 'RequestersController'})
    .when('/search', {templateUrl: views.search, controller: 'SearchController'})
    .otherwise({redirectTo: '/general'});
}])

.config(['$httpProvider', function($httpProvider) {
    $httpProvider.interceptors.push('interceptor');
}]);
