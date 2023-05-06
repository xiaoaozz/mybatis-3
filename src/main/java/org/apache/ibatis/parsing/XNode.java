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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Clinton Begin
 */
public class XNode {

  private final Node node; // mark 被包装的org.w3c.dom.Node对象
  private final String name; // mark 节点名称
  private final String body; // mark 节点内容
  private final Properties attributes; // mark 节点属性集合
  private final Properties variables; // mark 配置文件中<properties>节点下定义的键值对
  private final XPathParser xpathParser; // mark 封装了XPath解析器，负责XNode对象的生成，并提供解析XPath表达式的功能

  public XNode(XPathParser xpathParser, Node node, Properties variables) {
    this.xpathParser = xpathParser;
    this.node = node;
    this.name = node.getNodeName();
    this.variables = variables;
    this.attributes = parseAttributes(node);
    this.body = parseBody(node);
  }

  public XNode newXNode(Node node) {
    return new XNode(xpathParser, node, variables); // mark 根据传入的Node对象，创建XNode对象实例
  }

  /**
   * 获取父节点
   * @return 父节点是Element类型，返回封装好的XNode节点，否则返回null
   */
  public XNode getParent() {
    Node parent = node.getParentNode();
    if (!(parent instanceof Element)) {
      return null;
    }
    return new XNode(xpathParser, parent, variables);
  }

  /**
   * 获取节点路径
   * @return 节点的路径值
   *         比如 <A><B><C><C/><B/><A/>，对于C节点来说节点路径就是 A/B/C
   */
  public String getPath() {
    StringBuilder builder = new StringBuilder();
    Node current = node; // mark 当前节点
    while (current instanceof Element) {
      if (current != node) {
        builder.insert(0, "/");
      }
      builder.insert(0, current.getNodeName());
      current = current.getParentNode(); // mark 向上追溯节点，直到顶层节点
    }
    return builder.toString();
  }

  /**
   * 获取节点的识别码
   * @return 返回唯一标识字符串，如下面的C节点的唯一标识字符串就是 A_B[bid]_C[cid]
   * @Code <A>
   *          <B id="bid">
   *              <C id="cid" value="cvalue"/>
   *          </B>
   *      </A>
   */
  public String getValueBasedIdentifier() {
    StringBuilder builder = new StringBuilder();
    XNode current = this;
    while (current != null) {
      if (current != this) {
        builder.insert(0, "_");
      }
      String value = current.getStringAttribute("id",
          current.getStringAttribute("value", current.getStringAttribute("property", (String) null)));
      if (value != null) {
        value = value.replace('.', '_');
        builder.insert(0, "]");
        builder.insert(0, value);
        builder.insert(0, "[");
      }
      builder.insert(0, current.getName());
      current = current.getParent();
    }
    return builder.toString();
  }

  /**
   *  evalXXX()方法，调用XPathParser方法在当前节点下寻找符合表达式条件的节点，通常是文本节点，并
   *  将值转为为指定的类型，如果值无法转化为指定类型，则会报错。
   *  【支持数据类型】 String、Boolean、Double、Node、List<Node>
   */

  public String evalString(String expression) {
    return xpathParser.evalString(node, expression);
  }

  public Boolean evalBoolean(String expression) {
    return xpathParser.evalBoolean(node, expression);
  }

  public Double evalDouble(String expression) {
    return xpathParser.evalDouble(node, expression);
  }

  public List<XNode> evalNodes(String expression) {
    return xpathParser.evalNodes(node, expression);
  }

  public XNode evalNode(String expression) {
    return xpathParser.evalNode(node, expression);
  }

  public Node getNode() {
    return node;
  }

  public String getName() {
    return name;
  }

  /**
   * getXXXBody()方法：获取文本内容并将其转化为指定的数据类型
   * 【支持的数据类型】String、Boolean、Integer、Long、Double、Float
   */

  public String getStringBody() {
    return getStringBody(null);
  }

  public String getStringBody(String def) {
    return body == null ? def : body;
  }

  public Boolean getBooleanBody() {
    return getBooleanBody(null);
  }

  public Boolean getBooleanBody(Boolean def) {
    return body == null ? def : Boolean.valueOf(body);
  }

  public Integer getIntBody() {
    return getIntBody(null);
  }

  public Integer getIntBody(Integer def) {
    return body == null ? def : Integer.valueOf(body);
  }

  public Long getLongBody() {
    return getLongBody(null);
  }

  public Long getLongBody(Long def) {
    return body == null ? def : Long.valueOf(body);
  }

  public Double getDoubleBody() {
    return getDoubleBody(null);
  }

  public Double getDoubleBody(Double def) {
    return body == null ? def : Double.valueOf(body);
  }

  public Float getFloatBody() {
    return getFloatBody(null);
  }

  public Float getFloatBody(Float def) {
    return body == null ? def : Float.valueOf(body);
  }

