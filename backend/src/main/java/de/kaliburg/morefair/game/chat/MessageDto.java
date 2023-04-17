package de.kaliburg.morefair.game.chat;

import de.kaliburg.morefair.FairConfig;
import java.time.ZoneOffset;
import lombok.Data;

@Data
public class MessageDto {

  private final String message;
  private final String username;
  private final Long accountId;
  private final Long timestamp;
  private final String tag;
  private final Integer ahPoints;
  private final String metadata;

  public MessageDto(MessageEntity message, FairConfig config) {
    this.tag = config.getAssholeTag(message.getAccount().getAssholeCount());
    this.ahPoints = message.getAccount().getAssholePoints();
    this.message = message.getMessage();
    this.username = message.getAccount().getUsername();
    this.accountId = message.getAccount().getId();
    this.timestamp = message.getCreatedOn().withOffsetSameInstant(ZoneOffset.UTC).toEpochSecond();
    this.metadata = message.getMetadata();
  }
}
