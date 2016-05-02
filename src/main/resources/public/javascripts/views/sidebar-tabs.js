var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, translator) {

    views.SidebarTabs = Marionette.Layout.extend({

        filterEnabled: false,

        template: Handlebars.getTemplate('app', 'sidebar-tabs'),

        events : {
            'click #data' : 'clickData',
            'click #routing' : 'clickRouting',
            'click #analysis' : 'clickAnalysis',
            'click #localeItem' : 'selectLocale'
        },

        initialize : function() {
            var _this = this;
            _.bindAll(this, 'onMapClick', 'clickAnalysis', 'selectLocale');
        },

        onRender : function() {

            var _this = this;

            if(translator) {
                this.$("#localeList").empty();

                var locales = translator.getAvailableLocales() || {};

                _.each(locales, function(name, locale) {
                    _this.$("#localeList").append('<li><a href="#" id="localeItem" data-locale="' + locale + '">' + name + '</a></li>');
                });

                var currentLocale = locales[translator.getLocale()] || "";
                this.$('#localeLabel').text(currentLocale);
            }
        },

        mapMove: function() {
            if(A.app.sidebar) {
                A.app.sidebar.filterChanged = false;
                A.app.sidebar.update();
            }
        },

        selectLocale : function(evt) {
            var locale = $(evt.target).data('locale');
            if(translator) {
                translator.setLocale(locale);
                window.location.reload();
            }
        },

        clickData: function(evt) {
            this.endRouting();

            A.app.sidebar = new views.DataSidebar();
            A.app.instance.sidebar.show(A.app.sidebar);

            this.$("li").removeClass("active");
            this.$("#data").addClass("active");

            if(A.app.map.hasLayer(A.app.segmentOverlay))
                A.app.map.removeLayer(A.app.segmentOverlay);

            A.app.dataOverlay = L.tileLayer('/tile/data?z={z}&x={x}&y={y}').addTo(A.app.map).bringToFront();
        },

        clickRouting: function(evt) {
            if($('.remove-last-route-point').length > 0){
                return
            }
            this.startRouting();

            A.app.sidebar = new views.RoutingSidebar();
            A.app.instance.sidebar.show(A.app.sidebar);

            this.$("li").removeClass("active");
            this.$("#routing").addClass("active");

            if(A.app.map.hasLayer(A.app.dataOverlay))
                A.app.map.removeLayer(A.app.dataOverlay);

            if(A.app.map.hasLayer(A.app.segmentOverlay))
                A.app.map.removeLayer(A.app.segmentOverlay);

            if(A.app.pathOverlay) {
                A.app.pathOverlay.clearLayers();
            } else {
                A.app.pathOverlay = L.featureGroup([]).addTo(A.app.map);
            }

            A.app.sidebar.$("#routeSelections").hide();
        },

        clickAnalysis: function(evt) {
            A.app.sidebar = new views.AnalysisSidebar();
            A.app.instance.sidebar.show(A.app.sidebar);

            this.endRouting();

            this.$("li").removeClass("active");
            this.$("#analysis").addClass("active");

            if(A.app.map.hasLayer(A.app.dataOverlay))
                A.app.map.removeLayer(A.app.dataOverlay);

            A.app.sidebar.addTrafficOverlay()
        },

        resetRoute : function() {

            if(A.app.sidebar) {
                A.app.sidebar.$("#clickInfo").show();
                A.app.sidebar.$("#routeData").hide();
                A.app.sidebar.$("#routeSelections").hide();
            }

            this.routePoints = [];
            if(this.routePointsLayer) {
                this.routePointsLayer.clearLayers();
            }

            if(A.app.pathOverlay) {
                A.app.pathOverlay.clearLayers();
            }
            A.app.sidebarTabs.filterEnabled = false;
        },

        startRouting : function() {
            this.initializeRouteUndoButton();
            A.app.map.on("click", this.onMapClick);
            this.resetRoute();
        },

        endRouting : function() {
            if(this.undoRouteButton) {
                this.undoRouteButton.removeFrom(A.app.map);
            }

            A.app.map.off("click", this.onMapClick);
            this.resetRoute();
        },

        removeLastRoutePoint: function() {
            if(this.routePoints.length > 0) {
                this.routePoints.splice(this.routePoints.length - 1, 1);
            }

            if(this.routePoints.length < 2) {
                A.app.sidebar.$("#routeButtons").hide();
            }

            if(this.routePointsLayer) {
                var layers = this.routePointsLayer.getLayers();
                if(layers.length > 0) {
                    this.routePointsLayer.removeLayer(layers[layers.length - 1]);
                }

                layers = this.routePointsLayer.getLayers();
                if(layers.length > 1) {
                    var lastPoint = layers[layers.length - 1];
                    lastPoint.setStyle({fillColor: '#D00'});
                }else{
                    if(A.app.sidebar) {
                        A.app.sidebar.$("#clickInfo").show();
                        A.app.sidebar.$("#routeData").hide();
                    }
                }
            }

            this.getRoute();
        },

        initializeRouteUndoButton: function() {
            if(!this.undoRouteButton) {
                var _this = this;
                var button = new L.Control.CustomButton('Undo', {
                    title: translator.translate('remove_last_route_point'),
                    iconCls: 'remove-last-route-point',
                    position: 'topright',
                    clickCallback: function() {
                        _this.removeLastRoutePoint();
                    }
                });

                this.undoRouteButton = button;
            }

            this.undoRouteButton.addTo(A.app.map);
        },

        initializeRoutePoints: function() {
            if(!this.routePoints) {
                this.routePoints = [];
            }

            if(!this.routePointsLayer) {
                this.routePointsLayer = L.featureGroup([]).addTo(A.app.map);
                var _this = this;
                this.routePointsLayer.on('click', function(evt) {
                    var layer = evt.layer;
                    var layerLatlng = layer.getLatLng();

                    this.removeLayer(layer);
                    for(var i=0, pointCount=_this.routePoints.length; i<pointCount; i++) {
                        var point = _this.routePoints[i];
                        if(point.lat == layerLatlng.lat && point.lng == layerLatlng.lng) {
                            _this.routePoints.splice(i, 1);
                            break;
                        }
                    }
                    var layers = this.getLayers();
                    if(layers.length > 0) {
                        layers[0].setStyle({fillColor: '#0D0'});
                        if(layers.length > 1) {
                            layers[layers.length -1].setStyle({fillColor: '#D00'});
                        }
                    }
                    _this.getRoute();
                });
            }
        },

        localizeHour : function(hour) {
            var utcAdjustment = A.app.instance.utcTimezoneOffset || 0;
            hour = ((hour + utcAdjustment) % 168) + 1;
            return hour;
        },

        onMapClick : function(evt, skip, callback) {

            this.initializeRoutePoints();

            this.routePoints.push({
                lat: evt.latlng.lat,
                lng: evt.latlng.lng
            });

            if(this.routePoints.length == 1) {
                this.routePointsLayer.addLayer(L.circleMarker(evt.latlng, {fillColor: "#0D0", color: '#fff', fillOpacity: 1.0,opacity: 1.0, radius: 5}).addTo(A.app.map));
            }
            else {
                var existingLayers = this.routePointsLayer.getLayers();
                if(existingLayers.length > 1) {
                    existingLayers[existingLayers.length -1].setStyle({fillColor: '#00D'});
                }

                this.routePointsLayer.addLayer(L.circleMarker(evt.latlng, {fillColor: "#D00", color: '#fff', fillOpacity: 1.0,opacity: 1.0, radius: 5}).addTo(A.app.map));
                if(skip)
                    return;
                this.getRoute(null, null, null, callback);
                $('#routeData').show();
                $('#routeButtons').show();
            }
        },


        hourSelect: function(hour){
            var day = $('.daySelect:checked').val();

            this.getRoute(null, day, hour);
            $('#analyzeByTimeButtons').hide();
            $('#routeButtons').show();
            $('#analyzeByTime').hide();
            $('#returnToOverall').show();
        },

        getRoute : function(hours, day, hour, callback) {
            if(A.app.pathOverlay) {
                A.app.pathOverlay.clearLayers();
            }

            var routePoints = this.routePoints;
            if(!routePoints || routePoints.length < 2)
                return;

            var hoursStr;

            if(hours && hours.length > 0)
                hoursStr = hours.join(",");

            var w1List = A.app.sidebar.getWeek1List();
            var w2List = A.app.sidebar.getWeek2List();

            var uncorrectedDay = day;
            var uncorrectedHour = hour;

            var utcAdjustment = A.app.instance.utcTimezoneOffset || 0;

            if(hour && day && utcAdjustment){

                var numHour = parseInt(hour); //convert to number so can be used in following code
                var numDay = parseInt(day);
                var fixDay = false;
                if((numHour + utcAdjustment) > 24)
                    fixDay = true;
                numHour = (numHour + utcAdjustment) % 24;
                if(fixDay){
                    numDay = numDay + 1;
                    if(numDay > 7)
                        numDay = 1;
                }

                hour = numHour.toString();
                day = numDay.toString();
            }

            var params = {};
            params.hour = hour;
            params.utcAdjustment = utcAdjustment;
            params.day = day;
            params.confidenceInterval = this.$("#confidenceInterval").val();
            params.normalizeByTime = this.$("#normalizeByTime").val();
            params.compare = $("#compare").prop( "checked" );
            params.routePoints = this.routePoints;

            if(hoursStr)
                params.h = hoursStr

            if(w1List && w1List.length > 0)
                params.w1 = w1List.join(",");

            if(this.$("#compare").prop( "checked" )) {
                if (w2List && w2List.length > 0)
                    params.w2 = w2List.join(",");
            }

            var that = this;

            $.ajax({
                type: "POST",
                url: "/route",
                processData: false,
                contentType: 'application/json',
                data: JSON.stringify(params),
                success: function(data) {
                    if(A.app.pathOverlay) {
                        A.app.pathOverlay.clearLayers();
                    }
                    data = JSON.parse(data);
                    var distance = 0;
                    var time = 0;
                    var speedSum = 0;

                    var insufficientDataWarning = translator.translate('insufficient_data_warning');
                    var inferredDataNotification = translator.translate('inferred_data_notification');
                    var inferredDataBanner = translator.translate('inferred_data_banner');
                    var hasInferredData = false;
                    var routeInfoTemplate = Handlebars.getTemplate('app', 'route-popup');
                    for(i in data.pathEdges) {

                        var edge = data.pathEdges[i];
                        if(isNaN(edge.speed) || isNaN(edge.length))
                            continue;

                        var hoursArray = null;
                        if(hoursStr)
                            hoursArray = hoursStr.split(",");

                        var edge = data.pathEdges[i];
                        if(hoursArray && edge.speedMap){
                            var segmentSpeedSum = 0;
                            var hoursFoundCount = 0;
                            for(hourKey in edge.speedMap) {
                                var localizedHour = that.localizeHour(new Number(hourKey));
                                if(hoursArray.indexOf(localizedHour.toString()) > -1 && edge.speedMap[hourKey]){
                                    var hourSpeedSum = edge.speedMap[hourKey]  / 3.6;
                                    var hourCount = edge.countMap[hourKey];
                                    segmentSpeedSum += hourSpeedSum / hourCount;
                                    hoursFoundCount++;
                                }
                            }
                            edge.speed = segmentSpeedSum / hoursFoundCount;
                        }

                        speedSum += (edge.speed * edge.length);
                        var polyLine = L.Polyline.fromEncoded(edge.geometry);
                        polyLine = L.polyline(polyLine.getLatLngs(), {opacity: 1.0, color: edge.color});

                        var segmentPopupContent = insufficientDataWarning;
                        if(!isNaN(edge.length) && !isNaN(edge.speed && edge.stdDev)){
                            segmentPopupContent = routeInfoTemplate({
                                segment_length: new Number(edge.length).toFixed(2),
                                segment_speed: new Number(edge.speed).toFixed(2) * 3.6,
                                segment_std_dev: new Number(edge.stdDev).toFixed(2)
                            });
                            if(edge.inferred == true){
                                hasInferredData = true;
                                segmentPopupContent = inferredDataNotification + " " + segmentPopupContent;
                            }
                        }
                        polyLine.bindPopup(segmentPopupContent);

                        polyLine.on('mouseover', function(e) {
                            e.target.openPopup();
                        });

                        polyLine.on('mouseout', function(e) {
                            e.target.closePopup();
                        });

                        A.app.pathOverlay.addLayer(polyLine);

                        if(edge.speed > 0 && edge.length > 0) {
                            distance += edge.length;
                            time += edge.length * (1 /edge.speed);
                        }

                    }

                    //if(hasInferredData) {
                    if(data.inferred) {
                        if($('.inferred-data-warning').length == 0) {
                            var tags = "<span class='glyphicon glyphicon-info-sign inferred-data-warning' title='" + inferredDataBanner + "'></span>"
                            $('#avgSpeed').after(tags);
                        }
                    } else {
                        $('.inferred-data-warning').remove();
                    }


                    A.app.sidebar.$("#clickInfo").hide();
                    A.app.sidebar.$("#journeyInfo").show();

                    var seconds = time % 60;
                    var minutes = time / 60;

                    var speed =  (distance / time);
                    speed = speedSum / distance;

                    $('.travel-time-span').show();
                    var seconds = data.travelTimeInSeconds % 60;
                    var minutes = data.travelTimeInSeconds / 60;
                    if(isNaN(data.averageSpeedForRouteInKph)){
                        A.app.sidebar.$("#travelTime").text("No Data");
                        A.app.sidebar.$("#avgSpeed").text("No Data");
                    }else{
                        A.app.sidebar.$("#travelTime").text(Math.round(minutes) + "m " + Math.round(seconds) + "s");
                        A.app.sidebar.$("#avgSpeed").text(data.averageSpeedForRouteInKph +  " KPH");
                    }


                    //sort by hour attr
                    function compare(a,b) {
                        if (a.h < b.h)
                            return -1;
                        if (a.h > b.h)
                            return 1;
                        return 0;
                    }
                    data.weeklyStats.hours.sort(compare);

                    A.app.sidebar.loadChartData(data.weeklyStats);

                    A.app.sidebar.$("#clickInfo").hide();
                    A.app.sidebar.$("#routeData").show();

                    if(hour){

                        var dayLabel = new Array();
                        dayLabel[1] = translator.translate("mon");
                        dayLabel[2] = translator.translate("tue");
                        dayLabel[3] = translator.translate("wed");
                        dayLabel[4] = translator.translate("thu");
                        dayLabel[5] = translator.translate("fri");
                        dayLabel[6] = translator.translate("sat");
                        dayLabel[7] = translator.translate("sun");

                        $('#routeSelections').hide();
                        $('#byHourRouteButtons').show();
                        $('#selectedDateAndTime').show();
                        $("#selectedDate").text(dayLabel[uncorrectedDay]);
                        $("#selectedTime").text((uncorrectedHour < 10 ? 0 : '') + uncorrectedHour + ':00');
                    }
                    params.bounds = A.app.map.getBounds();
                    if(A.app.sidebar.hourExtent && ( A.app.sidebar.hourExtent[0] >= 1.0 ||  A.app.sidebar.hourExtent[1] >= 1.0)) {
                        params.hourExtent = A.app.sidebar.hourExtent;
                        params.dayExtent = A.app.sidebar.dayExtent;
                    }
                    if(params.w1){
                        params.week1FromList = $("#week1FromList").val();
                        params.week1ToList = $("#week1ToList").val();
                    }
                    if(params.w2){
                        params.week2FromList = $("#week2FromList").val();
                        params.week2ToList = $("#week2ToList").val();
                    }

                    A.app.sidebar.params = params;

                    if(callback)
                        callback();
                }
            });
        }
    });
})(Traffic, Traffic.views, Traffic.translations);