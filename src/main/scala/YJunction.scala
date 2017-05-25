/**
  * Created by mnielsen on 5/25/2017.
  */

import akka.actor.{Actor, ActorLogging, ActorRef, FSM, Props}
import akka.util.ByteString
import java.net.InetSocketAddress

import akka.event.{Logging, LoggingAdapter}
import akka.io.Tcp._
import ch.qos.logback.classic.Logger
import akka.io.{IO, Tcp}

class YJunction extends App {

}

class Server(serverAddress: InetSocketAddress, clientAddresses: Seq[InetSocketAddress], port: Int) extends Actor with ActorLogging {

  import context.system // implicitly used by IO(Tcp)
  IO(Tcp) ! Bind(self, serverAddress)

  override def receive: Receive = {
    case b@Bound(localAddress) =>
      log.info("Bound to {}", localAddress)
      context.parent ! b

    case CommandFailed(_: Bind) =>
      log.error("Command failed")
      context stop self

    case c@Connected(remote, local) =>

  }
}

sealed trait InboundMessage
case object Terminate extends InboundMessage
case object StatusReport extends InboundMessage

sealed trait State

case object NotInitialized extends State

case object AwaitingRetry extends State

case object Running extends State

sealed trait Data

case object Uninitialized extends Data

case class ConnectedToRemote(bytesHandled: Long, bytesBackwashed: Long, backwashActor: Option[BackwashHandler]) extends Data

/**
  * Represents a connection from a client to a remote server.
  * Data received from the client is relayed to the remote server
  * Data received from the remote server is logged as an error
  * If the connection to the remote server fails, reconnection is attempted until the `Terminate` message is received
  * Reports status to the sender of the `StatusReport` `InboundMessage`
  * @param remote
  */
class RelayHandler(remote: InetSocketAddress) extends FSM[State, Data] {

  import context.system // implicitly used by IO(Tcp)
  IO(Tcp) ! Connect(remote)
  startWith(NotInitialized, Uninitialized)
  when(NotInitialized) {
    case Event(CommandFailed(_: Connect), d@_) =>
      log.error("Initial connect failed")
      goto(AwaitingRetry) using d
    case Event(c@Connected(remote, local), d@_) =>
      log.info("Connected to {} via {}", remote, local)
      goto(Running) using ConnectedToRemote(0, 0, None)
  }
  when(Running) {

  }


}

object RelayHandler {
  def props(remote: InetSocketAddress, replies: ActorRef) = Props(classOf[RelayHandler], remote)
}

class BackwashHandler(remote: InetSocketAddress) extends Actor with ActorLogging {
  def receive = {
    case Received(data) => log.error("received backwash{} from {}", data, remote)
    case PeerClosed => context.parent ! RemoteTerminated
  }
}

object BackwashHandler {
  def props(remote: InetSocketAddress) = Props(classOf[BackwashHandler], remote)
}

case object RemoteTerminated