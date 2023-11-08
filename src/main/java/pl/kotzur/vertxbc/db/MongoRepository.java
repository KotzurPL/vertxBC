package pl.kotzur.vertxbc.db;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;

public class MongoRepository {

  private final MongoClient mongoClient;

  public MongoRepository(Vertx vertx) {
    JsonObject config = new JsonObject()
      .put("url", "mongodb://localhost:27017")
      .put("db_name", "vertxbcdb");
    mongoClient = MongoClient.create(vertx, config);
  }

  public void getItemsByOwnerId(String ownerId, Handler<AsyncResult<List<JsonObject>>> resultHandler) {
    JsonObject query = new JsonObject().put("owner", ownerId);
    mongoClient.find("items", query, response -> {
      if (response.succeeded()) {
        if (response.result() == null) {
          resultHandler.handle(Future.failedFuture("Error"));
        } else {
          List<JsonObject> items = response.result();
          resultHandler.handle(Future.succeededFuture(items));
        }
      } else {
        response.cause().printStackTrace();
      }
    });
  }

  public void getUserByLogin(String login, Handler<AsyncResult<JsonObject>> resultHandler) {
    JsonObject query = new JsonObject().put("login", login);
    mongoClient.findOne("users", query, null, response -> {
      if (response.succeeded()) {
        if (response.result() == null) {
          resultHandler.handle(Future.failedFuture("Error"));
        } else {
          JsonObject user = response.result();
          resultHandler.handle(Future.succeededFuture(user));
        }
      } else {
        response.cause().printStackTrace();
      }
    });
  }

  public void saveItem(JsonObject item, Handler<AsyncResult<JsonObject>> resultHandler) {
    mongoClient.save("items", item, response -> {
      if (response.succeeded()) {
        JsonObject message = new JsonObject().put("message", "Item created successfully.");
        resultHandler.handle(Future.succeededFuture(message));
      } else {
        response.cause().printStackTrace();
      }
    });
  }

  public void saveUser(JsonObject user, Handler<AsyncResult<JsonObject>> resultHandler) {
    mongoClient.save("users", user, response -> {
      if (response.succeeded()) {
        JsonObject message = new JsonObject().put("message", "User created successfully.");
        resultHandler.handle(Future.succeededFuture(message));
      } else {
        response.cause().printStackTrace();
      }
    });
  }

}
