import SockJs from "sockjs-client";
import Stomp from "stomp-websocket";
import Cookies from "js-cookie";
import store from "../store";

export class StompClient {
  constructor() {
    let isInDevelop = process.env.NODE_ENV === "development";
    let connection = isInDevelop
      ? "http://localhost:8080/fairsocket"
      : "/fairsocket";
    let socket = SockJs(connection);
    this.subscribeMap = new Map();
    this.stompClient = Stomp.over(socket);
    if (!isInDevelop) this.stompClient.debug = null;
  }

  connect(func) {
    this.stompClient.connect(
      {},
      () => {
        this.isFinished = new Promise((resolve) => {
          func(resolve);
        });
      },
      async () => {
        store.commit("chat/addRestartMessage");
        await this.reconnect();
      }
    );
  }

  async reconnect() {
    this.disconnect();
    console.log("Waiting 5min before trying to automatically reconnect...");
    await new Promise((r) => setTimeout(r, 300000));
    location.reload();
  }

  disconnect() {
    if (this.stompClient !== null) this.stompClient.disconnect();
    console.log("Your Client got disconnect from the Server.");
  }

  subscribe(destination, func) {
    let subscription = this.subscribeMap.get(destination);
    if (subscription) subscription.unsubscribe();
    subscription = this.stompClient.subscribe(
      destination,
      (message) => func(JSON.parse(message.body)),
      { uuid: Cookies.get("_uuid") }
    );
    this.subscribeMap.set(destination, subscription);
  }

  unsubscribe(destination) {
    let subscription = this.subscribeMap.get(destination);
    if (subscription) {
      subscription.unsubscribe();
      this.subscribeMap.delete(destination);
    }
  }

  send(destination, payload) {
    let data = { uuid: Cookies.get("_uuid") };
    if (!data.uuid) data.uuid = "";
    if (payload) {
      if (payload.content) data.content = payload.content;
      if (payload.event)
        data.event = JSON.stringify(serializeClickEvent(payload.event));
      if (payload.metadata) data.metadata = JSON.stringify(payload.metadata);
    }
    this.stompClient.send(destination, {}, JSON.stringify(data));
  }
}

function serializeClickEvent(event) {
  let serializableEvent = {
    isTrusted: event.isTrusted,
    screenX: event.screenX,
    screenY: event.screenY,
  };
  return serializableEvent;
}
