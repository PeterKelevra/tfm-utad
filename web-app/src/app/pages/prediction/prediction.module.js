(function () {
    'use strict';

    angular.module('BlurAdmin.pages.prediction', [])
    .config(routeConfig);

    /** @ngInject */
    function routeConfig($stateProvider) {
        $stateProvider
        .state('prediction', {
            url: '/prediction',
            templateUrl: 'app/pages/prediction/prediction.html',
            controller: "PredictionCtrl",
            controllerAs: "predictionCtrl",
            title: 'Event Prediction',
            sidebarMeta: {
                icon: 'ion-calendar',
                order: 800,
            },
        });
    }

})();
