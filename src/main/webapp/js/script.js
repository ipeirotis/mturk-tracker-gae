google.load('visualization', '1.1', {'packages':['annotationchart']});

$(function () {
	google.setOnLoadCallback(drawChart);

	var activeTab = '#hits';

	$('#chartsTab a').click(function (e) {
		e.preventDefault();
		$(this).tab('show');
		activeTab = $(this).attr('href');

		if($(this).attr('href') == '#marketStats'){
			drawMsChart();
		}
		
		if($(this).attr('href') == '#rewards'){
			drawRewardsChart();
		}

		if($(this).attr('href') == '#hits'){
			drawHitsChart();
		}
	});

	$.datepicker.setDefaults($.extend(
			{
				dateFormat: 'mm/dd/yy',
				showMonthAfterYear: false
			}
	));

	//datepickers
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

	$('#date_from').on('change', function() { drawChart(); });

	$('#date_to').on('change', function() { drawChart(); });

	//charts
	var options = {
			displayAnnotations: true
	};
	
	var msChart;//marketStatistics chart
	var msChartData = new google.visualization.DataTable();
	msChartData.addColumn('date', 'Date');
	msChartData.addColumn('number', 'HITs available');
	msChartData.addColumn('number', 'HIT groups available');

	var hitsChart;
	var hitsChartData = new google.visualization.DataTable();
	hitsChartData.addColumn('date', 'Date');
	hitsChartData.addColumn('number', 'Hits');

	var rewardsChart;
	var rewardsChartData = new google.visualization.DataTable();
	rewardsChartData.addColumn('date', 'Date');
	rewardsChartData.addColumn('number', 'Rewards');

	function drawChart() {
		var host = window.location.host;
		var protocol = host.indexOf('localhost', 0) == 0 ? 'http' : 'https';
		var url = protocol + '://' + host + '/_ah/api/mturk/v1/arrivalCompletions/list?from=' +
		$("#date_from").val() + '&to=' + $("#date_to").val();

		$.ajax({
			type: 'GET',
			url: url,
			dataType: 'json'})
			.done(function(response) {

				if(response.items){
					$.each(response.items, function( index, item ) {
						hitsChartData.addRows([[new Date(item.from), parseInt(item.hitsArrived)]]);
						rewardsChartData.addRows([[new Date(item.from), parseInt(item.rewardsArrived)]]);
					});
				}

				if(activeTab == '#marketStats'){
					drawMsChart();
				} else if(activeTab == '#hits'){
					drawHitsChart();
				} else if(activeTab == '#rewards'){
					drawRewardsChart();
				}
			})
			.fail(function(e) {
				//TODO
			});

	}
	
	function drawMsChart(){
		if(!msChart){
			msChart = new google.visualization.AnnotationChart(document.getElementById('marketStatsChart'));
		}
		msChart.draw(msChartData, options);
	}

	function drawHitsChart(){
		if(!hitsChart){
			hitsChart = new google.visualization.AnnotationChart(document.getElementById('hitsChart'));
		}
		hitsChart.draw(hitsChartData, options);
	}

	function drawRewardsChart(){
		if(!rewardsChart){
			rewardsChart = new google.visualization.AnnotationChart(document.getElementById('rewardsChart'));
		}
		rewardsChart.draw(rewardsChartData, options);
	}
});