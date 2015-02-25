angular.module('mturk').controller('SearchController',
  ['$scope', '$location', '$routeParams', 'dataService', 'ngTableParams', '$filter',
   function ($scope, $location, $routeParams, dataService, ngTableParams, $filter) {

     $scope.hitGroups = [];
     $scope.searchValue = $routeParams.searchValue;
     $scope.searchField = $routeParams.searchField;
      
     $scope.searchFields = [
                             { id: 'all', value: 'All' },
                             { id: 'requesterName', value: 'Requester name' },
                             { id: 'title', value: 'Title' },
                             { id: 'description', value: 'Description' },
                             { id: 'hitContent', value: 'HIT content' },
                             { id: 'keyword', value: 'Keyword' },
                             { id: 'qualification', value: 'Qualification' }
     ];
     
     $scope.search = function() {
         if($scope.searchValue && $scope.searchField) {
             $location.search('searchValue', $scope.searchValue);
             $location.search('searchField', $scope.searchField);
             var params = {};
             params[$scope.searchField] = $scope.searchValue;
             dataService.search(params, function(resp){
                 $scope.hitGroups = resp.items || [];
                 $scope.tableParams.reload();
             }, function(error){});
         }
     };
     $scope.search();
     
     $scope.tableParams = new ngTableParams({
         page: 1,
         count: 10,
         sorting: {
             requesterName: 'asc'
         }
     }, {
         total: $scope.hitGroups.length,
         getData: function($defer, params) {
             var orderedData = params.sorting() ?
                     $filter('orderBy')($scope.hitGroups, params.orderBy()) : $scope.hitGroups;
             params.total(orderedData.length);
             $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
         }
     });

}]);