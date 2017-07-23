var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, translator) {
  
  views.LoginButton = Marionette.Layout.extend({

    template: Handlebars.getTemplate('app', 'login-button'),

    events : {
      'click #login' : 'clickLogin'
    },

    clickLogin: function() {
      $.ajax({
          url: "/auth",
          type: "POST",
          data: {action: "logout"},
          success: function (xhr, status) {
             window.location.replace("/auth");
          }
       });
    }

  });
})(Traffic, Traffic.views, Traffic.translations);