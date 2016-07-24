(function () {
    'use strict';

    angular.module('BlurAdmin.pages.kibana.events', [])
    .config(routeConfig);

    /** @ngInject */
    function routeConfig($stateProvider) {
        $stateProvider
        .state('kibana.events', {
            url: '/events',
            templateUrl: 'app/pages/kibana/events/events.html',
            controller: "KibanaEventsCtrl",
            controllerAs: "kibanaEventsCtrl",
            title: 'Events Dashboard',
            sidebarMeta: {
                order: 200,
            },
        });
    }

})();
