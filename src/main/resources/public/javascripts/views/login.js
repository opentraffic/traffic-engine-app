var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, translator, C, dc) {

  views.Login = Marionette.Layout.extend({

    template: Handlebars.getTemplate('app', 'login'),
    tagName:   'div',
    className: 'login-area',

    ui: {
      loginForm:           '#loginForm',
      usernameField:       'input[name=username]',
      passwordField:       'input[name=password]',
      successMessage:      '.msg-success',
      authErrorMessage:    '.error-bad-auth',
      generalErrorMessage: '.error-unknown'
    },

    events : {
      'submit form': 'formSubmitted'
    },

    //Specify the model properties that we should rerender the view on.
    modelEvents: {
      'change:state':        'render',
      'change:stateDetails': 'render'
    },

    formSubmitted: function(event) {
      // Stop the form from actually submitting to the server.
      event.stopPropagation();
      event.preventDefault();

      this.model.set('username', this.$('input[name=username]').val()); 
      this.model.set('password', this.$('input[name=password]').val());

      // Fire off the global event for the controller so that it handles the server communication.
      A.app.instance.vent.trigger('login:submit', this.model)
    },

    onRender : function () {
      // This is where most of the state-dependent logic goes that used to be
      // written as random jQuery calls. Now, since the view is rerendered on
      // each state change, you just have to modify the DOM relative to the
      // initial content specified in the Underscore template.

      switch(this.model.get('state')) {
        case '':
        case this.model.notAuthState:
          // Focus the username field for the user's convenience.
          this.ui.usernameField.focus();
          break;

        case this.model.pendingAuthState:
          // Disable all the form controls and change the button text to show
          // the user that a request is pending.
          this.ui.loginForm.find('input, select, textarea').prop('disabled', true);
          this.ui.loginForm.find('input[type=submit]').val(translator.translate('logging_in'));
          break;

        case this.model.authFailState:
          // When the user submits invalid credentials, show them an
          // appropriate error message and focus the password field for their convenience.
          this.ui.authErrorMessage.show();
          this.ui.passwordField.focus();
          break;

        case this.model.authUnknownState:
          this.ui.generalErrorMessage.show();
          break;

       case this.model.authSuccessState:
          this.ui.successMessage.show();
          A.app.instance.vent.trigger('login:success')
         
          break;
        default:
          break;
      }

      this.$el.html(this.template(this.model.toJSON()));
    },

    onShow: function() {
      if (this.model.notAuthState == this.model.get('state')) {
        this.ui.usernameField.focus();
      }
    }
  });
})(Traffic, Traffic.views, Traffic.translations, crossfilter, dc);