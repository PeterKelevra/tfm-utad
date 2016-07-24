(function () {
    'use strict';

    angular.module('BlurAdmin.pages.alerts')
    .controller('TrendingCtrl', TrendingCtrl);

    /** @ngInject */
    function TrendingCtrl($scope, isoCountries, trendingTops) {
        console.log("TrendingCtrl", isoCountries, trendingTops);

        $scope.isoCountries = isoCountries;
        $scope.trendingTops = trendingTops;
    }

})();
