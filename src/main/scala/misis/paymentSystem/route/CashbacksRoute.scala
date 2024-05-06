package misis.paymentSystem.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import misis.paymentSystem.model._
import misis.paymentSystem.repository._
import scala.util.{Failure, Success}

class CashbacksRoute(repository: AccountCashbackRepository) extends FailFastCirceSupport {
  def route =
    (path("cashbacks") & get) {
      val offers = repository.getCashbacks()
      complete(offers)
    } ~
      path("cashback") {
        (post & entity(as[CreateCashback])) { newPerc =>
          complete(repository.createCashback(newPerc))
        }
      } ~
      path("cashback" / "change") {
        (put & entity(as[ChangeCashback])) { newPerc =>
          onComplete(repository.changeCashback(newPerc)) {
            case Success(value) => complete(value)
            case Failure(e: CashbackNonExist) => complete(StatusCodes.NotFound, "Такой вид кэшбека не существует.")
          }
        }
      } ~
      path("cashback" / Segment) { cat =>
        get {
          onComplete(repository.getPercentOfCashback(cat)) {
            case Success(value) => complete(value)
            case Failure(e: CashbackNonExist) => complete(StatusCodes.NotFound, "Такой вид кэшбека не существует.")
          }
        }
      }
}
