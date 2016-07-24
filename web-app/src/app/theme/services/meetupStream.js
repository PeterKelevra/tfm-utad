(function () {
    'use strict';

    angular.module('BlurAdmin.theme')
    .service('meetupStream', function ($websocket) {

        var responses       = [],
            countryCounters = {},
            cityCounters    = {};

        var dataStream = $websocket("ws://stream.meetup.com/2/rsvps");

        dataStream.onMessage(function (message) {

            if (message.data && message.data.length) {
                var rsvp = JSON.parse(message.data);
                responses.push(rsvp);

                var currentValues = countryCounters[rsvp.group.group_country] || {total: 0, yes: 0, no: 0};
                var cityKey = rsvp.group.group_city + " (" + rsvp.group.group_country + ")";
                var cityCurrentValues = cityCounters[cityKey] || {total: 0, yes: 0, no: 0};
                currentValues.total++;
                cityCurrentValues.total++;
                if (rsvp.response == "yes") {
                    currentValues.yes++
                    cityCurrentValues.yes++;
                } else {
                    currentValues.no++;
                    cityCurrentValues.no++;
                }
                countryCounters[rsvp.group.group_country] = currentValues;
                cityCounters[cityKey] = cityCurrentValues;
            }
        });

        return {
            getResponses: function () {
                return responses;
            },
            getCountryCounters: function () {
                return countryCounters;
            },
            getCityCounters: function () {
                return cityCounters;
            }
        }
    });

})();

