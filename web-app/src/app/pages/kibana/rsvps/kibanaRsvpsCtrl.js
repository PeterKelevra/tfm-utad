(function () {
    'use strict';

    angular.module('BlurAdmin.pages.kibana')
    .controller('KibanaRsvpsCtrl', KibanaRsvpsCtrl);

    /** @ngInject */
    function KibanaRsvpsCtrl($scope, $sce, KIBANA) {
        $scope.iframeSrc = $sce.trustAsResourceUrl(KIBANA.URL + 'goto/a6bdb403cb7a037661955383ad0a3983');
    }
})();
