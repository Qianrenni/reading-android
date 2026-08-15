package com.qianrenni.reading.util


fun indexToCN(index: Int): String {
    require(index >= 0)
    val cnDigits = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
    val cnUnits =
        arrayOf("", "十", "百", "千", "万", "十万", "百万", "千万", "亿", "十亿")
    val numString = index.toString()
    return numString.mapIndexed { index, ch ->
        when (val digit = ch.digitToInt()) {
            0 -> if (index == numString.length - 1) "" else "零"
            1 -> if (numString.length - 1 - index == 1) "十" else "${cnDigits[digit]}${cnUnits[numString.length - 1 - index]}"
            else -> "${cnDigits[digit]}${cnUnits[numString.length - 1 - index]}"
        }
    }.joinToString("").removeSuffix("零")
}