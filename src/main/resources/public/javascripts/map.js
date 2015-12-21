var Traffic = Traffic || {};

(function(A, $, L, C, dc) {

	A.app = {};

	A.app.instance = new Backbone.Marionette.Application();

	A.app.instance.addRegions({
		navbar: "#navbar",
		sidebarTabs: "#sidebar-tabs",
		sidebar: "#side-panel"
	});

	A.app.instance.addInitializer(function(options){

		A.app.nav = new A.app.Nav();
		A.app.instance.navbar.show(A.app.nav);

		A.app.sidebarTabs = new A.app.SidebarTabs();
		A.app.instance.sidebarTabs.show(A.app.sidebarTabs);

		var mapWrapper = Object.create(A.app.MapWrapper);
		mapWrapper.init('map', {
			center: [10.3036741,123.8982952],
			 zoom: 13
		});
		
		A.app.mapWrapper = mapWrapper;
		A.app.map = mapWrapper.LMmap;

		// Click Data in sidebar
		$('#data').click();

	});

	A.app.MapWrapper = {

		LMmap: null, // Leaflet map object

		ENGLISH_TILE_LAYER: L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
					attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors',
					maxZoom: 17
				}),
		LOCALIZED_TILE_LAYER: L.tileLayer('https://a.tiles.mapbox.com/v4/conveyal.gepida3i/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoiY29udmV5YWwiLCJhIjoiMDliQURXOCJ9.9JWPsqJY7dGIdX777An7Pw', {
					attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery &copy; <a href="http://mapbox.com">Mapbox</a>',
					maxZoom: 17
				}),

		init:function(mapId, options) {
			options = options || {};
      options.layers = [this.ENGLISH_TILE_LAYER, this.LOCALIZED_TILE_LAYER];

      this.LMmap = L.map(mapId, options);

			this.addScalebar();
			this.addLayersControl();
    },

    addScalebar: function() {
    	if(this.LMmap) {
    		L.control.scale().addTo(this.LMmap);
    	}
    },

    addLayersControl: function() {
    	var baseMaps = {
			  "English Basemap": this.ENGLISH_TILE_LAYER,
			  "International Basemap": this.LOCALIZED_TILE_LAYER
			};

			if(this.LMmap) {
				L.control.layers(baseMaps).addTo(this.LMmap);
			}
    }
	};

	A.app.DataSidebar = Marionette.Layout.extend({

		template: Handlebars.getTemplate('app', 'sidebar-data'),

		initialize : function() {

			var _this = this;

			setInterval(function(){
				$.getJSON('/stats', function(data){
					_this.$('#vehicleCount').text(data.vehicleCount);
					_this.$('#lastTime').text(new Date(data.lastUpdate).toString());
				});
			}, 3000);

		}
	});

	A.app.RoutingSidebar = Marionette.Layout.extend({

		template: Handlebars.getTemplate('app', 'sidebar-routing'),

		events : {
			'click #resetRoute' : 'resetRoute',
			'change #day' : 'getRoute',
			'change #hour' : 'getRoute',
			'change #week1ToList' : 'changeToWeek',
			'change #week2ToList' : 'changeToWeek',
			'change #week1FromList' : 'changeFromWeek',
			'change #week2FromList' : 'changeFromWeek',
			'click #compare' : 'clickCompare'
		},


		resetRoute : function() {
			A.app.sidebarTabs.resetRoute();
		},

		getRoute : function() {
			A.app.sidebarTabs.getRoute();
		},

		onShow : function() {
			var _this = this;
			this.$("#week1To").hide();
			this.$("#week2To").hide();
			this.$("#compareWeekSelector").hide();
			this.$("#percentChangeTitle1").hide();
			this.$("#percentChangeTitle2").hide();
			this.$("#percentChangeLegend").hide();
			this.$("#routeData").hide();


			$.getJSON('/weeks', function(data) {

				data.sort(function(a, b) {
					return a.weekId - b.weekId;
				});

				A.app.sidebar.weekList = data;

				_this.$("#week1FromList").empty();
				_this.$("#week1ToList").empty();

				_this.$("#week2FromList").empty();
				_this.$("#week2ToList").empty();

				_this.$("#week1FromList").append('<option value="-1">All Weeks</option>');
				for(var i in A.app.sidebar.weekList) {
					var weekDate = new Date( data[i].weekStartTime);
					_this.$("#week1FromList").append('<option value="' + data[i].weekId + '">Week of ' + (weekDate.getUTCMonth() + 1) + '/' + weekDate.getUTCDate() + '/' + weekDate.getUTCFullYear() + '</option>');
				}

				_this.$("#week2FromList").empty();
				_this.$("#week2FromList").append('<option value="-1">All Weeks</option>');
				for(var i in A.app.sidebar.weekList) {
					var weekDate = new Date( data[i].weekStartTime);
					_this.$("#week2FromList").append('<option value="' + data[i].weekId + '">Week of ' + (weekDate.getUTCMonth() + 1) + '/' + weekDate.getUTCDate() + '/' + weekDate.getUTCFullYear() + '</option>');
				}


			});

			this.changeFromWeek();
		},

		onClose : function() {
			A.app.sidebarTabs.resetRoute();
		},

		changeFromWeek : function() {

			A.app.sidebar.filterChanged = true;

			this.$("#week1To").hide();
			this.$("#week2To").hide();

			if(this.$("#week1FromList").val() > 0) {
				this.$("#week1ToList").empty();
				for(var i in A.app.sidebar.weekList) {
					if(A.app.sidebar.weekList[i].weekId >= this.$("#week1FromList").val()) {
						var weekDate = new Date( A.app.sidebar.weekList[i].weekStartTime);
						this.$("#week1ToList").append('<option value="' + A.app.sidebar.weekList[i].weekId + '">Week of ' + (weekDate.getUTCMonth() + 1) + '/' + weekDate.getUTCDate() + '/' + weekDate.getUTCFullYear() + '</option>');
					}
					this.$("#week1To").show();
				}
			}


			if(this.$("#week2FromList").val() > 0) {
				this.$("#week2ToList").empty();
				for(var i in A.app.sidebar.weekList) {
					if(A.app.sidebar.weekList[i].weekId >= this.$("#week2FromList").val()) {
						var weekDate = new Date( A.app.sidebar.weekList[i].weekStartTime);
						this.$("#week2ToList").append('<option value="' + A.app.sidebar.weekList[i].weekId + '">Week of ' + (weekDate.getUTCMonth() + 1) + '/' + weekDate.getUTCDate() + '/' + weekDate.getUTCFullYear() + '</option>');
					}
					this.$("#week2To").show();
				}
			}


			this.update();
		},

		changeToWeek : function() {

			A.app.sidebar.filterChanged = true;

			this.update();
		},

		update : function() {
			A.app.sidebar.filterChanged = true;

			if(!A.app.sidebar.hourlyChart)
				return;

			A.app.sidebar.hourExtent = A.app.sidebar.hourlyChart.brush().extent();
			A.app.sidebar.dayExtent = A.app.sidebar.dailyChart.brush().extent();

			var minHour, maxHour, minDay, maxDay;

			if(A.app.sidebar.hourExtent[0] < 1 && A.app.sidebar.hourExtent[1] < 1) {
				minHour = 1
				maxHour = 24;
			}
			else {
				minHour = Math.ceil(A.app.sidebar.hourExtent[0]);
				maxHour = Math.floor(A.app.sidebar.hourExtent[1]);
			}

			if(A.app.sidebar.dayExtent[0] < 1 && A.app.sidebar.dayExtent[1] < 1) {
				minDay = 1
				maxDay = 7;
			}
			else {
				minDay = Math.ceil(A.app.sidebar.dayExtent[0]);
				maxDay = Math.floor(A.app.sidebar.dayExtent[1]);
			}

			var hours = new Array();

			for(d = minDay; d <= maxDay; d++) {
				for(h = minHour; h <= maxHour; h++) {
					hours.push(h + (24 * (d -1)));
				}
			}

			A.app.sidebarTabs.getRoute(hours);
		},

		getWeek1List : function() {
			var wFrom = this.$("#week1FromList").val();

			var wList = new Array();

			if(wFrom == -1) {
				for(var i in A.app.sidebar.weekList) {
					wList.push(A.app.sidebar.weekList[i].weekId );
				}
			} else {
				var wTo = this.$("#week1ToList").val();
				for(var i in A.app.sidebar.weekList) {
					if(A.app.sidebar.weekList[i].weekId >= wFrom && A.app.sidebar.weekList[i].weekId <= wTo)
						wList.push(A.app.sidebar.weekList[i].weekId);
				}

			}

			return wList;
		},

		getWeek2List : function() {
			var wFrom = this.$("#week2FromList").val();

			var wList = new Array();

			if(wFrom == -1) {
				for(var i in A.app.sidebar.weekList) {
					wList.push(A.app.sidebar.weekList[i].weekId );
				}
			} else {
				var wTo = this.$("#week2ToList").val();
				for(var i in A.app.sidebar.weekList) {
					if(A.app.sidebar.weekList[i].weekId >= wFrom && A.app.sidebar.weekList[i].weekId <= wTo)
						wList.push(A.app.sidebar.weekList[i].weekId);
				}

			}

			return wList;
		},

		loadChartData : function(data) {

			data.hours.forEach(function (d) {
				d.hourOfDay = (d.h % 24) + 1;
				d.dayOfWeek = ((d.h - d.hourOfDay) / 24) + 1;
				d.s = d.s * 3.6; // convert from m/s km/h
			});

			if(!this.chartData) {
				this.chartData = C(data.hours);

				this.dayCount = this.chartData.dimension(function (d) {
					return d.dayOfWeek;       // add the magnitude dimension
				});

				this.dayCountGroup = this.dayCount.group().reduce(
					/* callback for when data is added to the current filter results */
					function (p, v) {
						p.count += v.c;
						p.sum += v.s;
						if(p.count > 0)
							p.avg = (p.sum / p.count);
						else
							p.avg = 0;
						return p;
					},
					/* callback for when data is removed from the current filter results */
					function (p, v) {
						p.count -= v.c;
						p.sum -= v.s;
						if(p.count > 0)
							p.avg = (p.sum / p.count);
						else
							p.avg = 0;
						return p;
					},
					/* initialize p */
					function () {
						return {
							count: 0,
							sum: 0,
							avg: 0
						};
					}
				);

				this.hourCount = this.chartData.dimension(function (d) {
					return d.hourOfDay;       // add the magnitude dimension
				});

				this.hourCountGroup = this.hourCount.group().reduce(
					/* callback for when data is added to the current filter results */
					function (p, v) {
						p.count += v.c;
						p.sum += v.s;
						if(p.count > 0)
							p.avg = (p.sum / p.count);
						else
							p.avg = 0;
						return p;
					},
					/* callback for when data is removed from the current filter results */
					function (p, v) {
						p.count -= v.c;
						p.sum -= v.s;
						if(p.count > 0)
							p.avg = (p.sum / p.count);
						else
							p.avg = 0;

						return p;
					},
					/* initialize p */
					function () {
						return {
							count: 0,
							sum: 0,
							avg: 0
						};
					}
				);

				var dayLabel = new Array();
				dayLabel[1] = "Mon";
				dayLabel[2] = "Tue";
				dayLabel[3] = "Wed";
				dayLabel[4] = "Thu";
				dayLabel[5] = "Fri";
				dayLabel[6] = "Sat";
				dayLabel[7] = "Sun";

				this.dailyChart = dc.barChart("#dailyChart");

				if(this.$("#compare").prop( "checked" ))
					A.app.sidebar.percentChange = true;
				else
					A.app.sidebar.percentChange = false;

				this.dailyChart.width(430)
					.height(75)
					.margins({top: 5, right: 10, bottom: 20, left: 40})
					.dimension(this.dayCount)
					.group(this.dayCountGroup)
					.transitionDuration(0)
					.centerBar(true)
					.valueAccessor(function (p) {
						return p.value.avg;
					})
					.gap(10)
					.x(d3.scale.linear().domain([0.5, 7.5]))
					.renderHorizontalGridLines(true)
					.elasticY(true)
					.xAxis().tickFormat(function (d) {
						return dayLabel[d]
					});

				this.dailyChart.yAxis().ticks(4);


				this.dailyChart.yAxis().tickFormat(function (d) {
					if(A.app.sidebar.percentChange)
						return Math.round(d * 100) + "%"
					else
						return d;
				});

				this.dailyChart.brush().on("brushend.custom", A.app.sidebar.update);

				this.hourlyChart = dc.barChart("#hourlyChart");

				this.hourlyChart.width(430)
					.height(100)
					.margins({top: 5, right: 10, bottom: 20, left: 40})
					.dimension(this.hourCount)
					.group(this.hourCountGroup)
					.transitionDuration(0)
					.centerBar(true)
					.valueAccessor(function (p) {
						return p.value.avg;
					})
					.gap(5)
					.x(d3.scale.linear().domain([0.5, 24.5]))
					.renderHorizontalGridLines(true)
					.elasticY(true)
					.xAxis().tickFormat();

				this.hourlyChart.yAxis().ticks(6);

				this.hourlyChart.yAxis().tickFormat(function (d) {
					if(A.app.sidebar.percentChange)
						return Math.round(d * 100) + "%"
					else
						return d;
				});

				this.hourlyChart.brush().on("brushend.custom", A.app.sidebar.update);

			}
			else {

				this.hourlyChart.filterAll();
				this.dailyChart.filterAll();
				this.chartData.remove();


				this.chartData.add(data.hours);

				if(A.app.sidebar.hourExtent && ( A.app.sidebar.hourExtent[0] >= 1.0 ||  A.app.sidebar.hourExtent[1] >= 1.0)) {
					this.hourlyChart.filter(dc.filters.RangedFilter(A.app.sidebar.hourExtent[0], A.app.sidebar.hourExtent[1]));
				}
				if(A.app.sidebar.dayExtent && ( A.app.sidebar.dayExtent[0] >= 1.0 ||  A.app.sidebar.dayExtent[1] >= 1.0)) {
					this.dailyChart.filter(dc.filters.RangedFilter(A.app.sidebar.dayExtent[0], A.app.sidebar.dayExtent[1]));
				}

				this.hourlyChart.brush().on("brushend.custom", A.app.sidebar.update);
				this.dailyChart.brush().on("brushend.custom", A.app.sidebar.update);
			}

			dc.renderAll();
		},

		initialize : function() {

			_.bindAll(this, 'update', 'changeFromWeek', 'changeToWeek');
		},

		onRender : function () {
			this.$("#journeyInfo").hide();
		}
	});

	A.app.AnalysisSidebar = Marionette.Layout.extend({

		template: Handlebars.getTemplate('app', 'sidebar-analysis'),

		events : {
			'change #week1ToList' : 'changeToWeek',
			'change #week2ToList' : 'changeToWeek',
			'change #week1FromList' : 'changeFromWeek',
			'change #week2FromList' : 'changeFromWeek',
			'change #confidenceInterval' : 'changeConfidenceInterval',
			'change #normalizeByTime' : 'changeNormalizeBy',
			'click #compare' : 'clickCompare'
		},

		resetRoute : function() {
			A.app.sidebarTabs.resetRoute();
		},

		getRoute : function() {
			A.app.sidebarTabs.getRoute();
		},

		initialize : function() {

			var _this = this;

			_.bindAll(this, 'updateTrafficTiles', 'addTrafficOverlay', 'update', 'changeFromWeek', 'changeToWeek', 'clickCompare', 'changeConfidenceInterval', 'changeNormalizeBy');

		},

		onClose : function() {
			A.app.map.off("moveend", A.app.sidebar.mapMove);
		},

		onShow : function() {

			var _this = this;

			A.app.map.on("moveend", A.app.sidebar.mapMove);

			this.$("#week1To").hide();
			this.$("#week2To").hide();
			this.$("#compareWeekSelector").hide();
			this.$("#percentChangeTitle1").hide();
			this.$("#percentChangeTitle2").hide();
			this.$("#percentChangeLegend").hide();


			$.getJSON('/weeks', function(data) {

				data.sort(function(a, b) {
					return a.weekId - b.weekId;
				});

				A.app.sidebar.weekList = data;

				_this.$("#week1FromList").empty();
				_this.$("#week1ToList").empty();

				_this.$("#week2FromList").empty();
				_this.$("#week2ToList").empty();

				_this.$("#week1FromList").append('<option value="-1">All Weeks</option>');
				for(var i in A.app.sidebar.weekList) {
					var weekDate = new Date( data[i].weekStartTime);
					_this.$("#week1FromList").append('<option value="' + data[i].weekId + '">Week of ' + (weekDate.getUTCMonth() + 1) + '/' + weekDate.getUTCDate() + '/' + weekDate.getUTCFullYear() + '</option>');
				}

				_this.$("#week2FromList").empty();
				_this.$("#week2FromList").append('<option value="-1">All Weeks</option>');
				for(var i in A.app.sidebar.weekList) {
					var weekDate = new Date( data[i].weekStartTime);
					_this.$("#week2FromList").append('<option value="' + data[i].weekId + '">Week of ' + (weekDate.getUTCMonth() + 1) + '/' + weekDate.getUTCDate() + '/' + weekDate.getUTCFullYear() + '</option>');
				}


			});

			this.changeFromWeek();

		},

		clickCompare : function() {

			A.app.sidebar.filterChanged = true;

			if(this.$("#compare").prop( "checked" )) {
				this.$("#compareWeekSelector").show();
				this.$("#percentChangeTitle1").show();
				this.$("#percentChangeTitle2").show();
				this.$("#percentChangeLegend").show();
				this.$("#speedLegend").hide();

				A.app.sidebar.percentChange = true;
			}
			else {
				this.$("#compareWeekSelector").hide();
				this.$("#percentChangeTitle1").hide();
				this.$("#percentChangeTitle2").hide();
				this.$("#percentChangeLegend").hide();
				this.$("#speedLegend").show();
				A.app.sidebar.percentChange = false;
			}

			this.update();
		},

		changeFromWeek : function() {

			A.app.sidebar.filterChanged = true;
			A.app.sidebar.hourExtent = false;
			A.app.sidebar.dayExtent = false;

			this.$("#week1To").hide();
			this.$("#week2To").hide();

			if(this.$("#week1FromList").val() > 0) {
				this.$("#week1ToList").empty();
				for(var i in A.app.sidebar.weekList) {
					if(A.app.sidebar.weekList[i].weekId >= this.$("#week1FromList").val()) {
						var weekDate = new Date( A.app.sidebar.weekList[i].weekStartTime);
						this.$("#week1ToList").append('<option value="' + A.app.sidebar.weekList[i].weekId + '">Week of ' + (weekDate.getUTCMonth() + 1) + '/' + weekDate.getUTCDate() + '/' + weekDate.getUTCFullYear() + '</option>');
					}
					this.$("#week1To").show();
				}
			}


			if(this.$("#week2FromList").val() > 0) {
				this.$("#week2ToList").empty();
				for(var i in A.app.sidebar.weekList) {
					if(A.app.sidebar.weekList[i].weekId >= this.$("#week2FromList").val()) {
						var weekDate = new Date( A.app.sidebar.weekList[i].weekStartTime);
						this.$("#week2ToList").append('<option value="' + A.app.sidebar.weekList[i].weekId + '">Week of ' + (weekDate.getUTCMonth() + 1) + '/' + weekDate.getUTCDate() + '/' + weekDate.getUTCFullYear() + '</option>');
					}
					this.$("#week2To").show();
				}
			}


			this.update();
		},

		changeToWeek : function() {

			A.app.sidebar.filterChanged = true;
			A.app.sidebar.hourExtent = false;
			A.app.sidebar.dayExtent = false;

			this.update();
		},

		changeNormalizeBy: function() {

			A.app.sidebar.filterChanged = true;
			this.update();
		},

		changeConfidenceInterval : function() {

			A.app.sidebar.filterChanged = true;
			this.update();
		},

		getWeek1List : function() {
			var wFrom = this.$("#week1FromList").val();

			var wList = new Array();

			if(wFrom == -1) {
				for(var i in A.app.sidebar.weekList) {
					wList.push(A.app.sidebar.weekList[i].weekId );
				}
			} else {
				var wTo = this.$("#week1ToList").val();
				for(var i in A.app.sidebar.weekList) {
					if(A.app.sidebar.weekList[i].weekId >= wFrom && A.app.sidebar.weekList[i].weekId <= wTo)
						wList.push(A.app.sidebar.weekList[i].weekId);
				}

			}

			return wList;
		},

		getWeek2List : function() {
			var wFrom = this.$("#week2FromList").val();

			var wList = new Array();

			if(wFrom == -1) {
				for(var i in A.app.sidebar.weekList) {
					wList.push(A.app.sidebar.weekList[i].weekId );
				}
			} else {
				var wTo = this.$("#week2ToList").val();
				for(var i in A.app.sidebar.weekList) {
					if(A.app.sidebar.weekList[i].weekId >= wFrom && A.app.sidebar.weekList[i].weekId <= wTo)
						wList.push(A.app.sidebar.weekList[i].weekId);
				}

			}

			return wList;
		},

		mapMove : function() {
			A.app.sidebar.filterChanged = false;
			A.app.sidebar.update();
		},

		update : function() {

			var _this = this;

			var bounds = A.app.map.getBounds();
			var x1 = bounds.getWest();
			var x2 = bounds.getEast();
			var y1 = bounds.getNorth();
			var y2 = bounds.getSouth();

			var confidenceInterval = this.$("#confidenceInterval").val();
			var normalizeByTime = this.$("#normalizeByTime").val();

			var url = '/weeklyStats?confidenceInterval=' + confidenceInterval + '&normalizeByTime=' + normalizeByTime + '&x1=' + x1 + '&x2=' + x2 + '&y1=' + y1 + '&y2=' + y2;

			var w1List = this.getWeek1List();
			var w2List = this.getWeek2List();

			url += "&w1=" + w1List.join(",");

			if(this.$("#compare").prop( "checked" )) {
				if (w2List && w2List.length > 0)
					url += '&w2=' + w2List.join(",");
			}

			$.getJSON(url, function(chartData){
				A.app.sidebar.loadChartData(chartData)
			});
		},

		loadChartData : function(data) {

			data.hours.forEach(function (d) {
				d.hourOfDay = (d.h % 24) + 1;
				d.dayOfWeek = ((d.h - d.hourOfDay) / 24) + 1;
				d.s = d.s * 3.6; // convert from m/s km/h
			});

			if(!this.chartData) {
				this.chartData = C(data.hours);

				this.dayCount = this.chartData.dimension(function (d) {
					return d.dayOfWeek;       // add the magnitude dimension
				});

				this.dayCountGroup = this.dayCount.group().reduce(
					/* callback for when data is added to the current filter results */
					function (p, v) {
						p.count += v.c;
						p.sum += v.s;
						if(p.count > 0)
							p.avg = (p.sum / p.count);
						else
							p.avg = 0;
						return p;
					},
					/* callback for when data is removed from the current filter results */
					function (p, v) {
						p.count -= v.c;
						p.sum -= v.s;
						if(p.count > 0)
							p.avg = (p.sum / p.count);
						else
							p.avg = 0;
						return p;
					},
					/* initialize p */
					function () {
						return {
							count: 0,
							sum: 0,
							avg: 0
						};
					}
				);

				this.hourCount = this.chartData.dimension(function (d) {
					return d.hourOfDay;       // add the magnitude dimension
				});

				this.hourCountGroup = this.hourCount.group().reduce(
					/* callback for when data is added to the current filter results */
					function (p, v) {
						p.count += v.c;
						p.sum += v.s;
						if(p.count > 0)
							p.avg = (p.sum / p.count);
						else
							p.avg = 0;
						return p;
					},
					/* callback for when data is removed from the current filter results */
					function (p, v) {
						p.count -= v.c;
						p.sum -= v.s;
						if(p.count > 0)
							p.avg = (p.sum / p.count);
						else
							p.avg = 0;

						return p;
					},
					/* initialize p */
					function () {
						return {
							count: 0,
							sum: 0,
							avg: 0
						};
					}
				);

				var dayLabel = new Array();
				dayLabel[1] = "Mon";
				dayLabel[2] = "Tue";
				dayLabel[3] = "Wed";
				dayLabel[4] = "Thu";
				dayLabel[5] = "Fri";
				dayLabel[6] = "Sat";
				dayLabel[7] = "Sun";

				this.dailyChart = dc.barChart("#dailyChart");

				if(this.$("#compare").prop( "checked" ))
					A.app.sidebar.percentChange = true;
				else
					A.app.sidebar.percentChange = false;

				this.dailyChart.width(430)
					.height(75)
					.margins({top: 5, right: 10, bottom: 20, left: 40})
					.dimension(this.dayCount)
					.group(this.dayCountGroup)
					.transitionDuration(0)
					.centerBar(true)
					.valueAccessor(function (p) {
						return p.value.avg;
					})
					.gap(10)
					.x(d3.scale.linear().domain([0.5, 7.5]))
					.renderHorizontalGridLines(true)
					.elasticY(true)
					.xAxis().tickFormat(function (d) {
						return dayLabel[d]
					});

				this.dailyChart.yAxis().ticks(4);


				this.dailyChart.yAxis().tickFormat(function (d) {
					if(A.app.sidebar.percentChange)
						return Math.round(d * 100) + "%"
					else
						return d;
				});

				this.dailyChart.brush().on("brushend.custom", A.app.sidebar.updateTrafficTiles);

				this.hourlyChart = dc.barChart("#hourlyChart");

				this.hourlyChart.width(430)
					.height(100)
					.margins({top: 5, right: 10, bottom: 20, left: 40})
					.dimension(this.hourCount)
					.group(this.hourCountGroup)
					.transitionDuration(0)
					.centerBar(true)
					.valueAccessor(function (p) {
						return p.value.avg;
					})
					.gap(5)
					.x(d3.scale.linear().domain([0.5, 24.5]))
					.renderHorizontalGridLines(true)
					.elasticY(true)
					.xAxis().tickFormat();

				this.hourlyChart.yAxis().ticks(6);

				this.hourlyChart.yAxis().tickFormat(function (d) {
					if(A.app.sidebar.percentChange)
						return Math.round(d * 100) + "%"
					else
						return d;
				});

				this.hourlyChart.brush().on("brushend.custom", A.app.sidebar.updateTrafficTiles);

			}
			else {

				this.hourlyChart.filterAll();
				this.dailyChart.filterAll();
				this.chartData.remove();


				this.chartData.add(data.hours);

				if(A.app.sidebar.hourExtent && ( A.app.sidebar.hourExtent[0] >= 1.0 ||  A.app.sidebar.hourExtent[1] >= 1.0)) {
					this.hourlyChart.filter(dc.filters.RangedFilter(A.app.sidebar.hourExtent[0], A.app.sidebar.hourExtent[1]));
				}
				if(A.app.sidebar.dayExtent && ( A.app.sidebar.dayExtent[0] >= 1.0 ||  A.app.sidebar.dayExtent[1] >= 1.0)) {
					this.dailyChart.filter(dc.filters.RangedFilter(A.app.sidebar.dayExtent[0], A.app.sidebar.dayExtent[1]));
				}

				this.hourlyChart.brush().on("brushend.custom", A.app.sidebar.updateTrafficTiles);
				this.dailyChart.brush().on("brushend.custom", A.app.sidebar.updateTrafficTiles);
			}



			dc.renderAll();

			this.addTrafficOverlay();
		},

		addTrafficOverlay : function(hours) {

			if(!A.app.sidebar.filterChanged)
				return;

			if(A.app.segmentOverlay && A.app.map.hasLayer(A.app.segmentOverlay))
				A.app.map.removeLayer(A.app.segmentOverlay);

			var hoursStr;

			if(hours && hours.length > 0)
				hoursStr = hours.join(",");

			var w1List = A.app.sidebar.getWeek1List();
			var w2List = A.app.sidebar.getWeek2List();

			var confidenceInterval = this.$("#confidenceInterval").val();
			var normalizeByTime = this.$("#normalizeByTime").val();

			var url = '/tile/traffic?z={z}&x={x}&y={y}&confidenceInterval=' + confidenceInterval + '&normalizeByTime=' + normalizeByTime;

			if(hoursStr)
				url += '&h=' + hoursStr;

			if(w1List && w1List.length > 0)
				url += '&w1=' + w1List.join(",");

			if(this.$("#compare").prop( "checked" )) {
				if (w2List && w2List.length > 0)
					url += '&w2=' + w2List.join(",");
			}

			A.app.segmentOverlay = L.tileLayer(url).addTo(A.app.map);
		},

		updateTrafficTiles : function() {

			A.app.sidebar.filterChanged = true;

			A.app.sidebar.hourExtent = A.app.sidebar.hourlyChart.brush().extent();
			A.app.sidebar.dayExtent = A.app.sidebar.dailyChart.brush().extent();

			var minHour, maxHour, minDay, maxDay;

			if(A.app.sidebar.hourExtent[0] < 1 && A.app.sidebar.hourExtent[1] < 1) {
				minHour = 1
				maxHour = 24;
			}
			else {
				minHour = Math.ceil(A.app.sidebar.hourExtent[0]);
				maxHour = Math.floor(A.app.sidebar.hourExtent[1]);
			}

			if(A.app.sidebar.dayExtent[0] < 1 && A.app.sidebar.dayExtent[1] < 1) {
				minDay = 1
				maxDay = 7;
			}
			else {
				minDay = Math.ceil(A.app.sidebar.dayExtent[0]);
				maxDay = Math.floor(A.app.sidebar.dayExtent[1]);
			}

			var hours = new Array();

			for(d = minDay; d <= maxDay; d++) {
				for(h = minHour; h <= maxHour; h++) {
					hours.push(h + (24 * (d -1)));
				}
			}

			A.app.sidebar.addTrafficOverlay(hours);
		},

		onRender : function () {
			this.$("#journeyInfo").hide();
		}
	});

	A.app.Nav = Marionette.Layout.extend({

		template: Handlebars.getTemplate('app', 'navbar'),

		events : {
			'click #location' : 'clickLocation'
		},

		initialize : function() {
			var _this = this;

			$.getJSON('/clusters', function(data) {
				var sortedClusters = _.sortBy(data.clusters, function(item){ return item.name});
				_this.clusters = sortedClusters;
				_this.render();
			});

			_.bindAll(this, 'clickLocation');
		},

		clickLocation : function(evt) {
			var lat = $(evt.target).data('lat');
			var lon = $(evt.target).data('lon');

			A.app.map.setView([lat,lon], 13);
		},

		onRender : function() {

			var _this = this;

			// Get rid of that pesky wrapping-div.
			// Assumes 1 child element present in template.
			this.$el = this.$el.children();

			this.$("#locationList").empty();

			_.each(this.clusters, function(cluster) {
				_this.$("#locationList").append('<li><a href="#" id="location" data-lat="' + cluster.lat + '" data-lon="' + cluster.lon + '">' + cluster.name + '</a></li>');
			});

			this.$el.unwrap();
			this.setElement(this.$el);

		}
	});

	A.app.SidebarTabs = Marionette.Layout.extend({

		template: Handlebars.getTemplate('app', 'sidebar-tabs'),

		events : {
			'click #data' : 'clickData',
			'click #routing' : 'clickRouting',
			'click #analysis' : 'clickAnalysis'
		},

		initialize : function() {
			var _this = this;

			_.bindAll(this, 'onMapClick', 'clickAnalysis');
		},

		clickData: function(evt) {
			this.endRouting();

			A.app.sidebar = new A.app.DataSidebar();
			A.app.instance.sidebar.show(A.app.sidebar);

			this.$("li").removeClass("active");
			this.$("#data").addClass("active");

			if(A.app.map.hasLayer(A.app.segmentOverlay))
				A.app.map.removeLayer(A.app.segmentOverlay);

			A.app.dataOverlay = L.tileLayer('/tile/data?z={z}&x={x}&y={y}').addTo(A.app.map);
		},

		clickRouting: function(evt) {
			this.startRouting();

			A.app.sidebar = new A.app.RoutingSidebar();
			A.app.instance.sidebar.show(A.app.sidebar);

			this.$("li").removeClass("active");
			this.$("#routing").addClass("active");

			if(A.app.map.hasLayer(A.app.dataOverlay))
				A.app.map.removeLayer(A.app.dataOverlay);

			if(A.app.map.hasLayer(A.app.segmentOverlay))
				A.app.map.removeLayer(A.app.segmentOverlay);

			if(A.app.map.hasLayer(A.app.pathOverlay))
				A.app.map.removeLayer(A.app.pathOverlay);
		},

		clickAnalysis: function(evt) {
			A.app.sidebar = new A.app.AnalysisSidebar();
			A.app.instance.sidebar.show(A.app.sidebar);

			this.endRouting();

			this.$("li").removeClass("active");
			this.$("#analysis").addClass("active");

			if(A.app.map.hasLayer(A.app.dataOverlay))
				A.app.map.removeLayer(A.app.dataOverlay);

			A.app.sidebar.addTrafficOverlay()
		},

		resetRoute : function() {

			if(A.app.sidebar) {
				A.app.sidebar.$("#clickInfo").show();
				A.app.sidebar.$("#routeData").hide();
			}

			if(this.startPoint != false) {
				if(A.app.map.hasLayer(this.startPoint))
					A.app.map.removeLayer(this.startPoint);
			}

			if(this.endPoint != false) {
				if(A.app.map.hasLayer(this.endPoint))
					A.app.map.removeLayer(this.endPoint);
			}

			if(A.app.map.hasLayer(A.app.pathOverlay))
				A.app.map.removeLayer(A.app.pathOverlay);

			this.startPoint = false;
			this.endPoint = false;


		},

		startRouting : function() {
			A.app.map.on("click", this.onMapClick);
			this.resetRoute();
		},

		endRouting : function() {
			A.app.map.off("click", this.onMapClick);
			this.resetRoute();
		},

		onMapClick : function(evt) {

			if(this.startPoint == false) {
				this.startPoint = L.circleMarker(evt.latlng, {fillColor: "#0D0", color: '#fff', fillOpacity: 1.0,opacity: 1.0, radius: 5}).addTo(A.app.map);
			}
			else if(this.endPoint == false) {
				this.endPoint = L.circleMarker(evt.latlng, {fillColor: "#D00", color: '#fff', fillOpacity: 1.0,opacity: 1.0, radius: 5}).addTo(A.app.map);
				this.getRoute();
			}

		},

		getRoute : function(hours) {

			if(A.app.map.hasLayer(A.app.pathOverlay))
				A.app.map.removeLayer(A.app.pathOverlay);

			if(!this.startPoint || !this.endPoint)
				return;

			var startLatLng = this.startPoint.getLatLng();
			var endLatLng = this.endPoint.getLatLng();

			var hoursStr;

			if(hours && hours.length > 0)
				hoursStr = hours.join(",");

			var w1List = A.app.sidebar.getWeek1List();
			var w2List = A.app.sidebar.getWeek2List();

			var confidenceInterval = this.$("#confidenceInterval").val();
			var normalizeByTime = this.$("#normalizeByTime").val();

			var url = '/route?fromLat=' + startLatLng.lat + '&fromLon=' + startLatLng.lng + '&toLat=' + endLatLng.lat + '&toLon=' + endLatLng.lng;

			if(hoursStr)
				url += '&h=' + hoursStr;

			if(w1List && w1List.length > 0)
				url += '&w1=' + w1List.join(",");

			if(this.$("#compare").prop( "checked" )) {
				if (w2List && w2List.length > 0)
					url += '&w2=' + w2List.join(",");
			}

			$.getJSON(url, function(data){

				var distance = 0;
				var time = 0;

				var lines = new Array();
				for(i in data.pathEdges) {
					var edge = data.pathEdges[i];

					var polyLine = L.Polyline.fromEncoded(edge.geometry);

					lines.push(L.polyline(polyLine.getLatLngs(), {opacity: 1.0, color: edge.color}));

					if(edge.speed > 0 && edge.length > 0) {
						distance += edge.length;
						time += edge.length * (1 /edge.speed);
					}
				}

				A.app.pathOverlay = L.featureGroup(lines);

				A.app.pathOverlay.addTo(A.app.map);

				A.app.sidebar.$("#clickInfo").hide();
				A.app.sidebar.$("#journeyInfo").show();

				var seconds = time % 60;
				var minutes = time / 60;

				var speed =  (distance / time) * 3.6;

				A.app.sidebar.$("#travelTime").text(Math.round(minutes) + "m " + Math.round(seconds) + "s");
				A.app.sidebar.$("#avgSpeed").text(speed.toPrecision(2) + "km");

				A.app.sidebar.loadChartData(data.weeklyStats);

				A.app.sidebar.$("#clickInfo").hide();
				A.app.sidebar.$("#routeData").show();

			});
		}
	});


})(Traffic, jQuery, L, crossfilter, dc);


$(document).ready(function() {

	Traffic.app.instance.start();

});
