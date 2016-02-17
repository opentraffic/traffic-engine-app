var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, translator) {
  
  views.BookmarkRouteButton = Marionette.Layout.extend({

    template: Handlebars.getTemplate('app', 'bookmarkroute-button'),

    events : {
        'click #bookmarkroute' : 'clickBookmarkRoute'
    },

    clickBookmarkRoute: function() {
        var params = A.app.sidebar.params;

        $.ajax({
            type: "POST",
            url: "/route/save",
            processData: false,
            contentType: 'application/json',
            data: JSON.stringify(params),
            success: function(data) {
                window.history.pushState('', '', '?route=' + data);
                A.app.instance.bookmarkRouteModal = new Backbone.BootstrapModal({
                    animate: true,
                    content: window.location.href,
                    title: translator.translate("bookmark_route_dialog_title"),
                    showFooter: false,
                    escape: false
                });
                A.app.instance.bookmarkRouteModal.on('shown', function() {
                });
                A.app.instance.bookmarkRouteModal.on('cancel', function() {
                    A.app.instance.bookmarkRouteModal = null;
                });
                A.app.instance.bookmarkRouteModal.open();
            }
        });
    }
  });
})(Traffic, Traffic.views, Traffic.translations);