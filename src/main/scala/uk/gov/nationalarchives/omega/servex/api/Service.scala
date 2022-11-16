package uk.gov.nationalarchives.omega.servex.api

import io.circe.{Decoder, Encoder, ParsingFailure}
import io.circe.syntax._
import io.circe.parser._

/**
 * Intermediary object that forces the caller of [[ServiceBuilder]]
 * to validate there parsed data before calling the service.
 */
class ParsedRequest[T <: ServiceRequest](serviceRequest: T) {
  type ValidationError = ServiceError
  type ValidationResult[A] = Either[ValidationError, A]

  /**
   * Validate a parsed response.
   *
   * @return a Validation Error or the valid ServiceRequest.
   */
  def validate(): ValidationResult[T] = {
    serviceRequest.validate().toLeft(serviceRequest)
  }
}

/**
 * A request to a Service, i.e. its input.
 */
trait ServiceRequest {
  def validate(): Option[ValidationError]
}

/**
 * A response from a Service, i.e. its output.
 */
trait ServiceResponse {
  // TODO(AR) do we want validation on the response? it could be useful as a sanity check to make sure the Service has populated the ServiceResponse correctly
}

/**
 * An error produced by a Service.
 */
trait ServiceError {
  def message: String
}

case class NoSuchServiceError(message: String) extends ServiceError
case class ValidationError(message: String) extends ServiceError

/**
 * The interface for a Service.
 */
trait Service[T <: ServiceRequest, U <: ServiceResponse, E <: ServiceError] {

  /**
   * The entry point for a Service to process a ServiceRequest,
   * and return a ServiceError or ServiceResponse
   *
   * @param request the request to the service
   *
   * @return the ServiceError or ServiceResponse
   */
  def process(request: T): Either[E, U]
}

/**
 * A utility interface which allows you to parse a ServiceRequest, obtain a Service, and serialise a ServiceResponse.
 */
trait ServiceBuilder[S <: Service[T, U, E], T <: ServiceRequest, U <: ServiceResponse, E <: ServiceError] {

  type RequestData = String
  type ResponseData = String
  case class ServiceRequestParserError(message: String, underlying: Throwable) extends ServiceError
  case class ServiceResponseSerialiserError(message: String, underlying: Throwable) extends ServiceError

  protected implicit val serviceRequestDecoder: Decoder[T]
  protected implicit val serviceResponseEncoder: Encoder[U]

  /**
   * Parse raw request data into a ServiceRequest type.
   * NOTE: This does not validate the ServiceRequest; for that call [[ParsedRequest#validate()]] or [[ServiceRequest#validate()]].
   *
   * @param requestData the raw request data
   *
   * @return the parse errors or the parsed request.
   */
  def parseRequest(requestData: RequestData): Either[ServiceRequestParserError, ParsedRequest[T]] = {
    parse(requestData) match {
      case Left(ParsingFailure(message, underlying)) =>
        Left(ServiceRequestParserError(message, underlying))
      case Right(json) =>
        json.as[T] match {
          case Left(decodingFailure) => Left(ServiceRequestParserError(decodingFailure.message, decodingFailure))
          case Right(echoRequest) => Right(new ParsedRequest(echoRequest))
        }
    }
  }

  /**
   * Serialises a ServiceResponse to a raw response.
   *
   * @param response the ServiceResponse to serialise.
   *
   * @return the serialisation errors or the serialised response.
   */
  def serializeResponse(response: U): Either[ServiceResponseSerialiserError, ResponseData] = {
    Right(response.asJson.toString)
  }

  /**
   * Create a new instance of the Service.
   *
   * If the service is immutable, i.e. does not contain mutable state member variables
   * then you may choose to return the same instance on each call, otherwise a new instance must
   * be returned each time.
   *
   * @return an instance of the Service.
   */
  def newService(): S
}

/**
 * The factory interface for creating a ServiceBuilder.
 */
trait ServiceBuilderFactory[S <: Service[T, U, E], T <: ServiceRequest, U <: ServiceResponse, E <: ServiceError] {
  type ServiceId = String
  protected val id: ServiceId

  /**
   * Create a new ServiceBuilder instance.
   *
   * If the ServiceBuilder is immutable, i.e. does not contain mutable state member variables
   * then you may choose to return the same instance on each call, otherwise a new instance must
   * be returned each time.
   *
   * @return the service builder.
   */
  protected def newInstance(): ServiceBuilder[S, T, U, E]

  /**
   * Get a Service Builder for a specific service.
   *
   * @param serviceId the identifier of the service to obtain a service builder for
   *
   * @return the service builder if available, else None.
   */
  def getServiceBuilder(serviceId: ServiceId): Option[ServiceBuilder[S, T, U, E]] = {
    if (id.equals(serviceId)) {
      Some(newInstance())
    } else {
      None
    }
  }
}
