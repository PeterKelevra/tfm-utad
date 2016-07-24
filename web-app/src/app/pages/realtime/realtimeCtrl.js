(function () {
    'use strict';

    angular.module('BlurAdmin.pages.realtime')
    .controller('RealTimeCtrl', RealTimeCtrl);


    /** @ngInject */
    function RealTimeCtrl($scope, $timeout, $interval, $sce, lodash, meetupStream, stopableInterval, isoCountries, GRAFANA) {
        console.log("RealTimeCtrl");

        $scope.pieChartOptions = {
            segmentShowStroke: false
        };

        $scope.responsesBySecondSrc = $sce.trustAsResourceUrl(GRAFANA.URL + 'dashboard-solo/db/rsvps-realtime?panelId=1');
        $scope.responsesByCountry = {};

        $timeout(refreshCharts, 1000);
        stopableInterval.start($interval, refreshCharts, 3000);

        function refreshCharts() {
            var countryResponses = meetupStream.getCountryCounters();
            var cityResponses = meetupStream.getCityCounters();

            var labels = lodash.takeRight(lodash.orderBy(lodash.keys(countryResponses), function (country) {
                return countryResponses[country].total;
            }), 10);

            var cityLabels = lodash.takeRight(lodash.orderBy(lodash.keys(cityResponses), function (city) {
                return cityResponses[city].total;
            }), 10);

            var values = lodash.map(labels, function (country) {
                return countryResponses[country].total;
            });
            var yesValues = lodash.map(labels, function (country) {
                return countryResponses[country].yes;
            });
            var noValues = lodash.map(labels, function (country) {
                return countryResponses[country].no;
            });

            $scope.responsesByCountry.pieLabels = labels;
            $scope.responsesByCountry.pieData = values;
            $scope.responsesByCountry.labels = labels;
            $scope.responsesByCountry.data = [yesValues, noValues];
            $scope.responsesByCountry.series = ["yes", "no"];

            //Devolvemos el nombre del país para mostrar en la tabla
            $scope.countriesTableData = lodash.map(labels, function (country) {
                var counters = countryResponses[country];
                counters.country = country;
                counters.countryName = isoCountries.getName(country);
                return counters;
            });

            $scope.citiesTableData = lodash.map(cityLabels, function (city) {
                var counters = cityResponses[city];
                counters.city = city;
                return counters;
            });


            $scope.tableData = lodash.map(labels, function (country) {
                var counters = countryResponses[country];
                counters.country = country;
                counters.countryName = isoCountries.getName(country);
                return counters;
            });


            var responses = meetupStream.getResponses();
            $scope.numCountries = lodash.keys(countryResponses).length;
            $scope.numCountries = lodash.keys(countryResponses).length;
            $scope.numCities = lodash.keys(cityResponses).length;

            $scope.numResponses = responses.length;
            var groupByResponse = lodash.groupBy(responses, "response");

            $scope.yesNoLabels = lodash.orderBy(lodash.keys(groupByResponse), _.identity, "desc");
            $scope.yesNoData = lodash.map($scope.yesNoLabels, function (key) {
                return groupByResponse[key].length;
            });
        }

    }

})
();
