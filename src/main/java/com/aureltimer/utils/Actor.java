package com.aureltimer.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * ✅ SINGLE ACTOR PATTERN
 * 
 * Thread unique pour toutes les mutations d'état, évite les race conditions.
 * Toutes les opérations sont sérialisées dans un seul thread.
 */
public class Actor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Actor.class);
    
    private final String name;
    private final ScheduledExecutorService executor;
    private volatile boolean shutdown = false;
    
    public Actor(String name) {
        this.name = name;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "Actor-" + name);
            thread.setDaemon(true); // Permet à la JVM de se fermer même si l'actor tourne
            return thread;
        });
        
        LOGGER.debug("🎬 Actor '{}' créé", name);
    }
    
    /**
     * Soumet une tâche à exécuter dans l'actor thread
     */
    public <T> CompletableFuture<T> submit(Callable<T> task) {
        if (shutdown) {
            return CompletableFuture.failedFuture(new IllegalStateException("Actor " + name + " is shutdown"));
        }
        
        CompletableFuture<T> future = new CompletableFuture<>();
        
        executor.submit(() -> {
            try {
                T result = task.call();
                future.complete(result);
            } catch (Throwable t) {
                LOGGER.error("❌ Erreur dans Actor '{}': {}", name, t.getMessage(), t);
                future.completeExceptionally(t);
            }
        });
        
        return future;
    }
    
    /**
     * Soumet une tâche sans retour à exécuter dans l'actor thread
     */
    public CompletableFuture<Void> submit(Runnable task) {
        return submit(() -> {
            task.run();
            return null;
        });
    }
    
    /**
     * Programme une tâche à exécuter après un délai
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        if (shutdown) {
            throw new IllegalStateException("Actor " + name + " is shutdown");
        }
        
        return executor.schedule(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                LOGGER.error("❌ Erreur tâche programmée dans Actor '{}': {}", name, t.getMessage(), t);
            }
        }, delay, unit);
    }
    
    /**
     * Programme une tâche à exécuter après un délai (Duration)
     */
    public ScheduledFuture<?> schedule(Runnable task, java.time.Duration delay) {
        LOGGER.debug("🕒 Actor '{}' programme tâche dans {}ms", name, delay.toMillis());
        ScheduledFuture<?> future = schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
        LOGGER.debug("✅ Tâche programmée dans Actor '{}' - ScheduledFuture: {}", name, future);
        return future;
    }
    
    /**
     * ✅ SCHEDULE SAFE : Programme une tâche périodique avec protection contre les crashes
     * 
     * Si une exécution crash, les suivantes continuent normalement.
     */
    public ScheduledFuture<?> scheduleAtFixedRateSafe(Runnable task, long initialDelay, long period, TimeUnit unit) {
        if (shutdown) {
            throw new IllegalStateException("Actor " + name + " is shutdown");
        }
        
        return executor.scheduleAtFixedRate(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                // ✅ PROTECTION TOTALE : même les Error sont catchées
                LOGGER.error("❌ Erreur tâche périodique dans Actor '{}' (continue quand même): {}", 
                           name, t.getMessage(), t);
                // Le scheduler continue même après cette erreur
            }
        }, initialDelay, period, unit);
    }
    
    /**
     * ✅ SCHEDULE AT FIXED RATE (Duration)
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, java.time.Duration initialDelay, java.time.Duration period) {
        return scheduleAtFixedRateSafe(task, initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * Retourne l'executor pour les cas avancés
     */
    public ScheduledExecutorService executor() {
        return executor;
    }
    
    /**
     * Arrêt propre de l'actor
     */
    public void shutdown() {
        if (shutdown) return;
        
        shutdown = true;
        LOGGER.info("🛑 Arrêt Actor '{}'", name);
        
        executor.shutdown();
        
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("⚠️ Actor '{}' ne s'est pas arrêté proprement, force shutdown", name);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.warn("⚠️ Interruption pendant arrêt Actor '{}'", name);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Vérifie si l'actor est arrêté
     */
    public boolean isShutdown() {
        return shutdown || executor.isShutdown();
    }
    
    @Override
    public String toString() {
        return "Actor{name='" + name + "', shutdown=" + shutdown + "}";
    }
}
