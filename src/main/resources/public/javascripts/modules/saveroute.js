(function(A, views, models, translator) {
  A.app.instance.module('Saveroute', function(module, app, backbone, marionette, $, _) {
    module.addInitializer(function(options){
      module.routeModel = app.route;
    });

    app.vent.on('saveroute:submit', function(routeModel) {
        $.ajax({
            type: "POST",
            url: "/route/save",
            processData: false,
            contentType: 'application/json',
            data: JSON.stringify(routeModel),
            success: function(data) {
                app.vent.trigger('saveroute:success', data);
            }
        });
    });

      app.vent.on('saveroute:success', function(routeId) {
          window.history.pushState('', '', '?route=' + routeId);
          setTimeout(function(){
              if(app.saverouteModal) {
                  app.saverouteModal.close();
              }
          }, 1000);
      });

      app.vent.on('saveroute:url', function(routeId) {
          A.app.sidebar.loadRouteFromUrl(routeId);
      });

      app.vent.on('route:rowclick', function(routeId) {
          A.app.sidebar.loadRouteFromUrl(routeId);
          window.history.pushState('', '', '?route=' + routeId);
      });
  });
})(Traffic, Traffic.views, Traffic.models, Traffic.translations);