(function () {
    'use strict';

    angular.module('BlurAdmin.pages.clustering', [])
    .config(routeConfig);

    /** @ngInject */
    function routeConfig($stateProvider) {
        $stateProvider
        .state('clustering', {
            url: '/clustering',
            templateUrl: 'app/pages/clustering/clustering.html',
            controller: "ClusteringCtrl",
            controllerAs: "ClusteringCtrl",
            title: 'Group Clusters',
            sidebarMeta: {
                icon: 'ion-ios-keypad',
                order: 800,
            },
        });
    }

})();
