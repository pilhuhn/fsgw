package de.bsd.fsgw;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.*;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.HashMap;
import java.util.Map;

public class MainVerticle extends AbstractVerticle {

  // Label on dust data
  private static final String TYP_FS = "{typ=\"fs\"}";

  @Override
  public void start(Future<Void> fut) {

    Map<String,String> values = new HashMap<>();

    // Create a router object.
    Router router = Router.router(vertx);

    // We need the body, so we need to enable it.
    router.route("/").handler(BodyHandler.create());

    // Bind "/" to our fs-input
    router.post("/").handler(routingContext -> {
      // Get the sensor id from the header
      String tmp = routingContext.request().getHeader("X-Sensor");
      String sensor_id = tmp.replace('-', '_');
      System.out.println("Sensor Id: " + sensor_id);
      JsonObject body_json = routingContext.getBodyAsJson();
      JsonArray value_array = body_json.getJsonArray("sensordatavalues");
      System.out.println(value_array);
      // value_type  value

      value_array.forEach((elem) -> {
        JsonObject jo = (JsonObject)elem;
        String key = jo.getString("value_type");
        String value = jo.getString("value");
        System.out.println("Key : " + key  + " Value : " + value);
        if (value.contains("dBm")) {
          value = value.substring(0,value.indexOf(" "));
        }
        values.put(sensor_id + "_" +key,value);
      });


      HttpServerResponse response = routingContext.response();
      response.setStatusCode(200)
          .end();
    });


    /*
     * Now the listener for scraping of data
     */
    router.get("/metrics").handler(routingContext -> {

      System.out.println("Request from " + routingContext.request().remoteAddress().toString());
      HttpServerResponse response = routingContext.response();

      // If we have no data yet, return a 204 'no content'
      if (values.isEmpty()) {
        response.setStatusCode(204)
            .end();
        return;
      }

      StringBuilder builder = new StringBuilder();
      values.forEach((k,v) -> {
        builder.append("# TYPE ").append(k).append(" gauge\n");
        if (k.contains("P1")) {
          builder.append("# HELP ").append(k).append(" PM 10 Wert\n");
          builder.append(k).append(TYP_FS + " ").append(v).append("\n");
        }
        else if (k.contains("P2")) {
          builder.append("# HELP ").append(k).append(" PM 2.5 Wert\n");
          builder.append(k).append(TYP_FS + " ").append(v).append("\n");
        }
        else{
          builder.append(k).append(" ").append(v).append("\n");
        }
      });

      response.setStatusCode(200)
          .putHeader("content-type","text/plain; version=0.0.4")
          .end(builder.toString());
    });


    // Create the HTTP server and pass the "accept" method to the request handler.
    vertx
        .createHttpServer()
        .requestHandler(router::accept)
        .listen(
            // Retrieve the port from the configuration,
            // default to 10080.
            config().getInteger("http.port", 10080),
            result -> {
              if (result.succeeded()) {
                fut.complete();
              } else {
                fut.fail(result.cause());
              }
            }
        );
  }}