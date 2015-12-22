var Traffic = Traffic || {};

Traffic.translations = (function() {
  var LOCAL_STORAGE_KEY = 'traffic-engine-locale';
  var _data = {};
  var _locale = localStorage.getItem(LOCAL_STORAGE_KEY) || "en";
  var _localeList = {
    en: "English"
  };

  var getLocale = function() {
    return _locale;
  };

  var setLocale = function(locale) {
    localStorage.setItem(LOCAL_STORAGE_KEY, locale);
    _locale = locale;
  };

  var fetchAvailableLocales = function() {
    $.ajax({
      url: "../locales/index.json",
      dataType: 'json',
      success: function (data) {
        _localeList = data;
      },
      async: false
    });
  };

  var getAvailableLocales = function() {
    return _localeList;
  };

  var fetchData = function() {
    var locale = getLocale();
    $.ajax({
      url: "../locales/" + locale + ".json",
      dataType: 'json',
      success: function (data) {
        _data = data;
      },
      async: false
    });
  };

  var getData = function() {
    return _data;
  };

  var translate = function (code) {
    var data = getData();
    var msg = data[code];

    if (msg !== undefined) {
      for (var i = 1; i < arguments.length; i++) {
        msg = msg.replace("{" + (i - 1) + "}", arguments[i]);
      }
    } else {
      return code;
    }

    return msg;
  };

  var init = function() {
    fetchAvailableLocales();
    fetchData();
  };  

  return {
    fetchAvailableLocales: fetchAvailableLocales,
    getAvailableLocales: getAvailableLocales,
    getLocale: getLocale,
    setLocale: setLocale,
    getData: getData,
    fetchData: fetchData,
    translate: translate,
    init: init
  };

})();