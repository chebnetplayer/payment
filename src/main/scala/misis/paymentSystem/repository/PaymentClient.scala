package misis.paymentSystem.repository

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpEntity, HttpMethods, HttpRequest, MediaTypes }
import akka.http.scaladsl.unmarshalling.Unmarshal
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.syntax.EncoderOps
import misis.paymentSystem.model._

import scala.concurrent.{ExecutionContext, Future}

class PaymentClient(implicit val ec: ExecutionContext, actorSystem: ActorSystem) extends FailFastCirceSupport {
  def payment(order: ChangeRequest): Future[Account] = {
    val request = HttpRequest(
      method = HttpMethods.PUT,
      uri = s"http://localhost:8081/account/refill",
      entity = HttpEntity(MediaTypes.`application/json`, order.asJson.noSpaces)
    )
    for {
      response <- Http().singleRequest(request)
      result <- Unmarshal(response).to[Account]
    } yield result

  }

}