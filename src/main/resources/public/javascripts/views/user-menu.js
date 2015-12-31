var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, translator) {
  
  views.UserMenu = Marionette.Layout.extend({

    template: Handlebars.getTemplate('app', 'user-menu'),

    events : {
      'click #signup' : 'clickSignup',
      'click #logout' : 'clickLogout',
      'click #usersLink' : 'clickUsersLink',
      'click #dataManagementLink' : 'clickDataLink'
    },

    clickSignup: function() {
      if(!A.app.instance.signupModal) {
        A.app.instance.signupModal = new Backbone.BootstrapModal({
          animate: true, 
          content: Traffic.app.instance.UserMenu.signupView, 
          title: translator.translate("new_user_dialog_title"),
          showFooter: false
        });
      }
      A.app.instance.signupModal.open();
    },

    clickLogout: function() {
      A.app.instance.vent.trigger('logout:success')
    },

    clickUsersLink: function() {
      if(!A.app.instance.usersModal) {
        A.app.instance.usersModal = new Backbone.BootstrapModal({
          animate: true, 
          content: Traffic.app.instance.UserMenu.usersView, 
          title: translator.translate("users_dialog_title"),
          showFooter: false
        });
      }
      A.app.instance.usersModal.open();
    },

    clickDataLink: function() {
      if(!A.app.instance.dataModal) {
        A.app.instance.dataModal = new Backbone.BootstrapModal({
          animate: true, 
          content: Traffic.app.instance.UserMenu.dataView, 
          title: translator.translate("data_dialog_title"),
          showFooter: false
        });
      }
      A.app.instance.dataModal.open();
    },

    onRender: function() {
      this.$el.html(this.template(A.app.instance.Login.userModel.toJSON()));
    }
  });
})(Traffic, Traffic.views, Traffic.translations);