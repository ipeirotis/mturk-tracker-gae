angular.module('mturk').controller('ActiveRequestersController',
  ['$scope', 'dataService', 'ngTableParams', '$filter', function ($scope, dataService, ngTableParams, $filter) {

      $scope.from = new Date();
      $scope.from.setMonth(new Date().getMonth() - 1);
      $scope.to = new Date();
      $scope.weekAgo = new Date();
      $scope.weekAgo.setDate(new Date().getDate() - 7);
      $scope.activeRequesters = [];

      $scope.activeRequestersChart = {
              type: 'AnnotationChart',
              options: {
                  scaleType: 'allfixed',
                  dateFormat: 'HH:mm MMMM dd, yyyy',
                  zoomStartTime:  $scope.weekAgo,
                  zoomEndTime:  $scope.to
                  },
              data: {"cols": [
                              {label: "Date", type: "date"},
                              {label: "Active requesters", type: "number"}],
                              "rows": []}
      };

      $scope.$watch('from+to', function() {
          if($scope.from && $scope.to){
              $scope.load();
          }
      });

      $scope.load = function() {
          dataService.load($filter('date')($scope.from, 'MM/dd/yyyy'), $filter('date')($scope.to, 'MM/dd/yyyy'), function(response){
              var rows = [];
              angular.forEach(response.items, function(item) {
                  rows.push({c:[{v: new Date(item.from)}, {v: parseInt(item.activeRequesters)}]});
              });
              
              $scope.activeRequestersChart.data.rows = rows;
          }, function(error){
              console.log(error);
          });
      };
      
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