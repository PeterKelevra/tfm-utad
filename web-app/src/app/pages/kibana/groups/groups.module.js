(function () {
    'use strict';

    angular.module('BlurAdmin.pages.kibana.groups', [])
    .config(routeConfig);

    /** @ngInject */
    function routeConfig($stateProvider) {
        $stateProvider
        .state('kibana.groups', {
            url: '/groups',
            templateUrl: 'app/pages/kibana/groups/groups.html',
            title: 'Groups Dashboard',
            controller: "KibanaGroupsCtrl",
            sidebarMeta: {
                order: 200,
            },
        });
    }

})();
