(function () {
    'use strict';

    angular.module('BlurAdmin.pages.trending', [])
    .config(routeConfig);

    /** @ngInject */
    function routeConfig($stateProvider) {
        $stateProvider
        .state('trending', {
            url: '/trending',
            templateUrl: 'app/pages/trending/trending.html',
            controller: "TrendingCtrl",
            controllerAs: "trendingCtrl",
            title: 'Trending Events',
            sidebarMeta: {
                icon: 'ion-android-star',
                order: 800,
            },
        });
    }

})();
