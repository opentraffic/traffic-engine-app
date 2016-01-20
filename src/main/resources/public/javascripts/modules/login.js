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
              })
              .fail(function(response) {
                  //cookie based login failed, silently reset state
                  Cookies.remove('login_username');
                  Cookies.remove('login_token');
                  app.Login.userModel.reset();
              });
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
      var user = app.user;
      if(user.get('remember_me')) {
        Cookies.set('login_username', user.get('username'), { expires: 30 });
        Cookies.set('login_token', user.get('token'), { expires: 30 });
      }
      
      setTimeout(function(){
        if(app.loginModal) {
          app.loginModal.close();
        }
        user.clearPasswords();
        A.app.nav.userMenuContainer.show(new views.UserMenu());
      }, 1000);
    });

    app.vent.on('logout:success', function() {
      Cookies.remove('login_username');
      Cookies.remove('login_token');
      app.Login.userModel.reset();
      A.app.nav.userMenuContainer.show(new views.LoginButton());
    });
  });
})(Traffic, Traffic.views, Traffic.models, Traffic.translations);