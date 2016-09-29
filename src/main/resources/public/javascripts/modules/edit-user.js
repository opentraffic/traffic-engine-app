(function(A, views, models, translator) {
  A.app.instance.module('EditUser', function(module, app, backbone, marionette, $, _) {
    module.addInitializer(function(options){
    });

    module.updateSuccess = function(user, data) {
      var userObj = JSON.parse(data);
      if(user.get('id') == app.user.get('id')) {
        app.user.set('username', userObj['username']);
        app.user.set('role', userObj['role']);
        $('#username').text(app.user.get('username'));
      }
      for(var attr in userObj) {
        user.set(attr, userObj[attr]);  
      }
      user.set('state', user.updateSuccessState);
    };

    module.updateFail= function(user, response) {
      user.set('state', user.updateFailState);
      user.set('stateDetails', translator.translate('update_user_failed') + ': ' +
            response.status + ' ' + response.statusText);
    };

    app.vent.on('edit-user:submit', function(user) {
      var inputData = user.toJSON();
      if(!inputData.username) {
        module.updateFail(user, {
          status: 402,
          statusText: translator.translate('username_required')
        });
      } else if(inputData.password != inputData.confirm_password) {
        module.updateFail(user, {
          status: 402,
          statusText: translator.translate('passwords_not_match')
        });
      } else {
        user.set('state', user.pendingUpdateState)
        $.ajax({
            url:'/users/' + user.get('id'), 
            method: 'put',
            data: inputData
          })
          .done(function(data) {
            module.updateSuccess(user, data);
          })
          .fail(function(response) {
            module.updateFail(user, response);
          });
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