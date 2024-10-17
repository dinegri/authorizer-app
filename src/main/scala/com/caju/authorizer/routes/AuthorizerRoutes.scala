package com.caju.authorizer.routes

import com.caju.authorizer.repository.Transaction
import com.caju.authorizer.routes.AuthorizationCode.encoder
import com.caju.authorizer.service.AuthorizationStatus.*
import com.caju.authorizer.service.{AuthorizationCode, AuthorizerService}
import zio.*
import zio.http.*
import zio.json.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

object AuthorizationCode {
	implicit val encoder: JsonEncoder[AuthorizationCode] =
		DeriveJsonEncoder.gen[AuthorizationCode]
}

object AuthorizerRoutes:

  def apply(): Routes[AuthorizerService, Response] =
    Routes(
      Method.POST / "transactions" -> handler { (req: Request) =>
        for {
          u <- req.body.to[Transaction].orElseFail(Response.badRequest)
          r <-
            AuthorizerService
              .authorize(u)
              .mapBoth(
								_ => Response.json(new AuthorizationCode(Unexpected.toString).toJson),
								id => Response.json(id.toJson)
							)
        } yield r
      }
    )
