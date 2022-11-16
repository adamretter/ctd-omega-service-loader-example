package uk.gov.nationalarchives.omega.servex.api.impl

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import uk.gov.nationalarchives.omega.servex.api.{Service, ServiceBuilder, ServiceBuilderFactory, ServiceError, ServiceRequest, ServiceResponse, ValidationError}

case class EchoRequest(text: String) extends ServiceRequest {
  override def validate(): Option[ValidationError] = {
    if (text.isEmpty) {
      Some(ValidationError("Echo text is empty"))
    } else {
      None
    }
  }
}
case class EchoResponse(text: String) extends ServiceResponse
sealed trait EchoError extends ServiceError
case class EchoFailedDueToBloopError() extends EchoError {
  override val message = "BLOOP!"
}

class EchoServiceBuilderFactory extends ServiceBuilderFactory[EchoService, EchoRequest, EchoResponse, EchoError] {
  override protected val id = "OSECHO001"
  def newInstance(): EchoServiceBuilder = new EchoServiceBuilder()
}

class EchoServiceBuilder extends ServiceBuilder[EchoService, EchoRequest, EchoResponse, EchoError] {
  protected override val serviceRequestDecoder = deriveDecoder[EchoRequest]
  protected override val serviceResponseEncoder = deriveEncoder[EchoResponse]
  override def newService(): EchoService = new EchoService()
}

class EchoService extends Service[EchoRequest, EchoResponse, EchoError] {
  override def process(request: EchoRequest): Either[EchoError, EchoResponse] = {
    if (request.text.startsWith("Bloop: ")) {
      Left(EchoFailedDueToBloopError())
    } else {
      Right(EchoResponse(request.text))
    }
  }
}
