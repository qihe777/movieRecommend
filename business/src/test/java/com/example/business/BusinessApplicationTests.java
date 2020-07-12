package com.example.business;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BusinessApplicationTests {
    @Test
    void contextLoads() {
    }
    @Test
    void normalTest(){
        Father son=new Son();
        System.out.println(son instanceof Son);
    }
}
class Father{

}
class Son extends Father{

}
