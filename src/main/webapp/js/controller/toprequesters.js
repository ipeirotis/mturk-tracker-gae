angular.module('mturk').controller('TopRequestersController',
  ['$scope', 'dataService', 'ngTableParams', '$filter', function ($scope, dataService, ngTableParams, $filter) {
      
      $scope.topRequesters = [];

      $scope.load = function() {
          dataService.toprequesters(function(resp){
              $scope.topRequesters = resp.items || [];
              $scope.tableParams.reload();
          }, function(error){});
      };
      
      $scope.load();
      
      $scope.tableParams = new ngTableParams({
          page: 1,
          count: 10,
          sorting: {
              reward: 'desc'
          }
      }, {
          total: $scope.topRequesters.length,
          getData: function($defer, params) {
              var orderedData = params.sorting() ?
                      $filter('orderBy')($scope.topRequesters, params.orderBy()) :
                          $scope.topRequesters;
              $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
              //$defer.resolve($scope.topRequesters.slice((params.page() - 1) * params.count(), params.page() * params.count()));
          }
      });
      
}]);