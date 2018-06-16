package jetzt.nicht.minecraft.idleShutdown;

import java.util.Timer;
import java.util.TimerTask;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import jetzt.nicht.minecraft.idleShutdown.PlayerQuitListener;
import jetzt.nicht.minecraft.idleShutdown.PlayerJoinListener;

public class IdleShutdown extends JavaPlugin {
	PlayerQuitListener playerQuitListener;
	PlayerJoinListener playerJoinListener;
	Timer idleTimer;

	@Override
	public void onEnable() {
		// Create our listeners once, assign them later.
		// We pass in a reference to ourselves so the listener can call back.
		this.playerQuitListener = new PlayerQuitListener(this);
		this.playerJoinListener = new PlayerJoinListener(this);

		// Register our listener that listens for players quitting.
		System.out.println("Registering PlayerQuitListener...");
		getServer().getPluginManager()
			.registerEvents(this.playerQuitListener, this);
	}

	@Override
	public void onDisable() {
		// Cancel the timer and unregister all Listeners
		if (this.idleTimer != null) {
			this.idleTimer.cancel();
		}
		HandlerList.unregisterAll(this);
	}

	public void onTimerExpired() {
		System.out.println("Timer expired, shutting down server!");

		// Unregister all our Listeners, just to be on the safe side.
		HandlerList.unregisterAll(this);

		getServer().shutdown();
	}

	void onPlayerQuit() {
		System.out.println("A player has quit!");

		if (noPlayersOnline()) {
			System.out.println("No players are online anymore!");

			// We now register PlayerJoinListener, so we can abort the timer
			// if a player joins in the meantime
			// XXX: This might be racy: if the Listener is activated before we
			// set the timer, we might crash when trying to abort the timer not
			// yet running!
			System.out.println("Registering PlayerJoinListener...");
			getServer().getPluginManager()
				.registerEvents(this.playerJoinListener, this);

			System.out.println("Creating and scheduling timer...");
			this.idleTimer = new Timer();
			TimerTask idleTimerTask = new TimerTask() {
				@Override
				public void run() {
					onTimerExpired();
				}
			};
			this.idleTimer.schedule(idleTimerTask, 30*1000);
		}
	}

	void onPlayerJoin() {
		System.out.println("A player has joined!");

		// XXX: This might also be racy, if a player quits and joins after we
		// unregister the Listener but before we abort the timer.
		System.out.println("Unregistering PlayerJoinListener...");
		HandlerList.unregisterAll(this.playerJoinListener);
		
		System.out.println("Aborting timer...");
		this.idleTimer.cancel();
	}

	private boolean noPlayersOnline() {
		// Note: The event gets fired before the player is acutally reported as
		// "offline", so we just say there is no one online if the last player
		// is in the process of quitting.
		if (getServer().getOnlinePlayers().size() <= 1) {
			return true;
		} else {
			return false;
		}
	}
}
