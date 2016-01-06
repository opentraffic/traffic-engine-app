var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, translator) {

  views.DataSidebar = Marionette.Layout.extend({

    template: Handlebars.getTemplate('app', 'sidebar-data'),

    initialize : function() {

      var _this = this;

      this.statsAPIInterval = setInterval(function(){
        $.getJSON('/stats', function(data){
          _this.$('#vehicleCount').text(data.vehicleCount);
          _this.$('#lastTime').text(new Date(data.lastUpdate).toString());
        });
      }, 3000);

    },

    onClose: function() {
      clearInterval(this.statsAPIInterval);
    }
  });
  
})(Traffic, Traffic.views, Traffic.translations);