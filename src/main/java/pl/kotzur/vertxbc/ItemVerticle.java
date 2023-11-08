package pl.kotzur.vertxbc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kotzur.vertxbc.db.MongoRepository;

import java.util.UUID;

public class ItemVerticle extends AbstractVerticle {

  public static final Logger logger = LoggerFactory.getLogger(ItemVerticle.class);

  private JWTAuth provider;

  private MongoRepository repository;

  @Override
  public void start(Promise<Void> startPromise) {

    JWTAuthOptions config = new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setBuffer("keyboard cat"));
    provider = JWTAuth.create(vertx, config);

    repository = new MongoRepository(vertx);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.route("/items").handler(JWTAuthHandler.create(provider));

    router.post("/register").handler(BodyHandler.create()).handler(this::postRegister);
    router.post("/login").handler(BodyHandler.create()).handler(this::postLogin);
    router.post("/items").handler(BodyHandler.create()).handler(this::postItem);
    router.get("/items").handler(this::getItems);

    HttpServer httpServer = vertx.createHttpServer();
    httpServer.requestHandler(router)
      .listen(8888)
      .onSuccess(ok -> {
        logger.info("http server running: http://localhost:8888");
        startPromise.complete();
      })
      .onFailure(startPromise::fail);
  }

  private void postRegister(RoutingContext routingContext) {
    JsonObject user = routingContext.body().asJsonObject();
    UUID uuidId = UUID.randomUUID();
    user.put("_id", uuidId.toString());
    String password = user.getString("password");
    String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
    user.remove("password");
    user.put("password", hashed);

    repository.saveUser(user, response -> {
      if (response.succeeded()) {
        routingContext.response().setStatusCode(204).end(response.result().encodePrettily());
      } else {
        response.cause().printStackTrace();
      }
    });
  }

  private void postLogin(RoutingContext routingContext) {
    JsonObject credentials = routingContext.body().asJsonObject();
    String login = credentials.getString("login");
    String password = credentials.getString("password");

    repository.getUserByLogin(login, response -> {
      if (response.succeeded()) {
        JsonObject userDB = response.result();
        String loginDB = userDB.getString("login");
        String passwordDB = userDB.getString("password");
        String id = userDB.getString("_id");
        String token;

        if (loginDB.equals(login) && BCrypt.checkpw(password, passwordDB)) {
          token = provider.generateToken(
            new JsonObject()
              .put("sub", login)
              .put("userId", id), new JWTOptions().setExpiresInMinutes(5));
        } else {
          token = "";
        }
        routingContext.response().end(new JsonObject().put("jwt", token).encode());
      } else {
        response.cause().printStackTrace();
      }
    });

  }

  private void postItem(RoutingContext routingContext) {
    JsonObject item = routingContext.body().asJsonObject();
    String userId = routingContext.user().get("userId");
    UUID uuidId = UUID.randomUUID();
    item.put("_id", uuidId.toString());
    UUID uuidOwner = UUID.fromString(userId);
    item.put("owner", uuidOwner.toString());

    repository.saveItem(item, response -> {
      if (response.succeeded()) {
        routingContext.response().end(response.result().encodePrettily());
      } else {
        response.cause().printStackTrace();
      }
    });
  }

  private void getItems(RoutingContext routingContext) {
    String userId = routingContext.user().get("userId");
    repository.getItemsByOwnerId(userId, response -> {
      if (response.succeeded()) {
        routingContext.response()
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject().put("data", response.result()).encode());
      } else {
        response.cause().printStackTrace();
      }
    });
  }

}
