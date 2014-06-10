angular.module('mturk').filter('ceil', function() {
    return function(input) {
        return Math.ceil(input);
    };
});