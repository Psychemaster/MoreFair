import Decimal from "break_infinity.js";

class Ranker {
  constructor({
    accountId = 1,
    username = "",
    rank = 1,
    points = new Decimal(0),
    power = new Decimal(1),
    bias = 0,
    multi = 1,
    you = true,
    growing = true,
    autoPromote = false,
    tag = "",
    ahPoints = 0,
    grapes = new Decimal(0),
    vinegar = new Decimal(0),
  } = {}) {
    // TODO: LOOK AT THIS @Tobi
    // Object.assign(this, arguments[0]);

    this.accountId = accountId;
    this.username = username;
    this.rank = rank;
    this.points = Object.freeze(new Decimal(points));
    this.power = Object.freeze(new Decimal(power));
    this.bias = bias;
    this.multi = multi;
    this.you = you;
    this.growing = growing;
    this.autoPromote = autoPromote;
    this.tag = tag;
    this.ahPoints = ahPoints;

    if (this.you) {
      this.grapes = Object.freeze(new Decimal(grapes));
      this.vinegar = Object.freeze(new Decimal(vinegar));
    } else {
      this.grapes = Object.freeze(new Decimal(0));
      this.vinegar = Object.freeze(new Decimal(0));
    }
  }
}

export default Ranker;
