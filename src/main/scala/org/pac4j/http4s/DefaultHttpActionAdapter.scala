package org.pac4j.http4s

import cats.effect.Sync
import org.http4s.{MediaType, Status}
import org.http4s.Charset.`UTF-8`
import org.http4s.headers.`Content-Type`
import org.pac4j.core.context.{HttpConstants, WebContext}
import org.pac4j.core.exception.http.{FoundAction, HttpAction, OkAction, SeeOtherAction}
import org.pac4j.core.http.adapter.HttpActionAdapter

/**
  * DefaultHttpActionAdapter sets the correct status codes on the response.
  *
  * @author Iain Cardnell
  */
class DefaultHttpActionAdapter[F[_] <: AnyRef : Sync] extends HttpActionAdapter {
  override def adapt(action: HttpAction, context: WebContext): AnyRef =
    Sync[F].delay {
      val hContext = context.asInstanceOf[Http4sWebContext[F]]
      action match {
        case fa: FoundAction =>
          hContext.setResponseStatus(Status.Found.code)
          hContext.setResponseHeader("Location", fa.getLocation)
        case sa: SeeOtherAction =>
          hContext.setResponseStatus(Status.Found.code)
          hContext.setResponseHeader("Location", sa.getLocation)
        case a => a.getCode match {
          case HttpConstants.UNAUTHORIZED => hContext.setResponseStatus(Status.Unauthorized.code)
          case HttpConstants.FORBIDDEN => hContext.setResponseStatus(Status.Forbidden.code)
          case HttpConstants.OK =>
            val okAction = a.asInstanceOf[OkAction]
            hContext.setContent(okAction.getContent)
            hContext.setContentType(`Content-Type`(MediaType.text.html, `UTF-8`))
            hContext.setResponseStatus(Status.Ok.code)
          case HttpConstants.NO_CONTENT => hContext.setResponseStatus(Status.NoContent.code)
        }
      }
      hContext.getResponse
    }
}
