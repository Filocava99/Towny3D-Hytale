package it.cava99.towny3d

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.ScheduledFuture

class LifecycleManager {
    val pluginScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scheduledTasks = mutableListOf<ScheduledFuture<*>>()

    fun addTask(task: ScheduledFuture<*>) = scheduledTasks.add(task)

    fun cancelAll() {
        scheduledTasks.forEach { it.cancel(false) }
        scheduledTasks.clear()
        pluginScope.cancel()
    }
}
