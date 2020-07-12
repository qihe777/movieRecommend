package com.qihe.kafkastream;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

import java.util.Properties;

public class Application {
    public static void main(String[] args){

        //以下参数应该为运行中提供。目前直接写死
        String input = "abc";
        String output= "recommender";

        //kafkastream 配置
        Properties properties = new Properties();
        //应用名称
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG,"logProcessor");
        //kafka端口
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        StreamsConfig config = new StreamsConfig(properties);

        //kafka链接拓扑
        Topology topology=new Topology();
        topology.addSource("source",input)
                .addProcessor("process", LogProcessor::new,"source")
                .addSink("sink",output,"process");

        KafkaStreams kafkaStreams=new KafkaStreams(topology,config);

        kafkaStreams.start();
    }
}
