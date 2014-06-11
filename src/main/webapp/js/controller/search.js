angular.module('mturk').controller('SearchController',
  ['$scope', 'dataService', 'ngTableParams', '$filter', function ($scope, dataService, ngTableParams, $filter) {

     $scope.hitGroups = [];
      
     $scope.searchFields = [
                             { id: 'requesterName', value: 'Requester name' },
                             { id: 'title', value: 'Title' },
                             { id: 'description', value: 'Description' },
                             { id: 'hitContent', value: 'Hit content' },
                             { id: 'keyword', value: 'Keyword' },
                             { id: 'qualification', value: 'Qualification' }
     ];
     
     $scope.search = function() {
         if($scope.searchValue && $scope.selectedSearchFields) {
             var params = {};
             angular.forEach($scope.selectedSearchFields, function(item){
                 params[$scope.selectedSearchFields] = $scope.searchValue;
             });
             dataService.search(params, function(resp){
                 $scope.hitGroups = resp.items || [];
                 $scope.tableParams.reload();
             }, function(error){});
         }
     };
     
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