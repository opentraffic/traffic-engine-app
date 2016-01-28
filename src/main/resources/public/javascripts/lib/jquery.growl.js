/*
 * jQuery Growl plugin
 * Version 2.0.0
 * Last Updated 2014-02-08
 * @requires jQuery v1.11.0 or later (untested on previous version)
 *
 * Examples at: http://fragmentedcode.com/jquery-growl
 * Copyright (c) 2008-2014 David Higgins
 * 
 * Special thanks to Daniel Mota for inspiration:
 * http://icebeat.bitacoras.com/mootools/growl/
 */
 
(function() {
  var growl = {
    getInstance: function(rebuild) {
      var instance = $('#' + settings.dockId);
      if(instance.length < 1 || rebuild === true) {
        instance = $(settings.dockTemplate).attr('id', settings.dockId).css(settings.dockCss).addClass(settings.dockClass);
        if(settings.defaultStylesheet) {
          $('head').appendTo('<link rel="stylesheet" type="text/css" href="' + settings.defaultStylesheet + '" />');
        }
        $(settings.dockContainerSelector).append(instance);
      }
      return instance;
    },
    notify: function(title, message, priority) {
      var instance = this.getInstance();
      var notice = $(settings.noticeTemplate).hide().css(settings.noticeCss).addClass(settings.noticeClass);
      
      if(!priority) { priority = 'primary'; }
      switch(priority) {
        case 'info': notice.addClass('panel-info'); break;
        case 'success': notice.addClass('panel-success'); break;
        case 'warning': notice.addClass('panel-warning'); break;
        case 'danger': notice.addClass('panel-danger'); break;
        default: notice.addClass('panel-primary'); break;        
      }

      $('.title', notice).css(settings.noticeTitleCss).html(title);
      
      $('.close', notice).click(function(evt) {
        evt.preventDefault();
        $(this).closest('.notice').remove();
      })
      
      $('.message', notice)
      .html(message);

      console.log(notice);
      instance.append(settings.noticeDisplay(notice));
      if(settings.displayTimeout > 0 && settings.doNotClose.indexOf(priority) < 0) {
        setTimeout(function() {
          settings.noticeRemove(notice, function() {
            notice.remove();
          });
        }, settings.displayTimeout);
      }
    },
    r: function(text, expr, val) {
      while(expr.test(text)) {
      text = text.replace(expr, val);
      }
      return text;
    },
  }
  
  var settings = {
    dockId: 'jqueryGrowlDock',
    dockClass: 'growl',
    dockTemplate: '<div></div>',
    dockContainerSelector: 'body',
    dockCss: {
      position: 'fixed',
      top: '10px',
      right: '10px',
      width: '300px',
      zIndex: 50000
    },
    noticeTemplate: 
      '<div class="notice panel">' +
        '<div class="panel-heading ">' +
          '<h3>' +
            '<a class="title"></a>' +
            '<button type="button" class="close">x</button>' +
          '</h3>' +
        '</div>' +
        '<div class="message panel-body">%message%</div>' +
      '</div>',
    noticeCss: {
    },
    noticeTitleCss: {
      color: '#fff',
      textDecoration: 'none',
    },
    noticeDisplay: function(notice) {
      return notice.fadeIn(settings.noticeFadeTimeout);
    },
    noticeRemove: function(notice, callback) {
      return notice.animate({opacity: '0', height: '0px'}, {duration: settings.noticeFadeTimeout, complete: callback });
    },
    doNotClose: ['danger', ],
    noticeFadeTimeout: 'slow',
    displayTimeout: 3500,
    defaultStylesheet: null,
    noticeElement: function(el) {
      this.noticeTemplate = $(el);
    }
  }
  
  $.growl = function(options) {
    if(typeof options === 'object') {
      if('settings' in options) {
        settings = $.extend(settings, options.settings);
      }
      var title = 'Notice', message = null, priority = 'primary';
      if('title' in options) {
        title = options.title;
      }
      if('message' in options) {
        message = options.message;
      }
      if('priority' in options) {
        priority = options.priority;
      }
      if(message != null) {
        growl.notify(title, message, priority);
      }
    }
  }
})();