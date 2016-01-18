package model;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import db.DBImport;

public class Restaurant {
	private String businessId;
	private String name;
	private String categories;
	private String city;
	private String state;
	private String fullAddress;
	private double stars;
	private double latitude;
	private double longitude;
	private String imageUrl;
	private String url;

	public Restaurant(JSONObject object) {
		try {
			if (object != null) {
				this.businessId = object.getString("id");
				JSONArray jsonArray = (JSONArray) object.get("categories");
				List<String> list = new ArrayList<String>();
				for (int i=0; i<jsonArray.length(); i++) {
					JSONArray subArray = jsonArray.getJSONArray(i);
					for (int j=0; j<subArray.length(); j++) {
				        list.add( subArray.getString(j) );
					}
				}
				this.categories = String.join(",", list);
				this.name = object.getString("name");
				this.imageUrl = object.getString("image_url");//either image_url (big) or mobile_url(small)
				this.stars = object.getDouble("rating");
				JSONObject location = (JSONObject) object.get("location");
				JSONObject coordinate = (JSONObject) location.get("coordinate");
				this.latitude = coordinate.getDouble("latitude");
				this.longitude = coordinate.getDouble("longitude");
				this.city = location.getString("city");
				this.state = location.getString("state_code");
				this.fullAddress = DBImport.jsonArrayToString((JSONArray) location.get("display_address"));
				this.url = object.getString("url");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Restaurant(String businessId, String name, String categories,
			String city, String state, double stars, String fullAddress,
			double latitude, double longitude, String url, String phone,
			String snippetText) {
		this.businessId = businessId;
		this.categories = categories;
		this.name = name;
		this.city = city;
		this.state = state;
		this.stars = stars;
		this.fullAddress = fullAddress;
		this.latitude = latitude;
		this.longitude = longitude;
		this.url = url;
	}

	public String getBusinessId() {
		return this.businessId;
	}

	public String getName() {
		return this.name;
	}

	public String getCategories() {
		return this.categories;
	}

	public String getCity() {
		return this.city;
	}

	public String getState() {
		return this.state;
	}

	public String getFullAddress() {
		return this.fullAddress;
	}

	public double getStars() {
		return this.stars;
	}

	public double getLatitude() {
		return this.latitude;
	}

	public double getLongitude() {
		return this.longitude;
	}

	public String getImageUrl() {
		return this.imageUrl;
	}

	public String getUrl() {
		return this.url;
	}

}
