(function () {
    'use strict';

    angular.module('BlurAdmin.pages.groups', [])
    .config(routeConfig);

    /** @ngInject */
    function routeConfig($stateProvider) {
        $stateProvider
        .state('groups', {
            url: '/groups',
            templateUrl: 'app/pages/groups/groups.html',
            controller: "GroupsCtrl",
            controllerAs: "groupsCtrl",
            title: 'Meetup Groups',
            sidebarMeta: {
                icon: 'ion-android-contacts',
                order: 800,
            },
        });
    }

})();
