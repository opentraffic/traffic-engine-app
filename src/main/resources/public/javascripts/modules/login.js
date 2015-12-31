(function(A, views, models, translator) {
  A.app.instance.module('Login', function(module, app, backbone, marionette, $, _) {
    module.addInitializer(function(options){
      module.userModel = new models.UserModel();
    });

    module.loginSuccess = function(data) {
      module.userModel.set('state', module.userModel.authSuccessState)
    };

    module.loginFail= function(response) {
      if(404 == response.status) {
        module.userModel.set('state', module.userModel.authFailState);
      } else {
        module.userModel.set('state', module.userModel.authUnknownState);
        module.userModel.set('stateDetails', translator.translate('unknown_server_response') + ': ' +
            response.status + ' ' + response.statusText);
      }
    };

    app.vent.on('login:submit', function(userModel) {
      userModel.set('state', userModel.pendingAuthState)
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

    app.vent.on('login:success', function(userModel) {
      setTimeout(function(){
        if(app.loginModal) {
          app.loginModal.close();
        }
        A.app.nav.userMenuContainer.show(new views.UserMenu());
      }, 1000);
    });

    app.vent.on('logout:success', function() {
      app.Login.userModel.reset();
      A.app.nav.userMenuContainer.show(new views.LoginButton());
    });
  });
})(Traffic, Traffic.views, Traffic.models, Traffic.translations);