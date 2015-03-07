angular.module('mturk').controller('GeneralController', ['$scope', '$filter', 'dataService', 
    function ($scope, $filter, dataService) {

    $scope.from = new Date();
    $scope.from.setMonth(new Date().getMonth() - 1);
    $scope.to = new Date();
    $scope.weekAgo = new Date();
    $scope.weekAgo.setDate(new Date().getDate() - 7);
    
    $scope.rows = {
            marketStatisticsChart: [],
            groupsChart: [],
            hitsChart: [],
            rewardsChart: []
    };

    $scope.chartsOptions = {
            scaleType: 'allfixed',
            dateFormat: 'HH:mm MMMM dd, yyyy',
            zoomStartTime:  $scope.weekAgo,
            zoomEndTime:  $scope.to
        };

    $scope.marketStatisticsChart = {
            type: 'AnnotationChart',
            options: {
                scaleType: 'allfixed',
                scaleColumns: [1,2],
                dateFormat: 'HH:mm MMMM dd, yyyy',
                zoomStartTime:  $scope.weekAgo,
                zoomEndTime:  $scope.to
                },
            data: {"cols": [
                            {label: "Date", type: "date"},
                            {label: "HITs available", type: "number"},
                            {label: "HIT groups available", type: "number"}],
                            "rows": []}
    };
    
    $scope.groupsChart = {
            type: 'AnnotationChart',
            options: $scope.chartsOptions,
            data: {"cols": [
                            {id: "id1", label: "Date", type: "date"},
                            {id: "id2", label: "HIT groups posted", type: "number"}],
                            "rows": []}
    };

    $scope.hitsChart = {
            type: 'AnnotationChart',
            options: $scope.chartsOptions,
            data: {"cols": [
                            {id: "id1", label: "Date", type: "date"},
                            {id: "id2", label: "HITs posted", type: "number"}],
                            "rows": []}
    };
    
    $scope.rewardsChart = {
            type: 'AnnotationChart',
            options: $scope.chartsOptions,
            data: {"cols": [
                            {id: "id1", label: "Date", type: "date"},
                            {id: "id2", label: "Rewards posted", type: "number"}],
                            "rows": []}
    };

    $scope.$watch('from+to', function(newValue, oldValue) {
        if($scope.from && $scope.to){
            $scope.load();
        }
    });
    
    $scope.charts = ['marketStatisticsChart', 'groupsChart', 'hitsChart', 'rewardsChart'];
    $scope.drawnCharts = [];
    $scope.visibleChart = 'marketStatisticsChart';
    $scope.activePill = 'marketStatisticsChartPill';
    
    $scope.load = function(){
        $scope.chartsOptions.zoomEndTime = $scope.to;
        dataService.load($filter('date')($scope.from, 'MM/dd/yyyy'), $filter('date')($scope.to, 'MM/dd/yyyy'), function(response){
            $scope.rows.marketStatisticsChart = [];
            $scope.rows.groupsChart = [];
            $scope.rows.hitsChart = [];
            $scope.rows.rewardsChart = [];
            angular.forEach(response.hourly, function(item){
                $scope.rows.marketStatisticsChart.push({c:[{v: new Date(item.from)}, {v: item.hitsAvailableUI}, {v: item.hitGroupsAvailableUI}]});
                $scope.rows.groupsChart.push({c:[{v: new Date(item.from)}, {v: parseInt(item.hitGroupsArrived)}]});
                $scope.rows.hitsChart.push({c:[{v: new Date(item.from)}, {v: parseInt(item.hitsArrived)}]});
                $scope.rows.rewardsChart.push({c:[{v: new Date(item.from)}, {v: parseInt(item.rewardsArrived) / 100}]});
            });
            $scope.drawnCharts = [];
            $scope.draw($scope.visibleChart);
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