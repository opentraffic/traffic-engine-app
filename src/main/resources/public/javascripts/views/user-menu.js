var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, models, translator) {
  
  views.UserMenu = Marionette.Layout.extend({

    template: Handlebars.getTemplate('app', 'user-menu'),

    events : {
      'click #newUser' : 'clickNewUser',
      'click #logout' : 'clickLogout',
      'click #usersLink' : 'clickUsersLink',
      'click #dataManagementLink' : 'clickDataLink'
    },

    clickNewUser: function() {
      A.app.instance.newUserModal = new Backbone.BootstrapModal({
        animate: true, 
        content: new views.NewUser({model: new models.UserModel()}), 
        title: translator.translate("new_user_dialog_title"),
        showFooter: false
      });
      
      A.app.instance.newUserModal.on('cancel', function() {
        this.options.content.remove(); //remove previous view
        A.app.instance.newUserModal = null;
      });

      A.app.instance.newUserModal.open();
    },

    clickLogout: function() {
      A.app.instance.vent.trigger('logout:success')
    },

    clickUsersLink: function() {
      A.app.instance.usersModal = new Backbone.BootstrapModal({
        animate: true, 
        content: new views.UsersList(), 
        title: translator.translate("users_dialog_title"),
        showFooter: false
      });
      
      A.app.instance.usersModal.on('cancel', function() {
        this.options.content.remove(); //remove previous view
        A.app.instance.usersModal = null;
      });

      A.app.instance.usersModal.open();
    },

    clickDataLink: function() {
      
      console.log('Data management dialog: TODO');
    },

    onRender: function() {
      this.$el.html(this.template(A.app.instance.Login.userModel.toJSON()));
    }
  });
})(Traffic, Traffic.views, Traffic.models, Traffic.translations);