  public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name) {
    return getEnumAttribute(enumType, name, null);
  }

  public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name, T def) {
    String value = getStringAttribute(name);
    return value == null ? def : Enum.valueOf(enumType, value);
  }

  /**
   * getXxxAttribute()方法：获取节点指定属性的属性值并将其转化为指定的数据类型
   */
  public String getStringAttribute(String name, Supplier<String> defSupplier) {
    String value = attributes.getProperty(name);
    return value == null ? defSupplier.get() : value;
  }

  public String getStringAttribute(String name) {
    return getStringAttribute(name, (String) null);
  }

  public String getStringAttribute(String name, String def) {
    String value = attributes.getProperty(name);
    return value == null ? def : value;
  }

  public Boolean getBooleanAttribute(String name) {
    return getBooleanAttribute(name, null);
  }

  public Boolean getBooleanAttribute(String name, Boolean def) {
    String value = attributes.getProperty(name);
    return value == null ? def : Boolean.valueOf(value);
  }

  public Integer getIntAttribute(String name) {
    return getIntAttribute(name, null);
  }

  public Integer getIntAttribute(String name, Integer def) {
    String value = attributes.getProperty(name);
    return value == null ? def : Integer.valueOf(value);
  }

  public Long getLongAttribute(String name) {
    return getLongAttribute(name, null);
  }

  public Long getLongAttribute(String name, Long def) {
    String value = attributes.getProperty(name);
    return value == null ? def : Long.valueOf(value);
  }

  public Double getDoubleAttribute(String name) {
    return getDoubleAttribute(name, null);
  }

  public Double getDoubleAttribute(String name, Double def) {
    String value = attributes.getProperty(name);
    return value == null ? def : Double.valueOf(value);
  }

  public Float getFloatAttribute(String name) {
    return getFloatAttribute(name, null);
  }

  public Float getFloatAttribute(String name, Float def) {
    String value = attributes.getProperty(name);
    return value == null ? def : Float.valueOf(value);
  }

  /**
   * 获取子节点，对Node.getChildNodes()做相应的封装得到List<XNode>
   * @return 子节点集合
   */
  public List<XNode> getChildren() {
    List<XNode> children = new ArrayList<>();
    NodeList nodeList = node.getChildNodes(); // <properties>标签解析 获取子节点列表
    if (nodeList != null) {
      for (int i = 0, n = nodeList.getLength(); i < n; i++) {
        Node node = nodeList.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          children.add(new XNode(xpathParser, node, variables));// <properties>标签解析 将节点对象封装到XNode中，并将XNode对象放入children列表中
        }
      }
    }
    return children;
  }

  /**
   * 获取所有子节点的属性名和属性值
   * @return 封装好的Properties对象
   */
  public Properties getChildrenAsProperties() {
    Properties properties = new Properties();
    for (XNode child : getChildren()) { // <properties>标签解析 获取并遍历子节点
      String name = child.getStringAttribute("name"); // <properties>标签解析 获取property节点的name和value属性
      String value = child.getStringAttribute("value");
      if (name != null && value != null) {
        properties.setProperty(name, value);// <properties>标签解析 设置属性到属性对象中
      }
    }
    return properties;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    toString(builder, 0);
    return builder.toString();
  }

  private void toString(StringBuilder builder, int level) {
    builder.append("<");
    builder.append(name);
    for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
      builder.append(" ");
      builder.append(entry.getKey());
      builder.append("=\"");
      builder.append(entry.getValue());
      builder.append("\"");
    }
    List<XNode> children = getChildren();
    if (!children.isEmpty()) {
      builder.append(">\n");
      for (XNode child : children) {
        indent(builder, level + 1);
        child.toString(builder, level + 1);
      }
      indent(builder, level);
      builder.append("</");
      builder.append(name);
      builder.append(">");
    } else if (body != null) {
      builder.append(">");
      builder.append(body);
      builder.append("</");
      builder.append(name);
      builder.append(">");
    } else {
      builder.append("/>");
      indent(builder, level);
    }
    builder.append("\n");
  }

  private void indent(StringBuilder builder, int level) {
    for (int i = 0; i < level; i++) {
      builder.append("    ");
    }
  }

  /**
   * 解析节点属性键值对，并将其放入Properties对象中
   * @param n 被解析的节点
   * @return Properties
   */
  private Properties parseAttributes(Node n) {
    Properties attributes = new Properties();
    NamedNodeMap attributeNodes = n.getAttributes(); // mark 获取所有包含节点属性的NamedNodeMap对象
    if (attributeNodes != null) {
      // mark 遍历NamedNodeMap，将属性名和属性值存放在Properties对象中
      for (int i = 0; i < attributeNodes.getLength(); i++) {
        Node attribute = attributeNodes.item(i);
        String value = PropertyParser.parse(attribute.getNodeValue(), variables);
        attributes.put(attribute.getNodeName(), value);
      }
    }
    return attributes;
  }

  /**
   * 解析节点内容
   * @param node 节点
   * @return data 返回文本节点内容。如果节点还有子节点，那么返回第一个属于文本节点的子节点的内容。
   */
  private String parseBody(Node node) {
    String data = getBodyData(node); // mark 获取节点的文本内容
    if (data == null) { // mark 如果是非文本节点，说明是有子节点嵌套
      NodeList children = node.getChildNodes(); // mark 获取该节点下所有子节点
      for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        data = getBodyData(child); // mark 遍历解析节点的内容
        if (data != null) {
          break; // mark 获取到第一个文本子节点的文本内容，结束循环
        }  // mark 例：<A><B att="val"></B><C>cbody</C><A/>
      }    // mark 当获取到第二个子节点C的内容`cbody`时会跳出循环并返回
    }
    return data; // mark 只有文本节点的内容才会返回字符串。比如 <A>aaa</A> => aaa
  }

  private String getBodyData(Node child) {
    // mark 传入节点的类型是CDATA节点或者是文本节点，即只处理文本类型的节点
    // mark CDATA是未解析字符数据，即不应该由XML解析器解析的文本数据，比如"<"和"&"
    if (child.getNodeType() == Node.CDATA_SECTION_NODE || child.getNodeType() == Node.TEXT_NODE) {
      String data = ((CharacterData) child).getData(); // mark 获取节点中的值。比如 ${database.driver} => database.driver
      return PropertyParser.parse(data, variables); // mark 返回带占位符的变量的值。比如将 database.driver => com.mysql.cj.jdbc.Driver
    }
    return null;
  }

}
