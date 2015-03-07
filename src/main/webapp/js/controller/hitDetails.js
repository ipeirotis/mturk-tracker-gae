angular.module('mturk').controller('HitDetailsController',
  ['$scope', '$routeParams', '$filter', 'dataService', function ($scope, $routeParams, $filter, dataService) {

      $scope.readyToShow = false;
      $scope.from = new Date().setDate(new Date().getDate() - 30);
      $scope.to = new Date();

      $scope.hitsChart = {
              options: {
                  scaleType: 'allfixed',
                  dateFormat: 'HH:mm MMMM dd, yyyy'
                  },
              type: 'AnnotationChart',
              data: {"cols": [
                              {id: "id1", label: "Date", type: "date"},
                              {id: "id2", label: "HITs", type: "number"},],
                              "rows": []}
      };

      $scope.$watch('from+to', function(newValue, oldValue) {
          if($scope.from && $scope.to){
              $scope.loadHits();
          }
      });

      $scope.loadHits = function(){
          dataService.loadHitInstances($routeParams.groupId,
                  $filter('date')($scope.from, 'MM/dd/yyyy'),
                  $filter('date')($scope.to, 'MM/dd/yyyy'), function(response){
              var rows = [];
              if($scope.hit.firstSeen) {
                  $scope.hitsChart.options.min = new Date($scope.hit.firstSeen);
              }
              if($scope.hit.lastSeen) {
                  $scope.hitsChart.options.max = new Date($scope.hit.lastSeen);
              }
              angular.forEach(response.items, function(item){
                  rows.push({c:[{v: new Date(item.timestamp)}, {v: parseInt(item.hitsAvailable)}]});
              });

              $scope.hitsChart.data.rows = rows;
          }, function(error){
              console.log(error);
          });
      };

      $scope.getRequester = function(requesterId) {
          dataService.getRequester(requesterId, function(resp){
              $scope.requester = resp;
          }); 
      };
      $scope.getRequester($routeParams.requesterId);

      $scope.loadHit = function() {
          dataService.hitgroup($routeParams.groupId, function(resp){
              $scope.hit = resp;
              $scope.loadHits();
              $scope.readyToShow = true;
          }, function(error){
              console.log(error);
          });
      };

      $scope.loadHit();

      $scope.openFromPicker = function($event) {
          $event.preventDefault();
          $event.stopPropagation();
          $scope.openedFrom = true;
      };

      $scope.openToPicker = function($event) {
          $event.preventDefault();
          $event.stopPropagation();
          $scope.openedTo = true;
      };
      
}]);