package de.kaliburg.morefair.api.websockets;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.kaliburg.morefair.api.utils.HttpUtils;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Log4j2
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

  private final static Integer MAX_CONNECTIONS_PER_MINUTE = 10;
  private final LoadingCache<Integer, Integer> connectionsPerIpAddress;

  public CustomHandshakeHandler() {
    super();
    connectionsPerIpAddress = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES)
        .build(new CacheLoader<>() {
          @Override
          public @Nullable Integer load(@NonNull Integer s) throws Exception {
            return 0;
          }
        });
  }

  @Override
  protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
      Map<String, Object> attributes) {

    Integer ip = 0;
    try {
      ip = HttpUtils.getIp(request);
    } catch (Exception e) {
      log.error("Could not determine IP address of client", e);
      return null;
    }

    if (isMaximumConnectionsPerMinuteExceeded(ip)) {
      return null;
    }

    UUID uuid = UUID.randomUUID();
    log.trace("Determining user for session {} with ip {} as {}", request.getURI().toString(),
        ip, uuid);
    return new UserPrincipal(uuid.toString(), ip);
  }

  private boolean isMaximumConnectionsPerMinuteExceeded(Integer ipAddress) {
    Integer requests;
    requests = connectionsPerIpAddress.get(ipAddress);
    if (requests != null) {
      if (requests >= MAX_CONNECTIONS_PER_MINUTE) {
        return true;
      }
    } else {
      requests = 0;
    }
    requests++;
    connectionsPerIpAddress.put(ipAddress, requests);
    return false;
  }
}
