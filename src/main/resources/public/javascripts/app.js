var Traffic = Traffic || {};

(function(A, $, translator, views, models, mapWrapper) {

	A.app = {};

	A.app.instance = new Backbone.Marionette.Application();

	A.app.instance.addRegions({
		navbar: "#navbar",
		sidebarTabs: "#sidebar-tabs",
		sidebar: "#side-panel"
	});

	A.app.instance.addInitializer(function(options){
		this.user = new models.UserModel();

		A.app.nav = new views.Nav();
		A.app.instance.navbar.show(A.app.nav);

		A.app.sidebarTabs = new views.SidebarTabs();
		A.app.instance.sidebarTabs.show(A.app.sidebarTabs);

		var mapWrapperObject = Object.create(mapWrapper);
		mapWrapperObject.init('map', {
			center: [10.3036741,123.8982952],
			 zoom: 13
		});
		
		A.app.map = mapWrapperObject.LMmap;

		// Click Data in sidebar
		$('#data').click();

	});

})(Traffic, jQuery, Traffic.translations, Traffic.views, Traffic.models, Traffic.MapWrapper);


$(document).ready(function() {
	if(Traffic.translations != undefined) {
		Traffic.translations.init();
	}

	Traffic.app.instance.start();

});
