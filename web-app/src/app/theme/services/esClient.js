(function () {
    'use strict';

    angular.module('BlurAdmin.theme')
    .service('esClient', function (esFactory, ES) {
        var client = esFactory({
            host: ES.HOST
        });

        return {
            search: function (request, callback) {
                client.search(request).then(function (resp) {
                    callback && callback(resp);
                }, function (err) {
                    console.trace(err.message);
                })
            },
            client: client
        }
    });

})();

