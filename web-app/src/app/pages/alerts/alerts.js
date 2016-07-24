(function () {
    'use strict';

    angular.module('BlurAdmin.pages.alerts')
    .controller('AlertsCtrl', AlertsCtrl);

    /** @ngInject */
    function AlertsCtrl($scope, $http, $uibModal, rsvpsAlerts, esClient, ES, SERVER) {
        console.log("AlertsCtrl", rsvpsAlerts);

        $scope.notifications = rsvpsAlerts;
        //Valor por defecto
        $scope.limit = 5;

        $http({
            method: 'GET',
            url: SERVER.URL + SERVER.RESPONSES_LIMIT
        }).then(function successCallback(response) {
            console.log(response);
            $scope.limit = response.data;
        }, function errorCallback(response) {
        });

        $scope.changeResponsesLimit = function (slider) {
            console.log("changeResponsesLimit", slider.from, $scope.limit);
            $http({
                method: 'POST',
                url: SERVER.URL + SERVER.RESPONSES_LIMIT + '/' + slider.from
            }).then(function successCallback(response) {
                console.log(response);
                $scope.limit = response.data;
            }, function errorCallback(response) {
            });

        }

        $scope.showEvent = function (eventId) {
            console.log(">>> showEvent", eventId);
            $uibModal.open({
                animation: true,
                scope: $scope,
                templateUrl: 'app/pages/events/eventDetail.html',
                size: 'lg',
                resolve: {
                    event: function () {
                        return esClient.client.get({
                            index: ES.EVENTS_INDEX,
                            type: ES.EVENTS_TYPE,
                            id: eventId,
                        }, function (error, response) {
                            $scope.event = response._source;
                        });
                    }
                }
            });
        };


    }

})();
