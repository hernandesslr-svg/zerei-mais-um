package com.zereimaisum.app.core

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/** Datas são guardadas como "epoch day" (dias desde 01/01/1970), sem fuso horário. */
object Dates {
    private val br: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

    private val monthNames = listOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    )

    fun today(): Long = LocalDate.now().toEpochDay()

    fun format(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(br)

    fun parse(text: String): Long? = try {
        LocalDate.parse(text.trim(), br).toEpochDay()
    } catch (e: DateTimeParseException) {
        null
    }

    fun monthOf(epochDay: Long): YearMonth = YearMonth.from(LocalDate.ofEpochDay(epochDay))

    fun monthLabel(month: YearMonth): String =
        "${monthNames[month.monthValue - 1].replaceFirstChar { it.uppercase() }} de ${month.year}"

    fun monthRange(month: YearMonth): LongRange =
        month.atDay(1).toEpochDay()..month.atEndOfMonth().toEpochDay()

    /** Soma meses preservando o dia quando possível (31/01 + 1 mês = 28 ou 29/02). */
    fun plusMonths(epochDay: Long, months: Long): Long =
        LocalDate.ofEpochDay(epochDay).plusMonths(months).toEpochDay()

    /** Mesmo [dayOfMonth] no mês informado, limitado ao último dia do mês. */
    fun dayInMonth(month: YearMonth, dayOfMonth: Int): Long =
        month.atDay(dayOfMonth.coerceIn(1, month.lengthOfMonth())).toEpochDay()
}
