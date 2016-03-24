package db;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;

import java.text.ParseException;

public class MongDBImport {
	public static void main(String[] args) throws ParseException {
		MongoClient mongoClient = new MongoClient();
		MongoDatabase db = mongoClient.getDatabase(DBUtil.DB_NAME);
		db.getCollection("users").insertOne(
				new Document().append("first_name", "John")
						.append("last_name", "Smith").append("user_id", "1111"));//not 1111
		mongoClient.close();
	}
}
