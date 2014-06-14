angular.module('mturk').controller('HitDetailsController',
  ['$scope', '$routeParams', 'dataService', function ($scope, $routeParams, dataService) {
      
      $scope.readyToShow = false;
      
      $scope.getRequester = function(requesterId) {
          dataService.getRequester(requesterId, function(resp){
              $scope.requester = resp;
          }); 
      };
      $scope.getRequester($routeParams.requesterId);
      
      $scope.loadHit = function() {
          dataService.hitgroup($routeParams.groupId, function(resp){
              $scope.hit = resp;
              $scope.readyToShow = true;
          }, function(error){});
      };
      
      $scope.loadHit();
}]);