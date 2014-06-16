angular.module('mturk').factory('dataService', ['$http', '$cacheFactory', function($http, $cacheFactory) {

    var requestersCache = $cacheFactory('requesterCache');

	return {
	    load: function(from, to, success, error) {
	        $http.get(this.getApiUrl() + '/arrivalCompletions/list?from=' + from + '&to=' + to)
	        .success(success).error(error);
	    },
	    loadHitInstances: function(groupId, from, to, success, error) {
            $http.get(this.getApiUrl() + '/hitinstance/list?from=' + from + '&to=' + to + '&groupId=' + groupId)
            .success(success).error(error);
        },
	    toprequesters: function(success, error) {
	        $http.get(this.getApiUrl() + '/toprequester/list').success(function(response) {
                angular.forEach(response.items, function(requester){
                    requestersCache.put(requester.requesterId, requester);
                });

                if(angular.isFunction(success)){
                    success(response.items);
                }
            }).error(error);
	    },
	    getRequester: function(requesterId, success, error) {
            var requester = requestersCache.get(requesterId);
            if(!requester) {
                $http.get(this.getApiUrl() + '/requester?requesterId=' + requesterId).success(success).error(error);
            } else {
                if(angular.isFunction(success)){
                    success(requester);
                }
            }
	    },
	    hitgroup: function(groupId, success, error) {
	        $http.get(this.getApiUrl() + '/hitgroup/getByGroupId?groupId=' + groupId)
	        .success(success).error(error);
	    },
	    hitgroups: function(requesterId, success, error) {
            $http.get(this.getApiUrl() + '/hitgroup/listByRequesterId?days=30&requesterId=' + requesterId)
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