var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, translator, C, dc) {

    views.RoutingSidebar = Marionette.Layout.extend({

        template: Handlebars.getTemplate('app', 'sidebar-routing'),

        regions: {
            saveRouteContainer: "#saveRouteContainer",
            bookmarkRouteContainer: "#bookmarkRouteContainer",
            exportDataContainer: "#exportDataContainer",
        },

        events : {
            'click #resetRoute' : 'resetRoute',
            'click #resetRoute2' : 'resetRoute',
            'click #resetRoute3' : 'resetRoute',
            'change #day' : '',
            'change #hour' : 'getRoute',
            'change #week1ToList' : 'changeToWeek',
            'change #week2ToList' : 'changeToWeek',
            'change #week1FromList' : 'changeFromWeek',
            'change #week2FromList' : 'changeFromWeek',
            'click #toggleFilters' : 'toggleFilters',
            'click #toggleFilters2' : 'toggleFilters',
            'click #compare' : 'clickCompare',
            'change #confidenceInterval' : 'changeConfidenceInterval',
            'change #normalizeByTime' : 'changeNormalizeBy',
            'click #analyzeByTime' : 'analyzeByTime',
            'click #returnToOverall' : 'returnToOverall',
            'click #returnToOverall1' : 'returnToOverall'
        },

        loadRouteFromUrl : function(routeId) {
            $.getJSON('/route/' + routeId, function(data) {
                if(data.json){
                    //user's saved route
                    A.app.sidebar.loadRoute(data.json);
                }else{
                    //anonymous bookmark
                    A.app.sidebar.loadRoute(data);
                }
            });
        },

        loadRoute : function(params) {

            A.app.sidebarTabs.resetRoute();
            A.app.map.fitBounds(new L.LatLngBounds(
                new L.LatLng(params.bounds._southWest.lat,params.bounds._southWest.lng),
                new L.LatLng(params.bounds._northEast.lat,params.bounds._northEast.lng)));

            for(var i = 0; i < params.routePoints.length; i++){
                var evt = {};
                evt.latlng = {
                    lat: params.routePoints[i].lat,
                    lng: params.routePoints[i].lng
                };
                if(i == params.routePoints.length -1){
                    var callback = function() {
                        A.app.sidebar.routeRendered(params);
                    };
                    A.app.sidebarTabs.onMapClick(evt, false, callback);
                }else{
                    A.app.sidebarTabs.onMapClick(evt, true);
                }
            }
        },

        routeRendered: function(params){
            if(params.hourExtent || params.dayExtent){
                this.toggleFilters(null, true);
                this.hourlyChart.filter(dc.filters.RangedFilter(params.hourExtent[0], params.hourExtent[1]));
                this.dailyChart.filter(dc.filters.RangedFilter(params.dayExtent[0], params.dayExtent[1]));
                dc.renderAll();
            }
            if(params.week1FromList){
                $("#week1FromList").val(params.week1FromList);
                this.changeFromWeek();
                if(params.week1ToList){
                    $("#week1ToList").val(params.week1ToList);
                }
                this.changeToWeek();
                if(params.week2FromList){
                    var that = this;
                    var callback = function() {
                        that.clickCompareCallback(params)
                    };
                    this.$("#compare").prop( "checked", true );
                    this.clickCompare(callback)
                }

            }
        },

        clickCompareCallback: function(params){
            $("#week2FromList").val(params.week2FromList);
            this.changeFromWeek();
            if(params.week2ToList){
                $("#week2ToList").val(params.week2ToList);
                this.changeToWeek();
            }
        },

        toggleFilters : function(event, overrideState) {

            //reset the filter state
            this.hourlyChart.filterAll();
            this.dailyChart.filterAll();
            dc.renderAll();

            //flip the filter on/off
            var brushOn;
            if(overrideState != null){
                brushOn = overrideState;
            }else{
                A.app.sidebarTabs.filterEnabled = !A.app.sidebarTabs.filterEnabled;
                brushOn =A.app.sidebarTabs.filterEnabled;
            }

            this.dailyChart.brushOn(brushOn);
            this.hourlyChart.brushOn(brushOn);
            if(brushOn){
                $(event.target).html(translator.translate("filter_on"))
            }else{
                $(event.target).html(translator.translate("filter_off"))
                A.app.sidebarTabs.getRoute();
            }
            dc.renderAll();

            //reset the cached filter handle positions
            A.app.sidebar.hourExtent = A.app.sidebar.hourlyChart.brush().extent();
            A.app.sidebar.dayExtent = A.app.sidebar.dailyChart.brush().extent();

        },

        resetRoute : function() {
            $('#routeButtons').hide();
            $('#routeCompareNotes').hide();
            $('#analyzeByTimeButtons').hide();
            $('#returnToOverall').hide();
            $('#analyzeByTime').show();
            $('#selectedDateAndTime').hide();
            A.app.sidebarTabs.resetRoute();

            this.$('#routeCompareNotes').hide();

            //reset the filter state
            $("#toggleFilters").html(translator.translate("filter_off"))
            this.hourlyChart.filterAll();
            this.dailyChart.filterAll();
            this.dailyChart.brushOn(false);
            this.hourlyChart.brushOn(false);
            dc.redrawAll();
            window.history.pushState('', '', '?');
        },

        getRoute : function() {
            A.app.sidebarTabs.getRoute();
        },

        onShow : function() {
            var _this = this;
            this.$("#week1To").hide();
            this.$("#week2To").hide();
            this.$("#compareWeekSelector").hide();
            this.$("#percentChangeTitle1").hide();
            this.$("#percentChangeTitle2").hide();
            this.$("#percentChangeLegend").hide();
            this.$("#routeData").hide();


            $.getJSON('/weeks', function(data) {

                data.sort(function(a, b) {
                    return a.weekId - b.weekId;
                });

                A.app.sidebar.weekList = data;

                _this.$("#week1FromList").empty();
                _this.$("#week1ToList").empty();

                _this.$("#week2FromList").empty();
                _this.$("#week2ToList").empty();

                _this.$("#week1FromList").append('<option value="-1">' + translator.translate("all_weeks") + '</option>');
                for(var i in A.app.sidebar.weekList) {
                    var weekDate = new Date( data[i].weekStartTime);
                    _this.$("#week1FromList").append('<option value="' + data[i].weekId + '">' + translator.translate("week_of") + ' ' + (weekDate.getUTCMonth() + 1) + '/' + weekDate.getUTCDate() + '/' + weekDate.getUTCFullYear() + '</option>');
                }

                _this.$("#week2FromList").empty();
                _this.$("#week2FromList").append('<option value="-1">' + translator.translate("all_weeks") + '</option>');
                for(var i in A.app.sidebar.weekList) {
                    var weekDate = new Date( data[i].weekStartTime);
                    _this.$("#week2FromList").append('<option value="' + data[i].weekId + '">' + translator.translate("week_of") + ' ' + (weekDate.getUTCMonth() + 1) + '/' + weekDate.getUTCDate() + '/' + weekDate.getUTCFullYear() + '</option>');
                }


            });

            this.changeFromWeek();
        },

        onClose : function() {
            A.app.sidebarTabs.resetRoute();
        },

        changeFromWeek : function() {

            A.app.sidebar.filterChanged = true;

            this.$("#week1To").hide();
            this.$("#week2To").hide();

            if(this.$("#week1FromList").val() > 0) {
                this.$("#week1ToList").empty();
                for(var i in A.app.sidebar.weekList) {
                    if(A.app.sidebar.weekList[i].weekId >= this.$("#week1FromList").val()) {
                        var weekDate = new Date( A.app.sidebar.weekList[i].weekStartTime);
                        this.$("#week1ToList").append('<option value="' + A.app.sidebar.weekList[i].weekId + '">' + translator.translate("week_of") + ' ' + (weekDate.getUTCMonth() + 1) + '/' + weekDate.getUTCDate() + '/' + weekDate.getUTCFullYear() + '</option>');
                    }
                    this.$("#week1To").show();
                }
            }


            if(this.$("#week2FromList").val() > 0) {
                this.$("#week2ToList").empty();
                for(var i in A.app.sidebar.weekList) {
                    if(A.app.sidebar.weekList[i].weekId >= this.$("#week2FromList").val()) {
                        var weekDate = new Date( A.app.sidebar.weekList[i].weekStartTime);
                        this.$("#week2ToList").append('<option value="' + A.app.sidebar.weekList[i].weekId + '">' + translator.translate("week_of") + ' ' + (weekDate.getUTCMonth() + 1) + '/' + weekDate.getUTCDate() + '/' + weekDate.getUTCFullYear() + '</option>');
                    }
                    this.$("#week2To").show();
                }
            }


            this.update();
        },


        changeNormalizeBy: function() {

            A.app.sidebar.filterChanged = true;
            this.update();
        },

        changeConfidenceInterval : function() {

            A.app.sidebar.filterChanged = true;
            this.update();
        },

        changeToWeek : function() {

            A.app.sidebar.filterChanged = true;

            this.update();
        },


        clickCompare : function(callback) {
            if(typeof callback != "function")
                callback = null;

            A.app.sidebar.filterChanged = true;

            if(this.$("#compare").prop( "checked" )) {
                this.$("#compareWeekSelector").show();
                this.$("#percentChangeTitle1").show();
                this.$("#percentChangeTitle2").show();
                this.$("#percentChangeLegend").show();
                this.$("#speedLegend").hide();

                A.app.sidebar.percentChange = true;

                this.$('#routeCompareNotes').show();
            }
            else {
                this.$("#compareWeekSelector").hide();
                this.$("#percentChangeTitle1").hide();
                this.$("#percentChangeTitle2").hide();
                this.$("#percentChangeLegend").hide();
                this.$("#speedLegend").show();
                A.app.sidebar.percentChange = false;

                this.$('#routeCompareNotes').hide();
            }

            this.update(callback);
        },

        update : function(callback) {
            A.app.sidebar.filterChanged = true;

            if(!A.app.sidebar.hourlyChart)
                return;

            A.app.sidebar.hourExtent = A.app.sidebar.hourlyChart.brush().extent();
            A.app.sidebar.dayExtent = A.app.sidebar.dailyChart.brush().extent();

            var minHour, maxHour, minDay, maxDay;

            if(!A.app.sidebar.hourExtent || (A.app.sidebar.hourExtent[0] < 1 && A.app.sidebar.hourExtent[1] < 1)) {
                minHour = 1
                maxHour = 24;
            }
            else {
                minHour = Math.ceil(A.app.sidebar.hourExtent[0]);
                maxHour = Math.floor(A.app.sidebar.hourExtent[1]);
            }

            if(!A.app.sidebar.dayExtent || (A.app.sidebar.dayExtent[0] < 1 && A.app.sidebar.dayExtent[1] < 1)) {
                minDay = 1
                maxDay = 7;
            }
            else {
                minDay = Math.ceil(A.app.sidebar.dayExtent[0]);
                maxDay = Math.floor(A.app.sidebar.dayExtent[1]);
            }

            var hours = new Array();

            for(d = minDay; d <= maxDay; d++) {
                for(h = minHour; h <= maxHour; h++) {
                    hours.push(h + (24 * (d -1)));
                }
            }

            A.app.sidebarTabs.getRoute(hours, null, null, callback);
        },

        getWeek1List : function() {
            var wFrom = this.$("#week1FromList").val();

            var wList = new Array();

            if(wFrom == -1) {
                for(var i in A.app.sidebar.weekList) {
                    wList.push(A.app.sidebar.weekList[i].weekId );
                }
            } else {
                var wTo = this.$("#week1ToList").val();
                for(var i in A.app.sidebar.weekList) {
                    if(A.app.sidebar.weekList[i].weekId >= wFrom && A.app.sidebar.weekList[i].weekId <= wTo)
                        wList.push(A.app.sidebar.weekList[i].weekId);
                }

            }

            return wList;
        },

        getWeek2List : function() {
            var wFrom = this.$("#week2FromList").val();

            var wList = new Array();

            if(wFrom == -1) {
                for(var i in A.app.sidebar.weekList) {
                    wList.push(A.app.sidebar.weekList[i].weekId );
                }
            } else {
                var wTo = this.$("#week2ToList").val();
                for(var i in A.app.sidebar.weekList) {
                    if(A.app.sidebar.weekList[i].weekId >= wFrom && A.app.sidebar.weekList[i].weekId <= wTo)
                        wList.push(A.app.sidebar.weekList[i].weekId);
                }

            }

            return wList;
        },

        loadChartData : function(data) {
            var compareCheckbox = this.$('#compare');

            data.hours.forEach(function (d) {

                d.hourOfDay = (d.h % 24) + 1;
                d.dayOfWeek = ((d.h - d.hourOfDay) / 24) + 1;

                /*var utcAdjustment = 8;
                var utcAdjustment = 0;
                var fixDay = false;
                if((d.hourOfDay + utcAdjustment) % 24 > 0)
                    fixDay = true;
                d.hourOfDay = (d.hourOfDay + utcAdjustment) % 24;
                if(fixDay){
                    d.dayOfWeek = d.dayOfWeek + 1;
                    if(d.dayOfWeek > 7)
                        d.dayOfWeek = d.dayOfWeek % 7;
                }*/
                //d.s = d.s * 3.6; // convert from m/s km/h
            });

            if(!this.chartData) {
                this.chartData = C(data.hours);

                this.dayCount = this.chartData.dimension(function (d) {
                    return d.dayOfWeek;       // add the magnitude dimension
                });

                this.dayCountGroup = this.dayCount.group().reduce(
                    /* callback for when data is added to the current filter results */
                    function (p, v) {
                        p.count += v.c;
                        p.sum += v.s;
                        if(p.count > 0)
                            p.avg = (p.sum / p.count);
                        else
                            p.avg = 0;
                        return p;
                    },
                    /* callback for when data is removed from the current filter results */
                    function (p, v) {
                        p.count -= v.c;
                        p.sum -= v.s;
                        if(p.count > 0)
                            p.avg = (p.sum / p.count);
                        else
                            p.avg = 0;
                        return p;
                    },
                    /* initialize p */
                    function () {
                        return {
                            count: 0,
                            sum: 0,
                            avg: 0
                        };
                    }
                );

                this.hourCount = this.chartData.dimension(function (d) {
                    return d.hourOfDay;       // add the magnitude dimension
                });

                this.hourCountGroup = this.hourCount.group().reduce(
                    /* callback for when data is added to the current filter results */
                    function (p, v) {
                        p.count += v.c;
                        p.sum += v.s;
                        if(p.count > 0)
                            p.avg = (p.sum / p.count);
                        else
                            p.avg = 0;
                        return p;
                    },
                    /* callback for when data is removed from the current filter results */
                    function (p, v) {
                        p.count -= v.c;
                        p.sum -= v.s;
                        if(p.count > 0)
                            p.avg = (p.sum / p.count);
                        else
                            p.avg = 0;

                        return p;
                    },
                    /* initialize p */
                    function () {
                        return {
                            count: 0,
                            sum: 0,
                            avg: 0
                        };
                    }
                );

                var dayLabel = new Array();
                dayLabel[1] = translator.translate("mon");
                dayLabel[2] = translator.translate("tue");
                dayLabel[3] = translator.translate("wed");
                dayLabel[4] = translator.translate("thu");
                dayLabel[5] = translator.translate("fri");
                dayLabel[6] = translator.translate("sat");
                dayLabel[7] = translator.translate("sun");

                this.dailyChart = dc.barChart("#dailyChart");

                if(this.$("#compare").prop( "checked" ))
                    A.app.sidebar.percentChange = true;
                else
                    A.app.sidebar.percentChange = false;

                this.dailyChart.width(430)
                    .height(150)
                    .margins({top: 5, right: 10, bottom: 20, left: 40})
                    .dimension(this.dayCount)
                    .group(this.dayCountGroup)
                    .transitionDuration(0)
                    .centerBar(true)
                    .valueAccessor(function (p) {
                        return p.value.avg;
                    })
                    .gap(10)
                    .x(d3.scale.linear().domain([0.5, 7.5]))
                    .renderHorizontalGridLines(true)
                    .elasticY(true)
                    .brushOn(false)
                    .xAxis().tickFormat(function (d) {
                        return dayLabel[d]
                    });
                this.hourlyData = data.hours;
                var that = this;
                this.dailyChart.title(function (d) {
                    if(!compareCheckbox.prop('checked') && !isNaN(d.value.avg) && d.value.avg > 0){
                        var wsd = that.wsd(d.key, that.hourlyData, 'dayOfWeek');
                        return translator.translate("avg_speed") + ' ' + Math.round(d.value.avg)
                            + ' KPH, ' + translator.translate("std_dev") + ': ' + wsd;
                    }
                    return null;
                });

                this.dailyChart.yAxis().ticks(4);


                this.dailyChart.yAxis().tickFormat(function (d) {
                    if(A.app.sidebar.percentChange)
                        return Math.round(d * 100) + "%"
                    else
                        return d;
                });

                this.dailyChart.brush().on("brushend.custom", A.app.sidebar.update);

                this.hourlyChart = dc.barChart("#hourlyChart");

                this.hourlyChart.width(430)
                    .height(150)
                    .margins({top: 5, right: 10, bottom: 20, left: 40})
                    .dimension(this.hourCount)
                    .group(this.hourCountGroup)
                    .transitionDuration(0)
                    .centerBar(true)
                    .valueAccessor(function (p) {
                        return p.value.avg;
                    })
                    .gap(5)
                    .brushOn(false)
                    .x(d3.scale.linear().domain([0.5, 24.5]))
                    .renderHorizontalGridLines(true)
                    .elasticY(true)
                    .xAxis().tickFormat();

                this.hourlyChart.title(function (d) {
                    if(!compareCheckbox.prop('checked') && !isNaN(d.value.avg) && d.value.avg > 0){
                        var wsd = that.wsd(d.key, that.hourlyData, 'hourOfDay');
                        return translator.translate("avg_speed") + ' ' + Math.round(d.value.avg)
                            + ' KPH, ' + translator.translate("std_dev") + ': ' + wsd;
                    }
                    return null;
                });


                this.hourlyChart.yAxis().ticks(6);

                this.hourlyChart.yAxis().tickFormat(function (d) {
                    if(A.app.sidebar.percentChange)
                        return Math.round(d * 100) + "%"
                    else
                        return d;
                });

                this.hourlyChart.brush().on("brushend.custom", A.app.sidebar.update);

            }
            else {

                this.hourlyChart.filterAll();
                this.dailyChart.filterAll();
                this.chartData.remove();

                this.chartData.add(data.hours);

                if(A.app.sidebar.hourExtent && ( A.app.sidebar.hourExtent[0] >= 1.0 ||  A.app.sidebar.hourExtent[1] >= 1.0)) {
                    this.hourlyChart.filter(dc.filters.RangedFilter(A.app.sidebar.hourExtent[0], A.app.sidebar.hourExtent[1]));
                }
                if(A.app.sidebar.dayExtent && ( A.app.sidebar.dayExtent[0] >= 1.0 ||  A.app.sidebar.dayExtent[1] >= 1.0)) {
                    this.dailyChart.filter(dc.filters.RangedFilter(A.app.sidebar.dayExtent[0], A.app.sidebar.dayExtent[1]));
                }

                this.hourlyChart.brush().on("brushend.custom", A.app.sidebar.update);
                this.dailyChart.brush().on("brushend.custom", A.app.sidebar.update);

                this.hourlyData = data.hours;
                var that = this;
                this.dailyChart.title(function (d) {
                    if(!compareCheckbox.prop('checked') && !isNaN(d.value.avg) && d.value.avg > 0){
                        var wsd = that.wsd(d.key, that.hourlyData, 'dayOfWeek');
                        return translator.translate("avg_speed") + ' ' + Math.round(d.value.avg)
                            + ' KPH, ' + translator.translate("std_dev") + ': ' + wsd;
                    }
                    return null;
                });
            }

            dc.renderAll();
        },

        wsd: function(key, hourlyData, keyParam){
            var hours = [];
            hourlyData.forEach(function (d) {
                if(d[keyParam] == key  && d.c > 0){
                    hours.push(d);
                }
            });

            var WSD = 0;  //Weighted std dev
            var totalCount = 0;
            hours.forEach(function (d) {
                WSD += d.c * d.std;
                totalCount += d.c;
            });
            WSD /= totalCount;
            WSD = Math.round(WSD * 100) / 100;
            return WSD;
        },

        returnToOverall: function(){
            $('#routeData').show();
            $('#routeButtons').show();
            $('#routeSelections').hide();
            $('#analyzeByTimeButtons').hide();
            $('#selectedDateAndTime').hide();
            $('#analyzeByTime').show();
            $('#returnToOverall').hide();
            this.getRoute();
        },

        renderHourButtons: function() {
            $('#byHourRouteButtons').hide();
            $('#routeSelections').hide();
            var timeButtons = $('#hourButtons');

            for(var i = 0; i < 24; i++){
                var caption = (i < 10 ? '0' + i : i) + ":00";
                var r =  $('<span type="button" class="col-md-4 btn hourButton" data-toggle="button" hour="' + i + '">' + caption + '</span>');
                timeButtons.append(r)
            }

            $(".hourButton").hover(
                function () {
                    $(this).addClass('btn-primary');
                },
                function () {
                    $(this).removeClass('btn-primary');
                    $(this).removeClass('active');
                }
            );

            $(".hourButton").click(
                function (evt) {
                    var hour = $(evt.target).attr('hour');
                    $(evt.target).removeClass('active');
                    A.app.sidebarTabs.hourSelect(hour);
                }
            );

            $('.daySelect').click(function(){
                if ($(this).is(':checked'))
                {
                    $(".hourButton").removeClass('disabled');
                }
            });
        },

        analyzeByTime: function(){
            if($('.hourButton').length == 0) {
                this.renderHourButtons();
            }
            $('#routeData').hide();
            $('#routeButtons').hide();
            $('#routeSelections').show();
            $('#analyzeByTimeButtons').show();
            $('.daySelect').attr('checked', false);
            $(".hourButton").addClass('disabled');
            $('#toggleFilters2').hide();
        },

        initialize : function() {
            $.getJSON('/colors', function(data) {
                var binWidth = 240 / data.colorStrings.length;
                $('#maxSpeedInKph').html(data.maxSpeedInKph);
                var parent = $('#speedLegend').children('svg');
                for(var i in data.colorStrings) {
                    var colorString = data.colorStrings[i];
                    var bin = $(document.createElementNS("http://www.w3.org/2000/svg", "rect")).attr({
                        fill: colorString,
                        x: i * binWidth,
                        width: binWidth,
                        height: 24
                    });
                    parent.append(bin);
                }
            });
            _.bindAll(this, 'update', 'changeFromWeek', 'changeToWeek', 'clickCompare', 'changeConfidenceInterval', 'changeNormalizeBy');
        },

        onRender : function () {
            var user = A.app.instance.user;
            if( !(user && user.isLoggedIn()) ){
                this.saveRouteContainer.show(new views.SaveRouteButton());
                this.exportDataContainer.show(new views.ExportDataButton());

                setTimeout(function() {
                    $('#saveroute').show();
                    $('#saveRouteContainer').removeClass('col-md-0').addClass('col-md-6');
                    $('#bookmarkRouteContainer').removeClass('col-md-12').addClass('col-md-6');
                }, 200);
            }
            this.bookmarkRouteContainer.show(new views.BookmarkRouteButton());
        }
    });
})(Traffic, Traffic.views, Traffic.translations, crossfilter, dc);