package com.smarthome.app.core.util

data class DeviceCommand(
    val deviceCode: String,
    val action: Int,
    val displayName: String
)

object CommandParser {

    private val devicePatterns = listOf(
        Triple("""(打开|开启|开)\s*(红灯|ledRed)""".toRegex(), "ledRed", 1),
        Triple("""(关闭|关)\s*(红灯|ledRed)""".toRegex(), "ledRed", 0),
        Triple("""(打开|开启|开)\s*(绿灯|led)""".toRegex(), "led", 1),
        Triple("""(关闭|关)\s*(绿灯|led)""".toRegex(), "led", 0),
        Triple("""(打开|开启|开)\s*(黄灯|ledYellow)""".toRegex(), "ledYellow", 1),
        Triple("""(关闭|关)\s*(黄灯|ledYellow)""".toRegex(), "ledYellow", 0),
        Triple("""(打开|开启|开)\s*(蜂鸣器|蜂鸣|警报|报警|buzzer)""".toRegex(), "buzzer", 1),
        Triple("""(关闭|关)\s*(蜂鸣器|蜂鸣|警报|报警|buzzer)""".toRegex(), "buzzer", 0),
    )

    private val deviceNameMap = mapOf(
        "ledRed" to "红灯",
        "led" to "绿灯",
        "ledYellow" to "黄灯",
        "buzzer" to "蜂鸣器"
    )

    // 天气相关关键词
    private val weatherKeywords = setOf("天气", "气温", "温度", "下雨", "下雪", "刮风")

    // 常见中国城市名（用户最可能查的），不是完整列表而是常用城市
    private val knownCities = setOf(
        "北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "南京", "西安",
        "重庆", "天津", "苏州", "长沙", "郑州", "东莞", "青岛", "沈阳", "宁波",
        "昆明", "大连", "厦门", "合肥", "佛山", "福州", "哈尔滨", "济南", "温州",
        "长春", "石家庄", "常州", "泉州", "南宁", "贵阳", "南昌", "太原", "烟台",
        "珠海", "惠州", "徐州", "海口", "乌鲁木齐", "绍兴", "兰州", "拉萨", "呼和浩特"
    )

    // 非城市关键词（排除这些词被误认为城市）
    private val nonCityWords = setOf("今天", "明天", "昨天", "后天", "前天", "本周", "下周", "这个", "那个", "什么", "怎么")

    /**
     * 解析设备控制命令
     */
    fun parseDeviceCommand(text: String): DeviceCommand? {
        val trimmed = text.trim()
        for ((regex, code, action) in devicePatterns) {
            if (regex.matches(trimmed) || regex.containsMatchIn(trimmed)) {
                val displayName = deviceNameMap[code] ?: code
                return DeviceCommand(code, action, displayName)
            }
        }
        return null
    }

    /**
     * 解析天气查询，返回城市名
     * "北京天气" → "北京"
     * "上海的天气怎么样" → "上海"
     * "查一下广州天气" → "广州"
     * 没有城市名则返回 null（走 AI 对话）
     */
    fun parseWeatherQuery(text: String): String? {
        val trimmed = text.trim()

        // 第一步：检查是否包含天气关键词
        val hasWeather = weatherKeywords.any { trimmed.contains(it) }
        if (!hasWeather) return null

        // 第二步：从已知城市列表中找
        for (city in knownCities) {
            if (trimmed.contains(city)) return city
        }

        // 第三步：正则提取城市名（兼容"北京的天气"、"北京天气"等格式）
        val cityPatterns = listOf(
            """([一-鿿]{2,4}(?:市)?)\s*的\s*(天气|气温|温度)""".toRegex(),
            """([一-鿿]{2,4}(?:市)?)\s*(天气|气温|温度|下雨|下雪|刮风)""".toRegex(),
            """(天气|气温|温度)\s*([一-鿿]{2,4}(?:市)?)""".toRegex(),
        )

        for (pattern in cityPatterns) {
            val match = pattern.find(trimmed)
            if (match != null) {
                // 取分组中长度 2-4 的中文且不是非城市词
                for (group in match.groupValues.drop(1)) {
                    if (group.length in 2..4 &&
                        group.none { it !in '一'..'鿿' } &&
                        group !in nonCityWords &&
                        group !in weatherKeywords) {
                        return group
                    }
                }
            }
        }

        return null
    }
}
