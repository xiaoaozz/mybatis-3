package com.zal.mybatis.parse;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * @Author zal
 * @Date 2024/02/28  20:39
 * @Description: 解析配置文件
 * @Version: 1.0
 */
public class ParseConfigXml {
  public static void main(String[] args) throws IOException {
    String resource = "config/mybatis-config.xml";
    InputStream inputStream = Resources.getResourceAsStream(resource);
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
  }
}
