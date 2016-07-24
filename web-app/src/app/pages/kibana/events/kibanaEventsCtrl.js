(function () {
    'use strict';

    angular.module('BlurAdmin.pages.kibana')
    .controller('KibanaEventsCtrl', KibanaEventsCtrl);

    /** @ngInject */
    function KibanaEventsCtrl($scope, $sce, KIBANA) {
        $scope.iframeSrc = $sce.trustAsResourceUrl(KIBANA.URL + 'goto/06005ba2c94b3df3bf448405755a1bd6');
    }
})();
