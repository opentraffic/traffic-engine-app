(function(A, views, models, translator) {
  A.app.instance.module('EditUser', function(module, app, backbone, marionette, $, _) {
    module.addInitializer(function(options){
    });

    module.updateSuccess = function(user, data) {
      user.set('state', user.updateSuccessState);
    };

    module.updateFail= function(user, response) {
      user.set('state', user.updateFailState);
      user.set('stateDetails', translator.translate('update_user_failed') + ': ' +
            response.status + ' ' + response.statusText);
    };

    app.vent.on('edit-user:submit', function(user) {
      user.set('state', user.pendingUpdateState)
      // $.ajax({
          //url:'/users/', 
          //method: 'patch',
          //data: user.toJSON()
        //})
      //   .done(function(data) {
      //     module.updateSuccess(user, data);
      //   })
      //   .fail(function(response) {
      //     module.updateFail(user, response);
      //   });

      var credentials = user.toJSON();

      //TODO: remove
      // for debugging only
      if(credentials.username == 'superadmin') {
        module.updateFail(user, {
          status: 402,
          statusText: 'This user is registered.'
        });
      } else if(!credentials.username) {
        module.updateFail(user, {
          status: 402,
          statusText: 'Username is required.'
        });
      } else if((credentials.password || credentials.confirm_password) && credentials.password != credentials.confirm_password) {
        module.updateFail(user, {
          status: 402,
          statusText: 'Confirmed password does not match.'
        });
      } else {
        module.updateSuccess(user, credentials);
      }

    });

    app.vent.on('edit-user:success', function(user) {
      setTimeout(function(){
        if(app.editUserModal) {
          app.editUserModal.close();
        }
      }, 1000);
    });
  });
})(Traffic, Traffic.views, Traffic.models, Traffic.translations);