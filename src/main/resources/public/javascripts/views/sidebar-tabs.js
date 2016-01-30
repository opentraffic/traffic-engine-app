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
      }

      if(this.startPoint != false) {
        if(A.app.map.hasLayer(this.startPoint))
          A.app.map.removeLayer(this.startPoint);
      }

      if(this.endPoint != false) {
        if(A.app.map.hasLayer(this.endPoint))
          A.app.map.removeLayer(this.endPoint);
      }

      if(A.app.map.hasLayer(A.app.pathOverlay))
        A.app.map.removeLayer(A.app.pathOverlay);

      this.startPoint = false;
      this.endPoint = false;


    },

    startRouting : function() {
      A.app.map.on("click", this.onMapClick);
      this.resetRoute();
    },

    endRouting : function() {
      A.app.map.off("click", this.onMapClick);
      this.resetRoute();
    },

    onMapClick : function(evt) {

      if(this.startPoint == false) {
        this.startPoint = L.circleMarker(evt.latlng, {fillColor: "#0D0", color: '#fff', fillOpacity: 1.0,opacity: 1.0, radius: 5}).addTo(A.app.map);
      }
      else if(this.endPoint == false) {
        this.endPoint = L.circleMarker(evt.latlng, {fillColor: "#D00", color: '#fff', fillOpacity: 1.0,opacity: 1.0, radius: 5}).addTo(A.app.map);
        this.getRoute();
        $('#routeButtons').show();
      }

    },

    getRoute : function(hours) {

      if(A.app.map.hasLayer(A.app.pathOverlay))
        A.app.map.removeLayer(A.app.pathOverlay);

      if(!this.startPoint || !this.endPoint)
        return;

      var startLatLng = this.startPoint.getLatLng();
      var endLatLng = this.endPoint.getLatLng();

      var hoursStr;

      if(hours && hours.length > 0)
        hoursStr = hours.join(",");

      var w1List = A.app.sidebar.getWeek1List();
      var w2List = A.app.sidebar.getWeek2List();

      var confidenceInterval = this.$("#confidenceInterval").val();
      var normalizeByTime = this.$("#normalizeByTime").val();
      var compare = $("#compare").prop( "checked" );

      var url = '/route?fromLat=' + startLatLng.lat + '&fromLon=' + startLatLng.lng + '&toLat=' + endLatLng.lat + '&toLon=' + endLatLng.lng
        + '&compare=' + compare + '&normalizeByTime=' + normalizeByTime + '&confidenceInterval=' + confidenceInterval;

      if(hoursStr)
        url += '&h=' + hoursStr;

      if(w1List && w1List.length > 0)
        url += '&w1=' + w1List.join(",");

      if(this.$("#compare").prop( "checked" )) {
        if (w2List && w2List.length > 0)
          url += '&w2=' + w2List.join(",");
      }

      $.getJSON(url, function(data){

        var distance = 0;
        var time = 0;

        var lines = new Array();
        var insufficientDataWarning = translator.translate('insufficient_data_warning');
        var inferredDataNotification = translator.translate('inferred_data_notification');
        var inferredDataNotificationTitle = translator.translate('inferred_data_notification_title');
        var inferredDataBanner = translator.translate('inferred_data_banner');
        var unableEstimateTravelTimeMessage = translator.translate('unable_estimate_travel_time');
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
          $('.travel-time-span').hide();
          $('#jqueryGrowlDock .panel').remove(); // remove previous sign
          $.growl({
            title: inferredDataNotificationTitle,
            message: inferredDataBanner,
            priority: 'primary'
          });
        }

        A.app.pathOverlay = L.featureGroup(lines);

        A.app.pathOverlay.addTo(A.app.map);

        A.app.sidebar.$("#clickInfo").hide();
        A.app.sidebar.$("#journeyInfo").show();

        var seconds = time % 60;
        var minutes = time / 60;

        var speed =  (distance / time) * 3.6;

        if(!hasInferredData) {
        $('.travel-time-span').show();
        A.app.sidebar.$("#travelTime").text(Math.round(minutes) + "m " + Math.round(seconds) + "s");
        }
        A.app.sidebar.$("#avgSpeed").text(speed.toPrecision(2) + "KPH");

        A.app.sidebar.loadChartData(data.weeklyStats);

        A.app.sidebar.$("#clickInfo").hide();
        A.app.sidebar.$("#routeData").show();

      });
    }
  });
})(Traffic, Traffic.views, Traffic.translations);