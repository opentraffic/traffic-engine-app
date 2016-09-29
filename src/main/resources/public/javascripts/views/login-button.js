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
        content: new views.Login({model: A.app.instance.Login.userModel}), 
        title: translator.translate("login_dialog_title"),
        showFooter: false,
        escape: false,
        okCloses: false,
        allowCancel: false,
      });
        A.app.instance.loginModal.on('shown', function() {
            $('#loginForm input[name=username]').focus();
            for (var property in A.app.instance.cities) {
                var cities = A.app.instance.cities[property];
                for (var index in cities) {
                    var city = cities[index];
                    var text = city.country + ": " + city.city;
                    var option = $('<option/>');
                    option.attr({ 'value': text }).text(text);
                    $('#login-city-select').append(option);
                }
            }
            if(Cookies.get('city')){
                $('#login-city-select').val(Cookies.get('city'));
            }
        });
      A.app.instance.loginModal.on('cancel', function() {
        this.options.content.remove(); //remove previous view
        A.app.instance.loginModal = null;
      });

      A.app.instance.loginModal.open();
    }
  });
})(Traffic, Traffic.views, Traffic.translations);