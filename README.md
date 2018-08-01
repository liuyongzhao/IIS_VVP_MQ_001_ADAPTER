# IIS_VVP_MQ_001_ADAPTER
通用适配器工程
可与《通用适配器软件需求规格书》配套使用
运行程序时需提供相应的RabbitMQ配置、Redis数据库配置
运行指令：
  mvn clean package
  java -jar target/IIS_VVP_MQ_001_ADAPTER-1.0-SNAPSHOT-fat.jar -conf src/conf/config.json
