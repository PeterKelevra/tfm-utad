(function () {
    'use strict';


    angular.module('BlurAdmin.theme')
    .factory('rsvpsAlerts', function ($websocket, SERVER) {
        // Open a WebSocket connection
        var dataStream = $websocket(SERVER.WS + SERVER.EVENT_ALERTS_TOPIC);

        var messages = [],
            unread   = [];

        dataStream.onMessage(function (message) {
            if (message.data && message.data.length) {
                var message = JSON.parse(message.data);
                message.time = new Date();
                messages.unshift(message);
                unread.unshift(message);
            }
        });

        return {
            unread: unread,
            messages: messages,
            get: function () {
                dataStream.send(JSON.stringify({action: 'get'}));
            },
            resetUnread: function () {
                unread.splice(0, unread.length);
            }
        };

    }).factory('trendingTops', function ($websocket, lodash, SERVER) {
        // Open a WebSocket connection
        var dataStream = $websocket(SERVER.WS + SERVER.TRENDING_TOPIC);

        var eventsByCountry = {};

        dataStream.onMessage(function (message) {
            console.log(">>> trendingTops", message);
            if (message.data && message.data.length) {
                var events = JSON.parse(message.data);
                angular.extend(eventsByCountry, lodash.groupBy(events, "country"));
            }
        });

        return {
            eventsByCountry: eventsByCountry
        };

    });

})();

