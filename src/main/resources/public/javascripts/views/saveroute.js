var Traffic = Traffic || {};
Traffic.views = Traffic.views || {};

(function(A, views, translator, C, dc) {

  views.Saveroute = Marionette.Layout.extend({

    template: Handlebars.getTemplate('app', 'saveroute'),
    tagName:   'div',
    className: 'saveroute-area',

    ui: {
      routeForm:           '#routeForm',
      nameField:       'input[name=name]',
      successMessage:      '.msg-success',
      generalErrorMessage: '.error-unknown'
    },

    events : {
      'submit form': 'formSubmitted'
    },

    //Specify the model properties that we should rerender the view on.
    modelEvents: {
    },

    initialize : function() {
      _.bindAll(this, 'formSubmitted');
    },

    formSubmitted: function(event) {
      // Stop the form from actually submitting to the server.
      event.stopPropagation();
      event.preventDefault();

      this.model.set('name', this.$('input[name=name]').val());
      this.model.set('json', A.app.sidebar.params);
      this.model.set('date', new Date());

      // Fire off the global event for the controller so that it handles the server communication.
      A.app.instance.vent.trigger('saveroute:submit', this.model)
    },

    onRender : function () {
      // This is where most of the state-dependent logic goes that used to be
      // written as random jQuery calls. Now, since the view is rerendered on
      // each state change, you just have to modify the DOM relative to the
      // initial content specified in the Underscore template.

        this.ui.nameField.focus();
    },

    onShow: function() {
        this.ui.nameField.focus();
    }
  });
})(Traffic, Traffic.views, Traffic.translations, crossfilter, dc);