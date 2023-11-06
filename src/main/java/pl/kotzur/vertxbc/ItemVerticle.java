package pl.kotzur.vertxbc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemVerticle extends AbstractVerticle {

  public static final Logger logger = LoggerFactory.getLogger(ItemVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) {

    Router router = Router.router(vertx);
    router.get("/items").handler(this::getItems);
    router.post("/items").handler(BodyHandler.create()).handler(this::postItem);
    router.post("/login").handler(BodyHandler.create()).handler(this::postLogin);
    router.post("/register").handler(BodyHandler.create()).handler(this::postRegister);

    HttpServer httpServer = vertx.createHttpServer();
    httpServer.requestHandler(router)
      .listen(8888)
      .onSuccess(ok -> {
        logger.info("http server running: http://localhost:8888");
        startPromise.complete();
      })
      .onFailure(startPromise::fail);

  }

  private void getItems(RoutingContext routingContext) {
    JsonObject item = new JsonObject();
    item.put("name", "Kotzur");
    routingContext.response().end(item.encodePrettily());
  }

  private void postItem(RoutingContext routingContext) {
    JsonObject item = routingContext.getBodyAsJson();
    System.out.println(item.toString());

    JsonObject response = new JsonObject();
    response.put("message", "Item created successfully.");
    routingContext.response().end(response.encodePrettily());
  }

  private void postLogin(RoutingContext routingContext) {
  }

  private void postRegister(RoutingContext routingContext) {
  }
}
