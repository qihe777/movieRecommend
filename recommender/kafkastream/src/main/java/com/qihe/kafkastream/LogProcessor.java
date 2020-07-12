package com.qihe.kafkastream;

import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;

public class LogProcessor implements Processor<byte[],byte[]> {

    public static final String PREFIX_MSG = "abc:";

    //上下文
    private ProcessorContext context;

    @Override
    public void init(ProcessorContext processorContext) {
        this.context=processorContext;
    }

    //处理数据
    @Override
    public void process(byte[] bytes, byte[] bytes2) {
        String ratingValue = new String(bytes2);
        if(ratingValue.contains(PREFIX_MSG)){
            String tmp = ratingValue.split(PREFIX_MSG)[1];
            context.forward("log".getBytes(),tmp.getBytes());
        }
    }

    @Override
    public void close() {

    }
}
