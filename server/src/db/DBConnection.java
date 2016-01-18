package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.Restaurant;

import org.json.JSONArray;
import org.json.JSONObject;

import yelp.YelpAPI;

public class DBConnection {
	private Connection conn = null;
	private static final int MAX_RECOMMENDED_RESTAURANTS = 10;
	private static final int MIN_RECOMMENDED_RESTAURANTS = 3;
	
	public DBConnection() {
		this(DBUtil.URL);
	}

	public DBConnection(String url) {
		try {
			//Forcing the class representing the MySQL driver to load and initialize.
			// The newInstance() call is a work around for some broken Java implementations
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void close(){
	    if (conn != null) {
	        try {
	        	conn.close();
	        } catch (Exception e) { /* ignored */}
	    }
	}

	public void SetVisitedRestaurants(String userId, List<String> businessIds) {
		try {
			if (conn == null) {
				return;
			}
			Statement stmt = conn.createStatement();
			String sql = "";
			for (String businessId : businessIds) {
				sql = "INSERT INTO USER_VISIT_HISTORY (`user_id`, `business_id`) VALUES (\""
						+ userId + "\", \"" + businessId + "\")";
				stmt.executeUpdate(sql);
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public void UnsetVisitedRestaurants(String userId, List<String> businessIds) {
		try {
			if (conn == null) {
				return;
			}
			Statement stmt = conn.createStatement();
			String sql = "";
			for (String businessId : businessIds) {
				sql = "DELETE FROM USER_VISIT_HISTORY WHERE `user_id`=\"" + userId + "\" and `business_id` = \""
						+ businessId + "\"";
				stmt.executeUpdate(sql);
			}

		} catch (Exception e) { /* report an error */
			System.out.println(e.getMessage());
		}
	}

	private Set<String> getCategories(String businessId) {
		try {
			if (conn == null) {
				return null;
			}
			Statement stmt = conn.createStatement();
			String sql = "SELECT categories from RESTAURANTS WHERE business_id='"
					+ businessId + "'";
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				Set<String> set = new HashSet<>();
				String[] categories = rs.getString("categories").split(",");
				for (String category : categories) {
					// ' Japanese ' -> 'Japanese'
					set.add(category.trim());
				}
				return set;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return new HashSet<String>();
	}

	private Set<String> getBusinessId(String category) {
		Set<String> set = new HashSet<>();
		try {
			if (conn == null) {
				return null;
			}
			Statement stmt = conn.createStatement();
			// if category = Chinese, categories = Chinese, Korean, Japanese, it's a match
			String sql = "SELECT business_id from RESTAURANTS WHERE categories LIKE '%"
					+ category + "%'";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				String businessId = rs.getString("business_id");
				set.add(businessId);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return set;
	}

	public Set<String> getVisitedRestaurants(String userId) {
		Set<String> visitedRestaurants = new HashSet<String>();
		try {
			Statement stmt = conn.createStatement();
			String sql = "SELECT business_id from USER_VISIT_HISTORY WHERE user_id="
					+ userId;
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				String visitedRestaurant = rs.getString("business_id");
				visitedRestaurants.add(visitedRestaurant);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return visitedRestaurants;
	}

	private JSONObject getRestaurantsById(String businessId) {
		try {
			Statement stmt = conn.createStatement();
			String sql = "SELECT * from "
					+ "RESTAURANTS where business_id='" + businessId + "'" + " ORDER BY stars DESC";
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				JSONObject obj = new JSONObject();
				obj.put("business_id", rs.getString("business_id"));
				obj.put("name", rs.getString("name"));
				obj.put("stars", rs.getFloat("stars"));
				obj.put("latitude", rs.getFloat("latitude"));
				obj.put("longitude", rs.getFloat("longitude"));
				obj.put("full_address", rs.getString("full_address"));
				obj.put("city", rs.getString("city"));
				obj.put("state", rs.getString("state"));
				obj.put("categories",
						DBImport.stringToJSONArray(rs.getString("categories")));
				obj.put("image_url", rs.getString("image_url"));
				obj.put("url", rs.getString("url"));
				return obj;
			}
		} catch (Exception e) { /* report an error */
			System.out.println(e.getMessage());
		}
		return null;
	}
	
	/*
	private Set<String> getMoreCategories(String category, int maxCount) {
		Set<String> allCategories = new HashSet<>();
		if (conn == null) {
			return null;
		}
		Statement stmt;
		try {
			stmt = conn.createStatement();

			String sql = "SELECT second_id from USER_CATEGORY_HISTORY WHERE first_id=\""
					+ category + "\" ORDER BY count DESC LIMIT " + maxCount;
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				String visited_restaurant = rs.getString("second_id");
				allCategories.add(visited_restaurant);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return allCategories;
	}
	
	private Set<String> getMoreCategories(Set<String> existingCategories) {
		Set<String> allCategories = new HashSet<>();
		for (String category : existingCategories) {
			allCategories.addAll(getMoreCategories(category, 5));
		}
		return allCategories;
	}*/

	public JSONArray RecommendRestaurants(String userId) {
		try {
			if (conn == null) {
				return null;
			}

			Set<String> visitedRestaurants = getVisitedRestaurants(userId);
			Set<String> allCategories = new HashSet<>();// why hashSet?
			for (String restaurant : visitedRestaurants) {
				allCategories.addAll(getCategories(restaurant));
			}
			Set<String> allRestaurants = new HashSet<>();
			for (String category : allCategories) {
				Set<String> set = getBusinessId(category);
				allRestaurants.addAll(set);
			}
			Set<JSONObject> diff = new HashSet<>();
			int count = 0;
			for (String businessId : allRestaurants) {
				// Perform filtering
				if (!visitedRestaurants.contains(businessId)) {
					diff.add(getRestaurantsById(businessId));
					count++;
					if (count >= MAX_RECOMMENDED_RESTAURANTS) {
						break;
					}
				}
			}
			/*
			// offline learning
			if (count < MIN_RECOMMENDED_RESTAURANTS) {
				diff.clear();
				allCategories.addAll(getMoreCategories(allCategories));
				for (String category : allCategories) {
					Set<String> set = getBusinessId(category);
					allRestaurants.addAll(set);
				}
				for (String businessId : allRestaurants) {
					if (!visitedRestaurants.contains(businessId)) {
						diff.add(getRestaurantsById(businessId));
						count++;
						if (count >= MAX_RECOMMENDED_RESTAURANTS) {
							break;
						}
					}
				}
			}*/	
			
			return new JSONArray(diff);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	public JSONArray GetRestaurantsNearLoation(String userId, double lat, double lon) {
		try {
			if (conn == null) {
				return null;
			}
			Set<String> visited = getVisitedRestaurants(userId);
			Statement stmt = conn.createStatement();
			String sql = "SELECT * from RESTAURANTS LIMIT 10";
			ResultSet rs = stmt.executeQuery(sql);
			List<JSONObject> list = new ArrayList<JSONObject>();
			while (rs.next()) {
				JSONObject obj = new JSONObject();
				obj.put("business_id", rs.getString("business_id"));
				obj.put("name", rs.getString("name"));
				obj.put("stars", rs.getFloat("stars"));
				obj.put("latitude", rs.getFloat("latitude"));
				obj.put("longitude", rs.getFloat("longitude"));
				obj.put("full_address", rs.getString("full_address"));
				obj.put("city", rs.getString("city"));
				obj.put("state", rs.getString("state"));
				obj.put("categories",
						DBImport.stringToJSONArray(rs.getString("categories")));
				obj.put("image_url", rs.getString("image_url"));
				obj.put("url", rs.getString("url"));
				if (visited.contains(rs.getString("business_id"))){
					obj.put("is_visited", true);
				} else {
					obj.put("is_visited", false);
				}
				list.add(obj);
			}
			return new JSONArray(list);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	public JSONArray GetRestaurantsNearLoationViaYelpAPI(String userId, double lat, double lon) {
		try {
			YelpAPI api = new YelpAPI();
			JSONObject response = new JSONObject(
					api.searchForBusinessesByLocation(lat, lon));
			JSONArray array = (JSONArray) response.get("businesses");
			if (conn == null) {
				return null;
			}
			Statement stmt = conn.createStatement();
			String sql = "";
			List<JSONObject> list = new ArrayList<JSONObject>();
			Set<String> visited = getVisitedRestaurants(userId);

			for (int i = 0; i < array.length(); i++) {
				JSONObject object = array.getJSONObject(i);
				Restaurant restaurant = new Restaurant(object);
				String business_id = restaurant.getBusinessId();
				String name = restaurant.getName();
				String categories = restaurant.getCategories();
				String city = restaurant.getCity();
				String state = restaurant.getState();
				String fullAddress = restaurant.getFullAddress();
				double stars = restaurant.getStars();
				double latitude = restaurant.getLatitude();
				double longitude = restaurant.getLongitude();
				String imageUrl = restaurant.getImageUrl();
				String url = restaurant.getUrl();
				JSONObject obj = new JSONObject();
				obj.put("business_id", business_id);
				obj.put("name", name);
				obj.put("stars", stars);
				obj.put("latitude", latitude);
				obj.put("longitude", longitude);
				obj.put("full_address", fullAddress);
				obj.put("city", city);
				obj.put("state", state);
				obj.put("categories", categories);
				obj.put("image_url", imageUrl);
				obj.put("url", url);
				if (visited.contains(business_id)){
					obj.put("is_visited", true);
				} else {
					obj.put("is_visited", false);
				}
				sql = "INSERT IGNORE INTO RESTAURANTS " + "VALUES ('"
						+ business_id + "', \"" + name + "\", \"" + categories
						+ "\", \"" + city + "\", \"" + state + "\", " + stars
						+ ", \"" + fullAddress + "\", " + latitude + ","
						+ longitude + ",\"" + imageUrl + "\", \"" + url + "\")";
				System.out.println(sql);
				stmt.executeUpdate(sql);
				list.add(obj);
			}
			return new JSONArray(list);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
	
	public static void main(String[] args) {
		//This is for test purpose
		DBConnection conn = new DBConnection();
		JSONArray array = conn.GetRestaurantsNearLoationViaYelpAPI("1111", 37.38, -122.08);
	}
}
