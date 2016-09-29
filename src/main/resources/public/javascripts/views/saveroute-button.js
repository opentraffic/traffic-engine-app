var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, translator) {
  
  views.SaveRouteButton = Marionette.Layout.extend({

    template: Handlebars.getTemplate('app', 'saveroute-button'),

    events : {
      'click #saveroute' : 'clickSaveRoute'
    },

    clickSaveRoute: function() {
      A.app.instance.saverouteModal = new Backbone.BootstrapModal({
        animate: true, 
        content: new views.Saveroute({model: A.app.instance.Saveroute.routeModel}),
        title: translator.translate("save_route_dialog_title"),
        showFooter: false,
        escape: false
      });
      A.app.instance.saverouteModal.on('shown', function() {
        $('#routeForm input[name=name]').focus();
      });
      A.app.instance.saverouteModal.on('cancel', function() {
        this.options.content.remove(); //remove previous view
        A.app.instance.saverouteModal = null;
      });

      A.app.instance.saverouteModal.open();
    }
  });
})(Traffic, Traffic.views, Traffic.translations);