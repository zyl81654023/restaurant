/**
 * Triggered when the whole page is loaded
 */

var lng = -122.08;
var lat = 37.38;

function onPageLoaded() {
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(onPositionUpdate);
  } else {
    console.warn('navigator.geolocation is not available');
    loadNearbyRestaurants();
  }
}

function onPositionUpdate(position) {
  lat = position.coords.latitude;
  lng = position.coords.longitude;

  console.log('current position: ' + lat + ' ' + lng);

  loadNearbyRestaurants();
}

/**
 * Load the nearby restaurants
 */
function loadNearbyRestaurants() {
  activeBtn('nearby-btn');

  // The request parameters
  var url = '/restaurant/restaurants';
  var params = 'user_id=1111&lat=' + lat + '&lon=' + lng;
  var req = JSON.stringify({});

  ajax('get', url + '?' + params, req, function (res) {
    var restaurants = JSON.parse(res);
    listRestaurants(restaurants);
  });
}

function loadFavoriteRestaurants() {
  activeBtn('fav-btn');

  // The request parameters
  var url = '/restaurant/history';
  var params = 'user_id=1111';
  var req = JSON.stringify({});

  ajax('get', url + '?' + params, req, function (res) {
    var restaurants = JSON.parse(res);
    listRestaurants(restaurants);
  });
}

/**
 * Load favourite restaurants
 */
function loadRecommendedRestaurants() {
  activeBtn('recommend-btn');

  // The request parameters
  var url = '/restaurant/recommendation';
  var params = 'user_id=1111';
  var req = JSON.stringify({});

  ajax('get', url + '?' + params, req, function (res) {
    var restaurants = JSON.parse(res);
    console.dir(restaurants);
    listRestaurants(restaurants);
  });
}

/**
 * List restaurants
 */
function listRestaurants(restaurants) {
  // Clear the current results
  var restaurantList = document.getElementsByClassName('restaurant-list')[0];
  restaurantList.innerHTML = '';

  for (var i = 0; i < restaurants.length; i++) {
    addRestaurant(restaurantList, restaurants[i]);
  }
}

function setFavouriteRestaurant(restaurantId) {
  // The request parameters
  var url = '/restaurant/history';
  var req = JSON.stringify({
    user_id: '1111',
    visited: [restaurantId]
  });

  ajax('post', url, req, function (res) {
    var result = JSON.parse(res);
    console.log(result);
  });
}

/**
 * Add restaurant to the list
 */
function addRestaurant(restaurantList, restaurant) {
  var li = $('li', {className: 'restaurant'});

  // image
  li.appendChild($('img', {src: restaurant.image_url}));

  // section
  var section = $('section');
  // title
  var title = $('title');
  title.innerHTML = restaurant.name;
  section.appendChild(title);
  // category
  var category = $('p', {className: 'category'});
  category.innerHTML = restaurant.categories[0];
  section.appendChild(category);
  // stars
  var stars = $('div', {className: 'stars'});
  for (var i = 0; i < restaurant.stars; i++) {
    var star = $('i', {className: 'fa fa-star'});
    stars.appendChild(star);
  }

  if (('' + restaurant.stars).match(/\.5$/)) {
    stars.appendChild($('i', {className: 'fa fa-star-half-o'}));
  }

  section.appendChild(stars);

  li.appendChild(section);

  // description
  var desc = $('p', {className: 'description'});
  desc.innerHTML = restaurant.full_address.replace(/,/g, '<br/>');
  li.appendChild(desc);

  // fav link
  var favLink = $('p', {className: 'fav-link'});
  favLink.onclick = function () {
    setFavouriteRestaurant(restaurant.business_id);
  }
  favLink.appendChild($('i', {className: restaurant.is_visited ? 'fa fa-heart' : 'fa fa-heart-o'}));
  li.appendChild(favLink);

  restaurantList.appendChild(li);
}

function $(tag, options) {
  var elem = document.createElement(tag);

  for (var option in options) {
    if (options.hasOwnProperty(option)) {
      elem[option] = options[option];
    }
  }

  return elem;
}

function activeBtn(btnId) {
  var btns = document.getElementsByClassName('main-nav-btn');
  for (var i = 0; i < btns.length; i++) {
    btns[i].className = btns[i].className.replace(/\bactive\b/, '');
  }

  var btn = document.getElementById(btnId);
  btn.className = btn.className + ' active';
}

/**
 * Ajax helper
 */
function ajax(method, url, data, callback) {
  var xhr = new XMLHttpRequest();

  xhr.open(method, url, true);

  xhr.onload = function () {
    if (xhr.status == 200) {
      callback(xhr.responseText);
    }
  };

  xhr.onerror = function () {
    console.error("The request couldn't be completed.");
  };

  if (data === null) {
    xhr.send();
  } else {
    xhr.setRequestHeader("Content-Type", "application/json;charset=utf-8");
    xhr.send(data);
  }
}
