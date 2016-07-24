/**
 * @author v.lugovksy
 * created on 16.12.2015
 */
(function () {
    'use strict';

    angular.module('BlurAdmin.theme.components')
    .controller('MsgCenterCtrl', MsgCenterCtrl);

    /** @ngInject */
    function MsgCenterCtrl($scope, rsvpsAlerts) {

        $scope.notifications = rsvpsAlerts;

    }
})();
