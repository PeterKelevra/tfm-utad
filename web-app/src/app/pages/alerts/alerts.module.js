(function () {
    'use strict';

    angular.module('BlurAdmin.pages.alerts', [])
    .config(routeConfig);

    /** @ngInject */
    function routeConfig($stateProvider) {
        $stateProvider
        .state('alerts', {
            url: '/alerts',
            templateUrl: 'app/pages/alerts/alerts.html',
            controller: "AlertsCtrl",
            controllerAs: "alertsCtrl",
            title: 'Rsvps Alerts',
            sidebarMeta: {
                icon: 'ion-android-notifications',
                order: 800,
            },
        });
    }

})();
