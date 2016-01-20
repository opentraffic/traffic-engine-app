(function(A, views, models, translator) {
  A.app.instance.module('NewUser', function(module, app, backbone, marionette, $, _) {
    module.addInitializer(function(options){
    });

    module.signupSuccess = function(user, data) {
      user.set('state', user.signupSuccessState);
    };

    module.signupFail= function(user, response) {
      user.set('state', user.signupFailState);
      user.set('stateDetails', translator.translate('create_new_user_failed') + ': ' +
            response.status + ' ' + response.statusText);
    };

    app.vent.on('new-user:submit', function(user) {
      user.set('state', user.pendingSignupState)
        user.set('state', user.pendingSignupState)
        $.post('/users', user.toJSON())
            .done(function(data) {
                module.signupSuccess(user, data);
            })
            .fail(function(response) {
                module.signupFail(user, response);
            });

      var credentials = user.toJSON();

      if(!credentials.username || !credentials.password) {
        module.signupFail(user, {
          status: 402,
          statusText: 'Username and password are required.'
        });
      } else if(credentials.password != credentials.confirm_password) {
        module.signupFail(user, {
          status: 402,
          statusText: 'Confirmed password does not match.'
        });
      } else {
        module.signupSuccess(user, credentials);
      }

    });

    app.vent.on('new-user:success', function(user) {
      setTimeout(function(){
        if(app.newUserModal) {
          app.newUserModal.close();
        }
      }, 1000);
    });
  });
})(Traffic, Traffic.views, Traffic.models, Traffic.translations);