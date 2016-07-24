(function () {
    'use strict';

    angular.module('BlurAdmin.pages')
    .constant('ES', {
        HOST: "sandbox:9200",
        RSVPS_INDEX: "meetup-rsvps",
        GROUPS_INDEX: "meetup-groups",
        CLUSTER_INDEX: "meetup-cluster",
        EVENTS_INDEX: "meetup-events",
        RSVPS_TYPE: "rsvps",
        GROUPS_TYPE: "groups",
        EVENTS_TYPE: "events"
    })
    .constant('SERVER', {
        URL: 'http://localhost:9000/',
        WS: 'ws://localhost:9000/',
        EVENT_ALERTS_TOPIC: 'event-alerts',
        TRENDING_TOPIC: 'trending-tops',
        RESPONSES_LIMIT: 'responses-limit'
    })
    .constant('GRAFANA', {
        URL: 'http://localhost:3001/'
    })
    .constant('KIBANA', {
        URL: 'http://localhost:5601/'
    });


})();
