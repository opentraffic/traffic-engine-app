/**
 * Copyright (c) 2014, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

import L from 'leaflet';

const position = [51.505, -0.09];
const map = L.map('map-container-test').setView(position, 13);

L.Icon.Default.imagePath = 'stylesheets/leaflet/images';

L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
}).addTo(map);

L.marker(position).addTo(map)
    .bindPopup('A pretty CSS3 popup. <br> Easily customizable.');