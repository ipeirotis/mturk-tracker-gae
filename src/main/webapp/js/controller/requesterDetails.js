angular.module('mturk').controller('RequesterDetailsController',
  ['$scope', '$routeParams', 'dataService', 'ngTableParams', '$filter', 
   function ($scope, $routeParams, dataService,  ngTableParams, $filter) {

      $scope.hitGroups = [];

      $scope.getRequester = function(requesterId) {
          dataService.getRequester(requesterId, function(resp){
              $scope.requester = resp;
          });
      };
      $scope.getRequester($routeParams.requesterId);

      $scope.loadHits = function() {
          dataService.hitgroups($routeParams.requesterId, function(resp){
              $scope.hitGroups = resp.items || [];
              $scope.tableParams.reload();
          }, function(error){});
      };
      $scope.loadHits();
      
      $scope.tableParams = new ngTableParams({
          page: 1,
          count: 10,
          sorting: {
              title: 'asc'
          }
      }, {
          total: $scope.hitGroups.length,
          getData: function($defer, params) {
              var orderedData = params.sorting() ?
                      $filter('orderBy')($scope.hitGroups, params.orderBy()) :
                          $scope.topRequesters;
              params.total(orderedData.length);
              $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
          }
      });
      
}]);