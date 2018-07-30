package DeployInfo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeployVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(DeployVerticle.class);

/*    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(DeployVerticle.class.getName());
        LOGGER.info("部署configDeployVerticle ");
    }*/

    @Override
    public void start() {
       // super.start();
        JsonArray threadList = config().getJsonArray("threads");
        JsonObject redisInfo = config().getJsonObject("redis");
        JsonObject rabbitmqInfo = config().getJsonObject("rabbitmq");

        for (Object q : threadList) {
            JsonObject listener = (JsonObject) q;
            // Deploy another verticle without configuration.
            JsonObject configTest =  new JsonObject();
            configTest.put("redis", redisInfo);
            configTest.put("rabbitmq", rabbitmqInfo);
            configTest.put("listener", listener);
            //System.out.print(configTest.toString());
           vertx.deployVerticle(ConsumerVerticle.class.getName(), new DeploymentOptions().setConfig(configTest));
        }
        LOGGER.info("输出测试：DeployVerticle.start");

    }
}
