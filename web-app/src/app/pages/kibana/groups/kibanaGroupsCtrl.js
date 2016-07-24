(function () {
    'use strict';

    angular.module('BlurAdmin.pages.kibana')
    .controller('KibanaGroupsCtrl', KibanaGroupsCtrl);

    /** @ngInject */
    function KibanaGroupsCtrl($scope, $sce, KIBANA) {
        $scope.iframeSrc = $sce.trustAsResourceUrl(KIBANA.URL + 'goto/6272be8f134e940bf2c337797a98eafc');
    }
})();
