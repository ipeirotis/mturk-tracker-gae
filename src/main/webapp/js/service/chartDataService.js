angular.module('mturk').factory('chartDataService',	['$http', function($http) {

	return {
	    load: function(from, to, success, error) {
	        $http.get(this.getApiUrl() + '?from=' + from + '&to=' + to).success(success).error(error);
	    },
	    getApiUrl: function(){
			var host = window.location.host;
			var protocol = host.indexOf('localhost', 0) == 0 ? 'http' : 'https';
			var url = protocol + '://' + host + '/_ah/api/mturk/v1/arrivalCompletions/list';
			
			return url;
	    }
	};
}]);