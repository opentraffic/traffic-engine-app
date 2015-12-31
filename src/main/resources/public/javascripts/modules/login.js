(function(A, views, models, translator) {
  A.app.instance.module('Login', function(module, app, backbone, marionette, $, _) {
    module.addInitializer(function(options){
      module.userModel = new models.UserModel();
      module.loginView = new views.Login({model: module.userModel});
    });

    module.loginSuccess = function(data) {
      module.userModel.set('state', module.userModel.authSuccessState)
    };

    module.loginFail= function(response) {
      if(404 == response.status) {
        module.userModel.set('state', module.userModel.authFailState);
      } else {
        module.userModel.set('state', module.userModel.authUnknownState);
        module.userModel.set('stateDetails', 'Unexpected Server Response: ' +
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

      module.loginSuccess({
        username: 'test',
        role: 'admin'
      });
    });

    app.vent.on('login:success', function(userModel) {
      app.loginModal.close();
      app.loginModal = null;
      A.app.nav.userMenuContainer.show(new views.UserMenu());
    });

    app.vent.on('logout:success', function() {
      app.Login.userModel.reset();
      A.app.nav.userMenuContainer.show(new views.LoginButton());
    });
  });
})(Traffic, Traffic.views, Traffic.models, Traffic.translations);