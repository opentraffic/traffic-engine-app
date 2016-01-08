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


// Override Backbone.sync to include user auth params into each API call
var overrideBackboneSync = function() {
	var _sync = Backbone.sync;
	Backbone.sync = function(method, model, options) {
		options = options || {};
		options.data = options.data || {};
		var user = Traffic.app.instance.user;
		if(user) {
			options.data.username = Traffic.app.instance.user.get('username');

			options = $.extend({
	      // In case the request is cross domain, keep these next 4 lines
	      //crossDomain: true,
	      //xhrFields: {
	      //    withCredentials: true
	      //},
	      // Add the token to the request header
	      beforeSend: function(xhr){
	          xhr.setRequestHeader('X-AUTH-TOKEN', user.get('token'));
	      }
	    }, options);
		}

	  return _sync.call( this, method, model, options );
	}
};

var createHtmlCustomCell = function() {
	var HtmlCell = Backgrid.HtmlCell = Backgrid.Cell.extend({

    /** @property */
    className: "html-cell",
    
    initialize: function () {
        Backgrid.Cell.prototype.initialize.apply(this, arguments);
    },

    render: function () {
        this.$el.empty();
        var rawValue = this.model.get(this.column.get("name"));
        var formattedValue = this.formatter.fromRaw(rawValue, this.model);
        this.$el.append(formattedValue);
        this.delegateEvents();
        return this;
    }
	});
};

$(document).ready(function() {
	overrideBackboneSync();
	createHtmlCustomCell();
	if(Traffic.translations != undefined) {
		Traffic.translations.init();
	}
	Traffic.app.instance.start();

});
