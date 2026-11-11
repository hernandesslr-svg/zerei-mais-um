package com.zereimaisum.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zereimaisum.app.MainActivity
import com.zereimaisum.app.R
import com.zereimaisum.app.ZmApplication
import com.zereimaisum.app.core.Dates
import com.zereimaisum.app.core.Money
import com.zereimaisum.app.data.repo.toView
import com.zereimaisum.app.domain.DashboardCalculator
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Lembretes de contas a vencer: uma verificação por dia (por volta das 9h)
 * avisa sobre despesas não pagas vencidas, vencendo hoje ou amanhã.
 */
object BillReminders {
    const val CHANNEL_ID = "vencimentos"
    private const val WORK_NAME = "lembrete-vencimentos"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Contas a vencer", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Avisos de contas vencidas ou que vencem hoje e amanhã" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    /** Agenda a verificação diária. KEEP: não reinicia o agendamento a cada abertura do app. */
    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(9, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val request = PeriodicWorkRequestBuilder<BillReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, title: String, text: String) {
        if (!hasPermission(context)) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permissão revogada entre a verificação e o envio: apenas ignora.
        }
    }
}

class BillReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as ZmApplication).container
        if (!container.session.remindersEnabled.first()) return Result.success()
        val userId = container.session.userId.first()
        if (userId <= 0) return Result.success()

        val today = Dates.today()
        val txs = container.db.transactions().all(userId).map { it.toView() }
        val bills = DashboardCalculator.upcomingBills(txs, today, days = 1)
        if (bills.isEmpty()) return Result.success()

        val overdue = bills.count { it.epochDay < today }
        val total = bills.sumOf { it.amountCents }
        val title = when {
            overdue > 0 -> "Você tem $overdue conta(s) vencida(s)"
            bills.size == 1 -> "Conta vencendo: ${bills.first().description}"
            else -> "${bills.size} contas vencem até amanhã"
        }
        val text = "Total de ${Money.format(total)}. Abra o Zerei Mais Um para pagar ou marcar como paga."
        BillReminders.show(applicationContext, title, text)
        return Result.success()
    }
}
