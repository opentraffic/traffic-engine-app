var Traffic = Traffic || {};

(function(A, $, L) {

	A.app = {};

	A.app.instance = new Backbone.Marionette.Application();

	A.app.instance.addRegions({
		navbar: "#navbar",
		sidebar: "#side-panel"
	});

	A.app.instance.addInitializer(function(options){

		A.app.nav = new A.app.Nav();

		A.app.instance.navbar.show(A.app.nav);

		A.app.map = L.map('map').setView([10.3586888,123.8830313], 10);

		L.tileLayer('https://a.tiles.mapbox.com/v4/conveyal.gepida3i/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoiY29udmV5YWwiLCJhIjoiMDliQURXOCJ9.9JWPsqJY7dGIdX777An7Pw', {
		attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery �� <a href="http://mapbox.com">Mapbox</a>',
		maxZoom: 17 }).addTo(A.app.map);

	});

	A.app.DataSidebar = Marionette.Layout.extend({

		template: Handlebars.getTemplate('app', 'sidebar-data'),

		initialize : function() {

			var _this = this;

			setInterval(function(){
				$.getJSON('/stats', function(data){
					_this.$('#vehicleCount').text(data.vehicleCount);
					_this.$('#lastTime').text(new Date(data.lastUpdate/1000).toString());
				});
			}, 3000);

		}
	});

	A.app.RoutingSidebar = Marionette.Layout.extend({

		template: Handlebars.getTemplate('app', 'sidebar-routing'),

		events : {
			'click #resetRoute' : 'resetRoute',
			'change #day' : 'getRoute',
			'change #hour' : 'getRoute'
		},

		resetRoute : function() {
			A.app.nav.resetRoute();
		},

		getRoute : function() {
			A.app.nav.getRoute();
		},

		initialize : function() {

			var _this = this;
		},

		onRender : function () {
			this.$("#journeyInfo").hide();
		}
	});

	A.app.Nav = Marionette.Layout.extend({

		template: Handlebars.getTemplate('app', 'navbar'),

		events : {
			'click #data' : 'clickData',
			'click #routing' : 'clickRouting',
			'click #analysis' : 'clickAnalysis'
		},

		initialize : function() {
			_.bindAll(this, 'onMapClick');
		},

		clickData: function(evt) {

			this.endRouting();

			A.app.sidebar = new A.app.DataSidebar();
			A.app.instance.sidebar.show(A.app.sidebar);

			this.$("li").removeClass("active");
			this.$("#data").addClass("active");

			if(A.app.map.hasLayer(A.app.segmentOverlay))
				A.app.map.removeLayer(A.app.segmentOverlay);

			A.app.dataOverlay = L.tileLayer('http://localhost:4567//tile/data?z={z}&x={x}&y={y}').addTo(A.app.map);
		},

		clickRouting: function(evt) {

			this.startRouting();

			A.app.sidebar = new A.app.RoutingSidebar();
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

			this.endRouting();

			this.$("li").removeClass("active");
			this.$("#analysis").addClass("active");

			if(A.app.map.hasLayer(A.app.dataOverlay))
				A.app.map.removeLayer(A.app.dataOverlay);

			A.app.segmentOverlay = L.tileLayer('http://localhost:4567//tile/segment?z={z}&x={x}&y={y}').addTo(A.app.map);
		},

		resetRoute : function() {

			if(A.app.sidebar) {
				A.app.sidebar.$("#clickInfo").show();
				A.app.sidebar.$("#journeyInfo").hide();
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
			}


		},

		getRoute : function() {

			if(A.app.map.hasLayer(A.app.pathOverlay))
				A.app.map.removeLayer(A.app.pathOverlay);

			var startLatLng = this.startPoint.getLatLng();
			var endLatLng = this.endPoint.getLatLng();

			var day = A.app.sidebar.$("#day").val();
			var hour = A.app.sidebar.$("#hour").val() * 1;

			$.getJSON('http://localhost:4567/route?fromLat=' + startLatLng.lat + '&fromLon=' + startLatLng.lng + '&toLat=' + endLatLng.lat + '&toLon=' + endLatLng.lng + '&day=' + day + '&time=' + hour + '&useTraffic=true', function(data){
					var encoded = encoded = data.itineraries[0].legs[0].legGeometry.points;
					A.app.pathOverlay = L.Polyline.fromEncoded(encoded);
					A.app.pathOverlay.addTo(A.app.map);

					A.app.sidebar.$("#clickInfo").hide();
					A.app.sidebar.$("#journeyInfo").show();

					var duration = data.itineraries[0].legs[0].duration;

					

					var seconds = duration % 60;
					var minutes = duration / 60;

					var speed =  (data.itineraries[0].legs[0].distance / duration) * 3.6;

					A.app.sidebar.$("#travelTime").text(Math.round(minutes) + "m " + Math.round(seconds) + "s");
					A.app.sidebar.$("#avgSpeed").text(speed.toPrecision(2) + "km")
			});

		},

		onRender : function() {

			// Get rid of that pesky wrapping-div.
			// Assumes 1 child element present in template.
			this.$el = this.$el.children();

			this.$el.unwrap();
			this.setElement(this.$el);

		}
	});


})(Traffic, jQuery, L);


$(document).ready(function() {

	Traffic.app.instance.start();

});
