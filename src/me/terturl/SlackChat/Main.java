package me.terturl.SlackChat;

import java.net.ServerSocket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("deprecation")
public class Main extends JavaPlugin implements Listener {
	
	public final Logger logger = Logger.getLogger("Minecraft");
	public static ServerSocket serverSocket;
	public static DefaultHttpServerConnection conn;
	private HashMap<String, String> playerNick = new HashMap<>();
	private int port = Bukkit.getPort() - 100;
	public HttpParams params = new BasicHttpParams();
	public HttpRequest request;
	
	@Override
	public void onEnable() {
		
		getConfig().options().copyDefaults();
		
		Bukkit.getPluginManager().registerEvents(this, this);
		playerNick.put("announcement", "Announcement");
		for (Player player : getServer().getOnlinePlayers()) {
			playerNick.put(player.getName().toLowerCase(), player.getDisplayName());
		}
		try {
			serverSocket = new ServerSocket(port);
			logger.info("[SlackChat] Connected to port " + port);
		} catch (Exception e) {
			e.printStackTrace();
		}
		conn = new DefaultHttpServerConnection();
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						conn.bind(serverSocket.accept(), params);
						request = conn.receiveRequestHeader();
						conn.receiveRequestEntity((HttpEntityEnclosingRequest) request);
						HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
						String data = EntityUtils.toString(entity);
						String[] tokens = data.split("&");
						String user = tokens[8].replace("user_name=", "");
						if (!user.equals("slackbot")) {
							String message = URLDecoder.decode(tokens[9].replace("text=", "").replace("+", " "), "UTF-8").replace("&amp;", "");
							if (getServer().getPlayer(user) != null) {
								Bukkit.getServer().broadcastMessage("[" + getServer().getPlayer(user).getDisplayName() + "] " + message);
							} else if (playerNick.containsKey(user)) {
								Bukkit.getServer().broadcastMessage("[" + playerNick.get(user) + "] " + message);
							} else {
								Bukkit.getServer().broadcastMessage("[" + user + "] " + message);
							}
						}
						HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
						response.setEntity(new StringEntity("Got it"));
						conn.sendResponseHeader(response);
						conn.sendResponseEntity(response);
						conn.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}, 0);
	}
	
	@EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
		final String player = event.getPlayer().getName();
		final String msg = event.getMessage();
        postPayload(msg, player);
    }
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
	    String player = event.getPlayer().getName();
	    postPayload("_joined the game_",player);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
	    String player = event.getPlayer().getName();
	    postPayload("_left the game_", player);
		playerNick.put(event.getPlayer().getName().toLowerCase(), event.getPlayer().getDisplayName());
	}
	
	@Override
	public void onDisable() {
		try {
			serverSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized void postPayload(String msg, String player) {

	    HttpClient httpClient = HttpClientBuilder.create().build();
		msg = msg.replace("\"", "\\\"");
		String serverName = Bukkit.getServerName().toLowerCase();

	    try {
	        HttpPost request = new HttpPost("https://hooks.slack.com/services/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");
	        StringEntity params = new StringEntity("payload={\"channel\": \"#" + serverName + "\", \"username\": \"" + player + "\", \"icon_url\": \"https://cravatar.eu/helmavatar/" + player + "/100.png\", \"text\": \"" + msg + "\"}");
	        request.addHeader("content-type", "application/x-www-form-urlencoded");
	        request.setEntity(params);
	        httpClient.execute(request);
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        httpClient.getConnectionManager().shutdown();
	    }
	}

}
