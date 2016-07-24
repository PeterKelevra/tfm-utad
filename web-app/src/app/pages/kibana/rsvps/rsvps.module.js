(function () {
    'use strict';

    angular.module('BlurAdmin.pages.kibana.rsvps', [])
    .config(routeConfig);

    /** @ngInject */
    function routeConfig($stateProvider) {
        $stateProvider
        .state('kibana.rsvps', {
            url: '/rsvps',
            templateUrl: 'app/pages/kibana/rsvps/rsvps.html',
            title: 'Rsvps Dashboard',
            controller: "KibanaRsvpsCtrl",
            controllerAs: "kibanaRsvpsCtrl",
            sidebarMeta: {
                order: 200,
            },
        });
    }

})();
