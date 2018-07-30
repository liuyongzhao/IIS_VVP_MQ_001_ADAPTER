package basicTest;
import redis.clients.jedis.*;
public class redisDirectTest {
    public static void main(String[] args){
        Jedis jedis = new Jedis("127.0.0.1",6379);
        System.out.println("Connection to server successfully");

        String setResult = jedis.set("name","test");
        String getResult = jedis.get("name");
        System.out.println("setResult:"+setResult);
        System.out.println("getResult:"+getResult);
    }
}
