(function () {
    'use strict';

    angular.module('BlurAdmin.pages.prediction')
    .controller('PredictionCtrl', PredictionCtrl);

    /** @ngInject */
    function PredictionCtrl($scope, esClient, ES) {
        console.log("PredictionCtrl");

        $scope.currentPage = 1;
        $scope.pageSize = 20;
        $scope.maxSize = 20;


        $scope.loadContent = loadContent;
        loadContent();

        function loadContent() {
            //Mostramos solo eventos que tienen mas de 10 respuestas y que han sido predecidos
            var request = {
                index: ES.EVENTS_INDEX,
                type: ES.EVENTS_TYPE,
                from: $scope.currentPage - 1,
                size: $scope.pageSize,
                body: {
                    query: {
                        bool: {
                            must: [{
                                range: {
                                    yes_rsvp_count: {
                                        gte: 10
                                    }
                                }
                            }, {
                                "exists": {"field": "prediction_yes"}
                            }]
                        }
                    },
                    sort: [{"time": {"order": "desc"}}]
                }
            };

            esClient.search(request, function (resp) {
                console.log(resp);
                $scope.events = resp.hits;
                $scope.totalItems = $scope.events.total;
            });
        }


    }

})();
