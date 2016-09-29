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
        if(!params.utcAdjustment)
            params.utcAdjustment = A.app.instance.utcTimezoneOffset || 0;

        if(!params.w1){
            var w1List = A.app.sidebar.getWeek1List();
            var w2List = A.app.sidebar.getWeek2List();

            if(w1List && w1List.length > 0)
                params.w1 = w1List.join(",");

            if(this.$("#compare").prop( "checked" )) {
                if (w2List && w2List.length > 0)
                    params.w2 = w2List.join(",");
            }
        }

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