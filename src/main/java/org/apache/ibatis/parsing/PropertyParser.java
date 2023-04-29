/*
 *    Copyright 2009-2023 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.parsing;

import java.util.Properties;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class PropertyParser {
  /**
   * 定义属性时的默认前缀
   */
  private static final String KEY_PREFIX = "org.apache.ibatis.parsing.PropertyParser.";
  /**
   * The special property key that indicate whether enable a default value on placeholder.
   * 指定是否在占位符上启用默认值的特殊属性键（是否开启默认值功能，默认不开启）
   * <p>
   * The default value is {@code false} (indicate disable a default value on placeholder) If you specify the
   * {@code true}, you can specify key and default value on placeholder (e.g. {@code ${db.username:postgres}}).
   * </p>
   * 默认值是false(表示禁用占位符上的默认值)。如果您指定为true，可以指定占位符上的键和默认值。
   * 例如 ${db.username:postgres}，如果db.username为空，则默认值为postgres。
   *
   * @since 3.4.2
   */
  public static final String KEY_ENABLE_DEFAULT_VALUE = KEY_PREFIX + "enable-default-value";

  /**
   * The special property key that specify a separator for key and default value on placeholder.
   * 特殊属性值，为键指定分隔符，并在占位符上指定默认值。（是否修改默认值的分隔符，默认是 : ）
   * <p>
   * The default separator is {@code ":"}. 默认的分隔符是 :
   * </p>
   *
   * @since 3.4.2
   */
  public static final String KEY_DEFAULT_VALUE_SEPARATOR = KEY_PREFIX + "default-value-separator";

  /**
   * 是否开启默认值功能的默认值。
   */
  private static final String ENABLE_DEFAULT_VALUE = "false";
  /**
   * 默认分隔符。
   */
  private static final String DEFAULT_VALUE_SEPARATOR = ":";

  private PropertyParser() {
    // Prevent Instantiation mark 私有构造方法，禁止实例化，该类主要是一个工具类，所有方法都是静态方法，所以不需要实例化。
  }

  /**
   * 解析，创建VariableTokenHandler对象和GenericTokenParser对象，然后解析替换其中的动态值
   * @param string 需要解析的代码片段
   * @param variables 需要填充的参数集合
   * @return
   *    (1) 创建VariableTokenHandler对象
   *    (2) 创建GenericTokenParser对象
   *    (3) 执行解析方法
   *    该方法主要是实现代码片段的解析处理工作。该方法定义了一个处理代码片段的处理器，然后交给GenericTokenParser实例对象，
   *    完成最终的解析工作。
   *    (1)主要是初始化一个标记处理器VariableTokenHandler(内部类)，完成真正的处理工作。
   *    (2)主要是初始化一个GenericTokenParser实例，并设置开始标记 "${"，结束标记 “}”，处理器为VariableTokenHandler实例。
   *    (3)主要是通过调用GenericTokenParser实例对象的parse()方法实现代码片段的解析工作。
   */
  public static String parse(String string, Properties variables) {
    VariableTokenHandler handler = new VariableTokenHandler(variables);
    GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
    return parser.parse(string);
  }

  /**
   * 字段构造器
   */
  private static class VariableTokenHandler implements TokenHandler {
    private final Properties variables; // mark 参数集合
    private final boolean enableDefaultValue; // mark 是否开启占位符默认值
    private final String defaultValueSeparator; // mark 定义占位默认值的分隔符

    private VariableTokenHandler(Properties variables) {
      this.variables = variables;
      this.enableDefaultValue = Boolean.parseBoolean(getPropertyValue(KEY_ENABLE_DEFAULT_VALUE, ENABLE_DEFAULT_VALUE));
      this.defaultValueSeparator = getPropertyValue(KEY_DEFAULT_VALUE_SEPARATOR, DEFAULT_VALUE_SEPARATOR);
    }

    private String getPropertyValue(String key, String defaultValue) {
      return variables == null ? defaultValue : variables.getProperty(key, defaultValue);
    }

    /**
     * 占位符的参数处理
     * @param content 要处理的代码片段
     * @return
     * 该方法主要用于占位符的参数处理。首先判断variables变量是否有值，如果为null，则返回 "${" + content + "}"字符串，
     * 否则，继续执行；然后判断是否开启了占位符默认值的功能；如果开启，就判断是否有默认值，
     * 如果有默认值就解析成对应的key和默认值defaultValue，然后返回key对应的参数值；
     * 如果没有开启默认值功能，就直接从variables获取key对应的参数值并返回。
     */
    @Override
    public String handleToken(String content) {
      if (variables != null) {
        String key = content;
        if (enableDefaultValue) { // mark 是否开启占位符默认值
          final int separatorIndex = content.indexOf(defaultValueSeparator);
          String defaultValue = null;
          if (separatorIndex >= 0) { // mark 判断是否有默认值
            key = content.substring(0, separatorIndex);
            defaultValue = content.substring(separatorIndex + defaultValueSeparator.length());
          }
          if (defaultValue != null) {
            return variables.getProperty(key, defaultValue); // mark 解析默认值，并返回key对应的参数
          }
        }
        if (variables.containsKey(key)) {
          // mark 如果没有开启默认值功能，则直接获取参数值并返回
          return variables.getProperty(key);
        }
      }
      // mark 如果variables变量为null，说明没有动态替换的值，直接返回 ${content}
      return "${" + content + "}";
    }
  }

}
