angular.module('mturk').factory('dataService',	['$http', function($http) {

	return {
	    load: function(from, to, success, error) {
	        $http.get(this.getApiUrl() + '/arrivalCompletions/list?from=' + from + '&to=' + to)
	        .success(success).error(error);
	    },
	    toprequesters: function(success, error) {
	        $http.get(this.getApiUrl() + '/toprequester/list')
	        .success(success).error(error);
	    },
	    hitgroups: function(requesterId, success, error) {
            $http.get(this.getApiUrl() + '/hitgroup/listByRequesterId?requesterId=' + requesterId)
            .success(success).error(error);
        },
	    search: function(params, success, error) {
	        $http.get(this.getApiUrl() + '/hitgroup/search', {params: params}).success(success).error(error);
	    },
	    getApiUrl: function(){
			var host = window.location.host;
			var protocol = host.indexOf('localhost', 0) == 0 ? 'http' : 'https';
			var url = protocol + '://' + host + '/_ah/api/mturk/v1';
			
			return url;
	    }
	};
}]);