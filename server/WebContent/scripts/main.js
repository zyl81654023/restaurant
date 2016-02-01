/**
 * Triggered when the whole page is loaded
 */

var lng = -122.08;
var lat = 37.38;
var user_id = '1111';
var projectname = '/chihuo';

function onPageLoaded() {
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(onPositionUpdated, onLoadPositionFailed, {maximumAge: 60000});
    showLoadingMessage('Retrieving your location...');
  } else {
	onLoadPositionFailed();
  }
}

function onPositionUpdated(position) {
  lat = position.coords.latitude;
  lng = position.coords.longitude;
  
  loadNearbyRestaurants();
}

function onLoadPositionFailed() {
  console.warn('navigator.geolocation is not available');
  loadNearbyRestaurants();
}

/**
 * Load the nearby restaurants
 */
function loadNearbyRestaurants() {
  activeBtn('nearby-btn');

  // The request parameters
  var url = projectname + '/restaurants';
  var params = 'user_id=' + user_id + '&lat=' + lat + '&lon=' + lng;
  var req = JSON.stringify({});

  showLoadingMessage('Loading nearby restaurants...');
  
  ajax('get', url + '?' + params, req, 
	function (res) {
      var restaurants = JSON.parse(res);
      if (!restaurants || restaurants.length === 0) {
    	showWarningMessage('No nearby restaurant.');
      } else {
        listRestaurants(restaurants);
      }
    },
    function () {
      showErrorMessage('Cannot load nearby restaurants.');
    }  
  );
}

function loadFavoriteRestaurants() {
  activeBtn('fav-btn');

  // The request parameters
  var url = projectname + '/history';
  var params = 'user_id=' + user_id;
  var req = JSON.stringify({});
  
  showLoadingMessage('Loading favorite restaurants...');

  ajax('get', url + '?' + params, req, 
    function (res) {
      var restaurants = JSON.parse(res);
      if (!restaurants || restaurants.length === 0) {
    	showWarningMessage('No favorite restaurant.');
      } else {
    	listRestaurants(restaurants);
      }
    },
    function () {
      showErrorMessage('Cannot load favorite restaurants.');
    }  
  );
}

/**
 * Load favorite restaurants
 */
function loadRecommendedRestaurants() {
  activeBtn('recommend-btn');

  // The request parameters
  var url = projectname + '/recommendation';
  var params = 'user_id=' + user_id;
  var req = JSON.stringify({});
  
  showLoadingMessage('Loading recommended restaurants...');

  ajax('get', url + '?' + params, req, 
    function (res) {
      var restaurants = JSON.parse(res);
      if (!restaurants || restaurants.length === 0) {
    	showWarningMessage('No recommended restaurant. Make sure you have favorites.');
      } else {
    	listRestaurants(restaurants);
      }
    },
    function () {
      showErrorMessage('Cannot load recommended restaurants.');
    } 
  );
}

function showLoadingMessage(msg) {
  var restaurantList = document.getElementsByClassName('restaurant-list')[0];
  restaurantList.innerHTML = '<p class="notice"><i class="fa fa-spinner fa-spin"></i> ' + msg + '</p>';
}

function showWarningMessage(msg) {
  var restaurantList = document.getElementsByClassName('restaurant-list')[0];
  restaurantList.innerHTML = '<p class="notice"><i class="fa fa-exclamation-triangle"></i> ' + msg + '</p>';
}

function showErrorMessage(msg) {
  var restaurantList = document.getElementsByClassName('restaurant-list')[0];
  restaurantList.innerHTML = '<p class="notice"><i class="fa fa-exclamation-circle"></i> ' + msg + '</p>';
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

function changeFavoriteRestaurant(business_id) {
  // Check whether this restaurant has been visited or not
  var li = document.getElementById('restaurant-' + business_id);
  var favIcon = document.getElementById('fav-icon-' + business_id);
  var isVisited = !(li.dataset.visited === "true");
  
  // The request parameters
  var url = projectname + '/history';
  var req = JSON.stringify({
    user_id: user_id,
    visited: [business_id]
  });
  var method = isVisited ? 'post' : 'delete';

  ajax(method, url, req, function (res) {
    var result = JSON.parse(res);
    if (result.status === 'OK') {
      li.dataset.visited = isVisited;
      favIcon.className = isVisited ? 'fa fa-heart' : 'fa fa-heart-o';
    }
  });
}

/**
 * Add restaurant to the list
 */
function addRestaurant(restaurantList, restaurant) {
  var business_id = restaurant['business_id'];
  
  var li = $('li', {
	id: 'restaurant-' + business_id,
    className: 'restaurant'
  });
  
  // set the data attribute
  li.dataset.business = business_id;
  li.dataset.visited = restaurant.is_visited;

  // image
  li.appendChild($('img', {src: restaurant.image_url}));

  // section
  var section = $('section');
  
  // title
  var title = $('a', {href: restaurant.url, target: '_blank'});
  title.innerHTML = restaurant.name;
  section.appendChild(title);
  
  // category
  var category = $('p', {className: 'category'});
  category.innerHTML = 'Category: ' + restaurant.categories.join(', ');
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

  // favorite link
  var favLink = $('p', {className: 'fav-link'});
  
  favLink.onclick = function () {
	changeFavoriteRestaurant(business_id);
  }
  
  favLink.appendChild($('i', {
	id: 'fav-icon-' + business_id,
    className: restaurant.is_visited ? 'fa fa-heart' : 'fa fa-heart-o'
  }));
  
  li.appendChild(favLink);

  restaurantList.appendChild(li);
}

function $(tag, options) {
  var element = document.createElement(tag);

  for (var option in options) {
    if (options.hasOwnProperty(option)) {
      element[option] = options[option];
    }
  }

  return element;
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
function ajax(method, url, data, callback, errorHandler) {
  var xhr = new XMLHttpRequest();

  xhr.open(method, url, true);

  xhr.onload = function () {
    if (xhr.status == 200) {
      callback(xhr.responseText);
    }
  };

  xhr.onerror = function () {
    console.error("The request couldn't be completed.");
    errorHandler();
  };

  if (data === null) {
    xhr.send();
  } else {
    xhr.setRequestHeader("Content-Type", "application/json;charset=utf-8");
    xhr.send(data);
  }
}
