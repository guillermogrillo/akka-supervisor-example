import akka.actor.{SupervisorStrategy, typed}
import akka.actor.typed.{ActorSystem, Behavior, PostStop, PreRestart, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object SupervisorActor {
  def apply(): Behavior[String] = Behaviors.setup(context => new SupervisorActor(context))
}

class SupervisorActor(context: ActorContext[String]) extends AbstractBehavior[String](context){
  private val child = context.spawn(
    Behaviors.supervise(ChildActor()).onFailure(typed.SupervisorStrategy.restart),
    name = "child-actor")

  override def onMessage(msg: String): Behavior[String] = {
    msg match {
      case "failing" =>
        child ! "failed"
        this
      case _ =>
        child ! "so far so god"
        this
    }
  }
}

object ChildActor {
  def apply(): Behavior[String] = Behaviors.setup(context => new ChildActor(context))
}

class ChildActor(context: ActorContext[String]) extends AbstractBehavior[String](context){

  println("Child Actor created")

  override def onMessage(msg: String): Behavior[String] = {
    msg match {
      case "failed" =>
        println("My supervisor told me to fail...")
        throw new Exception("No more")
      case _ =>
        println("Working on it")
        this
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PreRestart =>
      println("Restarting after failure")
      this
    case PostStop =>
      println("Stopping child actor")
      this
  }
}

object Main extends App {
  val root = ActorSystem(SupervisorActor(), "supervisor-actor")
  root ! "start"
  root ! "work harder"
  root ! "failing"
}