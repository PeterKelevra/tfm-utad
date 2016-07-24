(function () {
    'use strict';

    angular.module('BlurAdmin.pages.events')
    .controller('EventsCtrl', EventsCtrl);

    /** @ngInject */
    function EventsCtrl($scope, esClient, ES) {
        console.log("EventsCtrl");

        $scope.pageSize = 10;
        $scope.country = "usa";
        $scope.currentPage = 1;
        $scope.maxSize = 10;
        $scope.bigTotalItems = 175;
        $scope.bigCurrentPage = 1;

        $scope.sortFields = [{
            label: "Name",
            value: "name"
        }, {
            label: "Date",
            value: "time"
        }, {
            label: "Members",
            value: "group.members"
        }, {
            label: "Confirmations",
            value: "yes_rsvp_count"
        }, {
            label: "Category",
            value: "group.category.shortname"
        }];

        $scope.sort = "asc";

        $scope.loadContent = loadContent;

        $scope.resetFilter = function () {
            $scope.countries.selected = null;
            loadContent();
        }

        $scope.applyFitler = function () {
            $scope.currentPage = 1;
            loadContent();
        }


        loadCountries();
        loadContent();

        function loadContent() {
            var request = {
                index: ES.EVENTS_INDEX,
                type: ES.EVENTS_TYPE,
                from: $scope.currentPage - 1,
                size: $scope.pageSize,
                body: {}
            };
            //Añadimos filtro country si se ha seleccionado
            if ($scope.countries && $scope.countries.selected) {
                console.log($scope.countries.selected);
                request.body.query = {
                    term: {"group.localized_country_name": $scope.countries.selected.key}
                }
            }
            //Añadimos sort si se ha seleccionado
            if ($scope.sortFields.selected) {
                var sort = {};
                sort[$scope.sortFields.selected.value] = {order: $scope.sort};
                request.body.sort = [sort];
            }
            console.log(request);

            esClient.search(request, function (resp) {
                console.log(resp);
                $scope.groups = resp.hits;
                $scope.totalItems = $scope.groups.total;
            });
        }

        function loadCountries() {
            //Obtenemos los distintos paises de los grupos.
            var request = {
                index: ES.EVENTS_INDEX,
                type: ES.EVENTS_TYPE,
                body: {
                    size: 0,
                    aggs: {
                        countries: {
                            terms: {
                                field: "group.localized_country_name",
                                size:20
                            }
                        }
                    }
                }
            };
            esClient.search(request, function (resp) {
                console.log(resp);
                $scope.countries = resp.aggregations.countries.buckets;
            });
        }

    }

})();
