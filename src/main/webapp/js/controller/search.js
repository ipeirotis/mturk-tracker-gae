angular.module('mturk').controller('SearchController',
  ['$scope', 'dataService', 'ngTableParams', function ($scope, dataService, ngTableParams) {

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
         count: 10
     }, {
         total: $scope.hitGroups.length,
         getData: function($defer, params) {
             params.total($scope.hitGroups.length);
             $defer.resolve($scope.hitGroups.slice((params.page() - 1) * params.count(), params.page() * params.count()));
         }
     });

}]);