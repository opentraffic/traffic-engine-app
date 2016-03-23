(function(A, views, models, translator) {
  A.app.instance.module('Login', function(module, app, backbone, marionette, $, _) {
    module.addInitializer(function(options){
      module.userModel = app.user;
    });

    module.loginSuccess = function(data) {
      var userObj = JSON.parse(data);
      var userModel = module.userModel;
      for(var attr in userObj) {
        userModel.set(attr, userObj[attr]);
      }
      userModel.set('state', userModel.authSuccessState);
    };

    module.loginFail= function(response) {
      module.userModel.reset();
      if(403 == response.status || 404 == response.status) {
        module.userModel.set('state', module.userModel.authFailState);
      } else {
        module.userModel.set('state', module.userModel.authUnknownState);
        module.userModel.set('stateDetails', translator.translate('unknown_server_response') + ': ' +
            response.status + ' ' + response.statusText);
      }
    };

    app.vent.on('login:auto_auth', function(userModel) {
      var username = Cookies.get('login_username');
      var token = Cookies.get('login_token');
      if(username && token){
          var user = {};
          user.username = username;
          user.token = token;
          $.post('/login', user)
              .done(function(data) {
                  module.loginSuccess(data);
                  app.vent.trigger('login:success');
              })
              .fail(function(response) {
                  //cookie based login failed, silently reset state
                  Cookies.remove('login_username');
                  Cookies.remove('login_token');
                  app.Login.userModel.reset();
              });
      }else{
          $('#login').click();
      }
    });

    app.vent.on('login:submit', function(userModel) {
      userModel.set('state', userModel.pendingAuthState)
      $.post('/login', userModel.toJSON())
         .done(function(data) {
           module.loginSuccess(data);
         })
         .fail(function(response) {
           module.loginFail(response);
      });
    });

    app.vent.on('login:success', function() {
      $('#saveroute').show();
      $('#saveRouteContainer').removeClass('col-md-0').addClass('col-md-6');
      $('#bookmarkRouteContainer').removeClass('col-md-12').addClass('col-md-6');
      var user = app.user;
      var hostname = window.location.hostname.substring(window.location.hostname.lastIndexOf(".", window.location.hostname.lastIndexOf(".") - 1) + 1);
      if(user.get('remember_me')) {
          Cookies.set('login_username', user.get('username'), { domain: hostname, expires: 30 });
          Cookies.set('login_token', user.get('cookie'), { domain: hostname, expires: 30 });
          Cookies.set('city', user.get('city'), { domain: hostname, expires: 30 });
      }else if (!Cookies.get('login_token')){
          Cookies.set('login_username', user.get('username'), { domain: hostname});
          Cookies.set('login_token', user.get('cookie'), { domain: hostname});
          Cookies.set('city', user.get('city'), { domain: hostname});
      }

      setTimeout(function(){
        if(app.loginModal) {
          app.loginModal.close();
        }
        user.clearPasswords();
        A.app.nav.userMenuContainer.show(new views.UserMenu());
      }, 1000);

        if(window.location.href.indexOf('city=') > -1){
            var city = window.location.href.substr(window.location.href.indexOf('city=') + 5);
            for (var property in A.app.instance.cities) {
                var cities = A.app.instance.cities[property];
                for (var index in cities) {
                    var cityObj = cities[index];
                    if(cityObj.city.toLowerCase() == city){
                        $('#locationSearch').val(cityObj.city);
                        A.app.map.setView([cityObj.lat, cityObj.lng], 13);
                        window.history.pushState('', '', '?');
                    }
                }
            }
        }else{

            var countryAndCity = Cookies.get('city');
            var country = countryAndCity.split(":")[0].trim().toLowerCase();
            var city = countryAndCity.split(":")[1].trim().toLowerCase();

            if(window.location.href.toLowerCase().indexOf(country) < 0){
                var href = window.location.href.toLowerCase();
                var start = window.location.href.indexOf('//');
                var end = window.location.href.indexOf('.');
                var currentCountry = href.substring(start + 2, end);
                href = href.replace(currentCountry, country)
                href = href.substr(0, href.lastIndexOf("/") + 1)
                href += "?city=" + city;
                window.location.href = href;
            }else{
                for (var property in A.app.instance.cities) {
                    if(property.toLowerCase() == country.toLowerCase()){
                        var cities = A.app.instance.cities[property];
                        for (var index in cities) {
                            var cityObj = cities[index];
                            if(cityObj.city.toLowerCase() == city){
                                $('#locationSearch').val(cityObj.city);
                                A.app.map.setView([cityObj.lat, cityObj.lng], 13);
                            }
                        }
                    }
                }
            }
        }
    });

    app.vent.on('logout:success', function() {
      $('#saveroute').hide();
      $('#saveRouteContainer').removeClass('col-md-6').addClass('col-md-0');
      $('#bookmarkRouteContainer').removeClass('col-md-6').addClass('col-md-12');
      var hostname = window.location.hostname.substring(window.location.hostname.lastIndexOf(".", window.location.hostname.lastIndexOf(".") - 1) + 1);
      Cookies.remove('login_username', { domain: hostname});
      Cookies.remove('login_token', { domain: hostname});
      Cookies.remove('city', { domain: hostname});
      app.Login.userModel.reset();
      A.app.nav.userMenuContainer.show(new views.LoginButton());
      $('#login').click();
    });

  });
})(Traffic, Traffic.views, Traffic.models, Traffic.translations);