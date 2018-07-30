package DeployInfo;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class ConsumerVerticle extends AbstractVerticle {

    //定义日志对象及数据
    private static final Logger LOGGER = LogManager.getLogger(ConsumerVerticle.class);
    SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
    SimpleDateFormat dateformat = new SimpleDateFormat("yyyy MM dd HH:mm:ss");
    String dateStr = date.format(System.currentTimeMillis());
    String timestamp = dateformat.format(System.currentTimeMillis());
    InetAddress inetAddr;
    {
        try {
            inetAddr = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    //定义MQ客户端及Redis客户端
    protected RabbitMQClient client;
    Jedis jedis;
    //初始化：连接MQ服务器及Redis数据库
    private void init(JsonObject configTest) {
        super.init(vertx, context);

        //创建RabbitMQ连接
        JsonObject RabbitMQConfig = configTest.getJsonObject("rabbitmq");
        String RabbitMQHost = RabbitMQConfig.getString("host");
        Integer RabbitMQPort = Integer.parseInt(RabbitMQConfig.getString("port"));
        String logFront ="[ "+configTest.getJsonObject("listener").getString("threadId")+"-"+ dateStr+" ] [ "
                + inetAddr +"/"+ConsumerVerticle.class.getName()+" ] [ "+timestamp+" ] ";
        RabbitMQOptions config1 = new RabbitMQOptions();
        config1.setHost(RabbitMQHost);
        config1.setPort(RabbitMQPort);
        client = RabbitMQClient.create(vertx, config1);
        CompletableFuture<Void> latch = new CompletableFuture<>();
        client.start(ar -> {
            if (ar.succeeded()) {
                LOGGER.info(logFront + "[ LogType: info ] [ Message: Start client! ]");
                latch.complete(null);
            } else {
                LOGGER.error(logFront + "[ LogType: error ] [ Can't start client! ] [" + ar.cause() + " ]");
                latch.completeExceptionally(ar.cause());
            }
        });
        ConnectionFactory factory = new ConnectionFactory();
        try {
            Channel channel = factory.newConnection().createChannel();
        } catch (IOException e) {
            LOGGER.error(logFront + "[ LogType: error ] [ Can't start channel! ] [" + e + " ]");
            e.printStackTrace();
        } catch (TimeoutException e) {
            LOGGER.error(logFront + "[ LogType: error ] [ Can't start channel! ] [" + e + " ]");
            e.printStackTrace();
        }
            LOGGER.info(logFront + "[ LogType: info ] [ Create client!");

        //创建Redis连接
        JsonObject RedisConfig = configTest.getJsonObject("redis");
        String RedisHost = RedisConfig.getString("host");
        Integer RedisPort = Integer.parseInt(RedisConfig.getString("port"));
        jedis = new Jedis(RedisHost,RedisPort);
    }

    //创建监听队列，消息处理后发布新消息
    @Override
    public void start() {
        JsonObject config = config();
        init(config);
        //获取配置参数
        JsonObject listener = config.getJsonObject("listener");
        //日志格式前缀
        String logFrontA ="[ "+listener.getString("threadId")+"-"+ dateStr+" ] [ "
                + inetAddr +"/"+listener.getString("threadId")+"/"+ConsumerVerticle.class.getName()+" ] [ "+timestamp+" ] ";
        System.out.println(logFrontA);

        //创建监听队列
        JsonObject ClientConfig = config.getJsonObject("listener");
        String ConsumeQueue = ClientConfig.getString("consumeQueue");
        String ADDRESS = ConsumeQueue + ".address";

        // Setup the link between rabbitmq consumer and event bus address
        client.basicConsume(ConsumeQueue, ADDRESS, false, consumeResult -> {
            if (consumeResult.succeeded()) {
                LOGGER.info(logFrontA + "[ LogType: info ] [ Create consumer! ]");
                System.out.println("RabbitMQ consumer created in Consumer!");
            } else {
                LOGGER.error(logFrontA + "[ LogType: error ] [ Can't create consumer! ] [ " + consumeResult.cause() + " ]");
                consumeResult.cause().printStackTrace();
            }
        });
        // Create the event bus handler which messages will be sent to
        vertx.eventBus().consumer(ADDRESS, msg -> {
            JsonObject json = (JsonObject) msg.body();
            //System.out.println("Got message: " + json.getString("body"));
            // ack
            client.basicAck(json.getLong("deliveryTag"), false, asyncResult -> {
                //打印消息
                System.out.println("Got message: " + json.getString("body"));
                //读取消息数据
                JsonObject message = new JsonObject(json.getString("body")).getJsonObject("MSG");
                Long serialNo = Long.parseLong(message.getString("SerialNo"));
                String testCase = message.getString("TestCase");
                Long nestNo = serialNo + 1;
                String newNestNo = "TC "+ testCase +" "+String.format("%03d",nestNo);
                //组装日志前缀
                String logFrontB ="[ "+listener.getString("threadId")+"-"+ dateStr+ message.getString("SerialNo")+" ] [ "
                        + inetAddr +"/"+listener.getString("threadId")+"/"+ConsumerVerticle.class.getName()+" ] [ "+timestamp+" ] ";
                //读取redis数据
                String newValue = jedis.get(newNestNo);
                System.out.println("newValue "+newValue);
                //组装新消息
                JsonObject tempMessage = new JsonObject(newValue).getJsonObject("MSG");
                JsonObject newMessage = new JsonObject();
                newMessage.put("MSG",tempMessage);
                JsonObject publishMessage = new JsonObject().put("body", newMessage.toString());
                System.out.println(publishMessage);
                //组装mq配置参数
                String index = "TC "+ testCase +" "+ message.getString("SerialNo");
                String mqMessage = jedis.get(index);
                JsonObject mqPublish = new JsonObject(mqMessage).getJsonObject("MQPublish");
                String exchangeName = mqPublish.getString("ExchangeName");
                String routingKey = mqPublish.getString("RoutingKey");
                //String nextQueueName = mqPublish.getString("QueueName");
                //发布消息
                client.basicPublish(exchangeName, routingKey, publishMessage, rs -> {
                    if (rs.succeeded()) {
                        LOGGER.info(logFrontB+"[ LogType: info ] [ Message published : "+ newMessage + " to Exchange: "+exchangeName+" ! ]");
                        System.out.println("Message published !");
                    } else {
                        LOGGER.error(logFrontB +"[ LogType: error ] [ Can't publish message! ] [ "+rs.cause()+" ]");
                        rs.cause().printStackTrace();
                    }
                });
                if (asyncResult.succeeded()) {
                    LOGGER.info(logFrontB+"[ LogType: info ] [ Ack message! ]");
                } else {
                    LOGGER.error( logFrontB+"[ LogType: error ] [ Can't ack message! ]");
                }
            });
        });
        System.out.println("ConsumeQueue"+ConsumeQueue);
    }
    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        super.stop(stopFuture);
    }
}