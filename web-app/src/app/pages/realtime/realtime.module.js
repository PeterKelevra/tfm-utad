(function () {
    'use strict';

    angular.module('BlurAdmin.pages.realtime', [])
    .config(routeConfig);

    /** @ngInject */
    function routeConfig($stateProvider) {
        $stateProvider
        .state('realtime', {
            url: '/realtime',
            templateUrl: 'app/pages/realtime/realtime.html',
            controller: "RealTimeCtrl",
            controllerAs: "realtimeCtrl",
            title: 'RealTime',
            sidebarMeta: {
                icon: 'ion-android-home',
                order: 0,
            },
        });
    }

})();
