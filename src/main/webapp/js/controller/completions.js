angular.module('mturk').controller('CompletionsController', ['$scope', '$filter', 'dataService', 
    function ($scope, $filter, dataService) {

    $scope.from = new Date().setDate(new Date().getDate() - 7);
    $scope.to = new Date();
    
    $scope.rows = {
            hitsChart: [],
            hitGroupsChart: [],
            rewardsChart: []
    };

    $scope.hitsChart = {
            options: {scaleType: 'allfixed'},
            type: 'AnnotationChart',
            data: {"cols": [
                            {id: "id1", label: "Date", type: "date"},
                            {id: "id2", label: "HITs completed", type: "number"}],
                            "rows": []}
    };
    
    $scope.hitGroupsChart = {
            type: 'AnnotationChart',
            data: {"cols": [
                            {id: "id1", label: "Date", type: "date"},
                            {id: "id2", label: "HIT groups completed", type: "number"}],
                            "rows": []}
    };

    $scope.rewardsChart = {
            type: 'AnnotationChart',
            data: {"cols": [
                            {id: "id1", label: "Date", type: "date"},
                            {id: "id2", label: "Rewards completed", type: "number"}],
                            "rows": []}
    };

    $scope.$watch('from+to', function(newValue, oldValue) {
        if($scope.from && $scope.to){
            $scope.load();
        }
    });
    
    $scope.charts = ['hitsChart', 'hitGroupsChart', 'rewardsChart'];
    $scope.drawnCharts = [];
    $scope.visibleChart = 'hitsChart';
    $scope.activePill = 'hitsChartPill';
    
    $scope.load = function(){
        dataService.load($filter('date')($scope.from, 'MM/dd/yyyy'), $filter('date')($scope.to, 'MM/dd/yyyy'), function(response){ 
            angular.forEach(response.items, function(item){
                $scope.rows.hitsChart.push({c:[{v: new Date(item.from)}, {v: parseInt(item.hitsCompleted)}]});
                $scope.rows.hitGroupsChart.push({c:[{v: new Date(item.from)}, {v: parseInt(item.hitGroupsCompleted)}]});
                $scope.rows.rewardsChart.push({c:[{v: new Date(item.from)}, {v: parseInt(item.rewardsCompleted)/100}]});
            });
            
            $scope.drawnCharts = [];
            $scope.draw('hitsChart');
        }, function(error){
            //TODO
        });
    };
    
    $scope.draw = function(chart){
        $scope.visibleChart = chart;
        $scope.activePill = chart + 'Pill';
        
        var drawn = false;
        var i = $.inArray(chart, $scope.drawnCharts);
        if(i < 0){
            $scope.drawnCharts.push(chart);
        }else{
            drawn = true;
        }
        
        angular.forEach($scope.charts, function(chart){
            if($scope.visibleChart != chart){
                $('#'+chart).css({display:'none'});
            }else{
                if(drawn == false){
                    $('#'+chart).css({visibility:'hidden'});
                }
                $('#'+chart).css({display:'block'});
            }
        });
        
        if(drawn == false){
            var ch = $scope[chart];
            ch.data.rows = $scope.rows[chart];
            ch.ts = new Date();
        }
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
    
    $scope.chartReady = function(chart) {
        $('#'+chart).css({visibility:'visible'});
        fixGoogleChartsBarsBootstrap();
    };
    
    function fixGoogleChartsBarsBootstrap() {
        // Google charts uses <img height="12px">, which is incompatible with Twitter
        // * bootstrap in responsive mode, which inserts a css rule for: img { height: auto; }.
        // *
        // * The fix is to use inline style width attributes, ie <img style="height: 12px;">.
        // * BUT we can't change the way Google Charts renders its bars. Nor can we change
        // * the Twitter bootstrap CSS and remain future proof.
        // *
        // * Instead, this function can be called after a Google charts render to "fix" the
        // * issue by setting the style attributes dynamically.

       $(".google-visualization-table-table img[width]").each(function(index, img) {
           $(img).css("width", $(img).attr("width")).css("height", $(img).attr("height"));
       });
    };
}]);