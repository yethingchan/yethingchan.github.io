# 字符串场景下 indexOf / lastIndexOf 下标定位规则（分查找单个字符、查找子串两种情况）
## 一、查找单个字符 `str.indexOf(char)` / `str.lastIndexOf(char)`
返回**匹配到的那个字符自身的下标**。
示例：
```js
const s = "abcabx";
// 下标：0:a 1:b 2:c 3:a 4:b 5:x
s.indexOf('a');  // 0  第一个a的位置
s.lastIndexOf('a'); // 3 最后一个a的位置

s.indexOf('b'); // 1
s.lastIndexOf('b'); // 4
```

## 二、查找多字符子串 `str.indexOf(subStr)` / `str.lastIndexOf(subStr)`
**重点：返回匹配片段「第一个字符」的下标，不是结尾字符！**
### 示例1
```js
const str = "123abc456abc789";
// 子串 "abc" 出现两处：
// 第一处起始下标 3
// 第二处起始下标 9

str.indexOf("abc");    // 3 （匹配片段首字符a的下标）
str.lastIndexOf("abc");// 9 （最后一组匹配片段首字符a的下标）
```

### 示例2（结合你之前 `substringBetweenLast` 源码场景）
```js
const str = "[A][B][C]";
const close = "]";

// lastIndexOf("]") 找到最后一个 ] 的下标
str.lastIndexOf("]"); // 8
// substring(start+openLen, end) 左闭右开，不会包含这个 ]
```

## 三、带第二个参数的重载（限定查找起点）
1. `indexOf(目标, fromIndex)`：从 `fromIndex` 往右正向搜，返回第一个匹配下标
2. `lastIndexOf(目标, fromIndex)`：从 `fromIndex` 往左反向搜，只看 `0 ~ fromIndex` 区间，返回区间内最后一个匹配下标

```js
const s = "a1a2a3";
// 下标 0:a 1:1 2:a 3:2 4:a 5:3
s.lastIndexOf('a', 3); // 在0~3范围内反向找，匹配到下标2的a
```

## 四、统一兜底规则
找不到匹配内容时，JS / Java 行为一致：返回 `-1`。

## 五、一句话总结
1. 搜**单个字符**：返回该字符本身下标；
2. 搜**一段子串**：返回这段匹配内容**开头第一个字符**的下标；
3. indexOf 拿第一个匹配位置，lastIndexOf 拿最后一个匹配位置。