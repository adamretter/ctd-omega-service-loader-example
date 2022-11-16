package uk.gov.nationalarchives.omega.servex

import uk.gov.nationalarchives.omega.servex.api.{Service, ServiceBuilder, ServiceBuilderFactory, ServiceError, ServiceRequest, ServiceResponse}

import java.util.{ServiceLoader => JServiceLoader}
import scala.jdk.CollectionConverters._

object ServiceLoader {

  /**
   * Load an implementation of [[ServiceBuilderFactory]].
   *
   * @param serviceId the id of the Service's service builder to load.
   *
   * @return the [[ServiceBuilder]] of the available [[ServiceBuilderFactory]] implementations
   *         that implements the service identified by [[serviceId]].
   */
  def loadServiceBuilder(serviceId: String): Option[ServiceBuilder[Service[ServiceRequest, ServiceResponse, ServiceError], ServiceRequest, ServiceResponse, ServiceError]] = {
    val serviceLoader = JServiceLoader.load(classOf[ServiceBuilderFactory[Service[ServiceRequest, ServiceResponse, ServiceError], ServiceRequest, ServiceResponse, ServiceError]])
    serviceLoader.asScala.toList
      .map(_.getServiceBuilder(serviceId))
      .filter(_.nonEmpty)
      .flatten
      .headOption
  }
}
