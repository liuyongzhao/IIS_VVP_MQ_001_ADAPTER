package basicTest;


import io.vertx.core.*;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;



public class deployVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(deployVerticle.class);
    public static void main(String[] args) {
        VertxOptions vo = new VertxOptions();
        Vertx vertx = Vertx.vertx(vo);
        DeploymentOptions options = new DeploymentOptions().setInstances(100);

        vertx.deployVerticle(deployVerticle.class.getName(), options,e->{
            System.out.println(e.succeeded());
            System.out.println(e.failed());
            System.out.println(e.cause());
            System.out.println(e.result());
            LOGGER.info("OPTION "+options);
        });
    }
    @Override
    public void start() {
        Handler<HttpServerRequest> handler = e -> {
            HttpServerResponse response = e.response();
            response.putHeader("content-type", "application/json").end("Hello world");
        };
        vertx.createHttpServer().requestHandler(handler).listen(8080);
        LOGGER.info("HTTPRequest");
    }
}
