package de.kaliburg.morefair.api.utils;

import de.kaliburg.morefair.api.websockets.UserPrincipal;
import de.kaliburg.morefair.api.websockets.messages.WSMessageAnswer;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * A class to abstract the sending of Messages via Websockets over the Stomp-Protocol. The available
 * destination types are:
 * <ul>
 *   <li><code>/topic/{destination}</code></li>
 *   <li><code>/user/queue/{destination}/</code></li>
 *   <li><code>/private/{uuid}/{destination}</code></li>
 * </ul>
 */
@Component
@Log4j2
public class WsUtils {

  private final SimpMessagingTemplate simpMessagingTemplate;

  public WsUtils(SimpMessagingTemplate simpMessagingTemplate) {
    this.simpMessagingTemplate = simpMessagingTemplate;
  }

  /**
   * Sends a message stomp-privately to the user with the given UserPrincipal, this is mostly used
   * for answers to a /app request but this one defaults to the status HttpStatus.OK (200).
   *
   * @param sha     the HeaderAccessor that contains the UserPrincipal
   * @param dest    the destination so that /user/queue/{dest}/ is the destination of the message
   * @param content the content that gets send with the message
   */
  public void convertAndSendToUser(SimpMessageHeaderAccessor sha, String dest, Object content) {
    simpMessagingTemplate.convertAndSendToUser(sha.getUser().getName(), dest,
        new WSMessageAnswer<>(content));
  }

  /**
   * Sends a message stomp-privately to the user with the given UserPrincipal, this is mostly used
   * for answers to a /app request
   *
   * @param sha     the HeaderAccessor that contains the UserPrincipal
   * @param dest    the destination so that /user/queue/{dest}/ is the destination of the message
   * @param content the content that gets send with the message
   * @param status  the status of the request this is the answer to, in the format of a HttpRequest
   */
  public void convertAndSendToUser(SimpMessageHeaderAccessor sha, String dest, Object content,
      HttpStatus status) {
    simpMessagingTemplate.convertAndSendToUser(sha.getUser().getName(), dest,
        new WSMessageAnswer<>(content, status));
  }

  /**
   * Sends a message stomp-privately to the user with the given UserPrincipal, this is mostly used
   * for answers to a /app request
   *
   * @param sha    the HeaderAccessor that contains the UserPrincipal
   * @param dest   the destination so that /user/queue/{dest}/ is the destination of the message
   * @param status the status of the request this is the answer to, in the format of a HttpRequest
   */
  public void convertAndSendToUser(SimpMessageHeaderAccessor sha, String dest, HttpStatus status) {
    simpMessagingTemplate.convertAndSendToUser(sha.getUser().getName(), dest,
        new WSMessageAnswer<>("", status));
  }

  /**
   * Sends a message pseudo-privately to the user with a given uuid.
   *
   * @param uuid    the uuid of the account of the user that the message is directed at
   * @param dest    the destination so that /private/{uuid}/{dest}/ is the destination of the
   *                message
   * @param content the content that gets send with the message
   */
  public void convertAndSendToUser(UUID uuid, String dest, Object content) {
    StringBuilder sb =
        new StringBuilder().append("/private/").append(uuid.toString()).append("/").append(dest);
    if (!dest.endsWith("/")) {
      sb.append("/");
    }

    simpMessagingTemplate.convertAndSend(dest, content);
  }

  /**
   * Sends a message publicly to the destination.
   *
   * @param dest    the destination so that /topic/{dest}/ is the destination of the message
   * @param content the content that gets send with the message
   */
  public void convertAndSendToTopic(String dest, Object content) {
    StringBuilder sb = new StringBuilder().append("/topic/").append(dest);
    if (!dest.endsWith("/")) {
      sb.append("/");
    }

    simpMessagingTemplate.convertAndSend(dest, content);
  }

  /**
   * Retrieves the UserPrincipal from the SimpMessageHeaderAccessor.
   *
   * @param sha the SimpMessageHeaderAccessor that contains the UserPrincipal
   * @return the UserPrincipal from the SimpMessageHeaderAccessor
   */
  public UserPrincipal convertMessageHeaderAccessorToUserPrincipal(
      SimpMessageHeaderAccessor sha) {
    return ((UserPrincipal) sha.getUser());
  }
}
