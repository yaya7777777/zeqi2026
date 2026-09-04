import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * =====================================================================
 * 网站日志分析器 - 择栖后端招新题第一题（编程题）
 * =====================================================================
 *
 * 【题目回顾】
 * 给定一个 Nginx/Apache 格式的 access.log 访问日志文件，每一行记录一次用户请求。
 * 要求分析该文件并生成统计报告。
 *
 * 【日志格式】
 * [IP地址] - - [日期和时间] "请求方法 URL 协议" 状态码 响应大小
 * 示例：192.168.1.1 - - [10/Mar/2023:13:55:36 +0000] "GET /index.html HTTP/1.1" 200 1543
 *
 * 【必做任务】
 *   1. 读取 access.log 文件
 *   2. 计算总请求数（即总行数）
 *   3. 统计状态码 >= 400 的请求数，并计算占总请求数的百分比
 *
 * 【选做（加分）任务】
 *   1. 找出访问次数最多的前 3 个 IP 地址及其访问次数
 *   2. 找出访问量最大的小时（0-23）及该小时的总请求数
 * =====================================================================
 */
public class LogAnalyzer { 

    // =====================================================================
    // 第一部分：正则表达式 —— 解析日志行的核心
    // =====================================================================
    //
    // 【为什么要用正则表达式？】
    // 日志的每一行格式固定但内容复杂，用字符串 split() 切割会非常麻烦
    // （因为日期里有冒号、请求内容里有空格等）。
    // 正则表达式可以精确地"提取"我们想要的各个字段，是解析此类结构化文本的最佳选择。
    //
    // 【正则表达式分解解释】
    // ^(\S+)                         匹配 IP 地址：\S+ 表示非空白字符，^ 表示行首
    //   \s+-\s+-\s+                  匹配 " - - "：两个横杠，中间用空格分隔
    //   \[([^\]]+)\]                 匹配 [日期时间]：\[ 转义左方括号，[^\]]+ 表示非右方括号的任意字符
    //   \s+"(\S+)\s+(\S+)\s+(\S+)"   匹配 "GET /index.html HTTP/1.1"：分别提取方法、URL、协议
    //   \s+(\d+)                     匹配状态码：\d+ 表示一个或多个数字
    //   \s+(\d+)                     匹配响应大小
    //
    // 【捕获组（括号里的内容）对应关系】
    //   group(1) = IP地址
    //   group(2) = 日期时间字符串
    //   group(3) = 请求方法 (GET/POST等)
    //   group(4) = URL 路径
    //   group(5) = 协议 (HTTP/1.1等)
    //   group(6) = 状态码
    //   group(7) = 响应大小
    // =====================================================================
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^(\\S+)\\s+-\\s+-\\s+\\[([^\\]]+)\\]\\s+\"(\\S+)\\s+(\\S+)\\s+(\\S+)\"\\s+(\\d+)\\s+(\\d+)"
    );

    public static void main(String[] args) {
        // 日志文件路径 —— 相对于项目根目录
        String logFilePath = "2026择栖后端招新题/附件/access.log";

        // =====================================================================
        // 第二部分：数据结构选择
        // =====================================================================
        // HashMap<键, 值> 的特点：
        //   - 键唯一，值可以重复
        //   - 插入、查找、删除的平均时间复杂度都是 O(1)，非常高效
        //   - 适合做"频率统计"这类场景

        int totalRequests = 0;              // 总请求数（必做2）
        int errorRequests = 0;               // 错误请求数（状态码 >= 400，必做3）

        // ipCountMap: 统计每个IP的访问次数（选做1）
        //   Key   = IP地址字符串，如 "192.168.1.1"
        //   Value = 该IP访问的次数，如 12
        Map<String, Integer> ipCountMap = new HashMap<>();

        // hourCountMap: 统计每个小时的访问次数（选做2）
        //   Key   = 小时数（0-23），如 14
        //   Value = 该小时的请求总数，如 9
        Map<Integer, Integer> hourCountMap = new HashMap<>();

        // =====================================================================
        // 第三部分：读取并解析文件
        // =====================================================================
        //
        // 【为什么用 BufferedReader？】
        // FileReader 一次只读一个字符，效率很低。
        // BufferedReader 内部有一个缓冲区（默认8KB），一次性读取一大块内容，
        // 然后逐行返回给我们，处理大文件时性能提升明显。
        //
        // 【try-with-resources 语法】
        // try (声明资源) { ... } 是 Java 7 引入的语法糖。
        // 它会自动在 try 块结束后调用资源的 close() 方法，无需手动写 finally 关闭文件。
        // 避免了"忘记关闭文件导致资源泄漏"的常见Bug。
        // =====================================================================
        try (BufferedReader br = new BufferedReader(new FileReader(logFilePath))) {
            String line;

            // 逐行读取日志，readLine() 返回 null 表示读到文件末尾（EOF）
            while ((line = br.readLine()) != null) {
                // 跳过空行（防御性编程：防止日志文件有空行导致解析失败）
                if (line.trim().isEmpty()) {
                    continue;
                }

                // 总请求数 +1（必做任务2：每一行就是一次请求）
                totalRequests++;

                // 用正则表达式匹配当前行
                Matcher matcher = LOG_PATTERN.matcher(line);

                // matches() 方法判断整行是否完全匹配正则
                if (matcher.matches()) {
                    // ==================== 提取各个字段 ====================
                    String ip = matcher.group(1);                // IP地址
                    String dateTimeStr = matcher.group(2);       // 日期时间字符串，如 "10/Mar/2023:13:55:36 +0000"
                    int statusCode = Integer.parseInt(matcher.group(6));  // 状态码，转成int方便比较

                    // ==================== 必做任务3：统计错误请求 ====================
                    // HTTP 状态码知识：
                    //   2xx = 成功（如 200 OK）
                    //   3xx = 重定向（如 301 永久重定向、304 缓存未修改）
                    //   4xx = 客户端错误（如 403 Forbidden、404 Not Found）
                    //   5xx = 服务器错误（如 500 Internal Server Error）
                    // 题目要求 >= 400，即 4xx 和 5xx 都算"错误请求"
                    if (statusCode >= 400) {
                        errorRequests++;
                    }

                    // ==================== 选做任务1：统计IP访问次数 ====================
                    // HashMap 的 getOrDefault 方法（Java 8+）：
                    //   如果 key 存在，返回它的 value；
                    //   如果 key 不存在，返回我们给的默认值 0。
                    // 然后 +1 就是这次访问新的计数，再 put 回去覆盖旧值。
                    ipCountMap.put(ip, ipCountMap.getOrDefault(ip, 0) + 1);

                    // ==================== 选做任务2：统计每个小时的访问量 ====================
                    // 日期时间格式示例：10/Mar/2023:13:55:36 +0000
                    // 小时数在第一个冒号后面的两位，所以我们用简单的字符串切割来提取：
                    //   step1: 按 ":" 分割 → 得到 ["10/Mar/2023", "13", "55", "36 +0000"]
                    //   step2: 取索引为1的元素 → "13"
                    //   step3: Integer.parseInt() 转成整数 → 13
                    //
                    // 【为什么不用 SimpleDateFormat 解析完整日期？】
                    // 本题只需要小时数，直接切字符串比解析完整日期快得多，代码也更简洁。
                    // 如果题目要求精确到分钟或按日期排序，才需要用 DateTimeFormatter 等类。
                    String[] dateParts = dateTimeStr.split(":");
                    if (dateParts.length >= 2) {
                        int hour = Integer.parseInt(dateParts[1]);
                        hourCountMap.put(hour, hourCountMap.getOrDefault(hour, 0) + 1);
                    }
                } else {
                    // 如果某一行格式不匹配正则，打印警告（方便调试，生产环境可以记日志）
                    System.out.println("警告：无法解析行 -> " + line);
                }
            }
        } catch (IOException e) {
            // 异常处理：文件不存在、没有权限读取等情况会走这里
            // 打印错误信息和堆栈，方便定位问题
            System.err.println("读取日志文件失败：" + e.getMessage());
            e.printStackTrace();
            return;  // 出错就退出，不再往下执行
        }

        // =====================================================================
        // 第四部分：计算并输出统计报告
        // =====================================================================
        System.out.println("--- 网站日志分析报告 ---");

        // -------------------- 【总览】必做任务2 --------------------
        System.out.println("[总览]");
        System.out.println("总请求数：" + totalRequests);

        // -------------------- 【高频IP Top 3】选做任务1 --------------------
        System.out.println("[高频IP Top 3]");
        List<Map.Entry<String, Integer>> topIps = getTopN(ipCountMap, 3);
        int rank = 1;
        for (Map.Entry<String, Integer> entry : topIps) {
            // entry.getKey() = IP地址，entry.getValue() = 访问次数
            System.out.println(rank + ". " + entry.getKey() + " (" + entry.getValue() + " 次)");
            rank++;
        }

        // -------------------- 【流量高峰时段】选做任务2 --------------------
        System.out.println("[流量高峰时段]");
        // maxEntry 包含两个信息：Key = 小时数，Value = 该小时请求数
        Map.Entry<Integer, Integer> maxEntry = getMaxEntry(hourCountMap);
        if (maxEntry != null) {
            int peakHour = maxEntry.getKey();
            int peakCount = maxEntry.getValue();
            // 格式化输出，如 "14点（即 14:00 - 14:59）"
            System.out.println("- 高峰时段：" + peakHour + "点（即 "
                    + String.format("%02d", peakHour) + ":00 - "
                    + String.format("%02d", peakHour) + ":59）");
            System.out.println("- 请求总数：" + peakCount + " 次");
        }

        // -------------------- 【错误请求分析】必做任务3 --------------------
        System.out.println("[错误请求分析]");
        System.out.println("- 错误请求（状态码 >= 400）：" + errorRequests + " 次");

        // 计算错误率（百分比），要注意防止除零错误（日志文件为空的情况）
        // 【为什么用 double 强制转换？】
        // 如果两个整数相除（int / int），Java 会做"整数除法"，直接丢掉小数部分，
        // 例如 8 / 50 = 0，而不是 0.16。
        // 所以我们要先把其中一个数转成浮点数 double，结果才会是浮点数。
        double errorRate = (totalRequests == 0) ? 0.0 : (double) errorRequests / totalRequests * 100;
        // String.format("%.2f%%", ...) 格式化保留2位小数，%% 表示输出一个百分号%
        System.out.println("- 错误率：" + String.format("%.2f", errorRate) + "%");
    }

    // =====================================================================
    // 第五部分：通用工具方法 —— 求 Map 中 Value 最大的前 N 个 Entry
    // =====================================================================
    //
    // 【方法说明】
    // 这个方法既可以用于"Top N IP"（Map<String, Integer>），
    // 也可以用于找"流量最大小时"（Map<Integer, Integer>），
    // 所以我们用泛型 <K>（Key的类型可以任意），增加代码复用性。
    //
    // 【算法思路】
    //   1. 把 HashMap 的 entrySet() 转成 ArrayList
    //   2. 按 Value 从大到小排序（降序）
    //   3. 取前 N 个（如果总数不足N个，就取全部）
    //
    // 【时间复杂度】O(M log M)，其中 M = Map 中的元素个数（排序的代价）
    // 对于本题的日志规模，完全够用。如果是几TB的大日志，可以用"小顶堆"优化到 O(M log N)
    // =====================================================================
    private static <K> List<Map.Entry<K, Integer>> getTopN(Map<K, Integer> map, int n) {
        // step1: 将 Map 的所有键值对（Entry）放入 List，方便排序
        // map.entrySet() 返回所有 Entry 的集合，new ArrayList<>(...) 构造一个可变的列表
        List<Map.Entry<K, Integer>> list = new ArrayList<>(map.entrySet());

        // step2: 按 Value 降序排序（大的在前）
        // Comparator.comparingInt(Map.Entry::getValue) 表示"按 Entry 的 getValue() 比较"
        // .reversed() 表示反转顺序 → 降序
        // 如果不加 reversed() 就是默认升序（从小到大）
        list.sort(Comparator.comparingInt(Map.Entry<K, Integer>::getValue).reversed());

        // step3: 取前 n 个元素（如果 list 大小 < n，就取到 list.size()）
        // Math.min(a, b) 返回两个数中较小的那个，避免 IndexOutOfBoundsException
        return list.subList(0, Math.min(n, list.size()));
    }

    // =====================================================================
    // 辅助方法：求 Map 中 Value 最大的那个 Entry
    // =====================================================================
    //
    // 【和 getTopN 的关系】
    // 其实可以直接调用 getTopN(map, 1).get(0) 实现，
    // 但单独写一个方法演示另一种思路：遍历一次找最大值，时间复杂度 O(M)，比排序更快。
    //
    // 【算法思路】打擂台法
    //   - 初始化 maxEntry = null
    //   - 遍历每一个 entry：
    //       如果当前 entry 的 value 比"擂主"maxEntry 的 value 大，就把它踢下去，自己当擂主
    //   - 遍历完后，擂主就是最大值
    // =====================================================================
    private static <K> Map.Entry<K, Integer> getMaxEntry(Map<K, Integer> map) {
        if (map == null || map.isEmpty()) {
            return null;  // Map 为空就返回 null
        }

        Map.Entry<K, Integer> maxEntry = null;

        // 遍历 Map 中的每一个键值对
        for (Map.Entry<K, Integer> entry : map.entrySet()) {
            // 打擂台：当前 entry 比擂主大，就取代擂主
            if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
                maxEntry = entry;
            }
        }

        return maxEntry;
    }

    // =====================================================================
    // 学习要点总结（请仔细阅读）：
    // =====================================================================
    //
    // 1. 【正则表达式】是解析结构化文本（日志、配置、HTML片段）的利器。
    //    初学建议：在 https://regex101.com/ 上练习，把正则贴进去看每一部分匹配什么。
    //
    // 2. 【HashMap】是面试最高频考点，要掌握：
    //    - 什么时候用？计数/去重/快速查找场景
    //    - getOrDefault(key, defaultValue)：非常常用的计数写法
    //    - 底层原理（数组+链表/红黑树、hash冲突、扩容机制）是进阶必学
    //
    // 3. 【文件读取】try-with-resources 是最佳实践，不要再手写 close() 和 finally。
    //
    // 4. 【排序 & 集合操作】Java 8+ 的 Stream API 和 Comparator 非常强大：
    //    - list.sort(Comparator)：原地排序
    //    - list.subList(from, to)：取子列表（左闭右开）
    //
    // 5. 【泛型方法】<K> 让方法支持任意类型的 Key，避免写两份几乎一样的代码。
    //    这是"代码复用"的基础手段，体现了 DRY (Don't Repeat Yourself) 原则。
    //
    // 6. 【防御性编程】空行跳过、除零判断、格式不匹配打印警告，这些细节决定代码的健壮性。
    // =====================================================================
}
