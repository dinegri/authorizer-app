package com.caju.authorizer

import com.caju.authorizer.database.{PersistentAccountRepository, PersistentMccRepository, PersistentTransactionRepository}
import com.caju.authorizer.repository.Transaction
import com.caju.authorizer.routes.AuthorizerRoutes
import com.caju.authorizer.service.{AuthorizerService, AuthorizerServiceImpl}
import zio.*
import zio.http.*
import zio.http.netty.NettyConfig
import zio.http.netty.server.NettyDriver
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import zio.test.*

object AuthorizerRouteSpec extends ZIOSpecDefault:

	override def spec: Spec[Any, Any] = suite("AuthorizerService")(
		test("Se o mcc for '5411', deve-se utilizar o saldo de FOOD") {
			for {
				client <- ZIO.service[Client]
				_ <- TestServer.addRoutes(AuthorizerRoutes())
				port <- ZIO.serviceWith[Server](_.port)
				url = URL.root.port(port)
				transaction = Transaction("1", 100, "5411", "PADARIA DO ZE               SAO PAULO BR")
				createResponse <- client(
					Request.post(url / "transactions", Body.from[Transaction](transaction))
				)
				result <- createResponse.body.asString
			} yield assertTrue(result == "{\"code\":\"00\"}")
		}.provideSome[Client with Driver with AuthorizerService](
			TestServer.layer,
			Scope.default,
			AuthorizerServiceImpl.layer,
			PersistentMccRepository.layer,
			PersistentAccountRepository.layer,
			PersistentTransactionRepository.layer
		),
		test("Se o mcc for '5811', deve-se utilizar o saldo de MEAL") {
			for {
				client <- ZIO.service[Client]
				_ <- TestServer.addRoutes(AuthorizerRoutes())
				port <- ZIO.serviceWith[Server](_.port)
				url = URL.root.port(port)
				transaction = Transaction("1", 10, "5811", "PADARIA DO ZE               SAO PAULO BR")
				createResponse <- client(
					Request.post(url / "transactions", Body.from[Transaction](transaction))
				)
				result <- createResponse.body.asString
			} yield assertTrue(result == "{\"code\":\"51\"}")
		}.provideSome[Client with Driver with AuthorizerService](
			TestServer.layer,
			Scope.default,
			AuthorizerServiceImpl.layer,
			PersistentAccountRepository.layer,
			PersistentMccRepository.layer,
			PersistentTransactionRepository.layer
		),
		test("Para quaisquer outros valores do mcc, deve-se utilizar o saldo de CASH") {
			for {
				client <- ZIO.service[Client]
				_ <- TestServer.addRoutes(AuthorizerRoutes())
				port <- ZIO.serviceWith[Server](_.port)
				url = URL.root.port(port)
				transaction = Transaction("-1", 10, "5811", "UBER TRIP                   SAO PAULO BR ")
				createResponse <- client(
					Request.post(url / "transactions", Body.from[Transaction](transaction))
				)
				result <- createResponse.body.asString
			} yield assertTrue(result == "{\"code\":\"51\"}")
		}.provideSome[Client with Driver with AuthorizerService](
			TestServer.layer,
			Scope.default,
			AuthorizerServiceImpl.layer,
			PersistentAccountRepository.layer,
			PersistentMccRepository.layer,
			PersistentTransactionRepository.layer
		)
	).provide(
		ZLayer.succeed(Server.Config.default.onAnyOpenPort),
		Client.default,
		NettyDriver.customized,
		ZLayer.succeed(NettyConfig.defaultWithFastShutdown),
		AuthorizerServiceImpl.layer,
		PersistentMccRepository.layer,
		PersistentAccountRepository.layer,
		PersistentTransactionRepository.layer
	)

	override def aspects: Chunk[TestAspectPoly] =
		Chunk(TestAspect.timeout(60.seconds), TestAspect.timed)
