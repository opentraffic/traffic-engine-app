(function(A, views, models, translator) {
  A.app.instance.module('Login', function(module, app, backbone, marionette, $, _) {
    module.addInitializer(function(options){
      module.userModel = app.user;
    });

    module.loginSuccess = function(data) {
      module.userModel.set('token', data.token);
      module.userModel.set('state', module.userModel.authSuccessState);
    };

    module.loginFail= function(response) {
      module.userModel.reset();
      if(404 == response.status) {
        module.userModel.set('state', module.userModel.authFailState);
      } else {
        module.userModel.set('state', module.userModel.authUnknownState);
        module.userModel.set('stateDetails', translator.translate('unknown_server_response') + ': ' +
            response.status + ' ' + response.statusText);
      }
    };

    app.vent.on('login:auto_auth', function() {
      var username = Cookies.get('login_username');
      var token = Cookies.get('login_token');
      app.user.set('username', username);
      app.user.set('token', token);
      //TODO: real login api
      if(username == 'superadmin' && token == 'sample-token') {
        app.user.set('role', 'super_admin');
        A.app.nav.userMenuContainer.show(new views.UserMenu());
      }
    });

    app.vent.on('login:submit', function(userModel) {
      userModel.set('state', userModel.pendingAuthState)
      //TODO: real login api
      // $.post('/login', userModel.toJSON())
      //   .done(function(data) {
      //     module.loginSuccess(data);
      //   })
      //   .fail(function(response) {
      //     module.loginFail(response);
      //   });
      userModel.set('role', 'super_admin');
      var credentials = userModel.toJSON();

      //TODO: remove
      // for debugging only
      if(credentials.username == 'superadmin' && credentials.password == "welcome1") {
        credentials.token = 'sample-token';
        module.loginSuccess(credentials);
      } else {
        if(!credentials.username) {
          module.loginFail({
            status: 402,
            statusText: 'Something is wrong'
          });
        } else {
          module.loginFail({
            status: 404
          });
        }
      }

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