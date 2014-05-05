google.load('visualization', '1.1', {
	'packages' : [ 'annotationchart' ]
});






function drawAnnotationChart(chart, data, options) {
	chart.draw(data, options);
}




$(function() {
	google.setOnLoadCallback(drawChart);
	
	var msChart = new google.visualization.AnnotationChart(document.getElementById('marketStatsChart'));
	var msChartData = new google.visualization.DataTable();
	msChartData.addColumn('datetime', 'Date');
	msChartData.addColumn('number', 'HITs available');
	msChartData.addColumn('number', 'HIT groups available');

	var groupsChart = new google.visualization.AnnotationChart(document.getElementById('groupsChart'));
	var groupsChartData = new google.visualization.DataTable();
	groupsChartData.addColumn('datetime', 'Date');
	groupsChartData.addColumn('number', 'HIT groups posted');

	var hitsChart = new google.visualization.AnnotationChart(document.getElementById('hitsChart'));
	var hitsChartData = new google.visualization.DataTable();
	hitsChartData.addColumn('datetime', 'Date');
	hitsChartData.addColumn('number', 'HITs posted');

	var rewardsChart = new google.visualization.AnnotationChart(document.getElementById('rewardsChart'));
	var rewardsChartData = new google.visualization.DataTable();
	rewardsChartData.addColumn('datetime', 'Date');
	rewardsChartData.addColumn('number', 'Rewards posted');
	
	$.datepicker.setDefaults($.extend({
		dateFormat : 'mm/dd/yy',
		showMonthAfterYear : false
	}));

	// datepickers
	$('#date_from').datepicker();
	$('#date_to').datepicker();

	var pastDate = new Date();
	pastDate.setDate(pastDate.getDate() - 7);

	var maxDate = new Date();
	maxDate.setDate(maxDate.getDate() + 1);

	$('#date_from').datepicker('setDate', pastDate);
	$('#date_to').datepicker('setDate', new Date());
	$('#date_from').datepicker('option', 'maxDate', maxDate);
	$('#date_to').datepicker('option', 'maxDate', maxDate);


	$('#date_from').on('change', function() {
		fillArrivalData(host, protocol);
		
		drawChart();
	});

	$('#date_to').on('change', function() {
		fillArrivalData(host, protocol);
		
		drawChart();
	});
	
	var activeTab = '#marketStats';

	// charts
	var options = {
		displayAnnotations : true,
		scaleType : 'allmaximized',
		scaleColumns : [ 0, 1 ]
	};
	
	$('#chartsTab a').click(function(e) {
		e.preventDefault();
		$(this).tab('show');
		activeTab = $(this).attr('href');

		drawChart();
	});


	
	var host = window.location.host;
	var protocol = host.indexOf('localhost', 0) == 0 ? 'http' : 'https';
	fillArrivalData(host, protocol);
	
	drawChart();
	
	function fillArrivalData(host, protocol) {
		var url = protocol + '://' + host + '/_ah/api/mturk/v1/arrivalCompletions/list?from=' + $("#date_from").val() + '&to=' + $("#date_to").val();

		$.ajax({type : 'GET',  url : url, dataType : 'json'})
			.done(function(response) {
				if (response.items) {
					$.each(response.items, function(index, item) {
						msChartData.addRows([ [new Date(item.from), item.hitsAvailableUI,	item.hitGroupsAvailableUI ] ]);	
						groupsChartData.addRows([ [ new Date(item.from), parseInt(item.hitGroupsArrived) ] ]);
						hitsChartData.addRows([ [ new Date(item.from),	parseInt(item.hitsArrived) ] ]);
						rewardsChartData.addRows([ [ new Date(item.from), parseInt(item.rewardsArrived) / 100 ] ]);
					});
				}
			})
			.fail(function(e) {
			})
			.complete(function(e) {
				drawChart();
			})
			;
	}
	
	function drawChart() {

		if (activeTab == '#marketStats') {
			drawAnnotationChart(msChart, msChartData, options);
		} else if (activeTab == '#hits') {
			drawAnnotationChart(hitsChart, hitsChartData, options);
		} else if (activeTab == '#rewards') {
			drawAnnotationChart(rewardsChart, rewardsChartData, options);
		} else if (activeTab == '#groups') {
			drawAnnotationChart(groupsChart, groupsChartData, options);
		}

	}


});


