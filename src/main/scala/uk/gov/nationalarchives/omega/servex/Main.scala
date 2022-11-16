package uk.gov.nationalarchives.omega.servex

import uk.gov.nationalarchives.omega.servex.api.NoSuchServiceError

import java.util.UUID

object Main extends App {

  val echoServiceId = "OSECHO001"

  val goodRequestData =
    s"""{
       |"text": "${UUID.randomUUID()}"
       |}""".stripMargin

  val badRequestData =
    s"""{
      |"text": "Bloop: ${UUID.randomUUID()}"
      |}""".stripMargin

  val requestData = goodRequestData

  // NOTE(AR) one possible formulation
  (for {
    serviceBuilder <- ServiceLoader.loadServiceBuilder(echoServiceId).toRight(NoSuchServiceError(s"No Such Parser for ${echoServiceId}"))
    deserializedRequest <- serviceBuilder.parseRequest(requestData)
    serviceRequest <- deserializedRequest.validate()
    serviceResponse <- serviceBuilder.newService().process(serviceRequest)
    serializedResponse <- serviceBuilder.serializeResponse(serviceResponse)
  } yield serializedResponse) match {

    case Left(serviceError) =>
      sys.error(s"Error== ${serviceError}")
      sys.exit(2)

    case Right(responseData) =>
      println(s"Result== ${responseData}")
  }


// TODO(AR) alternative formulation
//  ServiceLoader.loadServiceBuilder(echoServiceId) match {
//    case Some(serviceBuilder) =>
//      val deserializedRequest = serviceBuilder.parseRequest(requestData)
//      val serviceRequest = deserializedRequest.flatMap(_.validate())
//      val service = serviceBuilder.newService()
//      val serviceResponse = serviceRequest.flatMap(service.process(_))
//      val serializedResponse = serviceResponse.flatMap(serviceBuilder.serializeResponse(_))
//
//      serializedResponse match {
//        case Left(error) =>
//          sys.error(s"Error== ${error}")
//          sys.exit(2)
//
//        case Right(responseData) =>
//          println(s"Result== ${responseData}")
//      }
//
//    case None =>
//      sys.error(s"Could not find service: $echoServiceId" )
//      sys.exit(1)
//  }

}
