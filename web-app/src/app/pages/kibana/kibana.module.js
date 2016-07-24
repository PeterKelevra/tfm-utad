(function () {
    'use strict';

    angular.module('BlurAdmin.pages.kibana', [
        'BlurAdmin.pages.kibana.groups',
        'BlurAdmin.pages.kibana.events',
        'BlurAdmin.pages.kibana.rsvps'
    ])
    .config(routeConfig);

    /** @ngInject */
    function routeConfig($stateProvider) {
        $stateProvider
        .state('kibana', {
            url: '/kibana',
            template: '<ui-view></ui-view>',
            abstract: true,
            title: 'Dashboards',
            sidebarMeta: {
                icon: 'ion-monitor',
                order: 100,
            },
        });
    }

})();
