var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, translator) {
  
  views.ExportDataButton = Marionette.Layout.extend({

    template: Handlebars.getTemplate('app', 'exportdata-button'),

    events : {
        'click #exportData' : 'clickExportData'
    },

    clickExportData: function() {
        var params = A.app.sidebar.params;

        $.ajax({
            type: "POST",
            url: "/csv",
            data: JSON.stringify(params),
            success: function(response, status, xhr) {
                $("body").append("<iframe src='/download?filename=" + response + "' style='display: none;' ></iframe>");
            }
        });
    }
  });
})(Traffic, Traffic.views, Traffic.translations);