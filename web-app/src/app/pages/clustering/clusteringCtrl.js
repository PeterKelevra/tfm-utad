(function () {
    'use strict';

    angular.module('BlurAdmin.pages.clustering')
    .controller('ClusteringCtrl', ClusteringCtrl);

    /** @ngInject */
    function ClusteringCtrl($scope, esClient, $uibModal, lodash, ES) {
        console.log("ClusteringCtrl");

        $scope.data = lodash.map(lodash.range(10), function (index) {
            return {
                title: 'Cluster ' + (index + 1),
                img: 'assets/img/app/clustering/cluster-' + index + '.svg'
            };
        });

        $scope.pageSize = 8;
        $scope.maxSize = 10;

        $scope.open = function (clusterId) {
            console.log(">>> open", clusterId);
            $scope.currentPage = 1;
            $scope.clusterId = clusterId;
            $uibModal.open({
                animation: true,
                scope: $scope,
                templateUrl: 'app/pages/clustering/clusteringGroups.html',
                size: 'lg',
                resolve: {
                    items: loadClusterGroups
                }
            });
        };

        $scope.pageChanged = function (page) {
            $scope.currentPage = page;
            loadClusterGroups();
        }

        function loadClusterGroups() {
            console.log(">>> loadClusterGroups. currentPage", $scope.currentPage);
            var request = {
                index: ES.CLUSTER_INDEX,
                type: ES.GROUPS_TYPE,
                from: $scope.currentPage - 1,
                size: $scope.pageSize,
                body: {}
            };
            //Añadimos filtro country si se ha seleccionado
            request.body.query = {
                term: {clusterId: $scope.clusterId}
            }

            request.body.sort = [{distance: 'asc'}];
            console.log(request);

            return esClient.search(request, function (resp) {
                console.log(resp);
                $scope.groups = resp.hits;
                $scope.totalItems = $scope.groups.total;
            });
        }

    }

})();
