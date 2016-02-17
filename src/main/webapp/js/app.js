angular.module('mturk', ['ngRoute', 'ngSanitize', 'ui.bootstrap', 'googlechart', 'ngTable'])
.config(['$routeProvider', 'views', function($routeProvider, views) {
    $routeProvider
    .when('/api', {templateUrl: views.api, controller: 'ApiController'})
    .when('/arrivals', {templateUrl: views.arrivals, controller: 'ArrivalsController'})
    .when('/completions', {templateUrl: views.completions, controller: 'CompletionsController'})
    .when('/general', {templateUrl: views.general, controller: 'GeneralController'})
    .when('/toprequesters', {templateUrl: views.topRequesters, controller: 'TopRequestersController'})
    .when('/activerequesters', {templateUrl: views.activeRequesters, controller: 'ActiveRequestersController'})
    .when('/requester/:requesterId', {templateUrl: views.requesterDetails, controller: 'RequesterDetailsController'})
    .when('/HITgroup/:groupId', {templateUrl: views.hitDetails, controller: 'HitDetailsController'})
    .when('/search', {templateUrl: views.search, controller: 'SearchController', reloadOnSearch: false})
    .otherwise({redirectTo: '/general'});
}])

.config(['$httpProvider', function($httpProvider) {
    $httpProvider.interceptors.push('interceptor');
}]);
