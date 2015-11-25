import concat from 'concat-stream'
import dbg from 'debug'
import http from 'http'

const LAYER_DEFAULTS = {}

const REQUEST_DEFAULTS = {
  accessModes: 'WALK',
  egressModes: 'WALK',
  date: new Date().toISOString().split('T')[0],
  fromTime: 25200,
  toTime: 32400,
  walkSpeed: 1.3333333333333333,
  bikeSpeed: 4.1,
  carSpeed: 20,
  streetTime: 90,
  maxWalkTime: 20,
  maxBikeTime: 45,
  maxCarTime: 45,
  minBikeTime: 10,
  minCarTime: 10,
  suboptimalMinutes: 5,
  analyst: true,
  bikeSafe: 1,
  bikeSlope: 1,
  bikeTime: 1
}

const debug = dbg('analyst.js')

/**
 * Create an instance of Analyst.js for use with single point requests.
 *
 * @param {Leaflet} L Pass in an instance of Leaflet so that it doesn't need to be packaged as a dependency.
 * @param {Object} options Options object.
 * @param {String} [options.baseUrl] The base url, will set the tile and api urls if they are left unset
 * @param {String} [options.apiUrl]
 * @param {String} [options.tileUrl]
 * @param {String} [options.connectivityType]
 * @param {Number} [options.timeLimit] Defaults to 3600
 * @param {Boolean} [options.showPoints] Defaults to false
 * @param {Boolean} [options.showIso] Defaults to true
 * @param {Object} [options.requestOptions] Pass in default request options to be used.
 * @param {Object} [options.tileLayerOptions] Pass in default tileLayerOptions to use.
 * @example
 * const analyst = new Analyst(window.L, {
 *   apiUrl: 'http://localhost:3000/api',
 *   tileUrl: 'http://localhost:4000/tile',
 *   baseUrl: 'http://localhost:3000'
 * })
 */

export default class Analyst {
  constructor (L, opts = {}) {
    this.apiUrl = opts.apiUrl
    this.tileUrl = opts.tileUrl
    this.baseUrl = opts.baseUrl

    if (this.apiUrl === undefined) {
      this.apiUrl = `${this.baseUrl}/api`
    }

    if (this.tileUrl === undefined) {
      this.tileUrl = `${this.baseUrl}/tile`
    }

    this.connectivityType = opts.connectivityType || 'AVERAGE'
    this.timeLimit = opts.timeLimit || 3600
    this.showPoints = !!opts.showPoints
    this.showIso = !!opts.showIso

    this.L = L

    this.requestOptions = Object.assign({}, REQUEST_DEFAULTS, opts.requestOptions)
    this.tileLayerOptions = Object.assign({}, LAYER_DEFAULTS, opts.tileLayerOptions)
  }

  /**
   * Update/create the single point layer for this Analyst.js instance.
   *
   * @param {String} key Key for accessing the single point layer tiles.
   * @param {String} [comparisonKey] Key for the layer to compare against.
   * @return {TileLayer} A Leaflet tile layer that pulls in the generated single point tiles.
   * @example
   * analyst.updateSinglePointLayer(key).redraw()
   */

  updateSinglePointLayer (key, comparisonKey) {
    let keyVal = key
    if (comparisonKey) {
      keyVal = comparisonKey + '/' + keyVal
    }

    const url = `${this.tileUrl}/single/${keyVal}/{z}/{x}/{y}.png?which=${this.connectivityType}&timeLimit=${this.timeLimit}&showPoints=${this.showPoints}&showIso=${this.showIso}`

    if (!this.singlePointLayer) {
      debug(`creating single point layer with url: ${url}`)
      this.singlePointLayer = this.L.tileLayer(url, this.tileLayerOptions)
    } else {
      debug(`updating single point layer to url: ${url}`)
      this.singlePointLayer.setUrl(url)
    }

    return this.singlePointLayer
  }

  /**
   * Get all of the available shapefiles.
   *
   * @return {Promise} Resolves with a JSON list of shapefiles.
   * @example
   * analyst.shapefiles().then(function (shapefiles) {
   *   console.log(shapefiles)
   * })
   */

  shapefiles () {
    debug('fetching available shapefiles')
    return this.get(this.apiUrl + '/shapefile').then(r => r.json())
  }

  /**
   * Run a single point request and generate a tile layer.
   *
   * @param {LatLng} point
   * @param {String} graphId Graph ID to use for this request.
   * @param {String} [shapefileId] Shapefile ID to be used with this request, can be omitted for a vector request.
   * @param {Object} [options] Options object.
   * @return {Promise} Resolves with an object containing the results data.
   * @example
   * analyst
   *   .singlePointRequest(marker.getLatLng())
   *   .then(function (data) {
   *     analyst.updateSinglePointLayer(data.key)
   *   })
   */

