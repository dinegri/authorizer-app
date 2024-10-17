package com.caju

import com.caju.authorizer.database.{PersistentAccountRepository, PersistentMccRepository, PersistentTransactionRepository}
import com.caju.authorizer.repository.AccountRepository
import com.caju.authorizer.routes.AuthorizerRoutes
import com.caju.authorizer.service.AuthorizerServiceImpl
import zio.*
import zio.http.*

object MainApp extends ZIOAppDefault:
  def run: ZIO[Any, Throwable, Nothing] =
    Server
      .serve(
        AuthorizerRoutes(),
      )
      .provide(
        Server.defaultWithPort(8080),
        AuthorizerServiceImpl.layer,
        PersistentMccRepository.layer,
        PersistentAccountRepository.layer,
        PersistentTransactionRepository.layer
      )
