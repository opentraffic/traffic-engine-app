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
      if(!A.app.instance.cities){
        $.getJSON('/cities.json', function(data) {
          A.app.instance.cities = data;
        }).always(function() {
          _this.initLocationTypeahead();
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

      var href = window.location.href.toLowerCase();
      var start = window.location.href.indexOf('//');
      var end = window.location.href.indexOf('.');
      var currentCountry = href.substring(start + 2, end);
      _this.cities = [];
      for (var property in A.app.instance.cities) {
        _this.cities = _this.cities.concat(A.app.instance.cities[property]);
      }

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
        var hostname = window.location.hostname.substring(window.location.hostname.lastIndexOf(".", window.location.hostname.lastIndexOf(".") - 1) + 1);
        Cookies.set('city', datum.country + ": " + datum.city, { domain: hostname, expires: 30 });
        if(window.location.href.toLowerCase().indexOf(datum.country.toLowerCase()) < 0){
          var href = window.location.href.toLowerCase();
          var start = window.location.href.indexOf('//');
          var end = window.location.href.indexOf('.');
          var currentCountry = href.substring(start + 2, end);
          href = href.replace(currentCountry, datum.country)
          href = href.substr(0, href.lastIndexOf("/") + 1)
          href += "?city=" + datum.city;
          window.location.href = href;
        }else{
          _this.zoomToLocation(datum.lat, datum.lng);
        }
      });

      if(Cookies.get('city')){
        var selectedCity = Cookies.get('city').split(":")[1].trim();
        if(_this.cities && selectedCity) {
          this.$('#locationSearch').val(selectedCity);
          var cityObj = _.find(_this.cities, function(cityData) {
            return cityData.city == selectedCity;
          })

          if(cityObj) {
            _this.zoomToLocation(cityObj.lat, cityObj.lng);
          }
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