package jetzt.nicht.minecraft.idleShutdown;

import java.util.logging.Logger;
import java.util.Timer;
import java.util.TimerTask;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import jetzt.nicht.minecraft.idleShutdown.PlayerQuitListener;
import jetzt.nicht.minecraft.idleShutdown.PlayerJoinListener;

public class IdleShutdown extends JavaPlugin {
	private Logger log;
	private Integer idleWaitTime;
	private PlayerQuitListener playerQuitListener;
	private PlayerJoinListener playerJoinListener;
	private Timer idleTimer;

	@Override
	public void onEnable() {
		// First of all, get the logger
		this.log = getLogger();

		// Write the default config, if it does not exist.
		saveDefaultConfig();
		// Get the current config, default or otherwise.
		this.idleWaitTime = getConfig().getInt("idle_wait_time");
		// Negative times are not supported (duh).
		if (this.idleWaitTime < 0) {
			log.warning("You cannot use a negative idle_wait_time! Time set to 0 seconds.");
			this.idleWaitTime = 0;
		}
		// Let the admin know the configured idle time.
		log.info(String.format(("This server is running IdleShutdown: " +
					"It will stop after %d seconds with no player online."),
					this.idleWaitTime));

		// Create our listeners once, assign them later.
		// We pass in a reference to ourselves so the listener can call back.
		this.playerQuitListener = new PlayerQuitListener(this);
		this.playerJoinListener = new PlayerJoinListener(this);

		// Register our listener that listens for players quitting.
		log.finer("Registering PlayerQuitListener...");
		getServer().getPluginManager()
			.registerEvents(this.playerQuitListener, this);

		// Run the playerQuit routine once. We might have been enabled at
		// runtime, or the server was started automatically without anyone
		// actually joining. TODO: Make this configurable.
		if (noPlayerOnline()) {
			log.fine("There are no players online!");
			startIdleTimer();
		}
	}

	@Override
	public void onDisable() {
		// Cancel the timer and unregister all Listeners
		if (this.idleTimer != null) {
			log.finer("Cancelling timer...");
			this.idleTimer.cancel();
		}
		HandlerList.unregisterAll(this);
	}

	public void onTimerExpired() {
		log.fine("Timer expired!");

		if(noPlayerOnline()) {
			log.info("No players online, shutting down");

			// Unregister all our Listeners, just to be on the safe side.
			HandlerList.unregisterAll(this);

			getServer().shutdown();
		} else {
			log.warning("A player has come online and we have "
					+ "not been notified! Something is very wrong here!");
		}
	}

	void onPlayerQuit() {
		log.fine("A player has quit!");

		if (lastPlayerOnline()) {
			log.info("The last player is leaving!");
			startIdleTimer();
		}
	}

	void onPlayerJoin() {
		log.fine("A player has joined!");

		// XXX: This might also be racy, if a player quits and joins after we
		// unregister the Listener but before we abort the timer.
		log.finer("Unregistering PlayerJoinListener...");
		HandlerList.unregisterAll(this.playerJoinListener);
		
		log.finer("Aborting timer...");
		this.idleTimer.cancel();
	}

	private boolean lastPlayerOnline() {
		// Note: When the PlayerQuit event is fired, the player is technically
		// still online. Thus, we check if it is the last player leaving.
		if (getServer().getOnlinePlayers().size() <= 1) {
			return true;
		} else {
			return false;
		}
	}

	private boolean noPlayerOnline() {
		if (getServer().getOnlinePlayers().size() <= 0) {
			return true;
		} else {
			return false;
		}
	}

	private void startIdleTimer() {
		if (this.idleWaitTime == 0) {
			log.fine("idle_wait_time is 0, not scheduling timer!");
			onTimerExpired();
		} else {
			// We now register PlayerJoinListener, so we can abort the timer
			// if a player joins in the meantime
			// XXX: This might be racy: if the Listener is activated before we
			// set the timer, we might crash when trying to abort the timer not
			// yet running!
			log.finer("Registering PlayerJoinListener...");
			getServer().getPluginManager()
				.registerEvents(this.playerJoinListener, this);

			log.finer("Creating and scheduling timer...");
			this.idleTimer = new Timer();
			TimerTask idleTimerTask = new TimerTask() {
				@Override
				public void run() {
					onTimerExpired();
				}
			};
			this.idleTimer.schedule(idleTimerTask, this.idleWaitTime*1000);
		}
	}
}
