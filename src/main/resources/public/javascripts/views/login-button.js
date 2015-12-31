var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, translator) {
  
  views.LoginButton = Marionette.Layout.extend({

    template: Handlebars.getTemplate('app', 'login-button'),

    events : {
      'click #login' : 'clickLogin'
    },

    clickLogin: function() {
      A.app.instance.loginModal = new Backbone.BootstrapModal({
        animate: true, 
        content: Traffic.app.instance.Login.loginView, 
        title: translator.translate("login_dialog_title"),
        showFooter: false
      });

      A.app.instance.loginModal.open();
    }
  });
})(Traffic, Traffic.views, Traffic.translations);