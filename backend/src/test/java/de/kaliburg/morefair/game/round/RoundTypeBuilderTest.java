package de.kaliburg.morefair.game.round;


import static org.assertj.core.api.Assertions.assertThat;

import de.kaliburg.morefair.utils.EnableLoggingPropertiesBeforeAll;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


@Slf4j
@EnableLoggingPropertiesBeforeAll
class RoundTypeBuilderTest {

  private static final List<Set<RoundType>> roundTypesList = new ArrayList<>();
  private static final HashMap<Set<RoundType>, Integer> countMap = new HashMap<>();

  @BeforeAll
  static void beforeAll() {
    for (int i = 0; i < 100; i++) {
      RoundTypeBuilder builder = new RoundTypeBuilder();
      roundTypesList.add(builder.build());
    }

    roundTypesList.forEach(roundTypes -> {
      countMap.merge(roundTypes, 1, Integer::sum);
    });
  }

  @Test
  void build_allRoundTypes_DefaultOnlyAlone() {
    roundTypesList.forEach(roundTypes -> {
      if (roundTypes.size() > 1) {
        assertThat(roundTypes).doesNotContain(RoundType.DEFAULT);
      }
    });
  }

  @Test
  @Disabled
  void build_round100_ChaosSlowAuto() {
    RoundTypeBuilder builder = new RoundTypeBuilder();
    builder.setRoundNumber(100);
    Set<RoundType> build = builder.build();

    LadderTypeBuilder ladderTypeBuilder = new LadderTypeBuilder();
    ladderTypeBuilder.setRoundTypes(build);
    ladderTypeBuilder.setLadderNumber(1);
    ladderTypeBuilder.setAssholeLadderNumber(30);
    ladderTypeBuilder.setRoundNumber(100);
    Set<LadderType> build1 = ladderTypeBuilder.build();
  }
}