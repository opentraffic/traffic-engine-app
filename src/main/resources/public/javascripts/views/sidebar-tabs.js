var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, translator) {

    views.SidebarTabs = Marionette.Layout.extend({

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
            this.startRouting();

            A.app.sidebar = new views.RoutingSidebar();
            A.app.instance.sidebar.show(A.app.sidebar);

            this.$("li").removeClass("active");
            this.$("#routing").addClass("active");

            if(A.app.map.hasLayer(A.app.dataOverlay))
                A.app.map.removeLayer(A.app.dataOverlay);

            if(A.app.map.hasLayer(A.app.segmentOverlay))
                A.app.map.removeLayer(A.app.segmentOverlay);

            if(A.app.map.hasLayer(A.app.pathOverlay))
                A.app.map.removeLayer(A.app.pathOverlay);
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

            if(A.app.map.hasLayer(A.app.pathOverlay))
                A.app.map.removeLayer(A.app.pathOverlay);

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

        onMapClick : function(evt) {
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
                this.getRoute();
                $('#routeData').show();
                $('#routeButtons').show();
            }

        },


        hourSelect: function(hour){
            var day = $('.daySelect:checked').val();
            this.getRoute(null, day, hour);
        },

        getRoute : function(hours, day, hour) {

            if(A.app.map.hasLayer(A.app.pathOverlay))
                A.app.map.removeLayer(A.app.pathOverlay);

            var routePoints = this.routePoints;
            if(!routePoints || routePoints.length < 2)
                return;

            var hoursStr;

            if(hours && hours.length > 0)
                hoursStr = hours.join(",");

            var w1List = A.app.sidebar.getWeek1List();
            var w2List = A.app.sidebar.getWeek2List();

            var params = {};
            params.hour = hour;
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

            $.ajax({
                type: "POST",
                url: "/route",
                processData: false,
                contentType: 'application/json',
                data: JSON.stringify(params),
                success: function(data) {
                    data = JSON.parse(data);
                    var distance = 0;
                    var time = 0;

                    var lines = new Array();
                    var insufficientDataWarning = translator.translate('insufficient_data_warning');
                    var inferredDataNotification = translator.translate('inferred_data_notification');
                    var inferredDataBanner = translator.translate('inferred_data_banner');
                    var hasInferredData = false;
                    var routeInfoTemplate = Handlebars.getTemplate('app', 'route-popup');
                    for(i in data.pathEdges) {
                        var edge = data.pathEdges[i];

                        var polyLine = L.Polyline.fromEncoded(edge.geometry);
                        polyLine = L.polyline(polyLine.getLatLngs(), {opacity: 1.0, color: edge.color});

                        var segmentPopupContent = insufficientDataWarning;
                        if(!isNaN(edge.length) && !isNaN(edge.speed && edge.stdDev)){
                            segmentPopupContent = routeInfoTemplate({
                                segment_length: new Number(edge.length).toFixed(2),
                                segment_speed: new Number(edge.speed).toFixed(2),
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

                        lines.push(polyLine);

                        if(edge.speed > 0 && edge.length > 0) {
                            distance += edge.length;
                            time += edge.length * (1 /edge.speed);
                        }
                    }

                    if(hasInferredData) {
                        if($('.inferred-data-warning').length == 0) {
                            var tags = "<span class='glyphicon glyphicon-info-sign inferred-data-warning' title='" + inferredDataBanner + "'></span>"
                            $('#avgSpeed').after(tags);
                        }
                    } else {
                        $('.inferred-data-warning').remove();
                    }

                    A.app.pathOverlay = L.featureGroup(lines);

                    A.app.pathOverlay.addTo(A.app.map);

                    A.app.sidebar.$("#clickInfo").hide();
                    A.app.sidebar.$("#journeyInfo").show();

                    var seconds = time % 60;
                    var minutes = time / 60;

                    var speed =  (distance / time);

                    $('.travel-time-span').show();
                    A.app.sidebar.$("#travelTime").text(Math.round(minutes) + "m " + Math.round(seconds) + "s");
                    A.app.sidebar.$("#avgSpeed").text(speed.toPrecision(2) + " KPH");

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
                        $("#selectedDate").text(dayLabel[day]);
                        $("#selectedTime").text((hour < 10 ? 0 : '') + hour + ':00');
                    }
                }
            });
        }
    });
})(Traffic, Traffic.views, Traffic.translations);