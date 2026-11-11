package com.zereimaisum.app.domain

import com.zereimaisum.app.core.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class DashboardCalculatorTest {
    private val nov = YearMonth.of(2026, 11)
    private fun d(s: String) = Dates.parse(s)!!

    private val txs = listOf(
        TxView(TxKind.INCOME, 500_000, d("05/11/2026"), paid = true),
        TxView(TxKind.EXPENSE, 120_000, d("10/11/2026"), paid = true),
        TxView(TxKind.EXPENSE, 30_000, d("15/11/2026"), paid = false),
        // pagamento de fatura: transferência, não pode contar como despesa
        TxView(TxKind.TRANSFER, 80_000, d("08/11/2026"), paid = true),
        TxView(TxKind.EXPENSE, 10_000, d("20/10/2026"), paid = true),
    )

    @Test fun transfersAreNeverCountedTwice() {
        val s = DashboardCalculator.summarize(txs, nov, reservedCents = 50_000, today = d("11/11/2026"))
        assertEquals(500_000, s.incomeCents)
        assertEquals(150_000, s.expenseCents)
        assertEquals(350_000, s.monthBalanceCents)
        // acumulado = receitas pagas - despesas pagas até hoje (outubro incluso)
        assertEquals(500_000 - 120_000 - 10_000L, s.accumulatedCents)
        assertEquals(s.accumulatedCents - 50_000, s.availableCents)
    }

    @Test fun upcomingBillsIncludesOverdueAndNextDays() {
        val today = d("11/11/2026")
        val list = listOf(
            TxView(TxKind.EXPENSE, 1, d("09/11/2026"), paid = false), // vencida
            TxView(TxKind.EXPENSE, 2, d("15/11/2026"), paid = false), // em 4 dias
            TxView(TxKind.EXPENSE, 3, d("30/11/2026"), paid = false), // longe
            TxView(TxKind.EXPENSE, 4, d("12/11/2026"), paid = true),  // já paga
        )
        assertEquals(listOf(1L, 2L), DashboardCalculator.upcomingBills(list, today).map { it.amountCents })
    }

    @Test fun greetingByHour() {
        assertEquals("Bom dia, Ana!", DashboardCalculator.greeting(8, "Ana"))
        assertEquals("Boa tarde, Ana!", DashboardCalculator.greeting(14, "Ana"))
        assertEquals("Boa noite, Ana!", DashboardCalculator.greeting(22, "Ana"))
    }
}

class ScheduleTest {
    @Test fun monthlyKeepsOriginalDay() {
        val start = Dates.parse("31/01/2026")!!
        val dates = Schedule.occurrences(start, Recurrence.MONTHLY, 3).map(Dates::format)
        assertEquals(listOf("31/01/2026", "28/02/2026", "31/03/2026"), dates)
    }

    @Test fun installmentsAlwaysSumToTotal() {
        val parts = Schedule.splitInstallments(100_00, 3)
        assertEquals(listOf(3334L, 3333L, 3333L), parts)
        assertEquals(100_00L, parts.sum())
        for (n in 1..24) assertEquals(99_999L, Schedule.splitInstallments(99_999, n).sum())
    }
}

class DebtRulesTest {
    private fun debt(id: Long, balance: Long, bp: Int, status: DebtStatus = DebtStatus.OPEN) =
        DebtView(id, "Credor $id", 100_000, balance, bp, 10, status)

    @Test fun paymentNeverGoesNegativeAndMarksPaid() {
        val r = DebtRules.applyPayment(debt(1, 30_000, 0), 50_000)
        assertEquals(0L, r.newBalanceCents)
        assertEquals(30_000L, r.appliedCents)
        assertEquals(DebtStatus.PAID, r.status)
    }

    @Test fun prioritizesHighestInterestThenSmallestBalance() {
        val ranked = DebtRules.prioritize(
            listOf(debt(1, 50_000, 199), debt(2, 90_000, 1200), debt(3, 10_000, 199), debt(4, 1, 9999, DebtStatus.PAID))
        ).map { it.debt.id }
        assertEquals(listOf(2L, 3L, 1L), ranked)
    }

    @Test fun neverClaimsCpfIsClean() {
        val msg = DebtRules.statusMessage(listOf(debt(1, 0, 0, DebtStatus.PAID)))
        assertTrue(msg.contains("cadastradas"))
        assertFalse(msg.contains("CPF", ignoreCase = true))
    }

    @Test fun parsesAndFormatsRates() {
        assertEquals(199, DebtRules.parseRate("1,99"))
        assertEquals(1200, DebtRules.parseRate("12%"))
        assertEquals(0, DebtRules.parseRate(""))
        assertNull(DebtRules.parseRate("1,999"))
        assertNull(DebtRules.parseRate("-1"))
        assertEquals("1,99%", DebtRules.formatRate(199))
    }
}

class BoxRulesTest {
    @Test fun fiveDefaultBoxes() {
        assertEquals(
            listOf("Emergência", "Viagem", "Reforma", "Lazer", "Objetivos pessoais"),
            BoxRules.DEFAULT_BOXES.map { it.name },
        )
    }

    @Test fun cannotWithdrawMoreThanReserved() {
        assertNotNull(BoxRules.validateWithdraw(10_000, 10_001))
        assertNull(BoxRules.validateWithdraw(10_000, 10_000))
        assertNotNull(BoxRules.validateTransfer(1, 1, 10_000, 100))
    }

    @Test fun plannedWithdrawalOptions() {
        val start = Dates.parse("11/11/2026")!!
        assertEquals("11/06/2027", Dates.format(BoxRules.plannedWithdrawalDate(start, 7)))
    }

    @Test fun monthlyNeededRoundsUp() {
        val today = Dates.parse("01/01/2026")!!
        val target = Dates.parse("01/04/2026")!! // 3 meses
        assertEquals(3334L, BoxRules.monthlyNeeded(0, 10_000, today, target))
        assertNull(BoxRules.monthlyNeeded(10_000, 10_000, today, target))
    }

    @Test fun projectionExample() {
        assertEquals(2_400_00L, Projections.estimatedSavings(200_00, 12))
    }
}
