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

/**
 * @author Clinton Begin
 */
public class GenericTokenParser {

  private final String openToken; // mark 开始标记符
  private final String closeToken; // mark 结束标记符
  private final TokenHandler handler; // mark 标记处理接口，具体操作取决于接口的实现方法

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

  /**
   * 文本解析方法
   *
   * @param text
   *
   * @return
   */
  public String parse(String text) {
    if (text == null || text.isEmpty()) {
      return ""; // mark 空值判断
    }
    // search open token
    int start = text.indexOf(openToken); // mark 获取开始标记在文本中的位置
    if (start == -1) {
      return text; // mark 位置索引为-1，说明不存在该开始标记标记符，直接返回文本
    }
    char[] src = text.toCharArray(); // mark 将文本字符串转化为字符数组
    int offset = 0; // mark 偏移量
    final StringBuilder builder = new StringBuilder(); // mark 用于拼接解析后的字符串
    StringBuilder expression = null;
    do {
      if (start > 0 && src[start - 1] == '\\') {
        // mark 如果开始标记之前被转义字符转义了，则删除转义字符
        builder.append(src, offset, start - offset - 1).append(openToken);
        offset = start + openToken.length();
      } else {
        // mark 已经找到开始标记，寻找结束标记
        if (expression == null) {
          expression = new StringBuilder();
        } else {
          expression.setLength(0);
        }
        builder.append(src, offset, start - offset);
        offset = start + openToken.length();
        int end = text.indexOf(closeToken, offset); // mark 结束标记符的索引值
        while (end > -1) {
          if ((end <= offset) || (src[end - 1] != '\\')) {
            expression.append(src, offset, end - offset); // mark 判断是否有转义字符，有就移除转义字符
            break;
          }
          // this close token is escaped. remove the backslash and continue.
          expression.append(src, offset, end - offset - 1).append(closeToken);
          offset = end + closeToken.length(); // mark 重新计算偏移量
          end = text.indexOf(closeToken, offset); // mark 重新计算结束标识符索引值
        }
        if (end == -1) {
          // mark 如果没有找到结束标记符
          builder.append(src, start, src.length - start);
          offset = src.length;
        } else {
          // mark 找到结束标记符，对该标记符进行值的替换
          builder.append(handler.handleToken(expression.toString()));
          offset = end + closeToken.length();
        }
      }
      start = text.indexOf(openToken, offset); // mark 继续计算下一组标记符
    } while (start > -1);
    if (offset < src.length) {
      builder.append(src, offset, src.length - offset);
    }
    return builder.toString();
  }
}
