import Decimal from "break_infinity.js";

export default {
  getMinimumPointsForPromote(ladder) {
    return ladder.basePointsToPromote;
  },
  getMinimumPeopleForPromote(settings, ladder) {
    return Math.max(settings.minimumPeopleForPromote, ladder.number);
  },
  getAutoPromoteCost(settings, ladder, rank) {
    let minPeople = this.getMinimumPeopleForPromote(settings, ladder);
    let divisor = Math.max(rank - minPeople + 1, 1);
    return settings.baseGrapesNeededToAutoPromote
      .div(new Decimal(divisor))
      .floor();
  },
  isLadderUnlocked(settings, ladder) {
    if (ladder.rankers.length <= 0) return false;
    let rankerCount = ladder.rankers.length;
    if (rankerCount < this.getMinimumPeopleForPromote(settings, ladder))
      return false;
    return (
      ladder.rankers[0].points.cmp(this.getMinimumPointsForPromote(ladder)) >= 0
    );
  },
  canThrowVinegar(settings, ladder) {
    return (
      ladder.rankers[0].growing &&
      !ladder.rankers[0].you &&
      ladder.rankers[0].points.cmp(this.getMinimumPointsForPromote(ladder)) >=
        0 &&
      ladder.rankers.length >=
        this.getMinimumPeopleForPromote(settings, ladder) &&
      ladder.yourRanker.vinegar.cmp(
        this.getVinegarThrowCost(settings, ladder)
      ) >= 0
    );
  },
  getVinegarThrowCost(settings, ladder) {
    return settings.baseVinegarNeededToThrow.mul(new Decimal(ladder.number));
  },
  getNextUpgradeCost(ladder, currentUpgrade, ladderTypes) {
    let flatMulti = 1;
    let ladderMulti = 1;
    if (ladderTypes.has("CHEAP")) {
      ladderMulti = 0.5;
      flatMulti = 0.5;
    }
    if (ladderTypes.has("EXPENSIVE")) {
      ladderMulti = 1.5;
      flatMulti = 1.5;
    }

    let ladderDec = new Decimal(ladder.number);
    ladderDec = ladderDec.mul(ladderMulti).add(new Decimal(1));

    let result = ladderDec.pow(currentUpgrade + 1).mul(flatMulti);
    return result.round().max(new Decimal(1));
  },
  calculatePointsNeededForPromote(settings, ladder) {
    // If not enough Players -> Infinity
    if (ladder.rankers.length < 2) {
      return new Decimal(Infinity);
    }

    // If before autopromote unlocks -> 1st place
    if (ladder.number < settings.autoPromoteLadder) {
      return ladder.rankers[0].you
        ? ladder.rankers[1].points.add(1)
        : ladder.rankers[0].points.add(1);
    }

    let leadingRanker = ladder.rankers[0].you
      ? ladder.yourRanker
      : ladder.rankers[0];
    let pursuingRanker = ladder.rankers[0].you
      ? ladder.rankers[1]
      : ladder.yourRanker;

    // How many more points does the ranker gain against his pursuer, every Second
    let powerDiff = (
      leadingRanker.growing ? leadingRanker.power : new Decimal(0)
    ).sub(pursuingRanker.growing ? pursuingRanker.power : 0);
    // Calculate the needed Point difference, to have f.e. 30seconds of point generation with the difference in power
    let neededPointDiff = powerDiff.mul(settings.manualPromoteWaitTime).abs();

    return Decimal.max(
      (leadingRanker.you ? pursuingRanker : leadingRanker).points.add(
        neededPointDiff
      ),
      this.getMinimumPointsForPromote(ladder)
    );
  },
  canPromote(settings, ladder) {
    // Don't have enough points
    if (
      ladder.yourRanker.points.cmp(
        this.calculatePointsNeededForPromote(settings, ladder)
      ) < 0
    ) {
      return false;
    }

    if (
      ladder.rankers.length < this.getMinimumPeopleForPromote(settings, ladder)
    ) {
      return false;
    }

    return ladder.yourRanker.rank <= 1;
  },
  canBuyAutoPromote(settings, ladder) {
    return (
      !(ladder.yourRanker.autoPromote || ladder.types.has("FREE_AUTO")) &&
      ladder.yourRanker.grapes.cmp(
        this.getAutoPromoteCost(settings, ladder, ladder.yourRanker.rank)
      ) >= 0 &&
      ladder.number >= settings.autoPromoteLadder &&
      !ladder.types.has("NO_AUTO")
    );
  },
};
