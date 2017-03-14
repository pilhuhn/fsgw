package de.bsd.fsgw;


import io.vertx.core.Vertx;

/**
 * @author hrupp
 */
public class Main {


    public static void main(String[] args) throws Exception {
        Vertx vertx;

        vertx = Vertx.vertx();
        vertx.deployVerticle(MainVerticle.class.getName());
    }
}
