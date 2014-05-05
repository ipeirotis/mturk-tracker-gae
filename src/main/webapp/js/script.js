google.load('visualization', '1.1', {
	'packages' : [ 'annotationchart' ]
});

$(function() {
	google.setOnLoadCallback(drawChart);
	


	var activeTab = '#marketStats';

	$('#chartsTab a').click(function(e) {
		e.preventDefault();
		$(this).tab('show');
		activeTab = $(this).attr('href');

		if ($(this).attr('href') == '#marketStats') {
			drawMsChart();
		}

		if ($(this).attr('href') == '#rewards') {
			drawRewardsChart();
		}

		if ($(this).attr('href') == '#hits') {
			drawHitsChart();
		}

		if ($(this).attr('href') == '#groups') {
			drawGroupsChart();
		}
	});

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
		drawChart();
	});

	$('#date_to').on('change', function() {
		drawChart();
	});

	// charts
	var options = {
		displayAnnotations : true,
		scaleType : 'allmaximized',
		scaleColumns : [ 0, 1 ]
	};

	var msChart;// marketStatistics chart
	var msChartData = new google.visualization.DataTable();
	msChartData.addColumn('datetime', 'Date');
	msChartData.addColumn('number', 'HITs available');
	msChartData.addColumn('number', 'HIT groups available');

	var groupsChart;
	var groupsChartData = new google.visualization.DataTable();
	groupsChartData.addColumn('datetime', 'Date');
	groupsChartData.addColumn('number', 'HIT groups posted');

	var hitsChart;
	var hitsChartData = new google.visualization.DataTable();
	hitsChartData.addColumn('datetime', 'Date');
	hitsChartData.addColumn('number', 'HITs posted');

	var rewardsChart;
	var rewardsChartData = new google.visualization.DataTable();
	rewardsChartData.addColumn('datetime', 'Date');
	rewardsChartData.addColumn('number', 'Rewards posted');
	
	var host = window.location.host;
	var protocol = host.indexOf('localhost', 0) == 0 ? 'http' : 'https';
	fillArrivalData(host, protocol);
	fillStatisticsData(host, protocol, null);

	
	function fillArrivalData(host, protocol) {
		var url = protocol + '://' + host
		+ '/_ah/api/mturk/v1/arrivalCompletions/list?from='
		+ $("#date_from").val() + '&to=' + $("#date_to").val();

		$.ajax({type : 'GET',  url : url, dataType : 'json'})
			.done(function(response) {
		
					if (response.items) {
						$.each(response.items, function(index, item) {
							groupsChartData.addRows([ [ new Date(item.from),
									parseInt(item.hitGroupsArrived) ] ]);
							hitsChartData.addRows([ [ new Date(item.from),
									parseInt(item.hitsArrived) ] ]);
							rewardsChartData.addRows([ [ new Date(item.from),
									parseInt(item.rewardsArrived) / 100 ] ]);
		
						});
					}
					drawChart();
		

				}).fail(function(e) {
			// TODO
		});
	}
	
	function fillStatisticsData(host, protocol, cursor) {
		var url = protocol + '://' + host
		+ '/_ah/api/mturk/v1/marketStatistics/list?from='
		+ $("#date_from").val() + '&to=' + $("#date_to").val();
		
		if (cursor) {
			url = url +'&cursor=' + cursor;
		}

			$.ajax({ type : 'GET',	url : url,	dataType : 'json' })
			.done(function(response) {

						if (response.items) {
							$.each(response.items, function(index, item) {
								msChartData.addRows([ [new Date(item.timestamp), parseInt(item.hitsAvailable),	parseInt(item.hitGroupsAvailable) ] ]);	});
						}

						if (response.nextPageToken) {
							fillStatisticsData(host, protocol, response.nextPageToken);
						} else{
							drawChart();
						}

					}).fail(function(e) {
				// TODO
			});

	}
	
	function drawChart() {

		if (activeTab == '#marketStats') {
			drawMsChart();
		} else if (activeTab == '#hits') {
			drawHitsChart();
		} else if (activeTab == '#rewards') {
			drawRewardsChart();
		} else if (activeTab == '#groups') {
			drawGroupsChart();
		}
		

	}

	function drawMsChart() {
		if (!msChart) {
			msChart = new google.visualization.AnnotationChart(document
					.getElementById('marketStatsChart'));
		}
		msChart.draw(msChartData, options);
	}

	function drawHitsChart() {
		if (!hitsChart) {
			hitsChart = new google.visualization.AnnotationChart(document
					.getElementById('hitsChart'));
		}
		hitsChart.draw(hitsChartData, options);
	}

	function drawGroupsChart() {
		if (!groupsChart) {
			groupsChart = new google.visualization.AnnotationChart(document
					.getElementById('groupsChart'));
		}
		groupsChart.draw(groupsChartData, options);
	}

	function drawRewardsChart() {
		if (!rewardsChart) {
			rewardsChart = new google.visualization.AnnotationChart(document
					.getElementById('rewardsChart'));
		}
		rewardsChart.draw(rewardsChartData, options);
	}
});