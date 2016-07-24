(function () {
    'use strict';

    angular.module('BlurAdmin.pages.events', [])
    .config(routeConfig);

    /** @ngInject */
    function routeConfig($stateProvider) {
        $stateProvider
        .state('events', {
            url: '/events',
            templateUrl: 'app/pages/events/events.html',
            controller: "EventsCtrl",
            controllerAs: "eventCtrl",
            title: 'Meetup Events',
            sidebarMeta: {
                icon: 'ion-calendar',
                order: 800,
            },
        });
    }

})();
