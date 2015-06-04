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
		A.app.sidebar = new A.app.Sidebar();
		
		A.app.instance.navbar.show(A.app.nav);
		A.app.instance.sidebar.show(A.app.sidebar);
		
		A.app.map = L.map('map').setView([0, 0], 3);

		L.tileLayer('https://a.tiles.mapbox.com/v4/conveyal.gepida3i/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoiY29udmV5YWwiLCJhIjoiMDliQURXOCJ9.9JWPsqJY7dGIdX777An7Pw', {
		attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery �� <a href="http://mapbox.com">Mapbox</a>',
		maxZoom: 17 }).addTo(A.app.map);

	});

	A.app.Sidebar = Marionette.Layout.extend({

		template: Handlebars.getTemplate('app', 'sidebar'),
		
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
	
	A.app.Nav = Marionette.Layout.extend({

		template: Handlebars.getTemplate('app', 'navbar'),
		
		events : {
			'click #data' : 'clickData',
			'click #routing' : 'clickRouting',
			'click #analysis' : 'clickAnalysis'
		},
		
		initialize : function() {

		},
		
		clickData: function(evt) {
			this.$("li").removeClass("active");
			this.$("#data").addClass("active");
			
			if(A.app.map.hasLayer(A.app.segmentOverlay))
				A.app.map.removeLayer(A.app.segmentOverlay);
			
			A.app.dataOverlay = L.tileLayer('/tile/data?z={z}&x={x}&y={y}').addTo(A.app.map);
		},
		
		clickRouting: function(evt) {
			this.$("li").removeClass("active");
			this.$("#routing").addClass("active");
			
			if(A.app.map.hasLayer(A.app.dataOverlay))
				A.app.map.removeLayer(A.app.dataOverlay);
			
			if(A.app.map.hasLayer(A.app.segmentOverlay))
				A.app.map.removeLayer(A.app.segmentOverlay);
		},
		
		clickAnalysis: function(evt) {
			this.$("li").removeClass("active");
			this.$("#analysis").addClass("active");
			
			if(A.app.map.hasLayer(A.app.dataOverlay))
				A.app.map.removeLayer(A.app.dataOverlay);
			
			A.app.segmentOverlay = L.tileLayer('/tile/segment?z={z}&x={x}&y={y}').addTo(A.app.map);
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
