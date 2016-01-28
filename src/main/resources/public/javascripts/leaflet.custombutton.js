L.Control.CustomButton = L.Control.extend({
    _pressed: false,
    _button: null,
    options: {
        position: 'topleft',
        title: '',
        iconCls: '',
        toggleable: false,
        ignorePopupWhenClick: false, //if clicks at a marker with a popup, whether continue finishing control_click_event
        pressedCls: 'leaflet-button-pressed',
        depressedCursorType: '',
        pressedCursorType: 'crosshair',
        disabled: false,
        disabledCls: 'leaflet-disabled',
        clickCallback: function() {}
    },
    initialize: function (controlId, options) {
        this.id = controlId;
        L.Util.setOptions(this, options);
    },
    checkIfIgnorePopupWhenClick: function() {
        return this.options.ignorePopupWhenClick;
    },
    getToggleable: function() {
        return this.options.toggleable;
    },
    getDisabled: function() {
        return this._disabled === true || false;
    },
    setDisabled: function(disabled) {
        disabled = disabled === true || false;
        this._disabled = disabled;
        var button = this._button || {};
        if(disabled) {
            L.DomUtil.addClass(button, this.options.disabledCls);
        } else {
            L.DomUtil.removeClass(button, this.options.disabledCls);
        }
    },
    getPressed: function() {
        return this._pressed;
    },
    setPressed: function(pressed) {
        if(!this.getToggleable()) {
            return;
        }
        this._pressed = pressed;
        var map = this._map;
        if(this._pressed) {
            L.DomUtil.addClass(this._button, this.options.pressedCls);
            if(map) {
                //depress other controls
                var customControls = map.customControls;
                if(customControls instanceof Array) {
                    var currentIndex = customControls.indexOf(this);
                    customControls.forEach( function(ctrl) {
                        if(currentIndex != customControls.indexOf(ctrl)) {
                            ctrl.setPressed(false);
                        }
                    });
                }

                L.DomUtil.get(map.getContainer().id).style.cursor = this.options.pressedCursorType || 'crosshair';
            }
        } else {
             L.DomUtil.removeClass(this._button, this.options.pressedCls);
             if(map) {
                L.DomUtil.get(map.getContainer().id).style.cursor = this.options.depressedCursorType || '';
            }
        }
    },

    onAdd: function (map) {
        var container = L.DomUtil.create('div', 'leaflet-bar leaflet-control');

        this._button = L.DomUtil.create('a', 'leaflet-bar-part ' + this.options.iconCls, container);
        if(this.options.faIconCls) {
            L.DomUtil.create('i', this.options.iconCls, this._button);
        }
        this._button.href = '#';
        this._button.title = this.options.title;

        L.DomEvent.on(this._button, 'click', this._click, this);

        this.setDisabled(this.options.disabled);

        //add current control into map
        if(!map.customControls) {
            map.customControls = [];
        }
        map.customControls.push(this);

        return container;
    },

    _click: function (e) {
        L.DomEvent.stopPropagation(e);
        L.DomEvent.preventDefault(e);
        if(!this.getDisabled()) {
            if(this.options.toggleable) {
                this.setPressed(!this._pressed);
            }
            this.options.clickCallback();
        }
    }
});