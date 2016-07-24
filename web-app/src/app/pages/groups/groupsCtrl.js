(function () {
    'use strict';

    angular.module('BlurAdmin.pages.groups')
    .controller('GroupsCtrl', GroupsCtrl);

    /** @ngInject */
    function GroupsCtrl($scope, esClient, ES) {
        console.log("GroupsCtrl");

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
            label: "Created",
            value: "created"
        }, {
            label: "Members",
            value: "members"
        }, {
            label: "Category",
            value: "category.shortname"
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
                index: ES.GROUPS_INDEX,
                type: ES.GROUPS_TYPE,
                from: $scope.currentPage - 1,
                size: $scope.pageSize,
                body: {}
            };
            //Añadimos filtro country si se ha seleccionado
            if ($scope.countries && $scope.countries.selected) {
                console.log($scope.countries.selected);
                request.body.query = {
                    term: {localized_country_name: $scope.countries.selected.key}
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
                index: ES.GROUPS_INDEX,
                type: ES.GROUPS_TYPE,
                body: {
                    size: 0,
                    aggs: {
                        countries: {
                            terms: {
                                field: "localized_country_name",
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
