package misis.paymentSystem

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.ConfigFactory
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import misis.paymentSystem.db.InitDb
import misis.paymentSystem.repository.{AccountCashbackRepositoryDb, PaymentClient}
import misis.paymentSystem.route.{AccountRoute, CashbacksRoute, HelloRoute}
import slick.jdbc.PostgresProfile.api._

object PaymentsDbApp extends App with FailFastCirceSupport {
  implicit val system: ActorSystem = ActorSystem("PayApp")
  implicit val ec = system.dispatcher
  implicit val db = Database.forConfig("database.postgres")
  val port = ConfigFactory.load().getInt("port")


  new InitDb().prepare()

  val client = new PaymentClient

  val repository = new AccountCashbackRepositoryDb(client)

  val helloRoute = new HelloRoute().route
  val accRoute = new AccountRoute(repository).route
  val cashbackRoute = new CashbacksRoute(repository).route

  Http().newServerAt("0.0.0.0", port = port).bind(helloRoute ~ accRoute ~ cashbackRoute)
}
