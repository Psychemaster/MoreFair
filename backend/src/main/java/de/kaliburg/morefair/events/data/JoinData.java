package de.kaliburg.morefair.events.data;

import lombok.Data;
import lombok.NonNull;

@Data
public class JoinData {

  @NonNull
  private String username;
  @NonNull
  private String assholeTag;
  @NonNull
  private Integer assholePoints;
}
