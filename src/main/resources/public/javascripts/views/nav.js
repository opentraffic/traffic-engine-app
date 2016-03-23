var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, translator) {
  
  views.Nav = Marionette.Layout.extend({

    template: Handlebars.getTemplate('app', 'navbar'),

    regions: {
      userMenuContainer: "#userMenuContainer"
    },

    initialize : function() {
      var _this = this;
      _this.cities = A.app.instance.cities;
      if(!this.cities){
        $.getJSON('/cities.json', function(data) {
          _this.cities = data;
        }).always(function() {
          _this.initLocationTypeahead();
          A.app.instance.cities = _this.cities;
        });
      }else{
        _this.initLocationTypeahead();
      }
    },

    zoomToLocation : function(lat, lng) {
      if(lat && lng) {
        A.app.map.setView([lat,lng], 13);
      }
    },

    initLocationTypeahead: function() {
      var _this = this;

      var citiesEngine = new Bloodhound({
        datumTokenizer: Bloodhound.tokenizers.obj.whitespace('city'),
        queryTokenizer: Bloodhound.tokenizers.whitespace,
        local: _this.cities,
        limit: 10
      });

      this.$('#locationSearch').typeahead(null,
      {
        name: 'cities',
        display: 'city',
        source: citiesEngine,
        templates: {
          empty: Handlebars.compile('<div class="empty-message">{{I18n "no_city_found"}}</div>'),
          suggestion: Handlebars.compile('<div><strong>{{country}}</strong>: {{city}}</div>')
        }
      });

      this.$('#locationSearch').bind('typeahead:select', function(obj, datum, name) { 
        _this.zoomToLocation(datum.lat, datum.lng);
        localStorage.setItem('traffic-engine-city', datum.city);
      });

      var selectedCity = localStorage.getItem('traffic-engine-city');
      if(_this.cities && selectedCity) {
        this.$('#locationSearch').val(selectedCity);
        var cityObj = _.find(_this.cities, function(cityData) {
          return cityData.city == selectedCity;
        })

        if(cityObj) {
          _this.zoomToLocation(cityObj.lat, cityObj.lng);
        }
      }
    },

    onRender: function() {
      var user = A.app.instance.user;
      if( !(user && user.isLoggedIn()) ){
        this.userMenuContainer.show(new views.LoginButton());
      }

      this.$('.dropdown input, .dropdown label').click(function(e) {
        e.stopPropagation();
      });

      this.$('#locationSearch').typeahead();
    }
  });
})(Traffic, Traffic.views, Traffic.translations);