  singlePointRequest (point, graphId, shapefileId, options = {}) {
    if (!point) return Promise.reject(new Error('Lat/lng point required.'))
    if (typeof shapefileId === 'object') {
      options = shapefileId
      shapefileId = undefined
    }

    const opts = Object.assign({}, this.requestOptions, options)
    opts.fromLat = opts.toLat = point.lat
    opts.fromLon = opts.toLon = point.lng

    debug(`making single point request to [${point.lng}, ${point.lat}]`, opts)
    return this.post(this.apiUrl + '/single', {
      destinationPointsetId: shapefileId,
      graphId: graphId,
      profileRequest: opts
    })
    .then((data) => {
      debug('single point request successful')

      return data
    })
  }

  /**
   * Compare two scenarios.
   *
   * @param {LatLng} point
   * @param {Object} options
   * @param {Object} comparisonOptions
   * @return {Promise} Resolves with an array containing `[results, comparisonResults]`
   * @example
   * analyst
   *   .singlePointComparison(marker.getLatLng(), { graphId: 'graph1' }, { graphId: 'graph2' })
   *   .then(([res, cres]) => {
   *     analyst.updateSinglePointLayer(res.key, cres.key)
   *   })
   */

  singlePointComparison (point, options, comparisonOptions) {
    return Promise.all([
      this.singlePointRequest(point, options.graphId, options.shapefileId, options),
      this.singlePointRequest(point, comparisonOptions.graphId, comparisonOptions.shapefileId, comparisonOptions)
    ])
  }

  /**
   * Set the client credentials for this analyst.js instance
   * @param {String} clientCredentials
   */

  setClientCredentials (clientCredentials) {
    this.clientCredentials = clientCredentials
  }

  /**
   * Get client credentials for this instance, optionally keeping them up-to-date automatically.
   * Note that using this function on the client side requires your API key and secret to be sent to the client, which
   * is non-ideal; a better pattern would be to keep the API key/secret on the server and only retrieve client credentials
   * there, and then deliver only the client credentials to the client, setting them using setClientCredentials.
   * Client credentials have a limited time to live, so they are less sensitive than API keys.
   *
   * @param {String} key Your API key
   * @param {String} secret Your API secret
   * @param {boolean} refresh if true (default), automatically update client credentials when they are about to expire.
   * @return the client credentials. They will also be set as the client credentials for this analyst instance.
   */

  obtainClientCredentials (key, secret, refresh = true) {
    const url = `${this.baseUrl}/oauth/token`
    const [, proto, host, port, path] = url.match(getHostPortAndPath)

    const params = {
      host,
      path,
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      withCredentials: false
    }

    if (port !== undefined) {
      params.port = port
    } else if (proto.toLowerCase() === 'https') {
      debug('https')
      params.port = 443
    }

    const req = http.request(params, (res) => {
      res.pipe(concat((data) => {
        const parsed = JSON.parse(data)
        this.setClientCredentials(parsed.access_token)

        // get new credentials two minutes before these expire
        // note: does not create tail recursion problems as setTimeout puts it in a global executor loop and
        // the call to obtainClientCredentials does not inherit the scope of the parent
        if (refresh) {
          setTimeout(() => {
            this.obtainClientCredentials(key, secret, true)
          }, (parsed.expires_in - 120) * 1000)
        }
      }))
    })

    req.write('grant_type=client_credentials&')
    req.write(`key=${encodeURIComponent(key)}&`)
    req.write(`secret=${encodeURIComponent(secret)}`)
    req.end()
  }

  post (url, data) {
    return new Promise((resolve, reject) => {
      let [, , host, port, path] = url.match(getHostPortAndPath)

      if (this.clientCredentials !== undefined) {
        if (path.indexOf('?') !== -1) {
          path += `&accessToken=${this.clientCredentials}`
        } else {
          path += `?accessToken=${this.clientCredentials}`
        }
      }

      const params = {
        host,
        path,
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        withCredentials: false
      }

      if (port !== undefined) params.port = port

      debug('POST', params)
      const req = http.request(params, (res) => {
        res.on('error', reject)
        res.pipe(concat((data) => {
          resolve(JSON.parse(data))
        }))
      })

      req.on('error', reject)
      req.write(JSON.stringify(data))
      req.end()
    })
  }

  get (url) {
    return new Promise((resolve, reject) => {
      let [, , host, port, path] = url.match(getHostPortAndPath)

      if (this.clientCredentials !== undefined) {
        if (path.indexOf('?') !== -1) {
          path += `&accessToken=${this.clientCredentials}`
        } else {
          path += `?accessToken=${this.clientCredentials}`
        }
      }

      const params = {
        host,
        path,
        method: 'GET',
        headers: {},
        withCredentials: false
      }

      if (port !== undefined) params.port = port

      http.request(params, (res) => {
        res.on('error', reject)
        res.pipe(concat((data) => {
          resolve(JSON.parse(data))
        }))
      }).on('error', reject)
       .end()
    })
  }
}

const getHostPortAndPath = new RegExp('^(.*)://([A-Za-z0-9\-\.]+):([0-9]+)?(.*)$')
